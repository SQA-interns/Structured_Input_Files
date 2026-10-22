// Lints a change request folder (04_changes/CR-nnn_slug): is it complete, consistent with the baseline and consistent with itself.
// Read-only: writes nothing but the report on stdout. Checks form and consistency, not whether the requirements are good.
//
//   node 01_input/00_general/skills/change-interview/lint-change.mjs <cr-folder> [--signed] [--example]
//
//   --signed   also require a UTC "Approved" time and an input.sha256 that matches the files (build session start, phase 6)
//   --example  do not require the baseline commit to exist in git (the shipped example)
//
// Exit 0: no errors (warnings are listed). Exit 1: at least one error. Rules: general/skills/change-interview/SKILL.md and templates/.
import fs from "node:fs";
import path from "node:path";
import crypto from "node:crypto";
import { fileURLToPath } from "node:url";
import { spawnSync } from "node:child_process";

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, "..", "..", "..", "..");
const args = process.argv.slice(2);
const flags = new Set(args.filter((a) => a.startsWith("--")));
const dirArg = args.find((a) => !a.startsWith("--"));
if (!dirArg) { console.error("usage: lint-change.mjs <cr-folder> [--signed] [--example]"); process.exit(2); }
const dir = path.resolve(dirArg);
if (!fs.existsSync(dir) || !fs.statSync(dir).isDirectory()) { console.error("lint-change: not a folder: " + dirArg); process.exit(2); }

const out = [];
const err = (f, l, m) => out.push({ lvl: "ERROR", f, l, m });
const warn = (f, l, m) => out.push({ lvl: "warning", f, l, m });

