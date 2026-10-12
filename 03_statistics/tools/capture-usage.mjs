// Status line command: keeps the harness's own usage numbers for the post-run report, so nobody has to copy them from the usage panel.
// Claude Code passes a JSON document on stdin after every response; this script stores the latest values of every session in
// 03_statistics/usage-snapshot.json (cost, API time, wall time, plan limits) and prints a short status line. It never fails the status line.
// Configured in .claude/settings.json ("statusLine"). MASTER COPY: 00_Analysis_Tool; the template holds an identical copy (sync-to-template.mjs).
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const here = path.dirname(fileURLToPath(import.meta.url));
const statsDir = path.resolve(here, "..");
const file = path.join(statsDir, "usage-snapshot.json");
let raw = "";
try { raw = fs.readFileSync(0, "utf8"); } catch { /* no stdin */ }
let d = {};
try { d = JSON.parse(raw); } catch { /* keep going with an empty document */ }

const n = (v) => (typeof v === "number" && Number.isFinite(v) ? v : null);
try {
  if (d && d.session_id && fs.existsSync(statsDir)) {
    let snap = { sessions: {} };
    try { snap = JSON.parse(fs.readFileSync(file, "utf8")); snap.sessions ||= {}; } catch { /* first write */ }
    const prev = snap.sessions[d.session_id] || {};
    const cost = d.cost || {}, rl = d.rate_limits || {};
    // totals only ever grow inside one session (a /clear resets them in the harness), so the larger value is kept
    const keepMax = (a, b) => (a == null ? b : b == null ? a : Math.max(a, b));
    snap.sessions[d.session_id] = {
      costUsd: keepMax(prev.costUsd, n(cost.total_cost_usd)),
      apiMs: keepMax(prev.apiMs, n(cost.total_api_duration_ms)),
      wallMs: keepMax(prev.wallMs, n(cost.total_duration_ms)),
      fiveHourPct: n(rl.five_hour?.used_percentage) ?? prev.fiveHourPct ?? null,
      sevenDayPct: n(rl.seven_day?.used_percentage) ?? prev.sevenDayPct ?? null,
      fiveHourResetsAt: n(rl.five_hour?.resets_at) ?? prev.fiveHourResetsAt ?? null,
      sevenDayResetsAt: n(rl.seven_day?.resets_at) ?? prev.sevenDayResetsAt ?? null,
      model: d.model?.id || d.model?.display_name || prev.model || null,
      contextPct: n(d.context_window?.used_percentage) ?? prev.contextPct ?? null,
      updatedAt: new Date().toISOString(),
    };
    const all = Object.values(snap.sessions);
    const sum = (k) => all.reduce((s, x) => s + (x[k] || 0), 0);
    const latest = all.reduce((a, b) => (!a || b.updatedAt > a.updatedAt ? b : a), null);
    snap.totals = { costUsd: Math.round(sum("costUsd") * 100) / 100, apiMs: sum("apiMs"), wallMs: sum("wallMs"), fiveHourPct: latest?.fiveHourPct ?? null, sevenDayPct: latest?.sevenDayPct ?? null, sessions: all.length };
    snap.note = "Written by tools/capture-usage.mjs from the status line JSON; read by build-report.mjs --run to fill usage.md.";
    fs.writeFileSync(file, JSON.stringify(snap, null, 2) + "\n");
  }
} catch { /* never break the status line */ }

const c = n(d.cost?.total_cost_usd), pct = n(d.context_window?.used_percentage);
process.stdout.write(`${d.model?.display_name || "claude"} · ${c == null ? "$–" : "$" + c.toFixed(2)}${pct == null ? "" : " · " + Math.round(pct) + "% context"}`);
