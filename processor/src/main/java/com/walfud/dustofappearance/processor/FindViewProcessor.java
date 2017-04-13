package com.walfud.dustofappearance.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.walfud.dustofappearance.Constants;
import com.walfud.dustofappearance.annotation.FindView;

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
public class FindViewProcessor extends AbstractProcessor {

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
            for (Element element : roundEnvironment.getElementsAnnotatedWith(FindView.class)) {
                VariableElement findViewField = (VariableElement) element;
                TypeElement clazz = (TypeElement) findViewField.getEnclosingElement();

                List<VariableElement> findViewFieldList = class_field.get(clazz);
                if (findViewFieldList == null) {
                    findViewFieldList = new ArrayList<>();
                    class_field.put(clazz, findViewFieldList);
                }
                findViewFieldList.add(findViewField);
            }

            //
            for (Map.Entry<TypeElement, List<VariableElement>> entry : class_field.entrySet()) {
                TypeElement clazz = entry.getKey();
                List<VariableElement> findViewFieldList = entry.getValue();

                // Class `Xxx$$DustOfAppearance`: public class Xxx$$DustOfAppearance
                TypeSpec.Builder codeClass = TypeSpec.classBuilder(clazz.getSimpleName().toString() + "$$" + Constants.CLASS_NAME)
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

                // Method `findView`: public void findView(View source)
                MethodSpec.Builder findViewMethod = MethodSpec.methodBuilder(Constants.METHOD_FIND_VIEW)
                        .addModifiers(Modifier.PUBLIC)
                        .returns(TypeName.VOID)
                        .addParameter(ClassName.get("android.view", "View"), "source");
                for (VariableElement findViewField : findViewFieldList) {
                    String javaName = findViewField.getSimpleName().toString();
                    String xmlName = javaName2XmlName(javaName);

                    findViewMethod.addStatement("mTarget.$L = ($T) source.findViewById(mTarget.getResources().getIdentifier($S, \"id\", mTarget.getPackageName()))", javaName, findViewField.asType(), xmlName);
                }
                codeClass.addMethod(findViewMethod.build());
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
        set.add(FindView.class.getCanonicalName());
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
}
