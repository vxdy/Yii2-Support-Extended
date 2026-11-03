package io.github.byscrimm.yii2insight.relations;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.documentation.phpdoc.psi.PhpDocProperty;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.Function;
import io.github.byscrimm.yii2insight.common.*;
import java.util.*;
import java.util.regex.*;

/**
 * SQL table aliases visible to a column string in an ActiveQuery chain. Models come from explicit
 * {@code alias()} calls and relation declarations of {@code joinWith()}; implicit table-name aliases,
 * custom scopes and dynamic values are not guessed. Conflicting aliases are dropped.
 */
public final class QueryAliases {
    /** Column-taking methods and the name of their first parameter. */
    private static final Map<String,String> COLUMN_METHODS = Map.ofEntries(
        Map.entry("where","condition"), Map.entry("andWhere","condition"), Map.entry("orWhere","condition"),
        Map.entry("having","condition"), Map.entry("andHaving","condition"),
        Map.entry("select","columns"), Map.entry("addSelect","columns"),
        Map.entry("orderBy","columns"), Map.entry("addOrderBy","columns"),
        Map.entry("groupBy","columns"), Map.entry("addGroupBy","columns"));
    private static final Set<String> JOIN_WITH = Set.of("joinWith","innerJoinWith");
    /** Fluent methods that keep the same query; anything else ends alias analysis. */
    private static final Set<String> FLUENT = Set.of("find","where","andWhere","orWhere","filterWhere","andFilterWhere","orFilterWhere",
        "orderBy","addOrderBy","groupBy","addGroupBy","having","andHaving","orHaving","limit","offset","select","addSelect","distinct",
        "indexBy","alias","with","joinWith","innerJoinWith","asArray","onCondition","andOnCondition","orOnCondition","cache","noCache",
        "emulateExecution","one","all","each","batch","column","scalar","count","exists");
    private static final Pattern SPEC = Pattern.compile("^\\s*([A-Za-z_][\\w.]*?)(?:\\s+(?:[Aa][Ss]\\s+)?([A-Za-z_]\\w*))?\\s*$");
    private static final int MAX_DEPTH = 24;

    private final YiiModelResolver resolver;
    public QueryAliases(YiiModelResolver resolver) { this.resolver = resolver; }

    /** Relation path and alias of a {@code joinWith} spec such as {@code 'orders.items i'}; null when not static. */
    public static String[] spec(String text) {
        if (text == null) return null;
        Matcher matcher = SPEC.matcher(text);
        if (!matcher.matches() || RelationPath.segments(matcher.group(1),false).isEmpty()) return null;
        return new String[]{matcher.group(1), matcher.group(2)};
    }

    /** The column-taking call when the literal is in a column position of its argument, otherwise null. */
    public static MethodReference columnCall(StringLiteralExpression literal) {
        MethodReference call = PsiTreeUtil.getParentOfType(literal,MethodReference.class);
        if (call == null || call.getName() == null || !COLUMN_METHODS.containsKey(call.getName())) return null;
        PsiElement argument = literal;
        while (argument != null && argument.getParent() != call.getParameterList()) argument = argument.getParent();
        if (argument == null || argument != PhpArguments.get(call,COLUMN_METHODS.get(call.getName()),0)) return null;
        return argument == literal || columnPosition(literal, argument, call.getName()) ? call : null;
    }

    /**
     * Conditions use hash keys, operator columns ({@code ['=', 'c.id', 1]}) and operands of
     * {@code and/or/not}; select and groupBy use values; orderBy uses keys.
     */
    private static boolean columnPosition(StringLiteralExpression literal, PsiElement argument, String method) {
        boolean condition = "condition".equals(COLUMN_METHODS.get(method));
        PsiElement element = literal;
        for (int depth = 0; depth < MAX_DEPTH; depth++) {
            PsiElement parent = element.getParent();
            Boolean key = null;
            if (parent instanceof PhpPsiElement wrapper && !(parent instanceof ArrayCreationExpression) && !(parent instanceof ArrayHashElement)
                    && wrapper.getParent() instanceof ArrayHashElement hash) {
                key = hash.getKey() == wrapper || PsiTreeUtil.isAncestor(hash.getKey(), element, false);
                parent = hash.getParent();
            } else if (parent instanceof ArrayHashElement hash) {
                key = hash.getKey() != null && PsiTreeUtil.isAncestor(hash.getKey(), element, false);
                parent = hash.getParent();
            } else if (parent instanceof PhpPsiElement wrapper && !(parent instanceof ArrayCreationExpression)
                    && wrapper.getParent() instanceof ArrayCreationExpression) parent = wrapper.getParent();
            if (!(parent instanceof ArrayCreationExpression array)) return false;
            boolean ok;
            if (key != null) ok = key == (condition || method.endsWith("rderBy"));
            else if (condition) ok = operand(array, element, 1);
            else ok = !method.endsWith("rderBy");
            if (!ok) return false;
            if (array == argument) return true;
            // Deeper arrays are only accepted as operands of logical condition operators.
            if (!condition) return false;
            element = array;
            PsiElement owner = array.getParent();
            if (owner instanceof PhpPsiElement wrapper && !(owner instanceof ArrayCreationExpression)) owner = wrapper.getParent();
            if (!(owner instanceof ArrayCreationExpression logical) || !logical(logical) || operand(logical, array, 0)) return false;
            if (logical == argument) return true;
            element = logical;
        }
        return false;
    }
    private static List<PsiElement> values(ArrayCreationExpression array) {
        List<PsiElement> values = new ArrayList<>();
        for (PsiElement child : array.getChildren()) {
            if (child instanceof ArrayHashElement) values.add(null);
            else if (child instanceof PhpPsiElement value) values.add(value instanceof ArrayCreationExpression || value.getFirstPsiChild() == null ? value : value.getFirstPsiChild());
        }
        return values;
    }
    /** Whether {@code element} is the value at {@code index} of an operator array. */
    private static boolean operand(ArrayCreationExpression array, PsiElement element, int index) {
        List<PsiElement> values = values(array);
        if (values.size() <= index || values.getFirst() == null || !(PhpArrays.string(values.getFirst()) instanceof String operator) || operator.isBlank()) return false;
        PsiElement value = values.get(index);
        return value != null && (value == element || PsiTreeUtil.isAncestor(value, element, false));
    }
    private static boolean logical(ArrayCreationExpression array) {
        List<PsiElement> values = values(array);
        String operator = values.isEmpty() || values.getFirst() == null ? null : PhpArrays.string(values.getFirst());
        return operator != null && Set.of("and","or","not").contains(operator.toLowerCase(Locale.ROOT));
    }

