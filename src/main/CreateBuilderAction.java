package main;

import com.intellij.ide.util.TreeClassChooser;
import com.intellij.ide.util.TreeClassChooserFactory;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.psi.PsiClass;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.util.containers.ContainerUtil;

/**
 * Created by thierry on 19/08/17.
 */
public class CreateBuilderAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent event) {
        Project project = event.getProject();
        TreeClassChooser chooser = TreeClassChooserFactory.getInstance(project)
                .createProjectScopeChooser("Select your class");
        chooser.showDialog();
        PsiClass clazz = chooser.getSelected();
        BuilderGenerator generator = BuilderGenerator.of(clazz);
        new WriteCommandAction.Simple(project) {
            @Override
            protected void run() throws Throwable {
                CommandProcessor.getInstance().executeCommand(project, generator::addBuilder, null, null);
                CodeStyleManager.getInstance(project).reformatText(clazz.getContainingFile(),
                        ContainerUtil.newArrayList(clazz.getContainingFile().getTextRange()));
            }
        }.execute();
    }
}
