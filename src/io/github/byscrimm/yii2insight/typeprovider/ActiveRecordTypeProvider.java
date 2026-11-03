package io.github.byscrimm.yii2insight.typeprovider;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.*;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider4;
import io.github.byscrimm.yii2insight.common.ClassUtils;
import io.github.byscrimm.yii2insight.common.QueryResultTypes;
import io.github.byscrimm.yii2insight.common.SignatureUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * Created by oleg on 2017-06-24.
 */
public class ActiveRecordTypeProvider  implements PhpTypeProvider4  {
    final static char TRIM_KEY = '\u0197';

    @Override
    public char getKey() {
        return '\u0856';
    }

    @Nullable
    @Override
    public PhpType getType(PsiElement psiElement) {
        if (psiElement instanceof MethodReference) {
            MethodReference methodReference = (MethodReference)psiElement;
            String methodName = methodReference.getName();
            if (methodName == null)
                return null;
            if (QueryResultTypes.isResultMethod(methodName)) {
                if (usesArrayResult(methodReference)) return null;
                String signature = methodReference.getSignature();
                int beginIndex = signature.indexOf("\\");
                int endIndex = signature.indexOf("|");
                if (endIndex < 0) {
                    endIndex = signature.length();
                }
                if (beginIndex > -1 && beginIndex < endIndex) {
                    signature = signature.substring(beginIndex, endIndex);
                    return new PhpType().add("#" + this.getKey() + signature);                    
                }
            }
        }
        return null;
    }

    public static boolean usesArrayResult(MethodReference methodReference) {
        PhpExpression caller = methodReference.getClassReference();
        while (caller instanceof MethodReference call) {
            if ("asArray".equals(call.getName())) {
                PsiElement value = io.github.byscrimm.yii2insight.common.PhpArguments.get(call,"value",0);
                return value == null || !"false".equalsIgnoreCase(value.getText());
            }
            caller = call.getClassReference();
        }
        return false;
    }

    @Override
    @Nullable
    public PhpType complete(String s, Project project) {
        PhpType phpType = new PhpType();

        int endIndex = s.lastIndexOf(this.getKey());
        if(endIndex == -1) {
            return null;
        }

        PhpClass classBySignature = SignatureUtils.getClassBySignature(s, project);
        boolean classInheritsFromAD = ClassUtils.isClassInherit(classBySignature, "\\yii\\db\\BaseActiveRecord", PhpIndex.getInstance(project));
        if (classInheritsFromAD) {
            for (String type : QueryResultTypes.types(classBySignature.getFQN(), s)) {
                phpType.add(type);
            }
        }
        return phpType;
    }

    @Override
    public Collection<? extends PhpNamedElement> getBySignature(String s, Set<String> set, int i, Project project) {
        return null;
    }
}
