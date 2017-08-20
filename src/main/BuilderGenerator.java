package main;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;

import java.util.stream.Stream;

public class BuilderGenerator {
    private PsiClass clazz;

    private BuilderGenerator(PsiClass clazz) {
        this.clazz = clazz;
    }

    public static BuilderGenerator of(PsiClass clazz) {
        return new BuilderGenerator(clazz);
    }

    public String getFields() {
        StringBuilder builder = new StringBuilder();
        for (PsiField field : clazz.getFields()) {
            builder.append(field.getType().getPresentableText() + " " + field.getName() + "\n");
        }

        return builder.toString();
    }

    public void addBuilder() {
        PsiElementFactory factory = PsiElementFactory.SERVICE.getInstance(clazz.getProject());
        PsiClass innerClass = factory.createClass("Builder");
        for (PsiField field : clazz.getAllFields()) {
            PsiMethod method = factory.createMethod(field.getName(), factory.createType(innerClass));
            method.getParameterList().add(factory.createParameter(field.getName(), field.getType()));
            method.getBody().add(factory.createStatementFromText("this." + field.getName() + " = " + field.getName() + ";", null));
            method.getBody().add(factory.createStatementFromText("return this;", null));
            innerClass.add(method);
            PsiField innerField = factory.createField(field.getName(), field.getType());
            innerField.getModifierList().setModifierProperty("private", true);
            innerClass.add(innerField);
        }
        PsiMethod method = factory.createMethod("build", factory.createType(clazz));
        Stream.of(clazz.getConstructors()).filter(constructor ->
                constructor.getParameterList().getParametersCount() == clazz.getAllFields().length).findAny()
                .ifPresent(constructor -> {
                    StringBuilder builder = new StringBuilder();
                    builder.append("return new " + clazz.getName() + "(");
                    PsiField[] fields = clazz.getAllFields();
                    for (int i = 0; i < fields.length; i++) {
                        builder.append(fields[i].getName());
                        if (i < fields.length - 1)
                            builder.append(", ");
                    }
                    builder.append(");");
                    method.getBody().add(factory.createStatementFromText(builder.toString(), null));
                });
        innerClass.add(method);
        innerClass.getModifierList().setModifierProperty("private", true);
        PsiMethod builderConstructor = factory.createConstructor();
        builderConstructor.getModifierList().setModifierProperty("private", true);
        innerClass.add(builderConstructor);
        clazz.add(innerClass);
        addGetters();
    }

    private void addGetters() {
        PsiElementFactory factory = PsiElementFactory.SERVICE.getInstance(clazz.getProject());
        for (PsiField field : clazz.getFields()) {
            PsiType type;
            if (field.getType().getPresentableText().contains("List")) {
                type = field.getType();
            } else {
                type = factory.createTypeFromText("Optional<" +
                        field.getType().getPresentableText() + ">", null);
            }
            PsiMethod getter = factory.createMethod(getNameForGetter(field.getName()), type);
            if (field.getType().getPresentableText().contains("List")) {
                getter.getBody().add(
                        factory.createStatementFromText("return this." + field.getName() + ";", null));
            } else {
                getter.getBody().add(
                        factory.createStatementFromText("return Optional.ofNullable(this."+ field.getName() + ");", null));
            }
            clazz.add(getter);
        }
    }

    private String getNameForGetter(final String name){
        char[] sequence = name.toCharArray();
        sequence[0] = Character.toUpperCase(sequence[0]);
        return "get" + String.valueOf(sequence);
    }
}
/*

Document document = PsiDocumentManager.getInstance(clazz.getProject()).getDocument(clazz.getContainingFile());
        String text = document.getCharsSequence().toString();
        int firstSemicolon = text.indexOf(";");
        text = text.substring(0, firstSemicolon) + "import java.util.Optional;" + text.substring(firstSemicolon);
        document.setText(text);
 */