const ID_SRC = "US-\\d{3}|AC-\\d{3}-\\d{2}|E2E-\\d{2}|DoD-P\\d{2}|DoD-\\d{2}|BR-\\d{2}|OQ-\\d{2}|AR-\\d{2}|SR-\\d{2}|NFR-\\d{2}|SB-\\d{2}|ES-\\d{2}";
const ID_RE = new RegExp("\\b(?:" + ID_SRC + ")\\b", "g"), ONE = new RegExp("^(?:" + ID_SRC + ")$");
const ADDED = "ADDED", MOD = "MODIFIED", REM = "REMOVED", CHANGES = [ADDED, MOD, REM];
const read = (p) => fs.readFileSync(p, "utf8").replace(/^\uFEFF/, "").replace(/\r\n/g, "\n");
const blank = (s) => s.replace(/[^\n]/g, " ");
const noComments = (s) => s.replace(/<!--[\s\S]*?-->/g, blank);   // keeps line numbers
const cells = (s) => s.trim().replace(/^\|/, "").replace(/\|$/, "").split("|").map((c) => c.trim());
const clean = (s) => s.replace(/[*`]/g, "").trim();
const lineOf = (text, idx) => text.slice(0, idx).split("\n").length;
const num = (id) => Number(id.match(/(\d+)$/)[1]);
const fam = (id) => (id.startsWith("AC-") ? id.slice(0, 6) : id.replace(/-?\d+$/, ""));   // AC-003 per story, else US, BR, E2E, DoD-P …
const idsIn = (s) => new Set(expand(s));

function expand(text) {   // ids and ranges: "AC-001-01..07", "E2E-01..03", "US-001..003"
  const ids = new Set(text.match(ID_RE) || []);
  const pad = (n, w) => String(n).padStart(w, "0");
  for (const m of text.matchAll(/\bAC-(\d{3})-(\d{2})\s*\.\.\s*(\d{2})\b/g)) for (let n = +m[2]; n <= +m[3]; n++) ids.add(`AC-${m[1]}-${pad(n, 2)}`);
  for (const m of text.matchAll(/\b(E2E|BR|OQ|AR|SR|NFR|SB|ES|DoD-P|DoD)-(\d{2})\s*\.\.\s*(\d{2})\b/g)) for (let n = +m[2]; n <= +m[3]; n++) ids.add(`${m[1]}-${pad(n, 2)}`);
  for (const m of text.matchAll(/\bUS-(\d{3})\s*\.\.\s*(\d{3})\b/g)) for (let n = +m[1]; n <= +m[2]; n++) ids.add(`US-${pad(n, 3)}`);
  return [...ids];
}

function sections(txt) {   // [{name, start (1-based heading line), lines: [{n, t}]}] split on "## "
  const lines = txt.split("\n"), res = [];
  let cur = { name: "", start: 1, lines: [] };
  lines.forEach((t, i) => {
    const m = t.match(/^##\s+(.+?)\s*$/);
    if (m) { res.push(cur); cur = { name: m[1], start: i + 1, lines: [] }; } else cur.lines.push({ n: i + 1, t });
  });
  res.push(cur);
  return res;
}

function tablesOf(sec) {   // pipe tables of a section: [{head: [..], rows: [{c: [..], n}]}]
  const res = []; let cur = null;
  for (const { n, t } of sec.lines) {
    if (/^\s*\|/.test(t)) {
      const c = cells(t);
      if (!cur) cur = { head: c, rows: [], n };
      else if (!c.every((x) => /^:?-{2,}:?$/.test(x))) cur.rows.push({ c, n });
    } else if (cur) { res.push(cur); cur = null; }
  }
  if (cur) res.push(cur);
  return res;
}

function definedIn(txt, into) {   // ids a baseline or CR file defines: first cell of a table row, or a heading
  for (const t of txt.split("\n")) {
    if (/^\s*\|/.test(t)) { const id = clean(cells(t)[0] || ""); if (ONE.test(id)) into.add(id); }
    const h = t.match(/^#{2,4}\s+((?:US)-\d{3})\b/); if (h) into.add(h[1]);
  }
}

function walk(d, skip, acc = []) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name);
    if (e.isDirectory()) { if (!skip(p)) walk(p, skip, acc); } else if (e.name.endsWith(".md")) acc.push(p);
  }
  return acc;
}

// ---------- the baseline and earlier CRs: which ids exist
const defined = new Set();
for (const f of walk(path.join(root, "01_input"), (p) => path.basename(p) === "change-interview")) definedIn(read(f), defined);
const changesDir = path.join(root, "04_changes");
if (fs.existsSync(changesDir)) for (const e of fs.readdirSync(changesDir, { withFileTypes: true })) {
  const p = path.join(changesDir, e.name);
  if (e.isDirectory() && /^CR-\d{3}/.test(e.name) && p !== dir) for (const f of ["change.md", "acceptance-criteria.md"]) if (fs.existsSync(path.join(p, f))) definedIn(read(path.join(p, f)), defined);
}

// ---------- files
const ALLOWED = ["change.md", "acceptance-criteria.md", "tech-stack.md", "interview-log.md", "input.sha256"];
const files = fs.readdirSync(dir);
for (const f of ["change.md", "acceptance-criteria.md", "interview-log.md"]) if (!files.includes(f)) err(f, 0, "missing file");
for (const f of files) if (!ALLOWED.includes(f)) err(f, 0, "not part of a change request (allowed: " + ALLOWED.join(", ") + ")");

const raw = {}, txt = {};
for (const f of files.filter((x) => x.endsWith(".md"))) {
  raw[f] = read(path.join(dir, f)); txt[f] = noComments(raw[f]);
  for (const m of raw[f].matchAll(/<!--\s*([\s\S]*?)-->/g)) if (!/^note:/.test(m[1])) err(f, lineOf(raw[f], m.index), "hint comment left; remove it once the field is filled (only <!-- note: --> comments may stay)");
  txt[f].replace(/`[^`\n]*`/g, blank).split("\n").forEach((t, i) => { const m = t.match(/<[A-Za-z][^<>]*>/); if (m) err(f, i + 1, "placeholder left: " + m[0] + " (put literal markup in backticks)"); });
}

const items = [];   // what this CR adds, modifies or removes: {id, change, f, n}
const changedRows = [];   // rows of "Baseline items changed": {id, action, n}
const add = (id, change, f, n) => items.push({ id, change, f, n });
const known = (id) => defined.has(id) || items.some((i) => i.id === id);

// ---------- change.md
const cm = "change.md";
if (txt[cm]) {
  const secs = sections(txt[cm]);
  const head = secs[0], byName = (n) => secs.find((s) => s.name === n);
  const title = txt[cm].split("\n")[0].match(/^# (CR-\d{3}): (.+)$/);
  if (!title) err(cm, 1, "first line must be '# CR-nnn: <title>'");
  else if (!path.basename(dir).startsWith(title[1])) err(cm, 1, `title ${title[1]} does not match the folder name ${path.basename(dir)}`);
  const hl = (k) => { const l = head.lines.find((x) => new RegExp("^- " + k + ":").test(x.t)); return l ? { n: l.n, v: l.t.replace(/^- [^:]+:\s*/, "") } : null; };
  const type = hl("Type"), base = hl("Baseline"), req = hl("Requested by"), sec = hl("Security impact");
  if (!type || !["add", "change", "remove"].includes(type.v.trim())) err(cm, type ? type.n : 1, "Type must be add, change or remove");
  if (!sec || !sec.v.trim()) err(cm, sec ? sec.n : 1, "'- Security impact: none | <what changes, with SR/SB ids>' line missing");
  if (!base) err(cm, 1, "Baseline line missing");
  else {
    const m = base.v.match(/^(\S+) @ ([0-9a-f]{7,40})$/);
    if (!m) err(cm, base.n, "Baseline must read '<branch> @ <commit>'");
    else if (!flags.has("--example") && spawnSync("git", ["cat-file", "-e", m[2] + "^{commit}"], { cwd: root }).status !== 0) err(cm, base.n, "baseline commit " + m[2] + " not found in git");
  }
  if (!req) err(cm, 1, "'Requested by' line missing");
  else {
    const m = req.v.match(/^(.*?)\s*·\s*Approved:\s*(.*)$/);
    if (!m || !m[1].trim()) err(cm, req.n, "'Requested by: <name> · Approved: pending | <UTC>' expected");
    else if (flags.has("--signed") ? !/^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\dZ$/.test(m[2].trim()) : !(m[2].trim() === "pending" || /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\dZ$/.test(m[2].trim()))) err(cm, req.n, flags.has("--signed") ? "Approved must be a UTC time (signed CR)" : "Approved must be 'pending' or a UTC time");
  }

  const KNOWN = ["Intent", "Stories", "Rules and data", "Design and quality", "Baseline items changed", "Must not change", "Open questions"];
  for (const s of secs.slice(1)) if (!KNOWN.includes(s.name)) err(cm, s.start, "unknown section '" + s.name + "'");
  for (const n of ["Intent", "Stories", "Baseline items changed", "Must not change", "Open questions"]) if (!byName(n)) err(cm, 1, "section '" + n + "' missing");
  const body = (s) => s.lines.map((l) => l.t).join("\n").trim();
  const emptyTable = (s) => { for (const t of tablesOf(s)) if (!t.rows.length) err(cm, t.n, "table without rows in '" + s.name + "'; delete the section if unchanged"); };

  const intent = byName("Intent");
  if (intent) { if (!body(intent)) err(cm, intent.start, "Intent is empty"); else if (!/Out of scope:/.test(body(intent))) warn(cm, intent.start, "Intent should end with 'Out of scope: …'"); }

  const stories = byName("Stories");   // new stories: "### US-nnn title" + one sentence + In/Out of scope
  if (stories) {
    const parts = stories.lines.map((l, i) => ({ ...l, i })).filter((l) => /^###\s/.test(l.t));
    if (!parts.length && !/^none$/m.test(body(stories))) err(cm, stories.start, "no story: add '### US-nnn <title>' or write 'none'");
    parts.forEach((p, k) => {
      const m = p.t.match(/^###\s+(US-\d{3})\s+\S/);
      if (!m) { err(cm, p.n, "story heading must be '### US-nnn <title>'"); return; }
      add(m[1], ADDED, cm, p.n);
      const b = stories.lines.slice(p.i + 1, k + 1 < parts.length ? parts[k + 1].i : undefined).map((l) => l.t).join(" ");
      if (!/\bAs an? .+\bI want\b.+\bso that\b/.test(b)) err(cm, p.n, m[1] + ": 'As a … I want … so that …' missing");
      if (!/In scope:/.test(b) || !/Out of scope:/.test(b)) warn(cm, p.n, m[1] + ": 'In scope:' and 'Out of scope:' expected");
    });
  }

  const tableRows = (secName, cols, prefix) => {
    const s = byName(secName); if (!s) return;
    emptyTable(s);
    for (const t of tablesOf(s)) {
      const ix = cols.map((c) => t.head.findIndex((h) => h === c));
      if (ix.some((i) => i < 0)) { err(cm, t.n, `table in '${secName}' needs the columns ${cols.join(", ")}`); continue; }
      for (const r of t.rows) {
        const id = clean(r.c[ix[0]] || ""), ch = (r.c[ix[1]] || "").trim();
        if (!new RegExp("^(?:" + prefix + ")-\\d{2}$").test(id)) { err(cm, r.n, `'${id}' is not a valid id here (${prefix.replace(/\|/g, ", ")})`); continue; }
        if (!CHANGES.includes(ch)) { err(cm, r.n, id + ": Change must be ADDED, MODIFIED or REMOVED"); continue; }
        if (ch !== REM && cols.slice(2).some((_, k) => !(r.c[ix[2 + k]] || "").trim())) err(cm, r.n, id + ": empty cell");
        add(id, ch, cm, r.n);
      }
    }
  };
  tableRows("Rules and data", ["ID", "Change", "Rule"], "BR");
  tableRows("Design and quality", ["ID", "Change", "Requirement", "Checked by"], "AR|SR|NFR|DoD-P");

  const oq = byName("Open questions");
  if (oq) {
    const tabs = tablesOf(oq);
    if (!tabs.length) { if (!/^none$/m.test(body(oq).replace(/^The answers.*$/m, "").trim())) err(cm, oq.start, "open-question table missing (write 'none' if there are no questions)"); }
    for (const t of tabs) {
      if (!t.rows.length) err(cm, t.n, "open-question table without rows (write 'none' instead)");
      for (const r of t.rows) {
        const [id, q, a] = r.c;
        if (!/^OQ-\d{2}$/.test(clean(id || ""))) { err(cm, r.n, `'${id}' is not an OQ id`); continue; }
        if (!(q || "").trim()) err(cm, r.n, id + ": question empty");
        if (!(a || "").trim()) err(cm, r.n, id + ": no answer; every open question is answered before sign-off");
        add(clean(id), ADDED, cm, r.n);
      }
    }
  }

  const bc = byName("Baseline items changed");
  if (bc) {
    const tabs = tablesOf(bc);
    if (!tabs.length) { if (!/^none$/m.test(body(bc))) err(cm, bc.start, "table missing (write 'none' if the CR is purely additive)"); }
    for (const t of tabs) {
      if (!t.rows.length) err(cm, t.n, "table without rows (write 'none' instead)");
      for (const r of t.rows) {
        const [id, action, reason] = r.c.map((x) => (x || "").trim());
        if (!ONE.test(clean(id))) { err(cm, r.n, `'${id}' is not an id`); continue; }
        if (![MOD, REM].includes(action)) err(cm, r.n, id + ": Action must be MODIFIED or REMOVED");
        if (!reason) err(cm, r.n, id + ": reason empty");
        changedRows.push({ id: clean(id), action, n: r.n });
      }
    }
  }

  const mnc = byName("Must not change");
  if (mnc) {
    const guard = expand(body(mnc));
    if (!guard.length) err(cm, mnc.start, "Must not change lists no ids");
    for (const id of guard) {
      if (!defined.has(id)) err(cm, mnc.start, id + ": not an existing baseline id");
      if (changedRows.some((r) => r.id === id)) err(cm, mnc.start, id + ": is in 'Must not change' and in 'Baseline items changed'");
    }
  }
  if (raw[cm].split("\n").length > 80) warn(cm, 0, "change.md is over 80 lines; every sentence must be checkable or not derivable from the baseline");
}

// ---------- acceptance-criteria.md
const ac = "acceptance-criteria.md";
let acRows = 0;
if (txt[ac]) {
  const secs = sections(txt[ac]);
  for (const s of secs.slice(1)) {
    const hs = s.name.match(/^(US-(\d{3}))\b/);
    const isE2E = s.name === "End-to-end scenarios";
    if (!hs && !isE2E) { err(ac, s.start, "unknown section '" + s.name + "'; expected '## US-nnn <title>' or '## End-to-end scenarios'"); continue; }
    const tabs = tablesOf(s);
    if (!tabs.length) err(ac, s.start, "section without a table; delete it");
    for (const t of tabs) {
      if (!t.rows.length) err(ac, t.n, "table without rows; delete the section if there is none");
      const col = (c) => t.head.findIndex((h) => h === c);
      const need = isE2E ? ["ID", "Change", "Scenario", "Steps and observations", "Covers"] : ["ID", "Change", "Criterion", "Refs"];
      if (need.some((c) => col(c) < 0)) { err(ac, t.n, "table needs the columns " + need.join(", ")); continue; }
      for (const r of t.rows) {
        const id = clean(r.c[col("ID")] || ""), ch = (r.c[col("Change")] || "").trim();
        const re = isE2E ? /^E2E-\d{2}$/ : /^AC-\d{3}-\d{2}$/;
        if (!re.test(id)) { err(ac, r.n, `'${id}' is not a valid id here`); continue; }
        if (!CHANGES.includes(ch)) { err(ac, r.n, id + ": Change must be ADDED, MODIFIED or REMOVED"); continue; }
        if (!isE2E) {
          acRows++;
          if (id.slice(3, 6) !== hs[2]) err(ac, r.n, `${id} is under the heading ${hs[1]}`);
          if (ch !== REM) {
            const c = r.c[col("Criterion")] || "", refs = r.c[col("Refs")] || "";
            if (!/^Given .+, when .+, then .+/i.test(c.trim())) err(ac, r.n, id + ": criterion must read 'Given …, when …, then …'");
            if (!(refs.match(ID_RE) || []).length) err(ac, r.n, id + ": Refs must cite at least one id");
          }
          if (ch === ADDED && !defined.has("US-" + id.slice(3, 6)) && !items.some((i) => i.id === "US-" + id.slice(3, 6))) err(ac, r.n, `${id}: story US-${id.slice(3, 6)} is not defined in the baseline or in change.md`);
        } else if (ch !== REM) {
          if (!(r.c[col("Scenario")] || "").trim() || !(r.c[col("Steps and observations")] || "").trim()) err(ac, r.n, id + ": scenario or steps empty");
          if (!(r.c[col("Covers")] || "").match(ID_RE)) err(ac, r.n, id + ": Covers must cite at least one id");
        }
        add(id, ch, ac, r.n);
      }
    }
  }
  if (acRows > 20) warn(ac, 0, acRows + " criteria in one CR; consider splitting it");
}
ID_RE.lastIndex = 0;

// ---------- consistency across the CR and with the baseline
const seen = new Map();
for (const it of items) {
  const where = `${it.f}:${it.n}`;
  if (seen.has(it.id)) { err(it.f, it.n, `${it.id} appears twice in this CR (also ${seen.get(it.id)})`); continue; }
  seen.set(it.id, where);
  if (it.change === ADDED && defined.has(it.id)) err(it.f, it.n, it.id + ": ADDED but already exists in the baseline or an earlier CR");
  if (it.change !== ADDED && !defined.has(it.id)) err(it.f, it.n, `${it.id}: ${it.change} but not an existing id`);
}
const byFam = new Map();   // numbering continues after the highest existing id of the same kind
for (const it of items) if (it.change === ADDED && !defined.has(it.id)) byFam.set(fam(it.id), [...(byFam.get(fam(it.id)) || []), it]);
for (const [f, list] of byFam) {
  const max = Math.max(0, ...[...defined].filter((d) => fam(d) === f).map(num));
  const want = (n) => `${f}-${String(n).padStart(f === "US" ? 3 : 2, "0")}`;
  list.sort((a, b) => num(a.id) - num(b.id)).forEach((it, k) => {
    if (num(it.id) !== max + 1 + k) err(it.f, it.n, `${it.id}: numbering must continue after the highest existing id of its kind; expected ${want(max + 1 + k)}`);
  });
}
const mods = items.filter((i) => i.change !== ADDED);
for (const r of changedRows) {
  const it = items.find((i) => i.id === r.id);
  if (!defined.has(r.id)) err(cm, r.n, r.id + ": not an existing id");
  else if (!it) { if (!(r.action === REM && r.id.startsWith("US-"))) err(cm, r.n, `${r.id}: listed as ${r.action} but this CR has no ${r.action} row for it`); }
  else if (it.change !== r.action) err(cm, r.n, `${r.id}: listed as ${r.action} but its row says ${it.change}`);
}
for (const it of mods) if (!changedRows.some((r) => r.id === it.id)) err(it.f, it.n, `${it.id}: ${it.change} but missing in 'Baseline items changed'`);
// every id cited in a table or in the security line resolves
for (const f of [ac, cm]) if (txt[f]) txt[f].split("\n").forEach((t, i) => { if (/^\s*\|/.test(t) || /^- Security impact:/.test(t)) for (const id of expand(t)) if (!known(id)) err(f, i + 1, id + ": unknown id"); });
for (const it of items) if (it.id.startsWith("US-") && it.change === ADDED && !items.some((a) => a.id.startsWith("AC-" + it.id.slice(3) + "-") && a.change === ADDED)) err(it.f, it.n, it.id + ": a new story needs at least one ADDED criterion");

// ---------- interview log: every open question and changed baseline item has a trail
const il = "interview-log.md";
if (txt[il]) {
  const trail = new Set();
  const tab = sections(txt[il]).flatMap(tablesOf)[0];
  if (!tab || !tab.rows.length) err(il, 1, "no questions logged");
  else {
    const a = tab.head.findIndex((h) => h === "Affects");
    if (a < 0) err(il, tab.n, "column 'Affects' missing");
    else for (const r of tab.rows) for (const id of expand(r.c[a] || "")) trail.add(id);
    for (const it of items) {
      const story = it.id.startsWith("AC-") ? "US-" + it.id.slice(3, 6) : null;
      if (trail.has(it.id) || (story && trail.has(story))) continue;
      if (it.id.startsWith("OQ-") || it.change !== ADDED) err(il, 0, `${it.id}: no row in the log's Affects column (a decision or changed item with no question or lookup behind it)`);
      else warn(il, 0, `${it.id}: not traced in the log's Affects column`);
    }
  }
}

// ---------- signed CR: input.sha256 matches the files
if (flags.has("--signed")) {
  const f = path.join(dir, "input.sha256");
  if (!fs.existsSync(f)) err("input.sha256", 0, "missing; a signed CR has one");
  else {
    const listed = new Map();
    for (const t of read(f).split("\n").filter(Boolean)) { const m = t.match(/^([0-9a-f]{64}) [ *](.+)$/); if (m) listed.set(m[2], m[1]); else err("input.sha256", 0, "not in sha256sum format: " + t.slice(0, 60)); }
    const expected = ["change.md", "acceptance-criteria.md", "tech-stack.md"].filter((x) => files.includes(x)).map((x) => path.relative(root, path.join(dir, x)).split(path.sep).join("/"));
    for (const e of expected) {
      if (!listed.has(e)) err("input.sha256", 0, e + ": not listed");
      else if (crypto.createHash("sha256").update(read(path.join(root, e))).digest("hex") !== listed.get(e)) err("input.sha256", 0, e + ": hash differs; the CR changed after sign-off");
    }
    for (const k of listed.keys()) if (!expected.includes(k)) err("input.sha256", 0, k + ": listed but not an input file of this CR");
  }
}

// ---------- report
const errors = out.filter((o) => o.lvl === "ERROR"), warns = out.filter((o) => o.lvl !== "ERROR");
for (const o of [...errors, ...warns]) console.log(`${o.lvl.padEnd(7)} ${o.f}${o.l ? ":" + o.l : ""}  ${o.m}`);
console.log(`${path.basename(dir)}: ${errors.length} error(s), ${warns.length} warning(s)${flags.has("--signed") ? " (signed)" : ""}`);
process.exit(errors.length ? 1 : 0);
