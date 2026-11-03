package io.github.byscrimm.yii2insight.database;

import com.intellij.codeInsight.completion.*;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.project.DumbService;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.psi.elements.PhpClass;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import io.github.byscrimm.yii2insight.relations.QueryAliases;
import io.github.byscrimm.yii2insight.relations.YiiModelResolver;
import org.jetbrains.annotations.NotNull;
import java.util.*;
import java.util.regex.*;

/** Table aliases and {@code alias.attribute} in ActiveQuery column arguments. */
public final class QueryAliasCompletionContributor extends CompletionContributor {
    private static final Pattern ATTRIBUTE = Pattern.compile("(?:^|[^\\w.])(?:\\[\\[)?([A-Za-z_]\\w*)(?:]])?\\.(?:\\[\\[)?(\\w*)$");
    private static final Pattern ALIAS = Pattern.compile("(?:^|[^\\w.\\[:])([A-Za-z_]\\w*)?$");

    public QueryAliasCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement().withParent(StringLiteralExpression.class), new CompletionProvider<>() {
            @Override protected void addCompletions(@NotNull CompletionParameters parameters, @NotNull ProcessingContext processing, @NotNull CompletionResultSet result) {
                if (DumbService.isDumb(parameters.getPosition().getProject())) return;
                StringLiteralExpression literal = (StringLiteralExpression) parameters.getPosition().getParent();
                var call = QueryAliases.columnCall(literal);
                if (call == null) return;
                int start = literal.getTextRange().getStartOffset() + literal.getValueRange().getStartOffset();
                int length = Math.max(0, Math.min(literal.getContents().length(), parameters.getOffset() - start));
                String typed = literal.getContents().substring(0, length);
                YiiModelResolver resolver = new YiiModelResolver(literal.getProject());
                QueryAliases aliases = new QueryAliases(resolver);
                Map<String,List<PhpClass>> visible = aliases.visible(call);
                if (visible.isEmpty()) return;
                Matcher attribute = ATTRIBUTE.matcher(typed);
                if (attribute.find()) {
                    List<PhpClass> models = visible.get(attribute.group(1));
                    if (models == null) return;
                    CompletionResultSet attributes = result.withPrefixMatcher(attribute.group(2));
                    Set<String> added = new HashSet<>();
                    for (PhpClass model : models) for (var property : aliases.attributes(model)) {
                        if (!added.add(property.getName())) continue;
                        attributes.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create(property, property.getName())
                            .withTypeText(property.getType().toString(), true).withTailText("  " + model.getName(), true).bold(), 3));
                    }
                    return;
                }
                Matcher alias = ALIAS.matcher(typed);
                if (!alias.find()) return;
                CompletionResultSet names = result.withPrefixMatcher(alias.group(1) == null ? "" : alias.group(1));
                visible.forEach((name, models) -> names.addElement(PrioritizedLookupElement.withPriority(LookupElementBuilder.create(name)
                    .withTypeText(String.join("|", models.stream().map(PhpClass::getName).toList()), true).withTailText(" alias", true), 2)));
            }
        });
    }
}
