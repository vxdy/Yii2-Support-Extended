Notes: This fork works for the current PHPStorm Version, although some Features are not working.

IntelliJ IDEA / PhpStorm Yii2 Support for PHPStorm 2023 &  PHPStorm 2024 
=====================================

Fork of https://github.com/nvlad/yii2support

Features
--------

### Views  (Currently not Working)
- View template names and parameters completion
- Add view parameters after completion
- Inspection for missing view templates
- QuickFix for missing view templates
- Jump to View file (go to declaration)
- Inspection for required and unused template parameters
- QuickFix for required and unused template parameters
- Updates path to view template when file is moved

### i18n
- Code completion
- Generate params array

### Configuration arrays
Code completion for Yii configuration arrays. Works both in configuration files and on object instantiation.
Following cases are supported:

- Array in `$config` parameter in `yii\base\Object` or its descendants constructor
- Array has a `class` key with valid class representation: fully qualified name as string, `ClassName::class` or `Class::className()`
- Array is a value of a key that corresponds to standard Yii classes (like `db`, `request`, `mailer`, and so on), and
  a file with this array is located within `config` directory
- `WidgetClass::widget()` and `WidgetClass::begin` calls in case `WidgetClass` is a descendant of `yii\base\Widget`
- `$field->widget()` method call on `yii\widgets\ActiveField` and its descendants
- Inside array in `GridView`, `columns` key
- `Yii::createObject` method

Go To Declaration, Rename, Find usages and Help popups work whenever code completion works.

### Database support
**Database connection is required**

#### Table Prefix support
Use plugin settings window to set up table prefix

#### ActiveQuery
- Code completion for `ActiveQuery` (`ActiveRecord::find()->where` for example)  
- Code completion inside `ActiveQuery` linked to ActiveRecord  
- Inspection in case if `ActiveQuery` not linked to `ActiveRecord`

#### ActiveRecord
- Code completion for `ActiveRecord` `findAll()`, `findOne()` and so on methods  
- Undetectable `ActiveRecord` table inspection  
- Code completion in relations methods  

#### Migrations
- Code completion for migrations  

#### Condition parameters
- Condition parameters code completion  
- Condition parameters inspection  

#### Properties
- Synchronize properties with database  
- Unused properties inspection
  
### Form support
- Complete model attribute for `$form->field($model, ...)` & `Html::active*($model, ...)` methods

### Validators support
- Autocomplete for arrays in model's rules() method

### Type resolution
- Yii::createMethod()
- one() and all() methods of ActiveQuery

### Migration tool
- Migrations UI Navigator
- Apply/Undo/Redo migrations from UI
- Support Remote CLI for Docker, Vagrant, VM, Remote servers
- Sync IDE DataBase schema after Apply/Undo/Redo migration(s)
- View command execution output

## Inspections
Inspections can be disabled inline `/** @noinspection MissedViewInspection */`.

### Views  (Currently not Working)
- `MissedViewInspection`  
Reports missing view templates
- `RequireParameterInspection`  
Analyzes SQL condition and checks if all declared parameters are set
- `UnusedParameterInspection`  
Detecting unused View parameters
- `ViewMissedPhpDocInspection`  
Inspect PhpDoc for incoming parameters

### Object Factory
- `ObjectFactoryMissedFieldInspection`  
Reports missing object properties

### Database
- `MissedParamInspection`  
Analyzes SQL condition and checks if all declared parameters are set
-  `PropertiesInspection`  
Detects properties do not correspond to class fields or table columns in case of ActiveRecord
- `UndetectableTableInspection`  
Finds ActiveRecord table and check if it exists in database connections
- `MissingActiveRecordInActiveQueryInspection`  
Check if ActiveQuery correctly linked to ActiveRecord

Installation
------------
- Download the .zip from Releases
- In PHPStorm go to Settings -> Plugins
- Click the three Dots next to "Installed" at the top
- Press "Install Plugin from Disk"
- Choose the Downloaded .zip File
- Click Apply

Contributing
------------
The plugin is [Open Source](LICENSE.md). You may contribute either by testing and or by sending pull requests. 

### Spreading the Word
Acknowledging and or citing the plugin is as important as direct contributions.

[Project Changelog](CHANGELOG.md)
