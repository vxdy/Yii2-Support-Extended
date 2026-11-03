# Release operations

Distribution starts with **GitHub Releases**, containing an installable ZIP. GitHub
Packages is not needed for this plugin; Docker is a build tool, not a required
published container. JetBrains Marketplace publication is a separate later operation.
No workflow currently uploads to Marketplace or modifies an installed IDE.

## Prepare a candidate

1. Work on a descriptive branch; read GOVERNANCE.md. Set the version in
   `gradle.properties` (currently `1.0.0`) and update the exact CHANGELOG.md section.
2. Run `python3 tools/check_repository.py`, the tooling tests, workflow validation,
   Docker suites and official Plugin Verifier. Submit a PR describing changes and
   actual results. Resolve findings instead of suppressing release checks.
3. Review attribution, dependency/asset licenses and notices in the ZIP. The independent
   identity is documented in IDENTITY.md; asset audit and isolated-IDE validation
   remain required before a Marketplace listing.
4. With publication preparation authorized, tag the reviewed commit, e.g.
   `git tag -a v1.0.0 -m 'Yii2 Insight 1.0.0'`, then push that exact tag.
   Push the branch first. Never use `git push --tags`, force-push a tag or overwrite assets.
5. The tag workflow builds and checks that exact revision, validates version/tag/ZIP,
   writes factual evidence, attests the ZIP, and creates a **draft** GitHub Release.
   RC/alpha/beta tags are marked prerelease. Existing releases are never overwritten;
   a failed attempt is inspected before any cleanup or retry.
6. Inspect the draft's ZIP, notes, checksums and reports. Publish it only when authorized.
   An RC can state that IDE tests are pending; a stable version cannot omit them.

`gh release create` is only used after successful build/eligibility jobs. Publication
credentials are scoped to the draft job; PR builds receive no write token or secrets.
Actions are pinned to full commit SHAs. There is no `pull_request_target` execution.

## Stable releases

- Stable tag commits must belong to `origin/master`, after an approved-by-process PR.
- Successful Plugin Verifier is required for both candidates and stable releases.
- Test the exact stable-version ZIP in an isolated IDE. Store the sanitized evidence
  as `docs/verification/runtime-<version>.json`, referencing its SHA-256 and a durable
  report URL. The required shape is illustrated in runtime-evidence.example.json.
- The runtime report must cover installation, completion, navigation, rename,
  inspections, settings, restart, indexing and an unrelated non-Yii project.
- A missing report blocks stable release preparation. A stale/different ZIP hash
  blocks reuse of an old report. Build output remains available as a CI artifact
  when eligibility fails, so that exact ZIP can be tested.
- To obtain the stable ZIP before tagging, build the final version on its release
  branch/PR. Add only evidence after testing; if the final ZIP changes, retest it.
  The tag workflow compares exact bytes, not a claim that source is "similar".
- Runtime evidence is a recorded test result, not automatic proof of honest execution.
  Link actual logs/scenarios, record exact IDE/OS scope and do not invent passed entries.

After all release assets are attached, publish the draft. With GitHub release
immutability enabled, tags/assets are protected after publication. If a defect is
found, describe it, mark the affected version as unsuitable and issue a new version.
Do not silently swap the old ZIP. Users can install the previous known-good release;
any future settings migration needs its own backward-compatibility assessment.

## Evidence and metrics

Each draft includes ZIP, per-ZIP checksum, SHA256SUMS, build.properties,
build-report.json, RELEASE_NOTES.md and test-reports.tar.gz. The report records source
commit, dirty status, SDK, workflow URL, archive size/hash, executed JUnit methods,
regression checks, verifier status and exact runtime evidence (or not_run).
GitHub also stores ZIP provenance attestations. Verify with
`gh attestation verify <zip> --repo byscrimm/phpstorm-yii2-support`.

Use METRICS.md for what counts as measured. CI artifacts expire after 30 days;
published release assets are the durable record. Build logs can contain paths:
review any manually attached local logs and never upload customer-project logs.

## Marketplace readiness

Name, plugin ID, vendor, ID namespacing and coexistence rules are settled (IDENTITY.md).
Remaining before a publication workflow: bundled-asset review (ASSET_AUDIT.md),
screenshots and exact-ZIP compatibility/runtime checks.
Preserve LICENSE.md, NOTICE.md and AUTHORS.md. Provide genuine support contacts,
source links, screenshots from test projects and an accurate feature description.
See https://plugins.jetbrains.com/docs/marketplace/jetbrains-marketplace-approval-guidelines.html.

## Remote configuration

See GITHUB_SETUP.md. Committing these workflows does not enable branch protection,
private vulnerability reporting, immutability or third-party bots by itself. Do not
claim any setting is applied without observing it on GitHub.
