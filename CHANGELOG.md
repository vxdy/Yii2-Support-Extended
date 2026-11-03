# Changelog

## 1.0.0-rc.7

First public pre-release of Yii2 Insight. It contains everything in the 1.0.0 section
below plus the changes listed here. Full isolated-IDE runtime scenarios for this exact
ZIP are still pending; the stable 1.0.0 release follows after they pass.

- `foreach` over `ActiveQuery::each()` yields the model and over `batch()` yields model arrays; array mode is left to the native provider.
- View index stores strings as UTF-8 (index version 34); view paths outside the Yii root no longer match by prefix.
- View-file rename keeps references as smart pointers and edits them through PSI manipulators.
- `Yii::$app` component types are not resolved during indexing (dumb mode).
- Relation completion and navigation resolve the model through parentheses, e.g. `(new City())->getRelation('')`.

## 1.0.0

First stable version of **Yii2 Insight**, an independent community fork maintained by **by_scrimm**. Requires PhpStorm **2026.2.2** (build 262.10315.130) or later within the 2026.2 branch.

### Added and improved in Yii2 Insight
- ActiveRecord relation completion in with, joinWith, innerJoinWith, getRelation and via, including nested paths, JOIN aliases, callback keys, named arguments and supported local query variables.
- Navigate relation segments to getters; Find Usages from getters/PHPDoc includes model-matched relation strings. Rename from declarations coordinates the getter, local PHPDoc property and usages with preview; Rename from strings is blocked.
- Missing relation PHPDoc inspection on class documentation/name and getter, with actions to add one or all missing local relations. Getter-only relations use @property-read; confirmed hasOne/hasMany targets receive nullable/array model types. Existing documentation is preserved.
- GridView/DetailView model and attribute resolution, nested relation attributes, attribute:format:label handling, filter-model separation and first value-callback parameter inference.
- More conservative handling of aliases, components, routes, translations, SQL parameters, local values and asArray(false); migration lifecycle/cancellation improvements.
- Independent plugin ID, settings and inspection identifiers; modern 262/Java 25 integration, regression suites, reproducible packaging and verification workflows.

### Retained from upstream
- View/template completion and navigation, view and parameter inspections/quick fixes, config-array/object-factory completion, forms/model rules and labels, database-assisted completion/inspections, translation and route support, and migration UI/commands.
- Original BSD license, source attribution and upstream history.

### Compatibility and usage
- Separate plugin identity: legacy settings are not automatically imported; conflicting Yii2 support plugins must not be enabled together.
- Static analysis runs locally without an AI account or API key. Database features require an IDE datasource; migration commands run only when explicitly invoked.
- Main workflows were manually exercised in isolated PhpStorm 2026.2.2 during release-candidate testing. Bugs may remain; please report reproducible problems through GitHub Issues.
- Dynamic PHP, arbitrary query/provider factories and some inherited/refactoring edge cases may remain unresolved. SQL-alias column completion and selected asArray result-key inference are planned, not included.


## 1.0.0-rc.6

### Added
- Highlight missing relation documentation on the class PHPDoc or class name. Offer quick fixes from the getter (one relation) and class (all missing relations).
- Add fully qualified model types with null for hasOne and array types for hasMany; preserve existing documentation and skip ambiguous result shapes. Source changes occur only when the user invokes a quick fix.

## 1.0.0-rc.5

### Fixed
- Include the local relation PHPDoc property in Find Usages from its getter, respecting the search scope.
- Suggest @property-read for missing relation documentation without a public setter; retain @property when a public setter exists. Existing PHPDoc is never rewritten automatically.

## 1.0.0-rc.4

### Changed
- Keep Rename blocked in relation strings while restoring model-aware Find Usages from getters and PHPDoc properties.
- Pair getter and local PHPDoc property renames in the native PHP refactoring operation, with preview; update relation path segments without changing suffixes or JOIN aliases.
- Suggest a missing class PHPDoc property for confirmed relations with a weak inspection; no source changes or synthetic property injection happen automatically.
- Full IDE behavior of the coordinated workflow still requires runtime validation.

## 1.0.0-rc.3

### Changed
- Keep relation strings as navigation-only links. Disable Rename from relation strings and leave them unchanged when a getter is renamed.
- Remove relation getter usage-search integration and the RC.2 PHPDoc rename workflow. Completion and navigation remain available.

## 1.0.0-rc.2

### Fixed
- Route Rename from an ActiveRecord relation string through a dialog instead of an inplace method template that can insert a getter name into the string.
- Require a getter name and include matching local PHPDoc relation properties in the same refactoring.
- Preserve native PHP method refactoring; isolated IDE verification of this fix is pending.

## 1.0.0-rc.1

### Changed
- Start the independent Yii2 Insight version sequence at 1.0.0-rc.1; the earlier 2.0.0-rc.1 identity build was never published.
- Rename the independent plugin to Yii2 Insight, maintained by by_scrimm.
- Move Java sources and tests to io.github.byscrimm.yii2insight; use that independent plugin ID.
- Namespace settings, inspections, actions, indexes and templates; package yii2-insight ZIPs.
- Preserve upstream authorship, BSD license and AI development disclosure.

