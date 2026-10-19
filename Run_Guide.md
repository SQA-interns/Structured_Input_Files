# Run guide (TDD)

This setup is run three times, once each by kyuhi, tanej and tjan, in one shared environment, and the three runs are compared. Everything below is therefore the same for everyone; the only things that may differ are the four lines in the table "If you are not kyuhi".

## 1. Prepare the run folder (clone, branch, .env)
1. **Clone the template** into a new folder named after the run. The TDD template is the branch `template/tdd` of the shared runs repository (`main` stays the spec-driven template 1.7); you never commit to either. Clone to a short path near the root of a drive (Windows stops at 260 characters in a path, and the run creates deep folders), then open a terminal in the new folder:
   ```bash
   git clone --branch template/tdd https://github.com/SQA-interns/Structured_Input_Files.git kyuhi_single-agent_tdd_v002
   cd kyuhi_single-agent_tdd_v002
   ```
   Use your own operator name in the folder name. Sign in to GitHub when asked.
2. **Create the run's branch** (it is the only branch you work and push on): `<operator>/single-agent/tdd/v002`, for kyuhi `kyuhi/single-agent/tdd/v002`:
   ```bash
   git switch -c kyuhi/single-agent/tdd/v002
   ```
3. **Keep `.env` out of git before you create it** (the template has no `.gitignore`; the agent writes one in phase 0):
   - Git Bash: `echo .env >> .git/info/exclude`
   - PowerShell: `Add-Content -Encoding ascii .git\info\exclude ".env"`
4. **Check `01_input/01_project/00_setup/run-config.md`** (the project inputs are already in the clone, there is nothing to copy).
      - operator `kyuhi`,
      - machine `kyuhiPC`,
      - plan `PRO`,
      - permission mode `auto`,
      - model `claude-opus-5-5`,
      - effort `medium`
      - Run label: **change it for every run** (`kyuhi_single-agent_tdd_v002`, then `v003`,...).
      - Run ID: **keep empty it generates by itself**
      - Starting commit: **keep empty it generates by itself**
5. **Check the price row in `03_statistics/usage.md`.** It is pre-filled for Opus 5.5; change it only if the prices or the model changed.
6. **Create `.env`**: copy `01_input/01_project/00_setup/secrets.env.example` to `.env` in the run folder and fill every value (database password, organizer login, NVD key). Never paste a secret into the chat.
7. **Commit your changes to `run-config.md` (and `usage.md`, if you changed it)** so the starting commit contains the right operator, machine, plan and label: `git add -A`, `git commit -m "run setup <run label>"`. If you changed nothing there is nothing to commit and the starting commit is the commit of `template/tdd` you cloned. Check that `git status` does not list `.env` before anything is pushed.
8. **Push the run's branch** (so the start state is on record before the agent starts; `origin` is already set by the clone):
   ```bash
   git push -u origin kyuhi/single-agent/tdd/v002
   ```
   Use your own operator name in the branch. Every operator pushes to a branch of its own, never to `main`. Do not push before `.env` is confirmed absent from `git status`.
9. **Set the same permission mode** you set in `run-config.md`.
10. **Start Docker Desktop and check that `docker info` answers** before you send the prompt (in v001 the first question of the run was a stopped Docker engine).
11. **Check the environment** (the same for all three operators): Docker Engine 29.8.1 and Compose 5.5.1, Node 24.13.0, Java 21, internet access for the scanners (NVD key in `.env`), no other heavy load on the machine during the run, one session only.

**If you are not kyuhi**, change only these lines in `01_input/01_project/00_setup/run-config.md` and nothing else (not the inputs, not the tools):

| Line | kyuhi | tanej | tjan |
|---|---|---|---|
| Operator | `kyuhi` | `tanej` | `tjan` |
| Machine | `kyuhiPC` | a label of your computer | a label of your computer |
| Plan | `PRO` | your plan | your plan |
| Run label | `kyuhi_single-agent_tdd_v002` | `tanej_single-agent_tdd_v002` | `tjan_single-agent_tdd_v002` |

