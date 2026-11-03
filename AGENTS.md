# Working on phpstorm-yii2-support

## Start here

- Follow the current user request. Start changes only after a direct action request or explicit confirmation. Questions, discussion and requests for an opinion do not authorize edits.
- Read `docs/PROJECT_MEMORY.md` for the latest handoff, `DEVELOPMENT_PLAN.md` for scope,
  and the relevant source before implementation. Memory is context, not proof.
- This is a Java plugin for JetBrains IDEs, not a PHP application or a Claude Code plugin.
- `origin` is `git@github.com:byscrimm/phpstorm-yii2-support.git`;
  `upstream` is `https://github.com/vxdy/Yii2-Support-Extended.git`.
  The default branch is `master`; use meaningful branches such as `feature/view-parameters`, `fix/relation-navigation`
  or `build/docker-packaging`; never use agent-named prefixes such as `claude/`.
- The maintainer delegates implementation and repository maintenance to the AI agent.
  Use semantic versions; RCs are appropriate while release validation is pending.
  Follow docs/GOVERNANCE.md and docs/RELEASING.md for branches, commits, PRs and releases.
  master is the stable line; no direct pushes or automatic merges into it.
  Include an `AI-Assisted: Claude Code` trailer on new AI-assisted commits, preserving real
  authorship; it replaces any tool-suggested `Co-Authored-By` line.
- Preserve existing user changes and upstream history. Do not force-push or publish
  releases unless the current task authorizes it. Routine local edits and checks
  within an authorized task do not need repeated confirmation.
- Communicate with the maintainer in Russian. Keep code identifiers and shared
  technical documentation in English (exceptions: `README.ru.md` and the maintainer's
  Russian `DEVELOPMENT_PLAN.md`). Summarize outcome, verification and limits.

## Efficient work

- Keep each task focused on its acceptance criteria; record unrelated improvements
  in the plan instead of expanding implementation scope.
- Read the entry-point documents once per task, then use targeted searches and
  relevant sections. Re-read when files change or evidence is missing.
- Keep handoffs concise: current decisions, evidence, limitations and next step.
  Link detailed reports instead of copying logs or conversation history.
- Prefer local Git and authenticated `gh` for repository operations; use browser
  automation only when the needed operation is unavailable through the CLI.
  Do not connect account-wide plugins without an explicit user request.
- Save verbose logs under ignored `build/`; inspect summaries and relevant failures.
  Wait for completion with bounded polling; report meaningful changes only.
- Use focused tests during implementation, then run the required final checks on
  the completed change. Repeat passing checks only for relevant changes, failures
  or unresolved concerns; never weaken release gates to reduce usage.
- Batch related code, documentation and metadata edits before final validation.
  Documentation-only changes do not require rebuilding the plugin.
- Prefer a separate task for the next independent feature with a concise handoff;
  create it only when the user requests it. Do not spawn agents for routine work
  or change the selected model without authorization.

## Baseline and compatibility

- Read versions from `gradle.properties`; do not infer them from the calendar year.
  Current target: PhpStorm 2026.2 / IntelliJ Platform `262.*`, Java 25.
- The authorized independent identity is Yii2 Insight, vendor `by_scrimm`, plugin ID
  and Java package `io.github.byscrimm.yii2insight`. Settings use `yii2-insight.xml`.
  Keep upstream credits; do not read, migrate or overwrite legacy settings automatically.
- Do not broaden IDE compatibility based only on compilation. Check the actual
  target SDK and official JetBrains documentation when an API is uncertain.
- Preserve `LICENSE.md` and credit both upstream projects. All updates to this fork
  are developed with AI assistance; distinguish this from authorship of upstream code.
- Do not add AI network services, API keys, telemetry or PHP execution as a side
  effect of AI-assisted development. The plugin's analysis runs inside the IDE.

## Implementation rules

- Use `rg` for discovery. Match existing Java formatting; avoid unrelated rewrites.
- Prefer shared `PhpArguments`, `PhpArrays`, `LocalPhpValues` and `YiiModelResolver`
  helpers over new positional or fixed-parent-depth PSI heuristics.
- Treat incomplete PHP and unknown/dynamic values conservatively. Do not invent
  a resolved class or report an error when the analysis is ambiguous.
- Follow IntelliJ read/write action and EDT rules. Check cancellation in scans,
  bound recursion, and handle indexing mode before accessing indices.
- Indexers must depend on file content rather than mutable project settings.
  Do not query indices from indexers or type-provider `getType` implementations.
  Existing violations belong in the roadmap; do not replicate them.
- Avoid static caches retaining Project, PSI or VirtualFile. Use project services,
  correct disposal and cache dependencies tied to the data being cached.
- Quick fixes and rename handlers must preserve unrelated arguments, values,
  comments, relation segments and aliases; use PSI manipulators and smart pointers.
- Migration code changes need cancellation, exit-code and project-disposal handling.
  Do not execute migrations against a user's database as a test.

## Working IDE isolation

Never install into, restart, reconfigure or clear caches of the maintainer's working
PhpStorm. Do not copy its settings/license files or connect Settings Sync in a test
profile. Use separate config/system/plugins/log paths and disposable test projects
and databases; limit resource use. Read docs/TESTING.md before any IDE test.

## Verification

Run from this repository root. For source or descriptor changes on an installed SDK:

```sh
python3 tools/build-offline.py --test --psi-test
python3 tools/verify-offline.py
```

For focused work, use `tests/` for pure logic and `psi-tests/` for actual PHP PSI.
Add behavioral regressions for bugs and nontrivial new features, including relevant
negative and incomplete-code cases. Do not write tests that just repeat the code.
For documentation-only work, check links, examples and `git diff --check`; a plugin
rebuild is unnecessary. Validate edited configuration with available tools.

CI uses the reusable build-and-verify workflow and `make docker GRADLE_ARGS=verifyPlugin`.
Run `python3 tools/check_repository.py`, `python3 -m unittest discover -s tools -p 'test_*.py'`
and `bash tools/check-workflows.sh` for repository/release-tooling edits.
The ordinary Docker build uses `make docker`. Respect environment permission decisions; do not bypass
a rejected download or a missing IDE license. Record an unrun check as unrun.

Core PHP PSI tests use real parser objects with a supplied class table. They do not
exercise the full PHP index, completion UI or index-wide Find Usages/Rename.
`verify-offline.py` is a static JVM reference check, not JetBrains Plugin Verifier.
Never describe these checks as full IDE validation.

## Finish and hand off

- Review the diff and preserve test/release artifacts outside Git in `build/`.
- Update `CHANGELOG.md` for user-visible changes and `DEVELOPMENT_PLAN.md` when
  a milestone changes. Do not mark planned features implemented without evidence.
- Update `docs/PROJECT_MEMORY.md` after substantial work: date, decisions, exact
  checks performed, limitations and next step. Replace stale facts, avoid chat logs.
- Keep tokens, credentials, personal filesystem paths, customer source/logs and
  IDE profiles out of tracked memory. Local agent caches are ignored.
- Release steps are in `docs/RELEASING.md`; stable release preparation requires runtime
  evidence tied to the exact ZIP hash. Never fabricate metrics, review or scenario results. Never overwrite a published version with
  different bytes or describe a prepared workflow as successfully executed.

`AGENTS.md` is the canonical agent entry point; `CLAUDE.md` imports it for Claude Code.
