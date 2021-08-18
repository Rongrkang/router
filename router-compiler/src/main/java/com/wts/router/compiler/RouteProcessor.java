package com.wts.router.compiler;

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableSet;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.wts.router.Route;

import net.ltgt.gradle.incap.IncrementalAnnotationProcessor;
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

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
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
public class RouteProcessor extends AbstractProcessor {

    private Filer mFiler;
    private Elements mElements;
    private Types mTypes;
    private Messager mMessager;
    private Map<String, Route> mRoutes;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        mFiler = processingEnv.getFiler();
        mElements = processingEnv.getElementUtils();
        mTypes = processingEnv.getTypeUtils();
        mMessager = processingEnv.getMessager();
        mRoutes = new TreeMap<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.errorRaised() && !roundEnv.processingOver()) {
            processRound(annotations, roundEnv);
        }
        return false;
    }

    private void processRound(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        processRoute(roundEnv);

        final String currentPackageName = Route.class.getPackage().getName();
        ClassName typeIRouteFilter = ClassName.get(currentPackageName, "IRouteTree");
        ClassName typeRouteImpl = ClassName.get(currentPackageName, "RouteImpl");
        ClassName typeIRoute = ClassName.get(currentPackageName, "IRoute");
        ClassName typeTextUtils = ClassName.get("android.text", "TextUtils");
        ClassName typeHashMap = ClassName.get(HashMap.class);
        TypeName mapTypeName = ParameterizedTypeName.get(typeHashMap, ClassName.get(String.class), typeIRoute);
        ClassName typeSet = ClassName.get(Set.class);
        TypeName setTypeName = ParameterizedTypeName.get(typeSet, ClassName.get(String.class));
        TypeName stringArray = ParameterizedTypeName.get(String[].class);

        FieldSpec mTreeField = FieldSpec.builder(mapTypeName, "mTree", Modifier.PRIVATE, Modifier.TRANSIENT).build();

        MethodSpec.Builder initBuilder = MethodSpec.methodBuilder("init").addModifiers(Modifier.PRIVATE);
        int routerSize = makeRouter(initBuilder, typeRouteImpl);
        MethodSpec initMethod = initBuilder.build();

        MethodSpec flux = MethodSpec.constructorBuilder()
                .addStatement("this.$N = new $T<>($L)", "mTree", typeHashMap, routerSize)
                .addStatement("init()")
                .build();

        MethodSpec sizeMethod = MethodSpec.methodBuilder("size")// 添加方法
                .addModifiers(Modifier.PUBLIC)// 添加方法修饰符
                .returns(int.class)
                .addStatement("return $N", "mTree.size()")
                .build();

        MethodSpec isEmptyMethod = MethodSpec.methodBuilder("isEmpty")// 添加方法
                .addModifiers(Modifier.PUBLIC)// 添加方法修饰符
                .returns(boolean.class)
                .addStatement("return $N", "mTree.isEmpty()")
                .build();

        MethodSpec containsMethod = MethodSpec.methodBuilder("contains")// 添加方法
                .addModifiers(Modifier.PUBLIC)// 添加方法修饰符
                .addParameter(String.class, "scheme")// 添加方法参数
                .addParameter(String.class, "host")// 添加方法参数
                .returns(boolean.class)
                .addStatement("return $N.containsKey($N+\":\"+$N)", "mTree", "scheme", "host")
                .build();

        MethodSpec getMethod = MethodSpec.methodBuilder("get")// 添加方法
                .addModifiers(Modifier.PUBLIC)// 添加方法修饰符
                .addParameter(String.class, "host")// 添加方法参数
                .returns(typeIRoute)
                .addStatement("IRoute iRoute = $N.get($N)", "mTree", "host")
                .addStatement("if(iRoute!=null)return iRoute.clone()")
                .addStatement("else return null")
                .build();

        MethodSpec getCloneRouterMethod = MethodSpec.methodBuilder("get")// 添加方法
                .addModifiers(Modifier.PUBLIC)// 添加方法修饰符
                .addParameter(String.class, "scheme")// 添加方法参数
                .addParameter(String.class, "host")// 添加方法参数
                .returns(typeIRoute)
                .addStatement("IRoute iRoute = $N.get($N+\":\"+$N)", "mTree", "scheme", "host")
                .addStatement("if(iRoute!=null)return iRoute.clone()")
                .addStatement("else return null")
                .build();

        StringBuilder getSchemesStatement = new StringBuilder();
        getSchemesStatement.append("return new String[]{");
        Set<String> schemes = new HashSet<>();
        for (String name : mRoutes.keySet()) {
            schemes.addAll(Arrays.asList(mRoutes.get(name).scheme()));
        }
        for (String s : schemes) {
            getSchemesStatement.append("\"")
                    .append(s).append("\",");
        }
        if (schemes.size() > 0) {
            getSchemesStatement.delete(getSchemesStatement.length() - 1, getSchemesStatement.length());
        }
        getSchemesStatement.append("}");

        MethodSpec getSchemesMethod = MethodSpec.methodBuilder("getSchemes")// 添加方法
                .addModifiers(Modifier.PUBLIC)// 添加方法修饰符
                .returns(stringArray)
                .addStatement(getSchemesStatement.toString())
                .build();

        MethodSpec keySetMethod = MethodSpec.methodBuilder("keySet")// 添加方法
                .addModifiers(Modifier.PUBLIC)// 添加方法修饰符
                .returns(setTypeName)
                .addStatement("return $N.keySet()", "mTree")
                .build();

        StringBuilder matchesStatement = new StringBuilder();
        matchesStatement.append("if (action != null) { \n")
                .append("for (String scheme : getSchemes()) {\n")
                .append("if (scheme.equalsIgnoreCase(action.getScheme())\n")
                .append("&& !$T.isEmpty(action.getHost())\n")
                .append("&& contains(scheme.toLowerCase(), action.getHost().toLowerCase())) {\n")
                .append(" return true;\n")
                .append("}}}\n")
                .append("return false");

        MethodSpec matchesMethod = MethodSpec.methodBuilder("matches")// 添加方法
                .addModifiers(Modifier.PUBLIC)// 添加方法修饰符
                .addAnnotation(Override.class)
                .addParameter(URI.class, "action")// 添加方法参数
                .returns(boolean.class)
                .addStatement(matchesStatement.toString(), typeTextUtils)
                .build();

        TypeSpec typeSpec = TypeSpec.classBuilder("RouteTree")// 设置要生成的类
                .addModifiers(Modifier.FINAL)
                .addSuperinterface(Cloneable.class)
                .addSuperinterface(Serializable.class)
                .addSuperinterface(typeIRouteFilter)
                .addField(mTreeField)
                .addMethod(flux)
                .addMethod(initMethod)
                .addMethod(sizeMethod)
                .addMethod(isEmptyMethod)
                .addMethod(containsMethod)
                .addMethod(getMethod)
                .addMethod(getCloneRouterMethod)
                .addMethod(getSchemesMethod)
                .addMethod(keySetMethod)
                .addMethod(matchesMethod)
                .build();

        //设置JavaFile
        JavaFile javaFile = JavaFile.builder(currentPackageName, typeSpec).build();
        try {
            javaFile.writeTo(mFiler);// 写入
        } catch (IOException ignored) {
        }
    }


    private void processRoute(RoundEnvironment roundEnv) {
        HashMap<String, String> deRepetition = new HashMap<>();
        TypeMirror activity = mElements.getTypeElement("android.app.Activity").asType();
        TypeMirror fragment = mElements.getTypeElement("androidx.fragment.app.Fragment").asType();
        for (Element element : roundEnv.getElementsAnnotatedWith(Route.class)) {
            TypeElement typeElement = (TypeElement) element;
            //获取className (com.wtsbxw.wts.ui.activities.MainActivity)
            String name = typeElement.getQualifiedName().toString();

            // 获取字段类型 (android.app.Activity)
            TypeMirror typeMirror = element.asType();

            if (!mTypes.isSubtype(typeMirror, activity) &&
                    !mTypes.isSubtype(typeMirror, fragment)) {
                throw new IllegalStateException(name + " must be Activity or Fragment subclass.");
            }

            Route route = element.getAnnotation(Route.class);

            if (mTypes.isSubtype(typeMirror, activity)) {//activity
                if (!"*".equals(route.attach())) {
                    throw new IllegalArgumentException(name + " attach must be '*'.");
                }
                if (route.position().length > 0) {
                    throw new IllegalArgumentException(name + " position length must be 0.");
                }
            } else {//fragment
                if (route.position().length == 0) {
                    throw new IllegalArgumentException(name + " position length must be > 0.");
                }

                String pwd = mElements.getPackageOf(element).asType().toString();

                String clazz = toAbsoluteFile(route.attach(), pwd);

                TypeElement attachElement = mElements.getTypeElement(clazz);
                if (attachElement == null) {
                    throw new IllegalArgumentException(name + " attach not find name: " + clazz);
                }
                if (!mTypes.isSubtype(attachElement.asType(), activity)) {
                    throw new IllegalArgumentException(name + " attach must be activity name: " + clazz);
                }
            }

            for (String s : route.scheme()) {
                for (String h : route.host()) {
                    String repetitionKey = s + ":" + h;
                    if (deRepetition.containsKey(repetitionKey)) {
                        throw new IllegalArgumentException(String.format("%s and %s have the same host",
                                name, deRepetition.get(repetitionKey)));
                    } else {
                        deRepetition.put(repetitionKey, name);
                    }
                }
            }

            mRoutes.put(name, route);
        }
    }

    private int makeRouter(MethodSpec.Builder builder, ClassName router) {
        int i = 0;
        for (String name : mRoutes.keySet()) {
            Route route = mRoutes.get(name);
            final String[] hosts = route.host();
            String[] params = route.param();
            String[] schemes = route.scheme();

            final int[] position = route.position();
            final boolean root = route.root();

            String attach = route.attach();

            if ("*".equals(attach)) {//activity
                attach = name;
            } else {//fragment
                TypeElement element = mElements.getTypeElement(name);
                String pwd = mElements.getPackageOf(element).asType().toString();
                attach = toAbsoluteFile(attach, pwd);
            }

            for (int p = 0; p < hosts.length; p++) {
                for (int k = 0; k < schemes.length; k++) {
                    final String scheme = schemes[k];

                    final String host = hosts[p];
                    String param = "";
                    if (params.length > p) {
                        param = params[p];
                    }

                    builder.addStatement("int[] value$L = new int[$L]", i, position.length);
                    for (int j = 0; j < position.length; j++) {
                        builder.addStatement("value$L[$L] = $L", i, j, position[j]);
                    }
                    builder.addStatement("this.$N.put($S,new $T($S,$S,$S,$S,$S,value$L,$L))", "mTree", scheme.toLowerCase() + ":" + host.toLowerCase(),
                            router, scheme, host, param, name, attach, i, root);
                    i++;
                }
            }
        }
        return i;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        types.add(Route.class.getCanonicalName());// 声明使用的注解
        return types;
    }

    /**
     * '/'        开头是以跟目录开始
     * './' | ''  当前目录开始
     * '../'      上级目录开始
     */
    private static String toAbsoluteFile(String path, String pwd) {
        if (path.startsWith("/")) {
            return path.replace("/", ".").replaceFirst(".", "");
        }
        if (path.startsWith("../")) {
            File packagePath = new File(pwd.replace(".", "/"));
            String parent = packagePath.getParent();
            parent = parent == null ? "" : parent;
            path = path.replaceFirst("../", "");
            return toAbsoluteFile(path, parent.replace(File.separator, "."));
        } else if (path.startsWith("./")) {
            path = path.replaceFirst("./", "");
            return toAbsoluteFile(path, pwd);
        } else {
            StringBuilder builder = new StringBuilder(40);
            if (pwd != null && pwd.length() > 0) {
                builder.append(pwd);
            }

            if (pwd != null && pwd.length() > 0 && path.length() > 0) {
                builder.append(".");
            }

            if (path.length() > 0) {
                builder.append(path);
            }

            return builder.toString();
        }
    }

    public static void main(String[] args) {
        String path = "../../../../../aa.bb.cc";
        String pwd = "11.22.33.44";
        String target = toAbsoluteFile(path, pwd);
        System.out.println(target);
    }
}
