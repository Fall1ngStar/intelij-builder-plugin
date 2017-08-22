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

        PsiMethod constructor = clazz.getConstructors()[0];

        for (PsiParameter parameter : constructor.getParameterList().getParameters()) {
            PsiMethod method = factory.createMethod(parameter.getName(), factory.createType(innerClass));
            method.getParameterList().add(factory.createParameter(parameter.getName(), parameter.getType()));
            method.getBody().add(factory.createStatementFromText("this." + parameter.getName() + " = " + parameter.getName() + ";", null));
            method.getBody().add(factory.createStatementFromText("return this;", null));
            innerClass.add(method);
            PsiField innerField = factory.createField(parameter.getName(), parameter.getType());
            innerField.getModifierList().setModifierProperty("private", true);
            innerClass.add(innerField);
        }
        PsiMethod method = factory.createMethod("build", factory.createType(clazz));
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
        innerClass.add(method);
        innerClass.getModifierList().setModifierProperty("public", true);
        innerClass.getModifierList().setModifierProperty("static", true);
        PsiMethod builderConstructor = factory.createConstructor();
        builderConstructor.getModifierList().setModifierProperty("private", true);
        innerClass.add(builderConstructor);
        clazz.add(innerClass);
        addBuildMethod(innerClass);
        addGetters(constructor);
    }

    private void addBuildMethod(final PsiClass innerClass) {
        PsiMethod method = factory.createMethod("builder", factory.createType(innerClass));
        method.getBody().add(factory.createStatementFromText("return new " + innerClass.getName() + "();", null));
        method.getModifierList().setModifierProperty("static", true);
        clazz.add(method);
    }

    private void addGetters(final PsiMethod constructor) {
        PsiParameter[] parameters = constructor.getParameterList().getParameters();

        for (PsiField field : clazz.getFields()) {
            if(!isFieldInConstructorParameters(field, parameters))
                continue;
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

    private boolean isFieldInConstructorParameters(final PsiField field,
                                                   final PsiParameter[] parameters){
        for(PsiParameter parameter : parameters){
            if(parameter.getName().equals(field.getName())){
                return true;
            }
        }
        return false;
    }
}