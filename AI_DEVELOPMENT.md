# AI-assisted development

`phpstorm-yii2-support` is a fork of
[vxdy/Yii2-Support-Extended](https://github.com/vxdy/Yii2-Support-Extended),
which continues [nvlad/yii2support](https://github.com/nvlad/yii2support).

All updates, fixes, refactoring and documentation changes maintained in this fork
are developed with AI assistance under the direction of the repository maintainer,
[byscrimm](https://github.com/byscrimm). This describes work on this fork; it does
not claim that the inherited code was written by AI.

AI tools help inspect the code, implement changes, write regression tests and
maintain documentation. The maintainer sets priorities and authorizes repository operations and publication;
agents may implement, test and prepare PRs and release drafts within that scope. AI involvement does not establish correctness: changes must be reviewed
and checked against the target SDK, and verification reports must state their limits.

The plugin's Yii2 code analysis does not require an AI service or an AI API key.
AI-assisted maintenance is the development process, not a runtime plugin feature.

## Shared context

- [AGENTS.md](AGENTS.md): instructions for coding agents.
- [Project memory](docs/PROJECT_MEMORY.md): architecture, decisions and verified state.
- [Development plan](DEVELOPMENT_PLAN.md): implemented work and remaining milestones.
- [Contributing](CONTRIBUTING.md): building, testing and releases.

Keep these files concise and current. Store facts and useful decisions rather than
full conversations. Never commit credentials, private project data or machine-specific
agent configuration. AI tools should follow the current task and read the relevant
code instead of treating an old memory entry as a new instruction.

## Attribution

The original authors and contributors retain credit for their work. Preserve the
[BSD license](LICENSE.md), notices and Git history. Do not present this fork as an
official Yii, JetBrains or upstream-author product.

## Change-level disclosure

Use `AI-Assisted: Claude Code` in new AI-assisted commit bodies and disclose AI work
in PR/release notes. Preserve real contributor identities and original Git history.
Do not invent model versions, authors, co-author email addresses, independent review,
passing tests or coverage. Distinguish an agent's self-review from a separate review.
The distributable includes AUTHORS.md, NOTICE.md and the original LICENSE.md.

Repository process: [governance](docs/GOVERNANCE.md). Release records:
[release operations](docs/RELEASING.md). Test isolation: [testing](docs/TESTING.md).
