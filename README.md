# Yii2 Insight

![Yii2 Insight](img/yii2-insight-github.png)

English | [Русский](README.ru.md)

**Yii2 development tools for PhpStorm: completion, navigation, relation refactoring and PHPDoc assistance.**

Yii2 Insight is developed and maintained by **[by_scrimm](https://github.com/byscrimm)**.
It builds on [Vxdy’s Yii2 Support Extended](https://github.com/vxdy/Yii2-Support-Extended)
and [NVlad’s Yii2 Support](https://github.com/nvlad/yii2support); their authorship and
the BSD license are preserved. Fork updates are developed with AI assistance (Claude Code);
the plugin itself needs no AI account, API key or network service.

## Compatibility

- PhpStorm **2026.2.x** (tested on 2026.2.2, build `262.10315.130`); Java 25 from the IDE.
- Plugin ID `io.github.byscrimm.yii2insight`; settings in `yii2-insight.xml`.
- IntelliJ IDEA with PHP is not separately tested.

## Features

| Area | Capabilities |
| --- | --- |
| ActiveRecord relations | Complete relation names in `with`, `joinWith`, `innerJoinWith`, `getRelation`, `via`; follow dotted paths, JOIN aliases, callback keys, named arguments and supported local query variables |
| Navigation and usages | Navigate each relation segment to its getter; find relation-string usages from getter or local PHPDoc property; getter search also shows its PHPDoc property |
| Relation refactoring | Rename from getter or PHPDoc declaration coordinates method/property names and matched usages, preserving path suffixes and JOIN aliases; preview/Undo use IDE refactoring; initiating Rename inside a string is blocked |
| Relation documentation | Highlight missing class PHPDoc properties; add one relation from its getter or all missing local relations from the class; use `@property-read` without a public setter, nullable model types for `hasOne`, arrays for `hasMany` |
| Query model types | Resolve supported ActiveRecord/query chains and local values, explicit `ActiveQuery`, `one()`/`all()` and `foreach` over `each()`/`batch()`; respect array mode and a final `asArray(false)` |
| GridView and DetailView | Complete model attributes and nested relations; understand `attribute:format:label`; infer the first `value` callback parameter from the provider’s model, preserving explicit/PHPDoc parameter types; keep filter-model and row-model contexts separate |
| Views | Complete template names/parameters, navigate to templates and render calls, inspect missing templates and required/unused parameters, offer associated quick fixes and view templates |
| Configuration and components | Complete/reference properties in supported configuration arrays, constructor configs, `Yii::createObject`, widget/ActiveField configs and application components |
| Forms and models | Complete model attributes in ActiveForm and active HTML helpers, validation rules and attribute labels |
| Database and SQL | With an IDE datasource: table/column and query-condition completion, table-prefix support, ActiveRecord/table/property inspections, schema-assisted property suggestions and SQL parameter checks/fixes |
| Routes and translations | Route/action-parameter completion and controller/action navigation in supported layouts; category/message completion from PHP translation catalogs and translation parameter assistance |
| Migrations | Browse migration history; explicitly apply/undo/redo configured Yii migration commands; inspect command output and synchronize the selected IDE datasource; supported remote-interpreter integrations depend on IDE plugins/configuration |

Configure under **Settings → PHP → Yii2 Insight** (Yii root, views, datasource, migration
commands). Analysis never executes PHP or SQL; migration actions run only your configured
command, on explicit request.

Views, configuration arrays, forms, rules, database helpers, basic query types and migration
tools are inherited. Yii2 Insight adds the shared model/relation analysis, nested relation
navigation, getter/PHPDoc usages and refactoring, relation PHPDoc fixes, reworked
GridView/DetailView and callback inference. Details: [provenance map](docs/FEATURES.md).

## Examples

```php
City::find()->with('region.cities');
City::find()->joinWith(['region r']);

GridView::widget([
    'dataProvider' => new ActiveDataProvider(['query' => City::find()]),
    'columns' => ['name:text:City', 'region.name',
        ['value' => fn($model) => $model->name]],
]);
```

Navigate `region` to `City::getRegion()`. Start Rename on `getRegion()` or its PHPDoc property to update matched references; starting Rename inside the relation string is deliberately disabled. Missing relation documentation can be added with **Alt+Enter** without replacing existing comments or unrelated properties.

## Installation

1. Download `yii2-insight-<version>.zip` from [GitHub Releases](https://github.com/byscrimm/phpstorm-yii2-support/releases) (not the source ZIP).
   Until the first release is published, build the ZIP yourself (see Build).
2. **Settings → Plugins → ⚙ → Install Plugin from Disk**, choose the ZIP, restart.
3. Disable other Yii2 Support plugins. Legacy settings are not migrated.

## Limits

Analysis is conservative: dynamic PHP, custom query scopes/factories, provider mutations
and some widget configurations may stay unresolved. Rename coordinates the getter and its
local PHPDoc, not every subclass. Planned work is in the [roadmap](DEVELOPMENT_PLAN.md).
**Bugs may remain** — report them in [Issues](https://github.com/byscrimm/phpstorm-yii2-support/issues)
with IDE/plugin versions and a minimal example.

## Build

```sh
make docker                                       # Docker: Java 25, Gradle and SDK included
make docker GRADLE_ARGS=verifyPlugin              # plus official Plugin Verifier
python3 tools/build-offline.py --test --psi-test  # installed PhpStorm SDK
python3 tools/verify-offline.py
```

Output goes to `build/distributions`. Details: [Contributing](CONTRIBUTING.md),
[testing](docs/TESTING.md), [releasing](docs/RELEASING.md), [changelog](CHANGELOG.md).

## Credits and license

Thanks to [Vxdy and contributors](https://github.com/vxdy/Yii2-Support-Extended) and
[Vladislav Nikishin (NVlad) and contributors](https://github.com/nvlad/yii2support).
See [AUTHORS.md](AUTHORS.md), [NOTICE.md](NOTICE.md), [LICENSE.md](LICENSE.md) and the
[upstream changelog](docs/UPSTREAM_CHANGELOG.md). Not an official Yii or JetBrains product.
