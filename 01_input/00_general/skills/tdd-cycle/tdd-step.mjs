// One TDD cycle step in one call: checks the commit subject, runs the cycle's tests, writes the log, checks the outcome the step needs and
// commits. Run it from the repository root in phase 4, once per red, green or refactor step:
//   node 01_input/00_general/skills/tdd-cycle/tdd-step.mjs <red|green|refactor> <cycle> "<subject>" [--body "<text>"] [--oversize "<reason>"] -- <test command>
// red       subject `test: …`; the change adds or edits a test file; the tests must fail on an assertion or a stub that reports "not implemented",
//           never on a compile, build or harness error
// green     subject `feat: …`; no test file changes; the tests must pass
// refactor  subject `refactor: …`; no test file changes; the tests must pass
// The test command runs in bash from the repository root (do not pipe it; the script captures the output). The full output goes to
// 02_output/logs/tdd/<cycle>-<step>.log; everything changed except 03_statistics/ is committed with the subject (and body). A commit over 15
// files or 400 lines (logs and lock files not counted) is refused unless --oversize gives the reason, which is added to the body.
// Exit 0 = committed; 1 = refused, nothing committed (the reason is printed); 2 = usage error. Prints at most a few lines.
import fs from "node:fs";
import path from "node:path";
import { spawnSync } from "node:child_process";

const STEPS = { red: "test", green: "feat", refactor: "refactor" };
const die = (code, msg) => { console.log("tdd-step: " + msg); process.exit(code); };
const argv = process.argv.slice(2), sep = argv.indexOf("--");
if (sep < 0) die(2, 'usage: tdd-step.mjs <red|green|refactor> <cycle> "<subject>" [--body "<text>"] [--oversize "<reason>"] -- <test command>');
const head = argv.slice(0, sep), testCmd = argv.slice(sep + 1).join(" ").trim();
const flag = (n) => { const i = head.indexOf(n); if (i < 0) return null; const v = head[i + 1]; head.splice(i, 2); return v ?? ""; };
const body = flag("--body"), oversize = flag("--oversize");
const [step, cycle, subject] = head;
if (!STEPS[step] || !cycle || !subject || !testCmd) die(2, "usage: tdd-step.mjs <red|green|refactor> <cycle> \"<subject>\" [--body …] [--oversize …] -- <test command>");
if (!/^[A-Za-z0-9._-]+$/.test(cycle)) die(2, "cycle id may contain only letters, digits, '.', '_' and '-' (for example C07b)");

const git = (args, opt = {}) => spawnSync("git", args, { encoding: "utf8", maxBuffer: 64 * 1024 * 1024, ...opt });
const top = git(["rev-parse", "--show-toplevel"]);
if (top.status !== 0) die(2, "not inside a git repository");
const root = top.stdout.trim();

// 1. the subject, before anything runs: `<type>: <summary> (IDs)`, at most 72 characters, naming the cycle's AC or AC range
const type = STEPS[step];
if (!new RegExp("^" + type + ": \\S.*\\(.*\\bAC-\\d{3}-\\d{2}.*\\)$").test(subject)) die(1, `refused: the ${step} subject must be "${type}: <summary> (<AC or AC range>)", got "${subject}"`);
if (subject.length > 72) die(1, `refused: subject has ${subject.length} characters, at most 72; shorten the summary and move details to --body`);