### Breaking changes
- This is a separate plugin identity, not an in-place update of Yii2 Support Extended.
- Project settings start in yii2-insight.xml; legacy yii2settings.xml is never imported or overwritten automatically.
- Legacy inspection profile selections, suppressions and action shortcuts are not automatically migrated.
- Declare incompatibility with the known upstream plugin IDs to avoid duplicate providers and inspections.
- Full isolated-IDE validation remains pending; this is a release candidate.

## Historical development builds (not published as Yii2 Insight releases)

The sections below retain the earlier development numbering. They are not a public release sequence.

## Unreleased

- Connect the development checkout to `byscrimm/phpstorm-yii2-support` while preserving upstream history.
- Add English/Russian project descriptions, feature and compatibility documentation, and explicit fork/AI-development attribution.
- Add agent instructions, shared project memory, contributor/release guidance and pull request reporting.
- Correct Renovate fork/default-branch configuration, remove inherited reviewer/automerge settings, and bound CI runtime/concurrency.
- Preserve upstream changelog separately in [docs/UPSTREAM_CHANGELOG.md](docs/UPSTREAM_CHANGELOG.md).

> Sections below (1.1.0-*) use the pre-rename local numbering. They were never published; see docs/IDENTITY.md.

## 1.1.0-rc.3

### Added

- Repository governance, descriptive branch names, Conventional Commit PR titles, issue forms and release evidence requirements.
- CI/release reports with ZIP integrity checks, checksums, actual test counts and explicit unmeasured metrics.
- Tag-based draft GitHub Releases with build provenance and a stable-release gate tied to IDE evidence for the exact ZIP.
- Original BSD license, notices and author credits inside the plugin distribution.

### Fixed

- Replace the internal database delegate API with the public data-source accessor, preserving migration refresh behavior.
- Pass the project to the view-call popup constructor to avoid the constructor scheduled for removal.

### Included from earlier local increments

- PhpStorm 2026.2 / Java 25 migration, safer SQL fixes, migration lifecycle, aliases, components, routes and translations.
- ActiveRecord relation completion/navigation/rename and GridView/DetailView model and callback support.
- Docker build, regression suites and searchable settings index. Detailed changes remain in the historical sections below.

### Limitations

- This is a release candidate. Full IDE behavior and performance verification remain pending.
- Marketplace identity migration and third-party asset/license review are required before a separate Marketplace listing.
- No GitHub or Marketplace publication is implied by this changelog entry.

## 1.1.0 (local build, not published)

- Adopt semantic versions without automatic RC suffixes; retain historical prerelease reports.
- Use descriptive feature/fix/build branch names for AI-maintained development.
- Fix Gradle verifier IDE configuration that prevented all Gradle builds.
- Verify Docker check/buildPlugin on Linux arm64, including both regression suites and searchable options.
- Export Docker build checksums, build properties and test reports alongside the installable ZIP.

## 1.1.0-rc.2

- Replace fixed-depth widget PSI heuristics with structural GridView/DetailView contexts.
- Infer GridView rows from ActiveDataProvider queries, local variables and named constructor/widget arguments; support widget/provider subclasses.
- Complete public/PHPDoc attributes, readable getters and nested ActiveRecord relation paths; use cursor-aware attribute:format:label parsing.
- Use filterModel for filterAttribute without confusing it with provider row types.
- Infer only the first value callback parameter, including arrow functions; preserve explicit/PHPDoc types and distinguish array/object query results.
- Defer callback model resolution until indices are available; remove the legacy callback type heuristic.
- Exclude labels, nested options, write-only properties, unrelated widgets, non-data columns and ambiguous queries.
- Add regression coverage using the real PHP PSI, field analysis and PHPDoc parsers from the SDK.

Full IDE completion/type-provider integration and Plugin Verifier remain unverified.

## 1.1.0-rc.1

- ActiveRecord relation completion, segment references and a search contributor for relation getter usages.
- Nested relations, JOIN aliases, callback keys, named arguments, local query variables, inherited relations, `self::class` and `static::class` relation targets.
- Conservative handling of unrelated classes, non-relation getters, SQL strings, ambiguous branches and incomplete PHP.
- Correct last-call handling of `asArray(false)` and safe PHP array-key ownership checks.
- PHP PSI regression suite using IntelliJ CoreApplicationEnvironment; real string-reference rename test.
- Updated Docker/Makefile/CI definitions, versioned ZIP checksums and build reports; deterministic ZIP entry metadata.

This is a release candidate. Local SDK tests and static ABI checks do not establish full IDE runtime compatibility. See the verification report shipped beside the ZIP.

## 1.0.0 (historical local build under the previous identity)

- Java 25 / PhpStorm 262 SDK migration, replacement of removed APIs and explicit Swing form construction.
- Alias cycle prevention, component application context, route navigation, SQL placeholder parsing and value-preserving fixes.
- Migration process/output handling and project lifecycle fixes.
- Safer translation completion and offline build tools.
