# Project memory

Last updated: 2026-11-03. This is a dated handoff, not an evergreen test result.
Read `AGENTS.md` first; verify current source and configuration before making changes.
Superseded RC history lives in `docs/verification/` and Git history, not here.

## Identity and intent

- Repository: `byscrimm/phpstorm-yii2-support`, default branch `master`.
- Fork of `vxdy/Yii2-Support-Extended`, itself based on `nvlad/yii2support`.
  Upstream base when connected: `cd9cb4485e457f12a13faf2f0632ca861d8b2685`.
- Name Yii2 Insight; vendor by_scrimm; ID/package `io.github.byscrimm.yii2insight`;
  settings `yii2-insight.xml`. Legacy settings are never read or migrated. See IDENTITY.md.
- All fork updates are developed with AI assistance (currently Claude Code); inherited
  authorship and the BSD license remain intact. See `AI_DEVELOPMENT.md`.
- Maintainer wants a reliable, useful Yii2 plugin for PhpStorm 2026.2 and communicates in Russian.

## Current repository state

- `master` = `0d5fed9`: PR #1 (1.0.0 integration), PR #3 (each/batch types, audit fixes,
  docs cleanup) and PR #4 (version `1.0.0-rc.7`) are merged.
- Published pre-release `v1.0.0-rc.7` (2026-11-03): https://github.com/byscrimm/phpstorm-yii2-support/releases/tag/v1.0.0-rc.7
  ZIP SHA-256 `2ab7a61af10e7de1ef4db95bc8bb2edd7a56e436c7a43d0d92e7418ceb398b03`; Docker suites
  (155 core / 170 PSI) and official Plugin Verifier passed in the release run; checksum and
  provenance attestation verified locally; full IDE runtime scenarios `not_run`.
- Stable `1.0.0` is blocked by tooling until `docs/verification/runtime-1.0.0.json` records a
  complete isolated-IDE run of the exact stable ZIP (RELEASING.md). Never invent it.
- Remote settings (2026-09-08): squash-only merges, protected `master` requiring PRs and both
  checks (`checks / Repository policy`, `checks / Build and verify`), immutable `v*` tags,
  read-only Actions token, SHA-pinned actions, Dependabot, private vulnerability reporting.
- Local branch `build/docker-packaging` (`ca8664e`) is an old pre-rename line (1.1.0); keep until reviewed.

## Build baseline

`gradle.properties` is the version source: plugin `1.0.0`, PhpStorm `2026.2.2`.
SDK used locally: `262.10315.130`; Java 25. Gradle Wrapper 9.1.0; IntelliJ Platform
Gradle plugin 2.18.1. Do not assume these are the latest releases.

- `tools/build-offline.py`: compiles with an installed PhpStorm SDK, optionally runs
  both suites, packages only after requested checks pass, writes ZIP/SHA/build info.
- `tools/test-php-psi.py`: lightweight IntelliJ Core environment with real PHP parser.
- `tools/verify-offline.py`: classfile/JVM member reference and descriptor checks.
- `tools/smoke-ide.py`: separate full IDE test profile; requires valid activation.
- `Dockerfile` / `Makefile`: BuildKit artifact export; `make docker` builds/tests.
  `OUTPUT_DIR` changes export location; `GRADLE_ARGS=--info` enables diagnostic logs.
- Verifier IDE configuration must use `ides.create(IntelliJPlatformType.PhpStorm, version)`,
  not the dependency DSL method `phpstorm`.
- `.github/workflows/ci.yml` calls build-and-verify.yml; release.yml creates draft
  GitHub Releases from version tags.

`build/` is generated and ignored. No IDE profile or license data belongs in the repository.

## Source map

Production Java is under `src/io/github/byscrimm/yii2insight/`; extension registration is in
`resources/META-INF/plugin.xml` and optional descriptors beside it.

