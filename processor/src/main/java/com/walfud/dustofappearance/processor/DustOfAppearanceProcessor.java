package com.walfud.dustofappearance.processor;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.walfud.dustofappearance.Constants;
import com.walfud.dustofappearance.Utils;
import com.walfud.dustofappearance.annotation.FindView;
import com.walfud.dustofappearance.annotation.OnClick;

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
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@AutoService(Processor.class)
public class DustOfAppearanceProcessor extends AbstractProcessor {

    private Messager mMessager;
    private Filer mFiler;
    private Elements mElementUtils;
    private Types mTypeUtils;

    private static final TypeName TYPE_ANDROID_VIEW = ClassName.get("android.view", "View");
    private static final TypeName TYPE_ANDROID_VIEW_ONCLICKLISTENER = ClassName.get("android.view", "View.OnClickListener");

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
        // Traversal source code
        Map<TypeElement, InjectorData> class_toInject = new HashMap<>();
        for (Element element : roundEnvironment.getElementsAnnotatedWith(FindView.class)) {
            VariableElement findViewElement = (VariableElement) element;
            TypeElement clazz = (TypeElement) findViewElement.getEnclosingElement();

            InjectorData injectorData = class_toInject.get(clazz);
            if (injectorData == null) {
                injectorData = new InjectorData();
                class_toInject.put(clazz, injectorData);
            }
            injectorData.findViewElementList.add(findViewElement);
        }
        for (Element element : roundEnvironment.getElementsAnnotatedWith(OnClick.class)) {
            ExecutableElement onClickElement = (ExecutableElement) element;
            TypeElement clazz = (TypeElement) onClickElement.getEnclosingElement();

            InjectorData injectorData = class_toInject.get(clazz);
            if (injectorData == null) {
                injectorData = new InjectorData();
                class_toInject.put(clazz, injectorData);
            }
            injectorData.onClickElementList.add(onClickElement);
        }

        // Generate injector for each target class
        for (Map.Entry<TypeElement, InjectorData> entry : class_toInject.entrySet()) {
            TypeElement targetClass = entry.getKey();
            InjectorData injectorData = entry.getValue();

            // Class `Xxx$$DustOfAppearance`: public class Xxx$$DustOfAppearance
            TypeSpec.Builder injectorClass = TypeSpec.classBuilder(targetClass.getSimpleName().toString() + "$$" + Constants.CLASS_NAME)
                    .addModifiers(Modifier.PUBLIC)
                    .addField(      // Target fields
                            FieldSpec.builder(TypeName.get(targetClass.asType()), "mTarget", Modifier.PRIVATE).build()
                    )
                    .addMethod(     // Constructor with target as parameter
                            MethodSpec.constructorBuilder()
                                    .addModifiers(Modifier.PUBLIC)
                                    .addParameter(TypeName.get(targetClass.asType()), "target")
                                    .addStatement("mTarget = target")
                                    .build()
                    );

            // Method `findView`: public void findView(View source)
            MethodSpec.Builder findViewMethod = MethodSpec.methodBuilder(Constants.METHOD_FIND_VIEW)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.VOID)
                    .addParameter(TYPE_ANDROID_VIEW, "source");
            for (VariableElement findViewElement : injectorData.findViewElementList) {
                String javaName = findViewElement.getSimpleName().toString();
                String xmlName = javaName2XmlName_findView(javaName);
                CodeBlock findFragment = CodeBlock.builder().add("($T) source.findViewById(source.getResources().getIdentifier($S, \"id\", source.getContext().getPackageName()))", findViewElement.asType(), xmlName).build();
                if (isPackageAccessible(findViewElement)) {
                    findViewMethod.addCode("mTarget.$L = ", javaName)
                            .addCode(findFragment)
                            .addStatement("");
                } else {
                    // Reflect
                    findViewMethod.addCode("$T.$L(mTarget, $S, ", Utils.class, "reflectFieldSet", javaName)
                            .addCode(findFragment)
                            .addStatement(")");
                }
            }
            injectorClass.addMethod(findViewMethod.build());

            // Method `setOnClickListener`: public void setOnClickListener(View source)
            MethodSpec.Builder setOnClickListenerMethod = MethodSpec.methodBuilder(Constants.METHOD_SET_ON_CLICK_LISTENER)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(TypeName.VOID)
                    .addParameter(TYPE_ANDROID_VIEW, "source");
            for (ExecutableElement onClickElement : injectorData.onClickElementList) {
                String javaName = onClickElement.getSimpleName().toString();
                String xmlName = javaName2XmlName_setOnClickListener(javaName);

                MethodSpec.Builder wrapperOnClickBuilder = MethodSpec.methodBuilder("onClick")
                        .addModifiers(Modifier.PUBLIC)
                        .addAnnotation(Override.class)
                        .addParameter(TYPE_ANDROID_VIEW, "view");
                if (isPackageAccessible(onClickElement)) {
                    wrapperOnClickBuilder.addStatement("mTarget.$L(view)", javaName);
                } else {
                    // Reflect
                    wrapperOnClickBuilder.addStatement("$T.$L(mTarget, $S, $T.class, view)", Utils.class, "reflectMethod1Invoke", javaName, TYPE_ANDROID_VIEW);
                }
                setOnClickListenerMethod.addStatement("source.findViewById(source.getResources().getIdentifier($S, \"id\", source.getContext().getPackageName())).setOnClickListener($L)",
                        xmlName, TypeSpec.anonymousClassBuilder("")
                                .superclass(TYPE_ANDROID_VIEW_ONCLICKLISTENER)
                                .addMethod(wrapperOnClickBuilder.build())
                                .build());
            }
            injectorClass.addMethod(setOnClickListenerMethod.build());

            // Generate java file
            try {
                JavaFile.builder(mElementUtils.getPackageOf(targetClass).toString(), injectorClass.build()).build().writeTo(mFiler);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
    private String javaName2XmlName_findView(String javaName) {
        // mTitleTv ('m' + desc + type)
        Pattern pattern = Pattern.compile("m(.*)([A-Z][a-z]*)");
        Matcher matcher = pattern.matcher(javaName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Naming NOT good(%s), follow: mTitleTv ('m' + Desc + Type)", javaName));
        }

        String desc = matcher.group(1);
        String type = matcher.group(2);

        String xmlName = type.toLowerCase() + desc;
        return xmlName.replaceAll("[A-Z]", "_$0").toLowerCase();
    }

    /**
     * mTv -> tv
     * mFooTv -> tv_foo
     *
     * @param javaName
     * @return
     * @throws Exception
     */
    private String javaName2XmlName_setOnClickListener(String javaName) {
        // mTitleTv ('m' + desc + type)
        Pattern pattern = Pattern.compile("onClick(.*)([A-Z][a-z]*)");
        Matcher matcher = pattern.matcher(javaName);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(String.format("Naming NOT good(%s), follow: onClick ('onClick' + Desc + Type)", javaName));
        }

        String desc = matcher.group(1);
        String type = matcher.group(2);

        String xmlName = type.toLowerCase() + desc;
        return xmlName.replaceAll("[A-Z]", "_$0").toLowerCase();
    }

    private boolean isPackageAccessible(Element element) {
        return !element.getModifiers().contains(Modifier.PROTECTED)
                && !element.getModifiers().contains(Modifier.PRIVATE);
    }

    private static class InjectorData {
        public List<VariableElement> findViewElementList = new ArrayList<>();
        public List<ExecutableElement> onClickElementList = new ArrayList<>();
    }
}
