# Run guide

## 1. Prepare the run folder
1. **Copy the template** (`00_Base_Template`) to a new folder named after the run, for example `kyuhi_single-agent_sdd_v003`. Open a terminal in it and run `git init`.
2. **Keep `.env` out of git before you create it.** The template has no `.gitignore` (the agent writes one in phase 0).
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
      - Run label: **change it for every run** (`kyuhi_single-agent_sdd_v003`, then `v004`,...).
      - Run ID: **keep empty it generates by itself**
      - Starting commit: **keep empty it generates by itself**
5. **Check the price row in `03_statistics/usage.md`.** It is pre-filled for Opus 5.5; change it only if the prices or the model changed.
6. **Create `.env`**: copy `01_input/01_project/00_setup/secrets.env.example` to `.env` in the run folder and fill every value (database password, organizer login, NVD key). Never paste a secret into the chat.
7. **Commit the inputs**: `git add -A`, `git commit -m "initial commit"`. Check that `git status` does not list `.env`.
8. **Set the same permission mode** you set in `run-config.md`.

## 2. Start the run
Open a **new agent session in the run folder** (only one session per run; do not open other sessions in this folder, the newest one is taken as the run's transcript) and send exactly:

```
Read AGENTS.md and start.
```

## 3. During the run
- Answer tooling and environment questions with the standard answers below, the same way in every run.
- To any question about **business behaviour** (what the system should do), answer only: *"No further information is available. Choose an option and record it as a decision."*
- Type at least one line of each answer yourself; a pasted-only answer makes the agent ask you to confirm.
- Do not change the input files. Do not leave a question unanswered for more than an hour: the cache expires and the wait is excluded from the results, but the time is lost. If you must stop, tell the agent so it records the wait.

**Standard answers (conference registration project; decision numbers differ, recognise them by subject)**
| Phase | The agent asks about | Answer |
|---|---|---|
| 0 | Host JDK, Node or npm differs from the pins | Approve the installed versions. |
| 0 | `vitest` or `jscpd` has Critical or High vulnerabilities | Move to the fixed versions the agent proposes (`jscpd` 5.4.0). `vitest` and `@vitest/coverage-v8` stay on 4.1.11: 5.x does not work with Stryker (see phase 6). No question if `tech-stack.md` already has them. |
| 0 | CVE-2025-7962 (High) on `angus-activation` | False positive (Low): the CVE is in the mail library before 2.0.4, the shipped `angus-mail` is 2.0.5. Suppress it for that one artifact only. |
| 6 | Semgrep "basic authentication" (High) on the organizer endpoint | Lower to Low and accept: HTTP Basic is required by `architecture.md` and `security-requirements.md`, over HTTPS or localhost only, password only as a BCrypt hash, failed logins rate limited. |
| 6 | Stryker reports an implausibly low frontend score (about 25 %) or the agent says mutants are not activated | Approve `vitest` and `@vitest/coverage-v8` 4.1.x, the same version for both; `@stryker-mutator/vitest-runner` 10.0.0 does not work with Vitest 5. No question if `tech-stack.md` has 4.1.11. |

Anything the agent decides on its own (for example a scanner false positive rated Medium) needs no answer.

## 4. After the run
1. In the **finished session** type `/cost` once (the plan limits are then saved in the transcript).
2. Open a **new session in the same folder** and send exactly:

   ```
   Run node 03_statistics/tools/build-report.mjs --run 03_statistics and report what it prints. Do not change anything else.
   ```

   This measures the run, backs up the transcript to `03_statistics/data/`, writes the report and everything else it generates to `03_statistics/results/`, and fills the empty cells of `usage.md`.
3. Add the number of **approval prompts** to `03_statistics/usage.md` (the only cell it cannot fill).
4. Open `03_statistics/results/run-report.html`. In "Trust in the measurement" the cost difference should say `ok` and the phase timestamps should come from the clock tool. If not, write down what you see.
5. Commit the statistics: `git add -A`, `git commit -m "statistics <label>"`. `git status` must not list `.env`. Push the branch if you use a remote.
6. Hand in the run folder, or its branch and the commit SHAs from steps 1.7 and 4.5, plus a note on anything unusual (a repeated run, a usage limit, a manual change).

## If something goes wrong
| Problem | What to do |
|---|---|
| `stat.mjs` or `build-report.mjs` fails with "node not found" | Install Node 20 or newer, open a new terminal, send the same prompt again. |
| The report says no transcript was found | Run `py 03_statistics/tools/claude_chat_to_md.py --list`, find the session, put the path of its `.jsonl` in `03_statistics/run-log.json` → `transcript` (a resumed run: every session file in `transcripts`) and run step 4.2 again. |
| `transcript.md` is missing | Python 3 is not installed. Everything else is complete. |
| The run was resumed in a new session | Same folder, same prompt text. List both session files in `transcripts` before step 4.2. |
| The cost check does not say `ok` | Write it down and hand it in; do not fix numbers by hand. |
