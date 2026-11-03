package io.github.byscrimm.yii2insight.relations;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import io.github.byscrimm.yii2insight.common.*;
import java.util.*;

/** Shared model/relation resolution. An instance is short-lived; no project or PSI global cache. */
public final class YiiModelResolver {
    @FunctionalInterface public interface Symbols { Collection<PhpClass> classes(String fqn); }
    public record Relation(String name, Method getter, List<PhpClass> targets) {}
    private static final String RECORD = "\\yii\\db\\BaseActiveRecord";
    private static final String QUERY = "\\yii\\db\\ActiveQuery";
    private final Symbols symbols;
    public YiiModelResolver(Project project) { this(fqn -> PhpIndex.getInstance(project).getAnyByFQN(fqn)); }
    public YiiModelResolver(Symbols symbols) { this.symbols = symbols; }
    public List<PhpClass> classes(String fqn) {
        if (fqn == null || fqn.isBlank()) return List.of();
        return List.copyOf(symbols.classes(fqn.startsWith("\\") ? fqn : "\\" + fqn));
    }
    public boolean inherits(PhpClass clazz, String fqn) {
        return inherits(clazz, fqn, new HashSet<>(), 0);
    }
    private boolean inherits(PhpClass clazz, String fqn, Set<String> seen, int depth) {
        if (clazz == null || depth > 32 || !seen.add(clazz.getFQN())) return false;
        if (clazz.getFQN().equals(fqn)) return true;
        ProgressManager.checkCanceled();
        for (PhpClass parent : classes(clazz.getSuperFQN())) if (inherits(parent,fqn,seen,depth+1)) return true;
        return false;
    }
    public List<PhpClass> models(PsiElement expression) { return models(expression, new HashSet<>(), 0); }
    private List<PhpClass> models(PsiElement expression, Set<PsiElement> seen, int depth) {
        if (expression == null || depth > 24 || !seen.add(expression)) return List.of();
        ProgressManager.checkCanceled();
        if (expression instanceof ParenthesizedExpression parenthesized) return models(parenthesized.getArgument(), seen, depth+1);
        if (expression instanceof Variable variable) {
            if ("this".equals(variable.getName())) {
                PhpClass owner = PsiTreeUtil.getParentOfType(variable, PhpClass.class);
                return owner != null && inherits(owner, RECORD) ? List.of(owner) : List.of();
            }
            return models(LocalPhpValues.resolve(variable), seen, depth+1);
        }
        if (expression instanceof ClassReference clazz) return classes(clazz.getFQN()).stream().filter(c -> inherits(c,RECORD)).toList();
        if (expression instanceof NewExpression object && object.getClassReference() != null) {
            List<PhpClass> types = classes(object.getClassReference().getFQN());
            List<PhpClass> records = types.stream().filter(c -> inherits(c,RECORD)).toList();
            if (!records.isEmpty()) return records;
            if (types.stream().anyMatch(c -> inherits(c,QUERY))) return classes(PhpArrays.className(PhpArguments.get(object,"modelClass",0)));
        }
        if (expression instanceof MethodReference call) {
            String name = call.getName();
            if ("hasOne".equals(name) || "hasMany".equals(name)) {
                if (models(call.getClassReference(),new HashSet<>(seen),depth+1).isEmpty()) return List.of();
                return classes(PhpArrays.className(PhpArguments.get(call,"class",0)));
            }
            List<PhpClass> source = models(call.getClassReference(),seen,depth+1);
            if (name != null && name.startsWith("get")) {
                List<PhpClass> targets = new ArrayList<>();
                for (PhpClass model : source) for (Relation relation : relations(model))
                    if (relation.getter.getName().equals(name)) targets.addAll(relation.targets);
                return List.copyOf(new LinkedHashSet<>(targets));
            }
            // Only recognized ActiveQuery fluent methods preserve the model.
            if (name != null && Set.of("find","findOne","findAll","where","andWhere","orWhere","filterWhere","andFilterWhere","orFilterWhere",
                "orderBy","addOrderBy","groupBy","addGroupBy","having","andHaving","orHaving","limit","offset","select","addSelect",
                "distinct","indexBy","alias","with","joinWith","innerJoinWith","join","innerJoin","leftJoin","rightJoin","asArray",
                "onCondition","andOnCondition","orOnCondition","via","viaTable","cache","noCache","emulateExecution","one","all").contains(name)) return source;
        }
        return List.of();
    }
    private void methods(PhpClass clazz, Map<String,Method> methods, Set<String> seen) {
        if (clazz == null || seen.size() > 32 || !seen.add(clazz.getFQN())) return;
        for (Method method : clazz.getOwnMethods()) methods.putIfAbsent(method.getName().toLowerCase(Locale.ROOT),method);
        for (String trait : clazz.getTraitNames()) for (PhpClass type : classes(trait)) methods(type,methods,seen);
        for (PhpClass parent : classes(clazz.getSuperFQN())) methods(parent,methods,seen);
    }
    public List<Relation> relations(PhpClass model) {
        if (!inherits(model, RECORD)) return List.of();
        Map<String,Method> methods = new LinkedHashMap<>(); methods(model,methods,new HashSet<>());
        List<Relation> result = new ArrayList<>();
        for (Method getter : methods.values()) {
            String name = RelationPath.property(getter.getName());
            if (name == null || getter.isStatic() || !getter.getAccess().isPublic()
                    || Arrays.stream(getter.getParameters()).anyMatch(p -> !p.isOptional() && !p.isVariadic())) continue;
            List<PhpClass> targets = relationTargets(getter, model);
            if (!targets.isEmpty()) result.add(new Relation(name,getter,targets));
        }
        return result;
    }
    private List<PhpClass> relationTargets(Method getter, PhpClass model) {
        Set<PhpClass> result = new LinkedHashSet<>();
        for (PhpReturn ret : PsiTreeUtil.findChildrenOfType(getter,PhpReturn.class)) {
            if (PsiTreeUtil.getParentOfType(ret,Function.class) != getter) continue;
            PsiElement expression = LocalPhpValues.resolve(ret.getFirstPsiChild());
            int depth = 0;
            while (expression instanceof MethodReference call && depth++ < 24) {
                if ("hasOne".equals(call.getName()) || "hasMany".equals(call.getName())) {
                    if (!(call.getClassReference() instanceof Variable variable) || !"this".equals(variable.getName())) break;
                    PsiElement target = PhpArguments.get(call,"class",0);
                    String fqn = PhpArrays.className(target);
                    if (target instanceof ClassConstantReference ref && ref.getClassReference() instanceof ClassReference clazz
                            && "static".equalsIgnoreCase(clazz.getName())) result.add(model);
                    else if (target instanceof ClassConstantReference ref && ref.getClassReference() instanceof ClassReference clazz
                            && "self".equalsIgnoreCase(clazz.getName())) {
                        PhpClass owner = getter.getContainingClass();
                        if (owner != null) result.add(owner.isTrait() ? model : owner);
                    }
                    else for (PhpClass type : classes(fqn)) if (inherits(type,RECORD)) result.add(type);
                    break;
                }
                expression = call.getClassReference();
            }
        }
        return List.copyOf(result);
    }
    public List<Relation> atPath(List<PhpClass> models, String parentPath) {
        if (!parentPath.isEmpty() && RelationPath.segments(parentPath,false).isEmpty()) return List.of();
        List<PhpClass> current = models;
        if (!parentPath.isEmpty()) for (String part : parentPath.split("\\.")) {
            List<PhpClass> targets = new ArrayList<>();
            for (PhpClass model : current) for (Relation relation : relations(model)) if (relation.name.equals(part)) targets.addAll(relation.targets);
            current = List.copyOf(new LinkedHashSet<>(targets));
            if (current.isEmpty()) return List.of();
        }
        List<Relation> result = new ArrayList<>();
        for (PhpClass model : current) result.addAll(relations(model));
        return result;
    }
}
