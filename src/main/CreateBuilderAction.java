package main;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.containers.ContainerUtil;


public class CreateBuilderAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        PsiClass clazz = getCurrentClass(project);
        if (clazz != null) {
            BuilderGenerator generator = BuilderGenerator.of(clazz);
            new WriteCommandAction.Simple(project) {
                @Override
                protected void run() throws Throwable {
                    CommandProcessor.getInstance().executeCommand(project, generator::addBuilder, null, null);
                }
            }.execute();
            new WriteCommandAction.Simple(project) {
                @Override
                protected void run() throws Throwable {
                    CommandProcessor.getInstance().executeCommand(project, generator::addOptionalImport, null, null);
                }
            }.execute();
            new WriteCommandAction.Simple(project) {
                @Override
                protected void run() throws Throwable {
                    CodeStyleManager.getInstance(project).reformatText(clazz.getContainingFile(),
                            ContainerUtil.newArrayList(clazz.getContainingFile().getTextRange()));
                }
            }.execute();
        } else {
            Messages.showMessageDialog("There is no class in this file", "Error", Messages.getErrorIcon());
        }
    }

    private PsiClass getCurrentClass(Project project) {
        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file instanceof PsiJavaFile) {
            PsiClass[] classes = ((PsiJavaFile) file).getClasses();
            if (classes.length > 0) {
                return classes[0];
            }
        }
        return null;
    }
}
