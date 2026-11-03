package io.github.byscrimm.yii2insight.views.refactor;

import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.refactoring.listeners.RefactoringElementListener;
import com.intellij.refactoring.rename.RenamePsiFileProcessor;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.php.lang.psi.PhpFile;
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression;
import io.github.byscrimm.yii2insight.utils.Yii2InsightSettings;
import io.github.byscrimm.yii2insight.views.index.ViewFileIndex;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by NVlad on 02.02.2017.
 */
public class RenameViewProcessor extends RenamePsiFileProcessor {
    // Smart pointers avoid retaining PSI between prepareRenaming and the post-rename callback.
    private final List<SmartPsiElementPointer<StringLiteralExpression>> renders = new ArrayList<>();

    @Override
    public boolean canProcessElement(@NotNull PsiElement psiElement) {
        return psiElement instanceof PhpFile;
    }

    @Override
    public void prepareRenaming(PsiElement psiElement, String s, Map<PsiElement, String> map) {
        renders.clear();

        if (!RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_FILE) {
            return;
        }

        for (PsiReference reference : findReferences(psiElement)) {
            final PsiElement element = reference.getElement();
            if (element instanceof StringLiteralExpression) {
                renders.add(SmartPointerManager.createPointer((StringLiteralExpression) element));
            }
        }
    }

    @Nullable
    @Override
    public Runnable getPostRenameCallback(PsiElement psiElement, String s, RefactoringElementListener refactoringElementListener) {
        return () -> {
            final Yii2InsightSettings settings = Yii2InsightSettings.getInstance(psiElement.getProject());

            final List<SmartPsiElementPointer<StringLiteralExpression>> pointers = new ArrayList<>(renders);
            renders.clear();
            for (SmartPsiElementPointer<StringLiteralExpression> pointer : pointers) {
                final StringLiteralExpression element = pointer.getElement();
                if (element == null || !element.isValid()) {
                    continue;
                }
                String fileName = element.getContents();
                if (fileName.endsWith("." + settings.defaultViewExtension)) {
                    fileName = fileName.substring(0, fileName.length() - settings.defaultViewExtension.length() - 1);
                    ElementManipulators.handleContentChange(element, fileName);
                }
            }

            FileBasedIndex.getInstance().requestRebuild(ViewFileIndex.identity);
        };
    }
}