## 2. Start the run
Open a **new agent session in the run folder** (only one session per run; do not open other sessions in this folder, the newest one is taken as the run's transcript) and send exactly:

```
Read AGENTS.md and start.
```

## 3. During the run
- Answer tooling and environment questions with the standard answers below, the same way in every run.
- The acceptance criteria, the end-to-end scenarios and the answers to the open questions are inputs, so the agent should not ask about business behaviour. If it does anyway, answer only: *"No further information is available. Choose the proposed default option and record it as a decision."*
- To **any other question not listed below**, give the same answer in every run: *"Choose the proposed default option and record it as a decision."* A frozen acceptance test that cannot pass (a defect in the test harness or in one assertion): approve the agent's proposed default fix (option 1). Do not add your own ideas or hints; different answers make the runs incomparable.
- The agent works until the **quality gate** is met (mutation score 90 %, line coverage 95 %, per component). Do not tell it to stop earlier or to go further.
- Type at least one line of each answer yourself; a pasted-only answer makes the agent ask you to confirm.
- Do not change the input files. Do not leave a question unanswered for more than an hour: the cache expires and the wait is excluded from the results, but the time is lost. If you must stop, tell the agent so it records the wait.

**Standard answers (conference registration project; decision numbers differ, recognise them by subject)**
| Phase | The agent asks about | Answer |
|---|---|---|
| 0 | Host JDK, Node, npm or Docker differs from the pins by a patch version | No question: pre-approved (PA-05). Anything else (a different minor or major version): approve the installed version only if the agent says everything runs. |
| 0 | `vitest` or `jscpd` has Critical or High vulnerabilities | Move to the fixed versions the agent proposes (`jscpd` 5.4.0). `vitest` and `@vitest/coverage-v8` stay on 4.1.11: 5.x does not work with Stryker (see phase 6). No question if `tech-stack.md` already has them. |
| 0 | CVE-2025-7962 on `angus-activation`, CVE-2025-15104 on `hibernate-validator`, dev-only `npm audit` advisories | No question: pre-approved in `01_project/00_setup/pre-approved.md` (PA-01 to PA-03). If the agent asks anyway, answer: *use pre-approved.md*. |
| 6 | Semgrep "basic authentication" (High) on the organizer endpoint | No question: pre-approved (PA-04). If the agent asks anyway, answer: *use pre-approved.md*. |
| 4 | A red test does not compile because the class or function does not exist yet | No question: stubs that report "not implemented" go into the `test:` commit (`working-rules.md`, Commits). If the agent asks anyway, answer: *use the rule in working-rules.md*. |
| 4 | Whether several acceptance criteria may share one cycle | No question: a range of ACs of one story with one code path is allowed (`phases.md`, "TDD cycle"). If the agent asks anyway, answer: *use the rule in phases.md*. |
| 4 | A frozen acceptance test cannot pass with a correct implementation | Approve the agent's proposed default fix (option 1), as in section 3. |
| 5, 6 | Stryker reports an implausibly low frontend score (about 25 %) or the agent says mutants are not activated | Approve `vitest` and `@vitest/coverage-v8` 4.1.x, the same version for both; `@stryker-mutator/vitest-runner` 10.0.0 does not work with Vitest 5. No question if `tech-stack.md` has 4.1.11. |

Anything the agent decides on its own (for example a scanner false positive rated Medium) needs no answer.

## 4. After the run
The measuring is automatic and nothing is typed or copied by hand: cost, tokens, times, context, approval prompts and commits are all calculated from the session transcript and git. `.claude/settings.json` in the run folder holds two hooks that build the report: the Stop hook does it about 8 seconds after the agent's last reply once the run has ended (the agent has written the end time with `stat.mjs phase 7 end`), and the SessionEnd hook repeats it when you close the session. Do not edit or delete the file. If Claude Code asks whether to trust the folder or its hooks, accept.
1. **Wait for the agent's final message, then about 30 seconds.** You do not need to close the session. Check that `03_statistics/results/run-report.html` exists; the hook has then written `03_statistics/results/` (report, measurements, usage summary, commits, readable transcript) and `03_statistics/data/` (transcript backup), and filled the Run, Time, Commits and Cost sections of `03_statistics/run-summary.md`. `03_statistics/results/auto-report.log` shows what it did. Closing the session with `/exit` is optional: it only repeats the build from the final transcript.
2. **Only if `results/run-report.html` is missing** (hook not run, session killed, folder not trusted): open a new session in the same folder and send exactly:

   ```
   Run node 03_statistics/tools/build-report.mjs --run 03_statistics and report what it prints. Do not change anything else.
   ```

3. Open `03_statistics/results/run-report.html`. At the top, "Quality gate" shows whether the run reached the gate (a run that did not is not comparable; write down which threshold failed). "Trust in the measurement" shows whether the evidence was complete (transcript found, clock tool used for every phase boundary, no idle time missing from the log).
4. Commit the statistics: `git add -A`, `git commit -m "statistics <label>"`. `git status` must not list `.env`. Then push the statistics: `git push` (the branch is already connected from step 1.8).
5. Hand in the run folder, or its branch (pushed in steps 1.8 and 4.4) and the start and end commit shown in the Run section of `03_statistics/run-summary.md`, plus a note on anything unusual (a repeated run, a usage limit, a manual change).

## If something goes wrong
| Problem | What to do |
|---|---|
| `stat.mjs` or `build-report.mjs` fails with "node not found" | Install Node 20 or newer, open a new terminal, send the same prompt again. |
| The report says no transcript was found | Run `py 03_statistics/tools/claude_chat_to_md.py --list`, find the session, put the path of its `.jsonl` in `03_statistics/run-log.json` → `transcript` (a resumed run: every session file in `transcripts`) and run step 4.2 again. |
| `transcript.md` is missing | Python 3 is not installed. Everything else is complete. |
| The run was resumed in a new session | Same folder, same prompt text. List both session files in `transcripts` before step 4.2. |
| The cost check does not say `ok` | Write it down and hand it in; do not fix numbers by hand. |
