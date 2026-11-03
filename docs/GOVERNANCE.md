# Repository governance

This is an independent BSD-licensed fork maintained by byscrimm with AI assistance.
The maintainer sets direction and authorizes publication. AI agents implement,
self-review, test, document and prepare pull requests. Self-review must never be
described as an independent human review. Preserve real Git authorship; use an
`AI-Assisted: Claude Code` commit trailer when applicable, without invented identities.

## Branches and changes

- `master` is the protected stable line. Work on branches and merge through PRs.
- `feature/<topic>`, `fix/<topic>`, `build/<topic>`, `ci/<topic>`, `docs/<topic>`,
  `refactor/<topic>`, `perf/<topic>`, `test/<topic>` and `chore/<topic>` describe work.
- `release/<major.minor.patch>` collects a release if several branches need integration.
  `hotfix/<topic>` starts from the latest stable tag; bring its fix into active release work.
- No agent-branded branch prefixes. No permanent `develop` branch until there is a need.
- Keep PRs focused. Use squash merge with the PR title as the commit subject; preserve
  meaningful contributor and AI trailers in the squash body. Never rewrite upstream history.

PR titles follow Conventional Commits, for example `feat(views): infer render parameters`,
`fix(relations): preserve aliases on rename`, `ci: verify release artifacts`.
Allowed types: feat, fix, docs, build, ci, test, refactor, perf, chore, revert.
Use `!` and a `BREAKING CHANGE:` explanation when a change is incompatible. Historical
commits are grandfathered; this convention does not authorize rewriting them.

## Review and merge

CI requires repository policy and Docker build/official verification to pass. Review
the diff, user-visible behavior, regression evidence, license impact and remaining
limits. Record what was actually reviewed. A green CI badge is not a runtime guarantee.
Changes to index-backed behavior require relevant isolated-IDE evidence before being
treated as stable. Workflow changes need workflow validation; documentation edits
do not need additional implementation-mirroring tests.

One-account maintenance cannot provide a second person's GitHub approval. Configure
required PRs and checks, but **zero required approving reviews** until an independent
reviewer is available. CODEOWNERS identifies ownership, not independent assurance.
Do not manufacture reviews through another account or claim AI self-review is independent.

## Versions and release boundaries

`gradle.properties` is the authoritative version source. Follow SemVer:

| Change | Example |
| --- | --- |
| Compatible bug fix | 1.1.0 → 1.1.1 |
| Compatible user-facing feature | 1.1.0 → 1.2.0 |
| Incompatible behavior/settings migration or dropped supported IDE line | 1.x → 2.0.0 |
| Incomplete/experimental preview | 1.2.0-alpha.1 or 1.2.0-beta.1 |
| Feature-complete candidate undergoing release checks | 1.2.0-rc.1 |

RCs are allowed and useful; an independent repository or AI development does not
change that. Increase the prerelease number for a newly distributed candidate.
Never replace released bytes or move released tags. `v<version>` tags name exact
commits; branch names are not versions. Documentation-only changes do not automatically
require a plugin version increment. Do not let Conventional Commit parsing decide
compatibility or publish releases without reviewing the actual change.

Every user-visible change belongs in CHANGELOG.md. Before tagging, consolidate the
release's changes since the previous public release, including work first announced
in RCs. Do not hide unreleased work behind a final version number. GitHub release
notes are generated from the exact version section plus measured build evidence.

## Bootstrap state

The governance workflow and earlier local increments were merged into `master`
through PR #1 (squash commit `b366c26`). Merging is not a release: no `v1.0.0` tag
or GitHub Release exists until RELEASING.md is completed.
Local version 1.1.0 was built but never publicly released. The repository candidate
was 1.1.0-rc.3. The independent identity build was initially numbered 2.0.0-rc.1
but was never published. On 2026-09-09 the maintainer chose a separate public
version sequence for Yii2 Insight: 1.0.0-rc.1, then 1.0.0 after validation.
Future increments follow the table above relative to published Yii2 Insight releases,
not upstream versions. See IDENTITY.md. Historical reports remain intact.

## Sources

- https://www.conventionalcommits.org/en/v1.0.0/
- https://semver.org/spec/v2.0.0.html
- https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches
