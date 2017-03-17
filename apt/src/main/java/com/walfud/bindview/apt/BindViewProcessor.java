package com.walfud.bindview.apt;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@AutoService(Processor.class)
public class BindViewProcessor extends AbstractProcessor {

    private Messager mMessager;
    private Filer mFiler;
    private Elements mElementUtils;
    private Types mTypeUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);

        mMessager = processingEnvironment.getMessager();
        mFiler = processingEnvironment.getFiler();
        mElementUtils = processingEnvironment.getElementUtils();
        mTypeUtils = processingEnvironment.getTypeUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        try {
            //
            Map<TypeElement, List<VariableElement>> class_field = new HashMap<>();
            for (Element element : roundEnvironment.getElementsAnnotatedWith(BindView.class)) {
                VariableElement bindViewField = (VariableElement) element;
                TypeElement clazz = (TypeElement) bindViewField.getEnclosingElement();

                List<VariableElement> bindViewFieldList = class_field.get(clazz);
                if (bindViewFieldList == null) {
                    bindViewFieldList = new ArrayList<>();
                    class_field.put(clazz, bindViewFieldList);
                }
                bindViewFieldList.add(bindViewField);
            }

            //
            for (Map.Entry<TypeElement, List<VariableElement>> entry : class_field.entrySet()) {
                TypeElement clazz = entry.getKey();
                List<VariableElement> bindViewFieldList = entry.getValue();

                // Class `Xxx$$BindView`: public class Xxx$$BindView
                TypeSpec.Builder codeClass = TypeSpec.classBuilder(clazz.getSimpleName().toString() + "$$BindView")
                        .addModifiers(Modifier.PUBLIC)
                        .addField(      // Target fields
                                FieldSpec.builder(TypeName.get(clazz.asType()), "mTarget", Modifier.PRIVATE).build()
                        )
                        .addMethod(     // Constructor with target as parameter
                                MethodSpec.constructorBuilder()
                                        .addModifiers(Modifier.PUBLIC)
                                        .addParameter(TypeName.get(clazz.asType()), "target")
                                        .addStatement("mTarget = target")
                                        .build()
                        );

                // Method `bind`: public void bind(View source)
                MethodSpec.Builder bindMethod = MethodSpec.methodBuilder("bind")
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.VOID)
                        .addParameter(ClassName.get("android.view", "View"), "source");
                for (VariableElement bindViewField : bindViewFieldList) {
                    String javaName = bindViewField.getSimpleName().toString();
                    String xmlName = javaName2XmlName(javaName);

                    bindMethod.addStatement("mTarget.$L = $$(source, getResourceId($S))", javaName, xmlName);
                }
                codeClass.addMethod(bindMethod.build());

                // Method `$`: private <T extends View> T $(View view, int id)
                MethodSpec.Builder $Method = MethodSpec.methodBuilder("$")
                        .addModifiers(Modifier.PRIVATE)
                        .addTypeVariable(TypeVariableName.get("T", ClassName.get("android.view", "View")))
                        .returns(TypeVariableName.get("T", ClassName.get("android.view", "View")))
                        .addParameter(ClassName.get("android.view", "View"), "source")
                        .addParameter(TypeName.INT, "id")
                        .addStatement("return (T) source.findViewById(id)");
                codeClass.addMethod($Method.build());

                // Method `getResourceId`: private int getResourceId(String name)
                MethodSpec.Builder getResourceIdMethod = MethodSpec.methodBuilder("getResourceId")
                        .addModifiers(Modifier.PRIVATE)
                        .returns(int.class)
                        .addParameter(String.class, "name")
                        .addStatement("$T rList = new $T()", ParameterizedTypeName.get(List.class, String.class), ParameterizedTypeName.get(ArrayList.class, String.class));
                for (Element rootElement : roundEnvironment.getRootElements()) {
                    String name = rootElement.toString();
                    if (name.endsWith(".R")) {
                        getResourceIdMethod.addStatement("rList.add($S)", name.substring(0, name.length() - 2));
                    }
                }
                getResourceIdMethod
                        .beginControlFlow("for ($T pkg : rList)", String.class)
                        .addStatement("$T id = mTarget.getResources().getIdentifier(name, \"id\", pkg);", int.class)
                        .beginControlFlow("if (id != 0)")
                        .addStatement("return id")
                        .endControlFlow()
                        .endControlFlow()
                        .addStatement("return 0");
                codeClass.addMethod(getResourceIdMethod.build());

                JavaFile.builder(mElementUtils.getPackageOf(clazz).toString(), codeClass.build()).build().writeTo(mFiler);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }

        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> set = new HashSet<>();
        set.add(BindView.class.getCanonicalName());
        return set;
    }

    /**
     * mTv -> tv
     * mFooTv -> tv_foo
     *
     * @param javaName
     * @return
     * @throws Exception
     */
    private String javaName2XmlName(String javaName) throws Exception {
        // mTitleTv ('m' + desc + type)
        Pattern pattern = Pattern.compile("m(.*)([A-Z][a-z]*)");
        Matcher matcher = pattern.matcher(javaName);
        if (!matcher.matches()) {
            throw new Exception(String.format("Naming NOT good(%s), follow: mTitleTv ('m' + desc + type)", javaName));
        }

        String desc = matcher.group(1);
        String type = matcher.group(2);

        String xmlName = type.toLowerCase() + desc;
        return xmlName.replaceAll("[A-Z]", "_$0").toLowerCase();
    }

    private static class BindViewData {
        public String type;
        public String javaName;
        public String xmlName;

        public BindViewData(String type, String javaName, String xmlName) {
            this.type = type;
            this.javaName = javaName;
            this.xmlName = xmlName;
        }
    }
}
