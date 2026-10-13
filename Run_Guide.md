# Run guide

This setup is run three times, once each by kyuhi, tanej and tjan, in one shared environment, and the three runs are compared. Everything below is therefore the same for everyone; the only things that may differ are the four lines in the table "If you are not kyuhi".

## 1. Prepare the run folder
1. **Copy the template** (the folder `kyuhi_single-agent_sdd_v007_structure`) to a new folder named after the run, for example `kyuhi_single-agent_sdd_v007`. Open a terminal in it.
2. **Initialise git on the run's branch and keep `.env` out of git before you create it.** The branch is named `<operator>/single-agent/sdd/v007` (for kyuhi: `kyuhi/single-agent/sdd/v007`).
   - `git init -b kyuhi/single-agent/sdd/v007` (use your own operator name)
   - then exclude `.env` (the template has no `.gitignore`; the agent writes one in phase 0):
   - Git Bash: `echo .env >> .git/info/exclude`
   - PowerShell: `Add-Content -Encoding ascii .git\info\exclude ".env"`
3. **Copy the project inputs** `01_input/01_project/` and paste it into run directory.
4. **Check `01_input/01_project/00_setup/run-config.md`.**
      - operator `kyuhi`, 
      - machine `kyuhiPC`, 
      - plan `PRO`, 
      - permission mode `auto`, 
      - model `claude-opus-5-5`, 
      - effort `medium`
      - Run label: **change it for every run** (`kyuhi_single-agent_sdd_v007`, then `v007`,...).
      - Run ID: **keep empty it generates by itself**
      - Starting commit: **keep empty it generates by itself**
5. **Check the price row in `03_statistics/usage.md`.** It is pre-filled for Opus 5.5; change it only if the prices or the model changed.
6. **Create `.env`**: copy `01_input/01_project/00_setup/secrets.env.example` to `.env` in the run folder and fill every value (database password, organizer login, NVD key). Never paste a secret into the chat.
7. **Commit the inputs**: `git add -A`, `git commit -m "initial commit"`. Check that `git status` does not list `.env` (it must be clean) before anything is pushed.
8. **Connect to the shared runs repository and push the initial setup** (so the start state is on record before the agent starts):
   ```bash
   git remote add origin https://github.com/SQA-interns/Structured_Input_Files.git
   git push -u origin kyuhi/single-agent/sdd/v007
   ```
   Use your own operator name in the branch. Every operator pushes to a branch of its own in this repository, never to `main`. Sign in to GitHub when asked. If `git remote add` says the remote already exists, check it with `git remote -v` and fix it with `git remote set-url origin https://github.com/SQA-interns/Structured_Input_Files.git`. Do not push before `.env` is confirmed absent from `git status`.
9. **Set the same permission mode** you set in `run-config.md`.
10. **Check the environment** (the same for all three operators): Docker Engine 29.8.1 and Compose 5.5.1, Node 24.13.0, Java 21, internet access for the scanners (NVD key in `.env`), no other heavy load on the machine during the run, one session only.

**If you are not kyuhi**, change only these lines in `01_input/01_project/00_setup/run-config.md` and nothing else (not the inputs, not the tools):

| Line | kyuhi | tanej | tjan |
|---|---|---|---|
| Operator | `kyuhi` | `tanej` | `tjan` |
| Machine | `kyuhiPC` | a label of your computer | a label of your computer |
| Plan | `PRO` | your plan | your plan |
| Run label | `kyuhi_single-agent_sdd_v007` | `tanej_single-agent_sdd_v007` | `tjan_single-agent_sdd_v007` |

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
| 6 | Stryker reports an implausibly low frontend score (about 25 %) or the agent says mutants are not activated | Approve `vitest` and `@vitest/coverage-v8` 4.1.x, the same version for both; `@stryker-mutator/vitest-runner` 10.0.0 does not work with Vitest 5. No question if `tech-stack.md` has 4.1.11. |

Anything the agent decides on its own (for example a scanner false positive rated Medium) needs no answer.

## 4. After the run
The measuring is automatic and nothing is typed or copied by hand: cost, tokens, times, context, approval prompts and commits are all calculated from the session transcript and git. `.claude/settings.json` in the run folder holds a hook that builds the report when the session ends (or about 8 seconds after the agent's last reply once the run has ended). Do not edit or delete it. If Claude Code asks whether to trust the folder or its hooks, accept.
1. **Close the session** (`/exit`) once the agent has written the final statistics. Wait for the terminal to return: the hook then writes `03_statistics/results/` (report, measurements, usage summary, commits, readable transcript) and `03_statistics/data/` (transcript backup). `03_statistics/results/auto-report.log` shows what it did.
2. **Only if `results/run-report.html` is missing** (hook not run, session killed, folder not trusted): open a new session in the same folder and send exactly:

   ```
   Run node 03_statistics/tools/build-report.mjs --run 03_statistics and report what it prints. Do not change anything else.
   ```

3. Open `03_statistics/results/run-report.html`. At the top, "Quality gate" shows whether the run reached the gate (a run that did not is not comparable; write down which threshold failed). "Trust in the measurement" shows whether the evidence was complete (transcript found, clock tool used for every phase boundary, no idle time missing from the log).
4. Commit the statistics: `git add -A`, `git commit -m "statistics <label>"`. `git status` must not list `.env`. Then push the statistics: `git push` (the branch is already connected from step 1.8).
5. Hand in the run folder, or its branch and the commit SHAs from steps 1.7 and 4.4 (both pushed), plus a note on anything unusual (a repeated run, a usage limit, a manual change).

## If something goes wrong
| Problem | What to do |
|---|---|
| `stat.mjs` or `build-report.mjs` fails with "node not found" | Install Node 20 or newer, open a new terminal, send the same prompt again. |
| The report says no transcript was found | Run `py 03_statistics/tools/claude_chat_to_md.py --list`, find the session, put the path of its `.jsonl` in `03_statistics/run-log.json` → `transcript` (a resumed run: every session file in `transcripts`) and run step 4.2 again. |
| `transcript.md` is missing | Python 3 is not installed. Everything else is complete. |
| The run was resumed in a new session | Same folder, same prompt text. List both session files in `transcripts` before step 4.2. |
| The cost check does not say `ok` | Write it down and hand it in; do not fix numbers by hand. |
