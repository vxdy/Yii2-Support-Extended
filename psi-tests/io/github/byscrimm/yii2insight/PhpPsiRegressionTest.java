package io.github.byscrimm.yii2insight;

import com.intellij.core.*;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocProperty;
import com.jetbrains.php.config.PhpProjectSharedConfiguration;
import com.jetbrains.php.lang.PhpFileType;
import com.jetbrains.php.lang.parser.PhpParserDefinition;
import com.jetbrains.php.lang.psi.elements.*;
import io.github.byscrimm.yii2insight.common.*;
import io.github.byscrimm.yii2insight.relations.*;
import io.github.byscrimm.yii2insight.widgetsconfig.*;
import java.util.*;

/** Real PhpStorm PHP parser/PSI in IntelliJ's Core test environment. No IDE UI or full index. */
public final class PhpPsiRegressionTest {
    private static int checks;
    private static com.intellij.mock.MockProject project;
    private static final Map<String,PhpClass> classes = new HashMap<>();
    private static final YiiModelResolver resolver = new YiiModelResolver(name -> classes.containsKey(name) ? List.of(classes.get(name)) : List.of());
    private static void eq(String test,Object expected,Object actual) {
        checks++;
        if (!Objects.equals(expected,actual)) throw new AssertionError(test+": expected "+expected+", got "+actual);
    }
    private static PsiFile php(String code) {
        PsiFile file=PsiFileFactory.getInstance(project).createFileFromText("fixture.php",PhpFileType.INSTANCE,"<?php "+code);
        for (PhpClass clazz:PsiTreeUtil.findChildrenOfType(file,PhpClass.class)) classes.put(clazz.getFQN(),clazz);
        SmartPointerManager.getInstance(project).createSmartPsiElementPointer(file);
        return file;
    }
    private static MethodReference call(PsiElement file,String name) {
        return PsiTreeUtil.findChildrenOfType(file,MethodReference.class).stream().filter(c->name.equals(c.getName())).findFirst().orElseThrow();
    }
    private static StringLiteralExpression literal(PsiFile file,String value) {
        return PsiTreeUtil.findChildrenOfType(file,StringLiteralExpression.class).stream().filter(c->value.equals(c.getContents())).findFirst().orElseThrow();
    }
    private static List<String> names(List<YiiModelResolver.Relation> relations) { return relations.stream().map(YiiModelResolver.Relation::name).sorted().toList(); }
    private static List<String> modelNames(PsiElement expression) { return resolver.models(expression).stream().map(PhpClass::getName).sorted().toList(); }
    public static void main(String[] args) {
        var disposable=Disposer.newDisposable();
        try {
            var app=new CoreApplicationEnvironment(disposable);
            app.registerFileType(PhpFileType.INSTANCE,"php"); app.registerParserDefinition(new PhpParserDefinition());
            app.addExplicitExtension(ElementManipulators.INSTANCE,StringLiteralExpression.class,new com.jetbrains.php.lang.psi.manipulators.StringLiteralManipulator());
            CoreApplicationEnvironment.registerApplicationExtensionPoint(com.intellij.psi.impl.source.tree.TreeCopyHandler.EP_NAME,com.intellij.psi.impl.source.tree.TreeCopyHandler.class);
            CoreApplicationEnvironment.registerApplicationExtensionPoint(com.intellij.openapi.extensions.ExtensionPointName.create("com.jetbrains.php.dfaStateFromAssertionProvider"), com.jetbrains.php.codeInsight.typeInference.PhpDfaStateFromAssertionProvider.class);
            CoreApplicationEnvironment.registerApplicationExtensionPoint(
                com.jetbrains.php.lang.documentation.phpdoc.parser.tags.PhpDocTagParserEP.EP_NAME,
                com.jetbrains.php.lang.documentation.phpdoc.parser.tags.PhpDocTagParserEP.class);
            for (String tag : List.of("property", "property-read", "property-write", "param")) {
                var parser = new com.jetbrains.php.lang.documentation.phpdoc.parser.tags.PhpDocTagParserEP();
                parser.tagName = tag;
                parser.implementationClass = "com.jetbrains.php.lang.documentation.phpdoc.parser.tags."
                    + (tag.equals("param") ? "PhpDocParamTagParser" : "PhpDocPropertyTagParser");
                parser.setPluginDescriptor(new com.intellij.openapi.extensions.DefaultPluginDescriptor(
                    com.intellij.openapi.extensions.PluginId.getId("com.jetbrains.php")));
                com.jetbrains.php.lang.documentation.phpdoc.parser.tags.PhpDocTagParserEP.EP_NAME.getPoint().registerExtension(parser, disposable);
            }
            CoreApplicationEnvironment.registerApplicationExtensionPoint(com.intellij.openapi.extensions.ExtensionPointName.create("com.jetbrains.php.docPrefixProvider"), com.jetbrains.php.lang.psi.resolve.types.PhpDocPrefixProvider.class);
            project=new CoreProjectEnvironment(disposable,app).getProject();
            Disposer.register(disposable,project);
            project.registerService(PhpProjectSharedConfiguration.class,new PhpProjectSharedConfiguration());
            project.registerService(com.intellij.pom.tree.TreeAspect.class,new com.intellij.pom.tree.TreeAspect());
            project.registerService(com.intellij.pom.PomModel.class,new com.intellij.pom.core.impl.PomModelImpl(project));
            arguments(); modelsAndRelations(); contexts(); references(); arrayMode(); widgets(); widgetCallbacks(); queryAliases(); incomplete();
            System.out.println("PASS: "+checks+" real PHP PSI regression checks");
        } finally { Disposer.dispose(disposable); }
    }
    private static void arguments() {
        var call=call(php("$this->render(params: compact('model'), view: 'index');"),"render");
        eq("named view reordered","'index'",PhpArguments.get(call,"view",0).getText());
        eq("named params reordered","compact('model')",PhpArguments.get(call,"params",1).getText());
        call=call(php("$this->render(params: []);"),"render");
        eq("missing named view has no positional fallback",null,PhpArguments.get(call,"view",0));
        call=call(php("$this->render('index', params: /* comment */ []);"),"render");
        eq("mixed arguments","'index'",PhpArguments.get(call,"view",0).getText());
        eq("commented named argument","[]",PhpArguments.get(call,"params",1).getText());
        call=call(php("$this->render();"),"render");
        eq("empty arguments",null,PhpArguments.get(call,"params",1));
        var config=php("namespace app; use app\\services\\Mailer as Mail; return ['class' => Mail::class];");
        eq("imported class alias","\\app\\services\\Mailer",PhpArrays.className(PsiTreeUtil.findChildOfType(config,ClassConstantReference.class)));
    }
    private static void modelsAndRelations() {
        php("namespace yii\\db; class BaseActiveRecord {} class ActiveRecord extends BaseActiveRecord {} class ActiveQuery {}");
        php("""
            namespace app;
            use yii\\db\\ActiveRecord;
            class Product extends ActiveRecord {}
            class Item extends ActiveRecord {
                public function getProduct() { return $this->hasOne(Product::class, ['id' => 'product_id']); }
            }
            class Order extends ActiveRecord {
                public function getItems() { return $this->hasMany(Item::class, ['order_id' => 'id'])->orderBy('id'); }
            }
            class User extends ActiveRecord {
                public function getOrders() { return $this->hasMany(Order::class, ['user_id' => 'id']); }
                public function getDisplayName() { return 'plain getter'; }
                protected function getSecret() { return $this->hasMany(Order::class, []); }
                public static function getStaticOrders() { return 'not a relation'; }
                public function getRequiresParameter($id) { return $this->hasOne(Order::class, []); }
                public function getClosure() { return function () { return $this->hasMany(Order::class, []); }; }
            }
            class ChildUser extends User { public function getOrders() { return []; } }
            class Other { public function getOrders() { return $this->hasMany(Order::class, []); } }
            """);
        php("namespace app; class Category extends User { public function getParent() { return $this->hasOne(self::class, []); } public function getChildren() { return $this->hasMany(static::class, []); } }");
        eq("self and static relations", List.of("children", "orders", "parent"), names(resolver.relations(classes.get("\\app\\Category"))));
        var user=classes.get("\\app\\User");
        eq("record inheritance",true,resolver.inherits(user,"\\yii\\db\\BaseActiveRecord"));
        eq("only real public relation getters",List.of("orders"),names(resolver.relations(user)));
        eq("nested relation path",List.of("product"),names(resolver.atPath(List.of(user),"orders.items")));
        eq("missing intermediate relation",List.of(),names(resolver.atPath(List.of(user),"orders.missing")));
        eq("override hides parent relation",List.of(),names(resolver.relations(classes.get("\\app\\ChildUser"))));
        eq("unrelated class",List.of(),names(resolver.relations(classes.get("\\app\\Other"))));
        var file=php("namespace app; User::find()->where(['id' => 1])->with('orders');");
        eq("static AR query chain",List.of("User"),modelNames(call(file,"with").getClassReference()));
        file=php("namespace app; $query = User::find(); $query->with('orders');");
        eq("query in local variable",List.of("User"),modelNames(call(file,"with").getClassReference()));
        file=php("namespace app; $query = User::find(); if ($test) {$query = Order::find();} $query->with('orders');");
        eq("ambiguous branch does not invent model",List.of(),modelNames(call(file,"with").getClassReference()));
        file=php("namespace app; $query = new \\yii\\db\\ActiveQuery(User::class); $query->with('orders');");
        eq("explicit ActiveQuery modelClass",List.of("User"),modelNames(call(file,"with").getClassReference()));
        file=php("namespace app; $user = new User(); $user->getOrders()->with('items');");
        eq("relation query model",List.of("Order"),modelNames(call(file,"with").getClassReference()));
        file=php("namespace app; $a=$b; $b=$a; $b->with('orders');");
        eq("cyclic local variables terminate",List.of(),modelNames(call(file,"with").getClassReference()));
        file=php("namespace app; $query=User::find(); $query=$query->where([]); $query->with('orders');");
        eq("self assignment preserves previous query",List.of("User"),modelNames(call(file,"with").getClassReference()));
        file=php("namespace app; (new User())->getRelation('orders');");
        eq("parenthesized new record",List.of("User"),modelNames(call(file,"getRelation").getClassReference()));
        file=php("namespace app; (User::find())->with('orders');");
        eq("parenthesized query chain",List.of("User"),modelNames(call(file,"with").getClassReference()));
        file=php("namespace app; (new Other())->getRelation('orders');");
        eq("parenthesized non-record",List.of(),modelNames(call(file,"getRelation").getClassReference()));
    }
    private static void contexts() {
        var file=php("namespace app; User::find()->joinWith(['orders o' => function($q) { $q->where(['status'=>'active']); }], false, 'LEFT JOIN');");
        var entry = PsiTreeUtil.getParentOfType(literal(file,"orders o"),ArrayHashElement.class);
        eq("array key extraction", "orders o", PhpArrays.string(entry.getKey()));
        eq("JOIN alias context",true,RelationContext.of(literal(file,"orders o")).allowAlias());
        eq("SQL key is not relation",null,RelationContext.of(literal(file,"status")));
        eq("SQL value is not relation",null,RelationContext.of(literal(file,"active")));
        eq("joinType is not relation",null,RelationContext.of(literal(file,"LEFT JOIN")));
        file=php("namespace app; User::find()->with(['orders' => ['notRelation']]);");
        eq("callback nested value is not relation",null,RelationContext.of(literal(file,"notRelation")));
        eq("callback array key is relation",true,RelationContext.of(literal(file,"orders")) != null);
        file=php("namespace app; User::find()->with(['orders' => 'callbackName']);");
        eq("callback value is not relation",null,RelationContext.of(literal(file,"callbackName")));
        file=php("namespace app; User::find()->with('orders', 'orders.items');");
        eq("variadic with",true,RelationContext.of(literal(file,"orders.items")) != null);
        file=php("namespace app; User::find()->joinWith(eagerLoading: true, with: ['orders']);");
        eq("named joinWith",true,RelationContext.of(literal(file,"orders")) != null);
        file=php("namespace app; class PivotUser extends User { public function getProducts() { return $this->hasMany(Product::class,[])->via('orders'); } }");
        eq("via uses declaring model",List.of("PivotUser"),modelNames(RelationContext.of(literal(file,"orders")).modelExpression()));
    }
    private static void references() {
        var file = php("namespace app; User::find()->joinWith(['orders.items.product AS p']);");
        var str = literal(file,"orders.items.product AS p");
        var references = RelationReferenceContributor.references(str,resolver);
        eq("one reference per segment",3,references.length);
        eq("first segment range", "orders", references[0].getRangeInElement().substring(str.getText()));
        eq("last segment range excludes alias", "product", references[2].getRangeInElement().substring(str.getText()));
        for (int i=0;i<references.length;i++) {
            var target = references[i].resolve();
            eq("segment target "+i, List.of("getOrders","getItems","getProduct").get(i), target instanceof Method m ? m.getName() : null);
        }
        eq("rename target is the selected nested getter", "getItems", RelationRenameHandler.target(str, references[1].getRangeInElement().getStartOffset(), resolver).getName());
        eq("JOIN alias does not start relation rename", null, RelationRenameHandler.target(str, str.getText().indexOf("AS p") + 3, resolver));
        final var ref = references[1];
        var target = ref.resolve();
        eq("relation link participates in getter usage search", true, ref.isReferenceTo(target));
        var renamed = ref.handleElementRename("getLines");
        eq("getter rename changes one segment and preserves alias", "'orders.lines.product AS p'", renamed.getText());

        var documented = php("namespace app; /** @property Order[] $orders Description stays.\n * @property int $orders_id */ class DocumentedUser extends User { public function getOrders() { return $this->hasMany(Order::class, []); } }");
        var getter = PsiTreeUtil.findChildrenOfType(documented, Method.class).stream().filter(m -> m.getName().equals("getOrders")).findFirst().orElseThrow();
        var docProperty = RelationSymbols.properties(getter).getFirst();
        var docReferences = RelationReferencesSearch.documentationReferences(getter);
        eq("getter search includes its PHPDoc property", 1, docReferences.size());
        eq("PHPDoc usage resolves to getter", getter, docReferences.getFirst().resolve());
        eq("PHPDoc usage is located on property", docProperty, docReferences.getFirst().getElement());
        docReferences.getFirst().handleElementRename("getPurchases");
        eq("getter usage does not independently rename PHPDoc to method spelling", "orders", docProperty.getName());
        eq("getter-only relation suggests property-read", "@property-read", RelationPhpDocInspection.suggestedTag(getter, resolver));
        var writable = php("namespace app; class WritableRelation extends User { public function setOrders($value) {} }");
        var inherited = php("namespace app; class WritableChild extends WritableRelation { public function getOrders() { return $this->hasMany(Order::class, []); } }");
        var writableGetter = PsiTreeUtil.findChildOfType(inherited, Method.class);
        eq("inherited setter keeps property writable", "@property", RelationPhpDocInspection.suggestedTag(writableGetter, resolver));
        var readDoc = php("namespace app; /** @property-read Order[] $orders */ class ReadDocumented extends User { public function getOrders() { return $this->hasMany(Order::class, []); } }");
        var readGetter = PsiTreeUtil.findChildOfType(readDoc, Method.class);
        eq("property-read appears in getter usages", 1, RelationReferencesSearch.documentationReferences(readGetter).size());
        eq("property-read satisfies missing documentation inspection", false, RelationPhpDocInspection.missing(readGetter, resolver));
        eq("PHPDoc binds to confirmed getter", getter, RelationSymbols.getter(docProperty, resolver));
        eq("unused relation name is allowed", null, RelationSymbols.conflict(getter, "getPurchases", resolver));
        eq("column name conflict is rejected", true, RelationSymbols.conflict(getter, "getOrders_id", resolver) != null);
        eq("existing inherited method conflict is rejected", true, RelationSymbols.conflict(docProperty, "displayName", resolver) != null);
        var missingFile = php("namespace app; /** Existing class description.\n * @property int $region_id Keep this column.\n */ class DocFixModel extends User { public function getRegion() { return $this->hasOne(Order::class, []); } public function getCities() { return $this->hasMany(Item::class, []); } }");
        var missingOwner = classes.get("\\app\\DocFixModel");
        eq("single relation doc uses nullable fully qualified target", List.of("@property-read \\app\\Order|null $region"), RelationPhpDocQuickFix.entries(missingOwner, "getRegion", resolver));
        eq("collection relation doc uses target array", List.of("@property-read \\app\\Item[] $cities"), RelationPhpDocQuickFix.entries(missingOwner, "getCities", resolver));
        eq("one relation quick fix applies", true, RelationPhpDocQuickFix.add(missingOwner, "getRegion", resolver));
        missingOwner = PsiTreeUtil.findChildOfType(missingFile, PhpClass.class);
        eq("single quick fix preserves class description", true, missingOwner.getDocComment().getText().contains("Existing class description."));
        eq("single quick fix preserves unrelated field comment", true, missingOwner.getDocComment().getText().contains("$region_id Keep this column."));
        eq("single quick fix does not add other missing relation", false, missingOwner.getDocComment().getText().contains("$cities"));
        eq("single quick fix does not duplicate PHPDoc", false, RelationPhpDocQuickFix.add(missingOwner, "getRegion", resolver));
        eq("class quick fix adds remaining local relations", true, RelationPhpDocQuickFix.add(missingOwner, null, resolver));
        var bare = php("namespace app; class BareDocFix extends User { public function getRegion() { return $this->hasOne(Order::class, []); } }");
        var bareOwner = PsiTreeUtil.findChildOfType(bare, PhpClass.class);
        eq("quick fix creates absent class PHPDoc", true, RelationPhpDocQuickFix.add(bareOwner, null, resolver));
        eq("new PHPDoc is attached to class", true, bareOwner.getDocComment() != null);
        eq("new PHPDoc contains relation", true, bareOwner.getDocComment().getText().contains("$region"));
        eq("class fix does not duplicate newly created documentation", false, RelationPhpDocQuickFix.add(bareOwner, null, resolver));
        eq("existing PHPDoc indentation is preserved", "/**\n     * Existing.\n     * @property-read X $x\n     */", RelationPhpDocQuickFix.append("/**\n     * Existing.\n     */", List.of("@property-read X $x")));
        var uncertain = php("namespace app; class UncertainDoc extends User { public function getMixed() { if ($flag) { return $this->hasOne(Order::class, []); } return $this->hasMany(Order::class, []); } public function getArrayRow() { return $this->hasOne(Order::class, [])->asArray(); } }");
        eq("ambiguous cardinality and array mode do not invent property types", List.of(), RelationPhpDocQuickFix.entries(PsiTreeUtil.findChildOfType(uncertain, PhpClass.class), null, resolver));
        var paired = new LinkedHashMap<PsiElement,String>();
        RelationSymbols.prepare(getter, "getPurchases", paired, resolver);
        eq("getter rename includes getter and property only", 2, paired.size());
        eq("getter rename maps PHPDoc without prefix", "purchases", paired.get(docProperty));
        paired.clear();
        RelationSymbols.prepare(docProperty, "purchases", paired, resolver);
        eq("property rename maps getter prefix", "getPurchases", paired.get(getter));
        eq("documented relation has no missing PHPDoc suggestion", false, RelationPhpDocInspection.missing(getter, resolver));
        eq("undocumented relation suggests PHPDoc", true, RelationPhpDocInspection.missing(Arrays.stream(classes.get("\\app\\User").getOwnMethods()).filter(m -> m.getName().equals("getOrders")).findFirst().orElseThrow(), resolver));
        eq("ordinary getter has no relation PHPDoc suggestion", false, RelationPhpDocInspection.missing(Arrays.stream(classes.get("\\app\\User").getOwnMethods()).filter(m -> m.getName().equals("getDisplayName")).findFirst().orElseThrow(), resolver));
        var incomplete = php("namespace app; class IncompleteRelation extends User { public function getUnknown() { return $this->hasMany($dynamic, []); } }");
        var unknown = PsiTreeUtil.findChildOfType(incomplete, Method.class);
        eq("dynamic target has no PHPDoc suggestion", false, RelationPhpDocInspection.missing(unknown, resolver));
        paired.clear();
        RelationSymbols.prepare(unknown, "getAnother", paired, resolver);
        eq("unknown relation does not create rename pairing", 0, paired.size());
        var usageFile = php("namespace app; DocumentedUser::find()->joinWith(['orders.items AS o']);");
        var usage = RelationReferenceContributor.references(literal(usageFile,"orders.items AS o"),resolver)[0];
        eq("Find Usages from PHPDoc matches relation string", true, usage.isReferenceTo(docProperty));
        eq("Find Usages from getter matches relation string", true, usage.isReferenceTo(getter));
        eq("same relation name on another model does not match", false, usage.isReferenceTo(Arrays.stream(classes.get("\\app\\User").getOwnMethods()).filter(m -> m.getName().equals("getOrders")).findFirst().orElseThrow()));
        var propertyUsage = RelationReferencesSearch.propertyReference(usage, docProperty);
        eq("PHPDoc result navigates to property", docProperty, propertyUsage.resolve());
        var changed = propertyUsage.handleElementRename("purchases");
        eq("PHPDoc rename preserves nested path and alias", "'purchases.items AS o'", changed.getText());
        changed = usage.handleElementRename("getPurchases");
        eq("paired renames are idempotent", "'purchases.items AS o'", changed.getText());
        var renamedDoc = docProperty.setName("purchases");
        eq("PHPDoc PSI rename changes property name", "purchases", ((com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocProperty) renamedDoc).getName());
        eq("PHPDoc rename preserves description", true, documented.getText().contains("Description stays."));
        eq("PHPDoc rename preserves column property", true, documented.getText().contains("$orders_id"));
        file = php("namespace app; Other::find()->with('orders');");
        eq("unrelated receiver has no references",0,RelationReferenceContributor.references(literal(file,"orders"),resolver).length);
    }
    private static void arrayMode() {
        var provider = new io.github.byscrimm.yii2insight.typeprovider.ActiveRecordTypeProvider();
        for (String chain : List.of("asArray()", "asArray(true)", "asArray(value: true)", "asArray($flag)", "asArray(false)->asArray()")) {
            var file=php("namespace app; User::find()->"+chain+"->one();");
            eq("array result stays with native provider: "+chain,null,provider.getType(call(file,"one")));
        }
        for (String method : List.of("each", "batch")) {
            var rows=php("namespace app; User::find()->asArray()->"+method+"(100);");
            eq("array rows stay with native provider: "+method,null,provider.getType(call(rows,method)));
        }
        var file=php("namespace app; User::find()->asArray()->asArray(value: false)->one();");
        eq("last asArray(false) restores object mode",false,io.github.byscrimm.yii2insight.typeprovider.ActiveRecordTypeProvider.usesArrayResult(call(file,"one")));
    }