    /** Aliases visible in the query that owns {@code call}: alias name to models. */
    public Map<String,List<PhpClass>> visible(MethodReference call) { return visible(call, 0); }

    private Map<String,List<PhpClass>> visible(MethodReference call, int depth) {
        Map<String,Set<PhpClass>> result = new LinkedHashMap<>();
        Set<String> conflicts = new HashSet<>();
        if (depth > 4) return Map.of();
        MethodReference outer = call;
        while (outer.getParent() instanceof MethodReference next && next.getClassReference() == outer) outer = next;
        List<MethodReference> chain = new ArrayList<>();
        PsiElement root = outer;
        Set<PsiElement> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < MAX_DEPTH && seen.add(root); i++) {
            ProgressManager.checkCanceled();
            if (root instanceof MethodReference ref) {
                if (ref.getName() == null || !FLUENT.contains(ref.getName())) {
                    if (!(ref.getClassReference() instanceof ClassReference)) return Map.of();
                    break; // static Model::find() and similar start a query
                }
                chain.add(ref);
                if (ref.getClassReference() instanceof ClassReference) break;
                root = ref.getClassReference();
            } else if (root instanceof ParenthesizedExpression parenthesized) root = parenthesized.getArgument();
            else if (root instanceof Variable variable) {
                PsiElement value = LocalPhpValues.resolve(variable);
                if (value == null) break;
                root = value;
            } else break;
        }
        // The query model; inside a joinWith() callback the parameter is the relation query.
        List<PhpClass> models = chain.isEmpty() ? List.of() : resolver.models(chain.getLast());
        String ownAlias = null;
        if (models.isEmpty() && root instanceof Variable variable) {
            Callback callback = callback(variable);
            if (callback != null) {
                models = callback.models;
                ownAlias = callback.alias;
                // ON conditions of a joined relation are merged into the outer query.
                visible(callback.join, depth+1).forEach((name, types) -> add(result, conflicts, name, types));
            }
        }
        if (models.isEmpty()) return Map.of();
        for (MethodReference ref : chain) {
            if ("alias".equals(ref.getName())) {
                String alias = PhpArrays.string(PhpArguments.get(ref,"alias",0));
                if (alias != null && alias.matches("[A-Za-z_]\\w*")) ownAlias = alias;
                else if (PhpArguments.get(ref,"alias",0) != null) return Map.of(); // dynamic alias
            }
        }
        if (ownAlias != null) add(result, conflicts, ownAlias, models);
        for (MethodReference ref : chain) if (JOIN_WITH.contains(ref.getName())) joins(ref, models, result, conflicts);
        Map<String,List<PhpClass>> visible = new LinkedHashMap<>();
        result.forEach((name, types) -> { if (!conflicts.contains(name)) visible.put(name, List.copyOf(types)); });
        return visible;
    }

    private void joins(MethodReference join, List<PhpClass> models, Map<String,Set<PhpClass>> result, Set<String> conflicts) {
        PsiElement with = PhpArguments.get(join,"with",0);
        List<Map.Entry<String,PsiElement>> specs = new ArrayList<>();
        if (with instanceof StringLiteralExpression literal) specs.add(new AbstractMap.SimpleEntry<>(literal.getContents(), null));
        else if (with instanceof ArrayCreationExpression array) {
            for (PsiElement child : array.getChildren()) {
                if (child instanceof ArrayHashElement hash) {
                    String key = PhpArrays.string(hash.getKey());
                    if (key != null) specs.add(new AbstractMap.SimpleEntry<>(key, hash.getValue()));
                } else if (child instanceof PhpPsiElement wrapper && PhpArrays.string(wrapper.getFirstPsiChild()) instanceof String value)
                    specs.add(new AbstractMap.SimpleEntry<>(value, null));
            }
        }
        for (var entry : specs) {
            String[] spec = spec(entry.getKey());
            if (spec == null) continue;
            List<PhpClass> targets = targets(models, spec[0]);
            if (targets.isEmpty()) continue;
            String alias = spec[1] != null ? spec[1] : callbackAlias(entry.getValue());
            if (alias != null) add(result, conflicts, alias, targets);
        }
    }

    private List<PhpClass> targets(List<PhpClass> models, String path) {
        int dot = path.lastIndexOf('.');
        String parent = dot < 0 ? "" : path.substring(0,dot), name = path.substring(dot+1);
        Set<PhpClass> targets = new LinkedHashSet<>();
        for (var relation : resolver.atPath(models,parent)) if (relation.name().equals(name)) targets.addAll(relation.targets());
        return List.copyOf(targets);
    }

    private record Callback(MethodReference join, List<PhpClass> models, String alias) {}

    /** A variable that is the first parameter of a callback bound to a joinWith() relation key. */
    private Callback callback(Variable variable) {
        Function function = PsiTreeUtil.getParentOfType(variable,Function.class);
        if (function == null || function.getParameters().length == 0 || !function.getParameters()[0].getName().equals(variable.getName())) return null;
        ArrayHashElement hash = PsiTreeUtil.getParentOfType(function,ArrayHashElement.class);
        if (hash == null || hash.getValue() == null || !PsiTreeUtil.isAncestor(hash.getValue(),function,false)) return null;
        MethodReference join = PsiTreeUtil.getParentOfType(hash,MethodReference.class);
        if (join == null || !JOIN_WITH.contains(join.getName()) || !(hash.getParent() instanceof ArrayCreationExpression array)
                || array != PhpArguments.get(join,"with",0)) return null;
        String[] spec = spec(PhpArrays.string(hash.getKey()));
        if (spec == null) return null;
        List<PhpClass> owners = resolver.models(join.getClassReference());
        List<PhpClass> targets = targets(owners, spec[0]);
        if (targets.isEmpty()) return null;
        return new Callback(join, targets, spec[1] != null ? spec[1] : callbackAlias(function));
    }

    /** {@code function ($q) { $q->alias('c'); }} names the joined table; anything dynamic yields null. */
    private static String callbackAlias(PsiElement value) {
        Function function = value instanceof Function f ? f : PsiTreeUtil.findChildOfType(value,Function.class);
        if (function == null || function.getParameters().length == 0) return null;
        String parameter = function.getParameters()[0].getName(), alias = null;
        for (MethodReference ref : PsiTreeUtil.findChildrenOfType(function,MethodReference.class)) {
            ProgressManager.checkCanceled();
            if (!"alias".equals(ref.getName()) || PsiTreeUtil.getParentOfType(ref,Function.class) != function) continue;
            PsiElement owner = ref.getClassReference();
            while (owner instanceof MethodReference previous) owner = previous.getClassReference();
            if (!(owner instanceof Variable variable) || !parameter.equals(variable.getName())) continue;
            String name = PhpArrays.string(PhpArguments.get(ref,"alias",0));
            if (name == null || !name.matches("[A-Za-z_]\\w*") || alias != null && !alias.equals(name)) return null;
            alias = name;
        }
        return alias;
    }

    private static void add(Map<String,Set<PhpClass>> result, Set<String> conflicts, String alias, List<PhpClass> models) {
        Set<PhpClass> existing = result.get(alias);
        if (existing == null) result.put(alias, new LinkedHashSet<>(models));
        else if (!existing.equals(new LinkedHashSet<>(models))) conflicts.add(alias);
    }

    /**
     * PHPDoc {@code @property} names declared on the model and its ActiveRecord ancestors,
     * excluding relation properties. Getters and virtual properties are not columns.
     */
    public List<PhpDocProperty> attributes(PhpClass model) {
        Set<String> relations = new HashSet<>();
        for (var relation : resolver.relations(model)) relations.add(relation.name());
        Map<String,PhpDocProperty> result = new LinkedHashMap<>();
        Set<String> seen = new HashSet<>();
        for (PhpClass type = model; type != null && seen.add(type.getFQN()) && seen.size() < 32; ) {
            if (type.getFQN().startsWith("\\yii\\")) break;
            for (Field field : type.getOwnFields())
                if (field instanceof PhpDocProperty property && !relations.contains(property.getName())) result.putIfAbsent(property.getName(), property);
            List<PhpClass> parents = resolver.classes(type.getSuperFQN());
            type = parents.size() == 1 ? parents.getFirst() : null;
        }
        return List.copyOf(result.values());
    }
}
