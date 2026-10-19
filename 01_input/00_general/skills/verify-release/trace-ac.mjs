// Prints the traceability table of phase 6: acceptance criterion -> tests -> commits. Run it once from the repository root:
//   node 01_input/00_general/skills/verify-release/trace-ac.mjs [path/to/acceptance-criteria.md]
// A test counts when its file names the AC id or a range that contains it (AC-001-02..06). A commit counts when its subject names the AC, a range that contains it
// (AC-003-01..03), or the user story of the AC (US-003 for AC-003-xx). TDD order: the first `test:` and the first `feat:` commit whose subject names
// the AC or a range that contains it; "ok" when the test commit comes first. Read-only: it writes nothing but the table on stdout.
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";

const acFile = process.argv[2] || "01_input/01_project/01_requirements/acceptance-criteria.md";
if (!fs.existsSync(acFile)) { console.error("no acceptance criteria at " + acFile); process.exit(1); }
const acs = [...new Set(fs.readFileSync(acFile, "utf8").match(/\bAC-\d{3}-\d{2}\b/g) || [])];

// "AC-001-02..06", "AC-001-01 … AC-001-07", "AC-001-01 to 07": a range names every AC in it
const RANGE = /\bAC-(\d{3})-(\d{2})\s*(?:\.\.\.?|…|–|—|to)\s*(?:AC-\1-)?(\d{2})\b/g;
const git = (args) => spawnSync("git", args, { encoding: "utf8", maxBuffer: 64 * 1024 * 1024 });
const isTest = (p) => /(^|\/)(src\/test|tests?|__tests__|e2e|acceptance)(\/|$)|\.(test|spec)\.[a-z]+$/i.test(p);
const files = git(["ls-files", "-co", "--exclude-standard"]).stdout.split("\n").filter((p) => p && isTest(p) && !/node_modules|\/target\/|\/dist\//.test(p));
const tests = new Map(acs.map((a) => [a, []]));
for (const f of files) {
  let text; try { text = fs.readFileSync(f, "utf8"); } catch { continue; }
  const found = text.match(/\bAC-\d{3}-\d{2}\b/g) || [];
  for (const m of text.matchAll(RANGE)) for (let n = +m[2]; n <= +m[3]; n++) found.push("AC-" + m[1] + "-" + String(n).padStart(2, "0")); // a range names every AC in it
  if (!found.length) continue;
  for (const a of new Set(found)) if (tests.has(a)) tests.get(a).push(path.basename(f) + (found.filter((x) => x === a).length > 1 ? " (" + found.filter((x) => x === a).length + ")" : ""));
}

const commits = git(["log", "--format=%h%x09%s"]).stdout.split("\n").filter(Boolean).map((l) => { const i = l.indexOf("\t"); return { h: l.slice(0, i), s: l.slice(i + 1) }; });
const byAc = new Map(acs.map((a) => [a, []]));
const firstOf = { test: new Map(), feat: new Map() }; // git log lists the newest commit first, so the last write is the oldest commit
for (const c of commits) {
  const hit = new Set(c.s.match(/\bAC-\d{3}-\d{2}\b/g) || []);
  for (const m of c.s.matchAll(RANGE)) for (let n = +m[2]; n <= +m[3]; n++) hit.add("AC-" + m[1] + "-" + String(n).padStart(2, "0"));
  const type = (c.s.match(/^(test|feat)\b/) || [])[1];
  if (type) for (const a of hit) firstOf[type].set(a, c);
  for (const m of c.s.matchAll(/\bUS-(\d{3})\b/g)) for (const a of acs) if (a.startsWith("AC-" + m[1] + "-")) hit.add(a);
  for (const a of hit) if (byAc.has(a)) byAc.get(a).push(c.h);
}

const pos = new Map(commits.map((c, i) => [c.h, i])); // a larger index is an older commit
const order = (a) => {
  const t = firstOf.test.get(a), f = firstOf.feat.get(a);
  if (!f) return t ? "no feat" : "**none**";
  if (!t) return "**feat without test**";
  return pos.get(t.h) > pos.get(f.h) ? "ok" : "**feat first**";
};
console.log("| AC | Tests | Commits | First test commit | First feat commit | TDD order |\n|---|---|---|---|---|---|");
for (const a of acs) {
  const t = tests.get(a), c = byAc.get(a);
  console.log("| " + a + " | " + (t.length ? t.join(", ") : "**none**") + " | " + (c.length ? c.slice(-6).reverse().join(", ") + (c.length > 6 ? " (+" + (c.length - 6) + ")" : "") : "**none**") + " | " + (firstOf.test.get(a)?.h || "–") + " | " + (firstOf.feat.get(a)?.h || "–") + " | " + order(a) + " |");
}
const noTest = acs.filter((a) => !tests.get(a).length), noCommit = acs.filter((a) => !byAc.get(a).length);
const notFirst = acs.filter((a) => !["ok", "no feat", "**none**"].includes(order(a)));
console.log("\n" + acs.length + " acceptance criteria; without a test: " + (noTest.join(", ") || "none") + "; without a commit: " + (noCommit.join(", ") || "none") + "; not test-first: " + (notFirst.join(", ") || "none") + ".");