    private static final WidgetModelResolver widgets = new WidgetModelResolver(resolver);
    private static List<String> widgetModels(PsiFile file, String value) {
        return widgets.models(WidgetContext.of(literal(file,value))).stream().map(PhpClass::getName).sorted().toList();
    }
    private static void widgets() {
        php("namespace yii\\base; class Model {}");
        php("namespace yii\\grid; class GridView {} class DataColumn {} class ActionColumn {}");
        php("namespace yii\\widgets; class DetailView {}");
        php("namespace yii\\data; class ActiveDataProvider {} class ArrayDataProvider {}");
        php("""
            namespace gridtest;
            class Profile extends \\yii\\db\\ActiveRecord { public $city; }
            class User extends \\yii\\db\\ActiveRecord {
                public $email;
                private $password;
                public static $cache;
                public function getProfile() { return $this->hasOne(Profile::class, []); }
                public function getDisplayName() { return 'display'; }
                protected function getPrivateName() { return 'hidden'; }
                public function getRequiresArg($id) { return 'hidden'; }
            }
            class Search extends \\yii\\base\\Model { public $searchTerm; }
            class ChildUser extends User { public $other; }
            class Grid extends \\yii\\grid\\GridView {}
            class Detail extends \\yii\\widgets\\DetailView {}
            class Provider extends \\yii\\data\\ActiveDataProvider {}
            class Column extends \\yii\\grid\\DataColumn {}
            class Other { public static function widget($config) {} }
            """);
        var file=php("namespace gridtest; Grid::widget(['dataProvider'=>new Provider(['query'=>User::find()]),'columns'=>['email']]);");
        eq("grid model from provider query",List.of("User"),widgetModels(file,"email"));
        var context=WidgetContext.of(literal(file,"email"));
        eq("grid attributes and readable getters",List.of("displayName","email","profile"),widgets.attributes(widgets.models(context),"").stream().map(WidgetModelResolver.Attribute::name).sorted().toList());
        eq("nested relation attributes",List.of("city"),widgets.attributes(widgets.models(context),"profile").stream().map(WidgetModelResolver.Attribute::name).toList());
        eq("unknown relation path stays empty",List.of(),widgets.attributes(widgets.models(context),"unknown"));
        eq("malformed relation path stays empty",List.of(),widgets.attributes(widgets.models(context),"profile..x"));
        file=php("namespace gridtest; $q=User::find()->where([]); $conf=['query'=>$q]; $p=new Provider(config:$conf); Grid::widget(config:['dataProvider'=>$p,'columns'=>[['attribute'=>'email']]]);");
        eq("named configs and provider/query variables",List.of("User"),widgetModels(file,"email"));
        file=php("namespace gridtest; Grid::widget(['filterModel'=>new Search(),'dataProvider'=>new Provider(['query'=>User::find()]),'columns'=>[['attribute'=>'email','filterAttribute'=>'searchTerm']]]);");
        eq("provider model precedes filterModel",List.of("User"),widgetModels(file,"email"));
        eq("filterAttribute uses filter model",List.of("Search"),widgetModels(file,"searchTerm"));
        file=php("namespace gridtest; Grid::widget(['filterModel'=>new Search(),'columns'=>['searchTerm']]);");
        eq("filterModel attribute fallback preserved",List.of("Search"),widgetModels(file,"searchTerm"));
        file=php("namespace gridtest; $model=new ChildUser(); Detail::widget(['model'=>$model,'attributes'=>['email']]);");
        eq("DetailView local model",List.of("ChildUser"),widgetModels(file,"email"));
        context=WidgetContext.of(literal(file,"email"));
        eq("inherited attributes",true,widgets.attributes(widgets.models(context),"").stream().anyMatch(a->a.name().equals("email")));
        file=php("namespace gridtest; Detail::widget(['model'=>new Search(),'attributes'=>[['attribute'=>'searchTerm','value'=>'display text']]]);");
        eq("DetailView non-AR model",List.of("Search"),widgetModels(file,"searchTerm"));
        eq("DetailView literal value is display text",false,widgets.supports(WidgetContext.of(literal(file,"display text"))));
        for (String row : List.of("['label'=>'target']", "['contentOptions'=>['class'=>'target']]", "['format'=>['date','target']]", "['filter'=>['target']]")) {
            file=php("namespace gridtest; Grid::widget(['filterModel'=>new Search(),'columns'=>["+row+"]]);");
            eq("unrelated column string excluded: "+row,null,WidgetContext.of(literal(file,"target")));
        }
        file=php("namespace gridtest; Other::widget(['dataProvider'=>new Provider(['query'=>User::find()]),'columns'=>['email']]);");
        eq("unrelated widget excluded",List.of(),widgetModels(file,"email"));
        file=php("namespace gridtest; Grid::widget(['dataProvider'=>new Provider(['query'=>User::find()]),'columns'=>[['class'=>\\yii\\grid\\ActionColumn::class,'attribute'=>'email']]]);");
        eq("non-data custom column excluded",List.of(),widgetModels(file,"email"));
        file=php("namespace gridtest; Grid::widget(['dataProvider'=>new Provider(['query'=>User::find()]),'columns'=>[['class'=>Column::class,'attribute'=>'email']]]);");
        eq("DataColumn subclass supported",List.of("User"),widgetModels(file,"email"));
        file=php("namespace gridtest; $q=User::find(); if ($flag) {$q=Profile::find();} Grid::widget(['dataProvider'=>new Provider(['query'=>$q]),'columns'=>['email']]);");
        eq("ambiguous query rejected",List.of(),widgetModels(file,"email"));
        file=php("namespace gridtest; $p=$q; $q=$p; Grid::widget(['dataProvider'=>$p,'columns'=>['email']]);");
        eq("cyclic provider variables terminate",List.of(),widgetModels(file,"email"));
        file=php("namespace gridtest; Grid::widget(['dataProvider'=>new Provider(['query'=>User::find()->asArray()]),'columns'=>[['value'=>'profile.city']]]);");
        eq("string GridView value is attribute path",List.of("User"),widgetModels(file,"profile.city"));
        file=php("namespace gridtest; Detail::widget(['model'=>new Search(),'attributes'=>[['target']]]);");
        eq("structural option context",WidgetContext.Kind.OPTION,WidgetContext.of(literal(file,"target")).kind());
        file=php("namespace gridtest; Grid::widget(['filterModel'=>new Search(),'columns'=>[['format'=>'date']]]);");
        eq("explicit format context",WidgetContext.Kind.FORMAT,WidgetContext.of(literal(file,"date")).kind());
        php("namespace gridtest; /** @property string $nickname */ class Documented extends User {}");
        eq("PHPDoc attributes retained",true,widgets.attributes(List.of(classes.get("\\gridtest\\Documented")),"").stream().anyMatch(a->a.name().equals("nickname")));
        for (String query:List.of("User::find()->one()", "User::find()->all()", "new User()")) {
            file=php("namespace gridtest; Grid::widget(['dataProvider'=>new Provider(['query'=>"+query+"]),'columns'=>['email']]);");
            eq("provider needs query rather than fetched rows: "+query,List.of(),widgetModels(file,"email"));
        }
        file=php("namespace gridtest; Detail::widget(['model'=>User::find()->one(),'attributes'=>['email']]);");
        eq("DetailView find one model",List.of("User"),widgetModels(file,"email"));
        for (String model:List.of("User::find()->all()", "User::find()->asArray()->one()", "User::find()")) {
            file=php("namespace gridtest; Detail::widget(['model'=>"+model+",'attributes'=>['email']]);");
            eq("DetailView does not treat query/array as row: "+model,List.of(),widgetModels(file,"email"));
        }
        php("namespace gridtest; /**\n * @property-read string $visibleName\n * @property-write string $writeOnly\n */ class AccessDoc extends User {}");
        var docAttrs=widgets.attributes(List.of(classes.get("\\gridtest\\AccessDoc")),"").stream().map(WidgetModelResolver.Attribute::name).toList();
        eq("read-only PHPDoc field is an attribute",true,docAttrs.contains("visibleName"));
        eq("write-only PHPDoc field excluded",false,docAttrs.contains("writeOnly"));
        eq("shorthand format after colon",new WidgetAttributePosition(true,"","da"),WidgetAttributePosition.parse("email:date:Label",8,true));
        eq("caret in attribute before format",new WidgetAttributePosition(false,"","em"),WidgetAttributePosition.parse("email:date:Label",2,true));
        eq("label never completed",null,WidgetAttributePosition.parse("email:date:Label",15,true));
        eq("explicit attribute has no format suffix",null,WidgetAttributePosition.parse("email:da",8,false));
        eq("nested prefix",new WidgetAttributePosition(false,"profile","ci"),WidgetAttributePosition.parse("profile.ci",10,true));
        eq("double dot prefix rejected",null,WidgetAttributePosition.parse("profile..ci",11,true));
    }
    private static Parameter parameter(PsiFile file,String name) {
        return PsiTreeUtil.findChildrenOfType(file,Parameter.class).stream().filter(p->name.equals(p.getName())).findFirst().orElseThrow();
    }
    private static String callbackType(PsiFile file, String name) {
        var type=WidgetCallbackTypeProvider.infer(parameter(file,name), widgets);
        return type==null ? null : String.join("|",type.getTypes());
    }
    private static void widgetCallbacks() {
        var file=php("namespace gridtest; Grid::widget(['dataProvider'=>new Provider(['query'=>User::find()]),'columns'=>[['value'=>function($model,$key,$index,$column){return $model->email;}]]]);");
        eq("Grid callback first parameter type","\\gridtest\\User",callbackType(file,"model"));
        for (String name:List.of("key","index","column")) eq("Grid callback other parameter untouched: "+name,null,callbackType(file,name));
        var provider=new WidgetCallbackTypeProvider();
        var physical=PsiFileFactory.getInstance(project).createFileFromText("callback.php",PhpFileType.INSTANCE,file.getText(),0,true);
        SmartPointerManager.getInstance(project).createSmartPsiElementPointer(physical);
        eq("index phase emits deferred signature without PhpIndex",true,provider.getType(parameter(physical,"model"))!=null);
        eq("index phase excludes second parameter",null,provider.getType(parameter(file,"key")));
        var legacy=new io.github.byscrimm.yii2insight.typeprovider.YiiTypeProvider();
        eq("legacy callback heuristic removed",null,legacy.getType(parameter(file,"key")));
        file=php("namespace gridtest; $query=User::find(); $provider=new Provider(['query'=>$query]); Grid::widget(['dataProvider'=>$provider,'columns'=>[['value'=>fn($row)=>$row->email]]]);");
        eq("arrow function model parameter","\\gridtest\\User",callbackType(file,"row"));
        file=php("namespace gridtest; $model=new Search(); Detail::widget(['model'=>$model,'attributes'=>[['value'=>function($row,$widget){return $row->searchTerm;}]]]);");
        eq("Detail callback first parameter","\\gridtest\\Search",callbackType(file,"row"));
        eq("Detail widget parameter untouched",null,callbackType(file,"widget"));
        file=php("namespace gridtest; Grid::widget(['filterModel'=>new Search(),'columns'=>[['value'=>fn($row)=>$row->email]]]);");
        eq("filterModel never becomes callback row",null,callbackType(file,"row"));
        for (String chain:List.of("asArray()","asArray(true)","asArray(value:true)","asArray($flag)")) {
            file=php("namespace gridtest; Grid::widget(['dataProvider'=>new Provider(['query'=>User::find()->"+chain+"]),'columns'=>[['value'=>fn($row)=>$row['email']]]]);");
            eq("callback array rows "+chain,chain.contains("$flag") ? null : "\\array",callbackType(file,"row"));
        }
        file=php("namespace gridtest; $query=User::find()->asArray(); $query=$query->asArray(value:false); Grid::widget(['dataProvider'=>new Provider(['query'=>$query]),'columns'=>[['value'=>fn($row)=>$row->email]]]);");
        eq("callback final false restores model","\\gridtest\\User",callbackType(file,"row"));
        for (String callback:List.of("fn(Search $row)=>$row", "function(...$row){return $row;}", "function($outer){return fn($row)=>$row;}")) {
            file=php("namespace gridtest; Grid::widget(['dataProvider'=>new Provider(['query'=>User::find()]),'columns'=>[['value'=>"+callback+"]]]);");
            eq("explicit/variadic/nested callback excluded: "+callback,null,callbackType(file,"row"));
        }
        file=php("namespace gridtest; Grid::widget(['dataProvider'=>new Provider(['query'=>User::find()]),'columns'=>[['value'=>/** @param Search $row */ function($row){return $row;}]]]);");
        eq("callback PHPDoc type preserved",null,callbackType(file,"row"));
        file=php("namespace gridtest; Grid::widget(['filterModel'=>new Search(),'dataProvider'=>new \\yii\\data\\ArrayDataProvider([]),'columns'=>[['value'=>fn($row)=>$row]]]);");
        eq("unknown array provider callback is not filter model",null,callbackType(file,"row"));
        file=php("namespace gridtest; Other::widget(['dataProvider'=>new Provider(['query'=>User::find()]),'columns'=>[['value'=>fn($row)=>$row]]]);");
        eq("unrelated widget callback excluded",null,callbackType(file,"row"));
        file=php("namespace gridtest; Grid::widget(['dataProvider'=>new Provider(['query'=>User::find()]),'columns'=>[['contentOptions'=>['value'=>fn($row)=>$row]]]]);");
        eq("nested unrelated value callback excluded",null,callbackType(file,"row"));
    }
    private static Map<String,List<String>> aliasNames(PsiFile file, String column) {
        var call = QueryAliases.columnCall(literal(file,column));
        if (call == null) return null;
        Map<String,List<String>> result = new TreeMap<>();
        new QueryAliases(resolver).visible(call).forEach((k,v) -> result.put(k, v.stream().map(PhpClass::getName).sorted().toList()));
        return result;
    }
    private static void queryAliases() {
        php("""
            namespace shop;
            use yii\\db\\ActiveRecord;
            /** @property int $id\n * @property string $name */
            class Region extends ActiveRecord {}
            /** @property int $id\n * @property int $region_id\n * @property-read Region $region */
            class City extends ActiveRecord {
                public function getRegion() { return $this->hasOne(Region::class, ['id' => 'region_id']); }
            }
            /** @property int $id\n * @property int $city_id */
            class AddressBase extends ActiveRecord {}
            /** @property string $street */
            class Address extends AddressBase {
                public function getCity() { return $this->hasOne(City::class, ['id' => 'city_id']); }
                public function getOtherCity() { return $this->hasOne(City::class, ['id' => 'other_id']); }
                public function getInfo() { return 'virtual'; }
            }
            """);
        eq("joinWith spec", List.of("city.region","r"), Arrays.asList(QueryAliases.spec("city.region r")));
        eq("joinWith AS spec", List.of("city","c"), Arrays.asList(QueryAliases.spec("city AS c")));
        eq("joinWith without alias", Arrays.asList("city", null), Arrays.asList(QueryAliases.spec("city")));
        eq("SQL expression is not a spec", null, QueryAliases.spec("city c, x"));
        var file = php("namespace shop; Address::find()->alias('a')->joinWith(['city c', 'city.region AS r'])->andWhere(['c.id' => 1, 'a.street' => 'x']);");
        eq("main, joined and nested aliases", Map.of("a",List.of("Address"),"c",List.of("City"),"r",List.of("Region")), aliasNames(file,"c.id"));
        eq("hash value is not a condition column", null, aliasNames(file,"x"));
        file = php("namespace shop; Address::find()->where('c.') ->joinWith('city c');");
        eq("joins after the condition still apply", Map.of("c",List.of("City")), aliasNames(file,"c."));
        file = php("namespace shop; Address::find()->joinWith(['city' => function ($q) { $q->alias('town')->andWhere(['town.id' => 1, 'a.id' => 2]); }])->alias('a')->select(['street' => 'town.name']);");
        eq("callback alias names the joined table", Map.of("a",List.of("Address"),"town",List.of("City")), aliasNames(file,"town.name"));
        eq("callback sees its own and outer aliases", Map.of("a",List.of("Address"),"town",List.of("City")), aliasNames(file,"town.id"));
        eq("select key is a result name", null, aliasNames(file,"street"));
        file = php("namespace shop; $q = Address::find()->joinWith('city c'); $q->andWhere(['=', 'c.region_id', 'value']);");
        eq("local query variable and operator column", Map.of("c",List.of("City")), aliasNames(file,"c.region_id"));
        eq("operator value is not a column", null, aliasNames(file,"value"));
        file = php("namespace shop; Address::find()->joinWith('city c')->where(['and', ['c.id' => 1], ['like', 'c.region_id', 'x']])->orderBy(['c.id' => SORT_ASC])->groupBy(['c.region_id']);");
        eq("logical operand hash key", Map.of("c",List.of("City")), aliasNames(file,"c.id"));
        eq("logical operand operator column", Map.of("c",List.of("City")), aliasNames(file,"c.region_id"));
        file = php("namespace shop; Address::find()->joinWith(['city x', 'otherCity x'])->where('x.id');");
        eq("same alias for one model is kept", Map.of("x",List.of("City")), aliasNames(file,"x.id"));
        file = php("namespace shop; Address::find()->alias('x')->joinWith('city x')->where('x.id');");
        eq("conflicting alias is dropped", Map.of(), aliasNames(file,"x.id"));
        file = php("namespace shop; Address::find()->alias($name)->joinWith('city c')->where('c.id');");
        eq("dynamic alias stops analysis", Map.of(), aliasNames(file,"c.id"));
        file = php("namespace shop; Address::find()->customScope()->joinWith('city c')->where('c.id');");
        eq("unknown query method stops analysis", Map.of(), aliasNames(file,"c.id"));
        file = php("namespace shop; Address::find()->joinWith('missing m')->where('m.id');");
        eq("unknown relation adds no alias", Map.of(), aliasNames(file,"m.id"));
        file = php("namespace shop; Address::find()->joinWith('city c')->where(['in', 'id', (new \\yii\\db\\Query())->select('c.inner')->from('t')]);");
        eq("subquery does not inherit outer aliases", Map.of(), aliasNames(file,"c.inner"));
        file = php("namespace shop; Address::find()->joinWith('city c')->limit('c.id');");
        eq("non-column method", null, aliasNames(file,"c.id"));
        var attributes = new QueryAliases(resolver).attributes(classes.get("\\shop\\City")).stream().map(PhpDocProperty::getName).sorted().toList();
        eq("relation property is not a column", List.of("id","region_id"), attributes);
        attributes = new QueryAliases(resolver).attributes(classes.get("\\shop\\Address")).stream().map(PhpDocProperty::getName).sorted().toList();
        eq("inherited documented columns", List.of("city_id","id","street"), attributes);
    }
    private static void incomplete() {
        for (String code:List.of("$x->with('","$x->with(['orders' =>", "$this->hasMany(", "$this->render(params:","$x->joinWith(['orders.", "User::find()->joinWith(['orders' => function($q) { $q->alias(", "User::find()->where(['and', ['","$a=$a; $a->with('x');", "\\yii\\grid\\GridView::widget(['columns'=>[['attribute'=>'", "\\yii\\widgets\\DetailView::widget(['model'=>new", "\\yii\\grid\\GridView::widget(['columns'=>[['value'=>fn($row)=>")) {
            var file=php(code);
            for (var str:PsiTreeUtil.findChildrenOfType(file,StringLiteralExpression.class)) {
                RelationContext.of(str);
                var columnCall = QueryAliases.columnCall(str); if (columnCall != null) new QueryAliases(resolver).visible(columnCall);
                widgets.models(WidgetContext.of(str));
            }
            for (var param:PsiTreeUtil.findChildrenOfType(file,Parameter.class)) WidgetCallbackTypeProvider.infer(param,widgets);
            for (var ref:PsiTreeUtil.findChildrenOfType(file,MethodReference.class)) resolver.models(ref.getClassReference());
            eq("incomplete PHP handled: "+code,true,true);
        }
    }
}
