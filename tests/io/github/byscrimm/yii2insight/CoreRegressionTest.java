package io.github.byscrimm.yii2insight;

import io.github.byscrimm.yii2insight.common.*;
import io.github.byscrimm.yii2insight.migrations.commands.OutputLines;
import java.util.*;

/** Dependency-free regressions, also invoked by the Gradle check task. */
public final class CoreRegressionTest {
    private static int checks;
    private static void equal(Object expected, Object actual) {
        checks++;
        if (!Objects.equals(expected, actual)) throw new AssertionError("Expected " + expected + ", got " + actual);
    }
    public static void main(String[] args) {
        Map<String,String> aliases = Map.of("@app", "backend", "@webroot", "@app/web", "@app/runtime", "/tmp/runtime");
        equal("backend", AliasResolver.resolve(aliases, "@app"));
        equal(List.of("\\app\\User", "null"), QueryResultTypes.types("\\app\\User", "#\u0856\\app\\User.find.one"));
        equal(List.of("\\app\\User[]"), QueryResultTypes.types("\\app\\User", "\\app\\User.find.all"));
        equal(List.of("\\yii\\db\\BatchQueryResult", "\\app\\User[]"), QueryResultTypes.types("\\app\\User", "\\app\\User.find.where.each"));
        equal(List.of("\\yii\\db\\BatchQueryResult", "\\app\\User[][]"), QueryResultTypes.types("\\app\\User", "\\app\\User.find.batch"));
        equal(List.of(), QueryResultTypes.types("\\app\\User", "\\app\\User.find.count"));
        equal(List.of(), QueryResultTypes.types(null, "\\app\\User.find.each"));
        equal(true, QueryResultTypes.isResultMethod("batch"));
        equal(false, QueryResultTypes.isResultMethod("eachRow"));
        equal(false, QueryResultTypes.isResultMethod(null));
        equal("backend/models/User.php", AliasResolver.resolve(aliases, "@app/models/User.php"));
        equal("backend/web/css/app.css", AliasResolver.resolve(aliases, "@webroot/css/app.css"));
        equal("/tmp/runtime/logs", AliasResolver.resolve(aliases, "@app/runtime/logs"));
        equal(null, AliasResolver.resolve(aliases, "@application/test"));
        equal(null, AliasResolver.resolve(Map.of("@a", "@b", "@b", "@a"), "@a/test"));
        equal(null, AliasResolver.resolve(Map.of("@a", "@a/more"), "@a"));
        equal(null, AliasResolver.resolve(aliases, null));
        equal("/tmp/file", AliasResolver.resolve(aliases, "/tmp/file"));
        equal(Set.of("a", "b"), SqlParameters.names("id=:a OR x=:b OR z=:a"));
        equal(Set.of("id"), SqlParameters.names("value::text=:id AND ':literal' = ':value' /* :comment */ -- :line\n"));
        equal(Set.of("id"), SqlParameters.names("a=$tag$:hidden$tag$ AND b=$$:hidden$$ AND id=:id"));
        equal(Set.of("id"), SqlParameters.names("`:column`=\":literal\" AND a='it''s :quoted' AND id=:id"));
        equal(Set.of(), SqlParameters.names("'unclosed :value"));
        equal(Set.of("_x", "name2"), SqlParameters.names(":_x + :name2 + :123"));
        equal("id", SqlParameters.normalize(":id"));
        equal("id", SqlParameters.normalize("id"));
        equal(Set.of("a", "b"), SqlParameters.names(":b + :a"));
        equal("user-profile", RouteNames.id("UserProfile"));
        equal("xml-parser", RouteNames.id("XMLParser"));
        equal("api-v2", RouteNames.id("ApiV2"));
        Locale previous = Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("tr")); equal("index", RouteNames.id("Index")); }
        finally { Locale.setDefault(previous); }
        equal("admin/site/index", RouteNames.normalize("//admin/./user/../site/index/"));
        equal("./migrations", AliasResolver.resolve(Map.of("@app", ""), "@app/migrations"));
        equal("admin/api/", RouteNames.modulePrefix("modules/admin/modules/api/controllers/SiteController.php"));
        equal("", RouteNames.modulePrefix("controllers/admin/UserController.php"));
        equal("admin/site/index", RouteNames.resolve("index", "admin/site", "admin/"));
        equal("admin/user/index", RouteNames.resolve("user/index", "admin/site", "admin/"));
        equal("site/index", RouteNames.resolve("/site/index", "admin/site", "admin/"));
        equal("site/index", RouteNames.resolve("site/index", "admin/user", ""));
        List<String> lines = new ArrayList<>();
        OutputLines output = new OutputLines();
        output.accept("one\r", lines::add); equal(List.of(), lines);
        output.accept("\ntwo\nthi", lines::add); equal(List.of("one","two"), lines);
        output.accept("rd", lines::add); output.flush(lines::add); equal(List.of("one","two","third"), lines);
        output.flush(lines::add); equal(3, lines.size());
        // Every possible output chunk boundary must yield the same migration lines.
        String text="*** applying m260908_120000_example\r\n*** applied m260908_120000_example (0.1s)\nlast";
        for (int split=0;split<=text.length();split++) {
            lines.clear(); output=new OutputLines();
            output.accept(text.substring(0,split),lines::add); output.accept(text.substring(split),lines::add); output.flush(lines::add);
            equal(List.of("*** applying m260908_120000_example","*** applied m260908_120000_example (0.1s)","last"), lines);
        }
        for (String status : List.of("applying", "applied", "reverting", "reverted", "failed to apply", "failed to revert")) {
            var event = io.github.byscrimm.yii2insight.migrations.commands.MigrationOutput.parse("*** " + status + " m260908_120000_example");
            equal(status, event.status()); equal("\\", event.namespace()); equal("m260908_120000_example", event.name());
        }
        var event = io.github.byscrimm.yii2insight.migrations.commands.MigrationOutput.parse("*** applied console\\migrations\\m260908_120000_example (time: 0.125s)");
        equal("\\console\\migrations\\", event.namespace()); equal(java.time.Duration.ofMillis(125), event.duration());
        equal(null, io.github.byscrimm.yii2insight.migrations.commands.MigrationOutput.parse("Unrelated output"));
        var path = io.github.byscrimm.yii2insight.relations.RelationPath.segments("orders.items.product AS p", true);
        equal(3, path.size()); equal("items", path.get(1).name());
        equal(List.of(), io.github.byscrimm.yii2insight.relations.RelationPath.segments("orders AS o", false));
        equal(List.of(), io.github.byscrimm.yii2insight.relations.RelationPath.segments("orders..items", true));
        equal(List.of(), io.github.byscrimm.yii2insight.relations.RelationPath.segments("orders; DROP", true));
        equal(List.of(), io.github.byscrimm.yii2insight.relations.RelationPath.segments("", true));
        equal("uRL", io.github.byscrimm.yii2insight.relations.RelationPath.property("getURL"));
        System.out.println("PASS: " + checks + " core regression checks");
    }
}