// 2. run the tests in bash (Git Bash on Windows, never the WSL launcher) and keep the whole output in the log
function bash() {
  if (process.env.TDD_STEP_SHELL) return process.env.TDD_STEP_SHELL;
  if (process.platform !== "win32") return "bash";
  const exec = git(["--exec-path"]).stdout.trim(); // <Git>/mingw64/libexec/git-core
  const candidate = path.resolve(exec, "..", "..", "..", "bin", "bash.exe");
  return fs.existsSync(candidate) ? candidate : "bash";
}
const started = Date.now();
const run = spawnSync(bash(), ["-c", testCmd], { cwd: root, encoding: "utf8", maxBuffer: 256 * 1024 * 1024, timeout: 30 * 60 * 1000, windowsHide: true });
const output = ((run.stdout || "") + (run.stderr || "")).replace(/\u001b\[[0-9;]*[A-Za-z]/g, "");
const logRel = path.posix.join("02_output", "logs", "tdd", `${cycle}-${step}.log`);
fs.mkdirSync(path.join(root, "02_output", "logs", "tdd"), { recursive: true });
fs.writeFileSync(path.join(root, logRel), `$ ${testCmd}\n# step ${step}, cycle ${cycle}, exit ${run.status}, ${Math.round((Date.now() - started) / 1000)} s, ${new Date().toISOString()}\n\n${output}`);
if (run.error) die(1, `refused: the test command did not run (${run.error.code || run.error.message}); log ${logRel}`);

// 3. read the outcome: Maven Surefire, JUnit, Vitest and Playwright summaries
const HARNESS = /COMPILATION ERROR|cannot find symbol|error: package \S+ does not exist|\berror TS\d{4}\b|Failed to resolve import|Cannot find module|Transform failed|Failed to load url|SyntaxError:|No tests found|No test files found|Tests run: 0, Failures: 0, Errors: 0|Failed to load ApplicationContext|Could not find a valid Docker environment|BUILD FAILURE[\s\S]*Could not resolve dependencies/i;
const FAILED = /Tests run: \d+, Failures: [1-9]|Tests run: \d+, Failures: \d+, Errors: [1-9]|There are test failures|Tests\s+\d+ failed|^\s*\d+ failed\b/im;
const RAN = /Tests run: [1-9]|Tests\s+(?:\d+ failed \| )?\d+ passed|^\s*\d+ passed\b/im;
const counts = (() => {
  const mvn = [...output.matchAll(/Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)\s*$/gm)].pop();
  if (mvn) return `${mvn[1]} run, ${mvn[2]} failed, ${mvn[3]} errors`;
  const vt = [...output.matchAll(/Tests\s+(.+?)\s*\((\d+)\)/g)].pop();
  if (vt) return `${vt[1]} of ${vt[2]}`;
  const pw = output.match(/^\s*(\d+ failed[\s\S]*?\d+ passed|\d+ passed)\b/m);
  return pw ? pw[1].replace(/\s+/g, " ") : "no summary line";
})();
const harness = output.match(HARNESS);
if (step === "red") {
  if (run.status === 0) die(1, `refused: red tests passed (${counts}); a red test must fail because the behaviour is missing; log ${logRel}`);
  if (harness) die(1, `refused: red failed for a harness reason ("${harness[0]}"), not on an assertion; add the stubs the tests need; log ${logRel}`);
  if (!FAILED.test(output)) die(1, `refused: the tests failed but no failing test is reported (${counts}); log ${logRel}`);
} else {
  if (run.status !== 0) die(1, `refused: ${step} needs passing tests (${counts}, exit ${run.status}); log ${logRel}`);
  if (!RAN.test(output)) die(1, `refused: no test ran (${counts}); log ${logRel}`);
}

// 4. stage everything except 03_statistics/ and check what the commit contains
const isTest = (p) => /(^|\/)(src\/test|tests?|__tests__|e2e|acceptance)(\/|$)|\.(test|spec)\.[a-z]+$/i.test(p);
const notCounted = (p) => /(^|\/)(logs?|node_modules|target|dist|build|coverage)\//i.test(p) || /(package-lock\.json|\.lock)$/i.test(p);
const add = git(["add", "-A", "--", ".", ":(exclude)03_statistics"], { cwd: root });
if (add.status !== 0) die(1, "refused: git add failed: " + add.stderr.trim());
const unstage = (msg) => { git(["reset", "-q"], { cwd: root }); die(1, msg); };
const files = git(["diff", "--cached", "--numstat"], { cwd: root }).stdout.split("\n").filter(Boolean).map((l) => { const [a, d, ...p] = l.split("\t"); return { path: p.join("\t"), lines: (+a || 0) + (+d || 0) }; });
const counted = files.filter((f) => !notCounted(f.path));
const tests = counted.filter((f) => isTest(f.path));
if (!counted.length) unstage("refused: nothing to commit apart from the log");
if (step === "red" && !tests.length) unstage("refused: a red commit must add or change a test file");
if (step !== "red" && tests.length) unstage(`refused: a ${step} commit must not change tests (${tests.map((f) => f.path).slice(0, 3).join(", ")})`);
const lines = counted.reduce((s, f) => s + f.lines, 0);
if ((counted.length > 15 || lines > 400) && oversize === null) unstage(`refused: ${counted.length} files, ${lines} lines (limit 15 / 400); split the cycle by layer, or give --oversize "<reason>" for one generated file`);

// 5. commit
const msg = ["-m", subject];
const bodyText = [body, oversize !== null ? `Oversize: ${oversize}` : null].filter(Boolean).join("\n\n");
if (bodyText) msg.push("-m", bodyText);
const commit = git(["commit", "-q", ...msg], { cwd: root });
if (commit.status !== 0) unstage("refused: git commit failed: " + (commit.stderr || commit.stdout).trim());
const hash = git(["rev-parse", "--short", "HEAD"], { cwd: root }).stdout.trim();
console.log(`${step} ${cycle}: ${counts}; committed ${hash} (${counted.length} files, ${lines} lines); log ${logRel}`);
