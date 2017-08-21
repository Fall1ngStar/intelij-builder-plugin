package main;

import com.intellij.openapi.editor.Document;
import com.intellij.psi.*;

import java.util.stream.Stream;

public class BuilderGenerator {
    private PsiClass clazz;
    private PsiElementFactory factory;


    private BuilderGenerator(PsiClass clazz) {
        this.clazz = clazz;
        factory = PsiElementFactory.SERVICE.getInstance(clazz.getProject());
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
        PsiClass innerClass = factory.createClass(clazz.getName() + "Builder");
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
                    PsiParameter[] parameters = constructor.getParameterList().getParameters();
                    for (int i = 0; i < parameters.length; i++) {
                        builder.append(parameters[i].getName());
                        if (i < parameters.length - 1)
                            builder.append(", ");
                    }
                    builder.append(");");
                    method.getBody().add(factory.createStatementFromText(builder.toString(), null));
                });
        innerClass.add(method);
        innerClass.getModifierList().setModifierProperty("public", true);
        innerClass.getModifierList().setModifierProperty("static", true);
        PsiMethod builderConstructor = factory.createConstructor();
        builderConstructor.getModifierList().setModifierProperty("private", true);
        innerClass.add(builderConstructor);
        clazz.add(innerClass);
        addBuildMethod(innerClass);
        addGetters();
    }

    private void addBuildMethod(final PsiClass innerClass) {
        PsiMethod method = factory.createMethod("builder", factory.createType(innerClass));
        method.getBody().add(factory.createStatementFromText("return new " + innerClass.getName() + "();", null));
        method.getModifierList().setModifierProperty("static", true);
        clazz.add(method);
    }

    private void addGetters() {
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
                        factory.createStatementFromText("return Optional.ofNullable(this." + field.getName() + ");", null));
            }
            clazz.add(getter);
        }
    }

    private String getNameForGetter(final String name) {
        char[] sequence = name.toCharArray();
        sequence[0] = Character.toUpperCase(sequence[0]);
        return "get" + String.valueOf(sequence);
    }

    public void addOptionalImport() {
        Document document = PsiDocumentManager.getInstance(clazz.getProject()).getDocument(clazz.getContainingFile());
        String text = document.getCharsSequence().toString();
        int firstSemicolon = text.indexOf(";");
        text = text.substring(0, firstSemicolon + 1) + "import java.util.Optional;" + text.substring(firstSemicolon + 1);
        document.setText(text);
    }
}