| Area | Main entry points / purpose |
| --- | --- |
| `common` | PHP arguments/arrays, local values, aliases, Yii context, routes, SQL scanning, query result types |
| `relations` | `YiiModelResolver`, `RelationContext`, completion, segment references, usage search, rename, PHPDoc inspection |
| `typeprovider` | Deferred Yii component and ActiveRecord result type resolution |
| `configurations`, `objectfactory` | Component index, config completion and property references |
| `views` | Render/view lookup, references, inspections, settings and view index |
| `url`, `i18n` | Route and translation completion |
| `database`, `properties` | Schema-assisted attributes and SQL parameter inspections/fixes |
| `forms`, `validation`, `widgetsconfig`, `attributeLabels` | Form/model/widget completion |
| `migrations` | Project service, process output, cancellation and tool-window UI |
| `tests`, `psi-tests`, `gradle-tests` | Pure logic tests, PHP PSI regressions, JUnit adapters |
| `integration` | Separate full IDE smoke-test application starter |

## Implementation findings

- PHP named argument names/colons can be siblings of the expression. Use
  `PhpArguments.get`, not a positional-only parameter lookup.
- A string array key can have a PSI wrapper between it and `ArrayHashElement`.
  Use `PhpArrays.keyEntry`; do not infer ownership from the direct parent alone.
- `LocalPhpValues` is bounded syntax analysis. It rejects ambiguous branch-only
  assignments and cycles; it is not general PHP dataflow analysis.
- `YiiModelResolver` is short-lived and uses injected symbols in tests; production symbols
  come from `PhpIndex`. It unwraps parentheses (`(new City())->with(...)`). Custom Query
  scopes remain unsupported. Do not make it a global cache of PSI or projects.
- Getter names (`getOrders`) differ from relation strings (`orders`); direct soft
  references alone do not provide index-wide getter usage search.
- Relation contract (maintainer-approved): strings navigate to getters but cannot start
  Rename; Find Usages from getter/PHPDoc finds model-matched strings; Rename from the
  declaration pairs getter, local PHPDoc property and strings, preserving suffixes and aliases.
- Closure values may be PhpExpression wrappers around Function; inline @param documentation
  may sit before the wrapper. Do not rely only on `Parameter.getDocTag()`.
- The PSI core environment lacks `PhpCaches`, so positive `getType` cases cannot run in
  `psi-tests/`; test type mapping as pure logic in `tests/`.
- Core test environment needs `TreeAspect` before `PomModelImpl`, parser registry defaults
  from the SDK, and smart-pointer initialization while the app is alive.

## Last verified state (merged in PR #3, released as 1.0.0-rc.7)

- Changes: Claude Code tooling (`CLAUDE.md` imports `AGENTS.md`); `each()`/`batch()` result
  types; view index UTF-8 (index version 34); Yii root boundary; view rename smart pointers;
  `Yii::$app` types skipped in dumb mode; parenthesized model expressions; docs/branding cleanup.
- Checks: `build-offline.py --test --psi-test` (155 core / 170 PSI) and `verify-offline.py`
  (201 classes / 1285 SDK jars) passed; repository policy, tooling tests and workflow checks
  passed before the R-7 fix. Docker and the official Plugin Verifier were not run for this tree.
- Isolated IDE (`build/ide-runtime-claude`, ignored): each/batch completion and navigation,
  relation scenarios R-1..R-6 and negatives passed manually; other scenarios not run.
  Manual fixtures: `build/ide-runtime-claude/project/scenarios` (not tracked).
  Screen automation there is costly and unreliable: Cmd+L/Cmd+B select all text,
  Ctrl+Space does not open completion, subagents cannot take full-screen control.

## Known violations and next work

- `ViewFileIndex` depends on project settings (indexer rule); `YiiTypeProvider.getType`
  runs local inference; migration cancellation ignores the progress indicator; migration
  exit code is unused.
- Deprecated API usages remain; full-index Find Usages/Rename and completion UI are not
  automated.
- Roadmap order: SQL aliases/attributes, asArray result keys, controller-to-view flow,
  config merging/DI, routes/i18n, indexing/performance. See `DEVELOPMENT_PLAN.md`.
- Next for stable 1.0.0: set version 1.0.0 on a release branch, test that exact CI ZIP in the
  isolated IDE (fixtures in build/ide-runtime-claude/project/scenarios), add runtime evidence, tag.
