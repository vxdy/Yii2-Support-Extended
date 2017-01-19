package com.yii2support.views;

import com.intellij.codeInsight.template.macro.SplitWordsMacro;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.php.lang.parser.PhpElementTypes;
import com.jetbrains.php.lang.psi.elements.FunctionReference;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import com.jetbrains.php.lang.psi.elements.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by NVlad on 15.01.2017.
 */
class ViewsUtil {
    static final Key<String> RENDER_VIEW = Key.create("com.yii2support.views.render.view");
    static final Key<PsiFile> RENDER_VIEW_FILE = Key.create("com.yii2support.views.viewFile");
    static final Key<Long> VIEW_FILE_MODIFIED = Key.create("com.yii2support.views.viewFileModified");
    static final Key<ArrayList<String>> VIEW_VARIABLES = Key.create("com.yii2support.views.viewVariables");

    @Nullable
    static PsiFile getViewPsiFile(PsiElement psiElement) {
        if (!(psiElement instanceof StringLiteralExpression)) {
            return null;
        }

        PsiFile psiFile = psiElement.getContainingFile();
        StringLiteralExpression expression = (StringLiteralExpression) psiElement;
        String filename = expression.getContents();
        if (filename.contains("/")) {
            filename = filename.substring(filename.lastIndexOf("/") + 1);
        }
        if (!filename.contains(".")) {
            filename = filename.concat(".php");
        }

        PsiDirectory directory = getViewsPsiDirectory(psiFile, psiElement);

        if (directory == null) {
            return null;
        }

        return directory.findFile(filename);
    }

    @Nullable
    static PsiDirectory getViewsPsiDirectory(PsiFile psiFile, PsiElement psiElement) {
        String fileName = psiFile.getName().substring(0, psiFile.getName().lastIndexOf("."));
        PsiDirectory psiDirectory = psiFile.getContainingDirectory();

        if (fileName.endsWith("Controller")) {
            psiDirectory = psiFile.getContainingDirectory().getParentDirectory();
            if (psiDirectory != null) {
                psiDirectory = psiDirectory.findSubdirectory("views");

                if (psiDirectory != null) {
                    String container = fileName.substring(0, fileName.length() - 10);
                    container = new SplitWordsMacro.LowercaseAndDash().convertString(container);

                    psiDirectory = psiDirectory.findSubdirectory(container);
                }
            }
        }

        String enteredText = ((StringLiteralExpression) psiElement).getContents();
        String enteredPath = enteredText;
        if (enteredText.startsWith("/")) {
            while (psiDirectory != null && !psiDirectory.getName().equals("views")) {
                psiDirectory = psiDirectory.getParentDirectory();
            }
            enteredPath = enteredPath.substring(1);
        }

        if (!enteredPath.endsWith("/") && enteredPath.contains("/")) {
            enteredPath = enteredPath.substring(0, enteredPath.lastIndexOf("/") + 1);
            if (enteredPath.length() == 1) {
                enteredPath = "";
            }
        }

        if (enteredPath.endsWith("/")) {
            String directory;
            while (!enteredPath.equals("")) {
                directory = enteredPath.substring(0, enteredPath.indexOf("/"));
                enteredPath = enteredPath.substring(directory.length() + 1);
                if (psiDirectory != null) {
                    psiDirectory = psiDirectory.findSubdirectory(directory);
                }
            }
        }

        return psiDirectory;
    }

    @NotNull
    static ArrayList<String> getViewVariables(PsiFile psiFile) {
        ArrayList<String> result = null;

        Long viewModified = psiFile.getUserData(VIEW_FILE_MODIFIED);
        if (viewModified != null && psiFile.getModificationStamp() == viewModified) {
            result = psiFile.getUserData(VIEW_VARIABLES);
        }

        if (result == null) {
            result = new ArrayList<>();
            final ArrayList<String> allVariables = new ArrayList<>();
            final ArrayList<String> declaredVariables = new ArrayList<>();
            final Collection<Variable> viewVariables = PsiTreeUtil.findChildrenOfType(psiFile, Variable.class);

            for (FunctionReference reference : PsiTreeUtil.findChildrenOfType(psiFile, FunctionReference.class)) {
                if (reference.getNode().getElementType() == PhpElementTypes.FUNCTION_CALL && psiFile.getUseScope().equals(reference.getUseScope())) {
                    if (reference.getName() != null && reference.getName().equals("compact")) {
                        for (PsiElement element : reference.getParameters()) {
                            if (element instanceof StringLiteralExpression) {
                                allVariables.add(((StringLiteralExpression) element).getContents());
                            }
                        }
                    }
                }
            }

            for (Variable variable : viewVariables) {
                String variableName = variable.getName();
                if (variable.isDeclaration()) {
                    if (!declaredVariables.contains(variableName)) {
                        declaredVariables.add(variableName);
                    }
                } else {
                    if (!(variableName.equals("this") || variableName.equals("_file_") || variableName.equals("_params_"))) {
                        if (!allVariables.contains(variableName) && psiFile.getUseScope().equals(variable.getUseScope())) {
                            allVariables.add(variableName);
                        }
                    }
                }
            }

            for (String variable : allVariables) {
                if (!declaredVariables.contains(variable)) {
                    result.add(variable);
                }
            }

            psiFile.putUserData(VIEW_VARIABLES, result);
            psiFile.putUserData(VIEW_FILE_MODIFIED, psiFile.getModificationStamp());
        }

        return new ArrayList<>(result);
    }
}
