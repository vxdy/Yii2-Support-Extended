package io.github.byscrimm.yii2insight.typeprovider;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.elements.impl.VariableImpl;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import io.github.byscrimm.yii2insight.common.ClassUtils;
import io.github.byscrimm.yii2insight.common.MethodUtils;
import io.github.byscrimm.yii2insight.common.PsiUtil;
import io.github.byscrimm.yii2insight.objectfactory.ObjectFactoryUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * Created by oleg on 2017-06-08.
 */
public class YiiTypeProvider implements PhpTypeProvider4 {
    final static char TRIM_KEY = '\u0197';
    final static char TRIM_KEY2 = '\u0199';

    @Override
    public char getKey() {
        return '\u0857';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement psiElement) {
        if (MethodUtils.isYiiCreateObjectMethod(psiElement)) {
            PhpPsiElement firstParam = (PhpPsiElement)MethodUtils.getParameter((MethodReference)psiElement, 0);
            //  Case: Yii::createObject($obj->array_var)
            if (firstParam instanceof FieldReference ) {
                String signature = ((MethodReference)psiElement).getSignature();
                return new PhpType().add("#" + this.getKey() + signature + TRIM_KEY + ((FieldReference) firstParam).getSignature());
            }
            // Case: Yii::createObject($array_var)
            if (firstParam instanceof Variable  && ((VariableImpl) firstParam).getType().getTypes().contains("\\array") ) {
                firstParam = getArrayCreationByVariableRef(firstParam);
            }
            if (firstParam instanceof ArrayCreationExpression) {
                return getClassByArrayCreationOptimized((ArrayCreationExpression) firstParam);
            }
            else {
                return getClass(firstParam);
            }
        }else if(psiElement instanceof FieldReference) {
            String fieldName = PsiUtil.getYiiAppField((FieldReference) psiElement);
            var file = fieldName == null || psiElement.getContainingFile() == null ? null
                    : io.github.byscrimm.yii2insight.common.FileUtil.getVirtualFile(psiElement.getContainingFile());
            if (file != null) {
                return new PhpType().add("#" + this.getKey() + TRIM_KEY2 + fieldName + TRIM_KEY2 + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(file.getUrl().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
            }
        }
        return null;
    }

    @Override
    public @Nullable PhpType complete(String s, Project project) {
        if (com.intellij.openapi.project.DumbService.isDumb(project)) {
            return null;
        }
        PhpType phpType = new PhpType();

        int trimIndex = s.indexOf(TRIM_KEY);
        if (trimIndex > -1 && s.length() + 1 > trimIndex) {
            String origSignature = s.substring(0, trimIndex);
            if (origSignature.endsWith("\\Yii.createObject")) {
                String variableRef = s.substring(trimIndex + 1);
                final Collection<? extends PhpNamedElement> indexedVariabled = PhpIndex.getInstance(project).getBySignature(variableRef);
                for (PhpNamedElement elem : indexedVariabled) {
                    if (elem instanceof Field) {
                        Field field = (Field) elem;
                        if (field.getLastChild() instanceof ArrayCreationExpression) {
                            PhpClass classByArray = ObjectFactoryUtils.findClassByArray((ArrayCreationExpression) field.getLastChild());
                            if(classByArray != null) {
                                phpType.add(classByArray.getType());
                            }
                        }
                    }
                }
            }
        }else {
            int trimIndexStart = s.indexOf(TRIM_KEY2);
            int trimIndexEnd = s.lastIndexOf(TRIM_KEY2);
            if (trimIndexStart > -1 && trimIndexStart < trimIndexEnd) {
                String fieldName = s.substring(trimIndexStart + 1, trimIndexEnd);
                final GlobalSearchScope scope = GlobalSearchScope.projectScope(project);

                com.intellij.openapi.vfs.VirtualFile origin;
                try {
                    String url = new String(java.util.Base64.getUrlDecoder().decode(s.substring(trimIndexEnd + 1)), java.nio.charset.StandardCharsets.UTF_8);
                    origin = com.intellij.openapi.vfs.VirtualFileManager.getInstance().findFileByUrl(url);
                } catch (IllegalArgumentException ex) { return null; }
                for (String className : io.github.byscrimm.yii2insight.configurations.ComponentResolver.classes(project, origin, fieldName)) {
                    for (PhpClass phpClass : PhpIndex.getInstance(project).getAnyByFQN(className)) {
                        phpType.add(phpClass.getType());
                    }
                }
            }
        }
        return phpType;
    }

    @NotNull
    private PhpPsiElement getArrayCreationByVariableRef(PhpPsiElement firstParam) {
        Collection<? extends PhpNamedElement> localResolvedVariables = ((VariableImpl) firstParam).resolveLocal();
        if (localResolvedVariables.isEmpty()) return firstParam;
        PhpNamedElement firstElem = localResolvedVariables.iterator().next();
        if (firstElem instanceof Variable) {
            Variable variableDecl = (Variable)firstElem;
            if (variableDecl.getParent() != null) {
                PsiElement array = variableDecl.getParent().getLastChild();
                if (array instanceof ArrayCreationExpression) {
                    firstParam = (ArrayCreationExpression) array;
                }
            }
        }
        return firstParam;
    }

    @Nullable
    private PhpType getClassByArrayCreationOptimized(ArrayCreationExpression arrayCreationExpr) {
        for (ArrayHashElement elem : arrayCreationExpr.getHashElements()) {
            if (elem.getKey() != null && elem.getKey().getText() != null &&
                    ClassUtils.removeQuotes(elem.getKey().getText()).equals("class")) {
                return getClass(elem.getValue());
            }
        }
        return null;
    }

    private PhpType getClass(PhpPsiElement elem) {
        if (elem instanceof ClassConstantReference) {
            if (elem.getName() != null && elem.getName().equals("class")
                    && ((ClassConstantReference) elem).getClassReference() != null)
                return ((ClassConstantReference) elem).getClassReference().getType();
        }
        if (elem instanceof MethodReference) {
            if (elem.getName() != null && elem.getName().equals("className")
                    && ((MethodReference) elem).getClassReference() != null)
                return ((MethodReference) elem).getClassReference().getType();
        }

        return null;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String s, Set<String> set, int i, Project project) {
        return null;
    }

}
