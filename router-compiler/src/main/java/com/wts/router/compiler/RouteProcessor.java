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
import com.wts.router.ParamBoolean;
import com.wts.router.ParamByte;
import com.wts.router.ParamDouble;
import com.wts.router.ParamFloat;
import com.wts.router.ParamInt;
import com.wts.router.ParamLong;
import com.wts.router.ParamShort;
import com.wts.router.ParamString;
import com.wts.router.ParamTyped;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.MirroredTypesException;
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
        ClassName typeHashMap = ClassName.get(LinkedHashMap.class);
        ClassName typeArrayMap = ClassName.get("android.util", "ArrayMap");
        ClassName typeSet = ClassName.get(Set.class);

        TypeName mapTypeName = ParameterizedTypeName.get(typeHashMap, ClassName.get(String.class), typeIRoute);
        TypeName setTypeName = ParameterizedTypeName.get(typeSet, ClassName.get(String.class));
        TypeName stringArray = ParameterizedTypeName.get(String[].class);
        TypeName arrayMap = ParameterizedTypeName.get(typeArrayMap, ClassName.get(String.class), ClassName.get(String.class));

        FieldSpec mTreeField = FieldSpec.builder(mapTypeName, "mTree", Modifier.PRIVATE, Modifier.TRANSIENT).build();

        MethodSpec.Builder initBuilder = MethodSpec.methodBuilder("init").addModifiers(Modifier.PRIVATE);
        int routerSize = makeRouter(initBuilder, typeRouteImpl, arrayMap);
        MethodSpec initMethod = initBuilder.build();

        MethodSpec flux = MethodSpec.constructorBuilder()
                .addStatement("this.$N = new $T<>($L)", "mTree", typeHashMap, routerSize)
                .addStatement("init()")
                .build();

        MethodSpec sizeMethod = MethodSpec.methodBuilder("size")// ????????????
                .addModifiers(Modifier.PUBLIC)// ?????????????????????
                .returns(int.class)
                .addStatement("return $N", "mTree.size()")
                .build();

        MethodSpec isEmptyMethod = MethodSpec.methodBuilder("isEmpty")// ????????????
                .addModifiers(Modifier.PUBLIC)// ?????????????????????
                .returns(boolean.class)
                .addStatement("return $N", "mTree.isEmpty()")
                .build();

        MethodSpec containsMethod = MethodSpec.methodBuilder("contains")// ????????????
                .addModifiers(Modifier.PUBLIC)// ?????????????????????
                .addParameter(String.class, "scheme")// ??????????????????
                .addParameter(String.class, "host")// ??????????????????
                .returns(boolean.class)
                .addStatement("return $N.containsKey($N+\":\"+$N)", "mTree", "scheme", "host")
                .build();

        MethodSpec getMethod = MethodSpec.methodBuilder("get")// ????????????
                .addModifiers(Modifier.PUBLIC)// ?????????????????????
                .addParameter(String.class, "host")// ??????????????????
                .returns(typeIRoute)
                .addStatement("IRoute iRoute = $N.get($N)", "mTree", "host")
                .addStatement("if(iRoute!=null)return iRoute.clone()")
                .addStatement("else return null")
                .build();

        MethodSpec getCloneRouterMethod = MethodSpec.methodBuilder("get")// ????????????
                .addModifiers(Modifier.PUBLIC)// ?????????????????????
                .addParameter(String.class, "scheme")// ??????????????????
                .addParameter(String.class, "host")// ??????????????????
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

        MethodSpec getSchemesMethod = MethodSpec.methodBuilder("getSchemes")// ????????????
                .addModifiers(Modifier.PUBLIC)// ?????????????????????
                .returns(stringArray)
                .addStatement(getSchemesStatement.toString())
                .build();

        MethodSpec keySetMethod = MethodSpec.methodBuilder("keySet")// ????????????
                .addModifiers(Modifier.PUBLIC)// ?????????????????????
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

        MethodSpec matchesMethod = MethodSpec.methodBuilder("matches")// ????????????
                .addModifiers(Modifier.PUBLIC)// ?????????????????????
                .addAnnotation(Override.class)
                .addParameter(URI.class, "action")// ??????????????????
                .returns(boolean.class)
                .addStatement(matchesStatement.toString(), typeTextUtils)
                .build();

        TypeSpec typeSpec = TypeSpec.classBuilder("RouteTree")// ?????????????????????
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

        //??????JavaFile
        JavaFile javaFile = JavaFile.builder(currentPackageName, typeSpec).build();
        try {
            javaFile.writeTo(mFiler);// ??????
        } catch (IOException ignored) {
        }
    }


    private void processRoute(RoundEnvironment roundEnv) {
        HashMap<String, String> deRepetition = new HashMap<>();
        TypeMirror activity = mElements.getTypeElement("android.app.Activity").asType();
        TypeMirror fragment = mElements.getTypeElement("androidx.fragment.app.Fragment").asType();
        TypeMirror Void = mElements.getTypeElement("java.lang.Void").asType();
        for (Element element : roundEnv.getElementsAnnotatedWith(Route.class)) {
            TypeElement typeElement = (TypeElement) element;
            //??????className (com.wtsbxw.wts.ui.activities.MainActivity)
            String name = typeElement.getQualifiedName().toString();

            // ?????????????????? (android.app.Activity)
            TypeMirror typeMirror = element.asType();

            if (!mTypes.isSubtype(typeMirror, activity) &&
                    !mTypes.isSubtype(typeMirror, fragment)) {
                throw new IllegalStateException(name + " must be Activity or Fragment subclass.");
            }

            Route route = element.getAnnotation(Route.class);

            TypeMirror attach = getAttach(route);

            if (mTypes.isSubtype(typeMirror, activity)) {//activity
                if (!mTypes.isSameType(attach, Void)) {
                    throw new IllegalArgumentException(name + " attach must be 'Void.class'.");
                }
                if (route.position().length > 0) {
                    throw new IllegalArgumentException(name + " position length must be 0.");
                }
            } else {//fragment
                if (route.position().length == 0) {
                    throw new IllegalArgumentException(name + " position length must be > 0.");
                }


                String clazz = attach.toString();

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

    private int makeRouter(MethodSpec.Builder builder, ClassName router, TypeName map) {
        TypeMirror paramTypedTypeMirror = mElements.getTypeElement(ParamTyped.class.getName()).asType();

        int i = 0;
        for (String name : mRoutes.keySet()) {
            Route route = mRoutes.get(name);
            final String[] hosts = route.host();
            String[] params = route.param();
            String[] schemes = route.scheme();

            final int[] position = route.position();
            final boolean root = route.root();

            String[] paramKey = route.paramKey();
            List<? extends TypeMirror> paramTyped = getParamTyped(route);
            if (paramKey.length != paramTyped.size()) {
                throw new IllegalArgumentException(name + " paramKey length must be equal to paramTyped length.");
            }

            TypeMirror attachType = getAttach(route);
            String attach;

            if ("java.lang.Void".equals(attachType.toString())) {//activity
                attach = name;
            } else {//fragment
                attach = attachType.toString();
            }

            for (int p = 0; p < hosts.length; p++) {
                for (int k = 0; k < schemes.length; k++) {
                    final String scheme = schemes[k];

                    final String host = hosts[p];
                    String param = "";
                    if (params.length > p) {
                        param = params[p];
                    }

                    if (position.length > 0) {
                        builder.addStatement("int[] value$L = new int[$L]", i, position.length);
                        for (int j = 0; j < position.length; j++) {
                            builder.addStatement("value$L[$L] = $L", i, j, position[j]);
                        }
                    }

                    if (paramKey.length > 0) {
                        builder.addStatement("$T paramTyped$L = new $T($L)", map, i, map, paramKey.length);
                        for (int j = 0; j < paramKey.length; j++) {
                            TypeMirror typeMirror = paramTyped.get(j);
                            String paramTypedName = typeMirror.toString();

                            TypeElement typeElement = mElements.getTypeElement(paramTypedName);
                            TypeMirror paramTypeMirror = null;
                            if (typeElement != null) {
                                paramTypeMirror = typeElement.asType();
                            }

                            if (paramTypeMirror != null && mTypes.isSubtype(paramTypeMirror, paramTypedTypeMirror)) {
                                Element element = mTypes.asElement(typeMirror);
                                Set<Modifier> modifiers = element.getModifiers();
                                boolean publicClazz = false;
                                for (Modifier modifier : modifiers) {
                                    if (modifier == Modifier.ABSTRACT) {
                                        throw new IllegalArgumentException(name + " paramTyped element " + paramTypedName + " cannot be an abstract class.");
                                    } else if (modifier == Modifier.PUBLIC) {
                                        publicClazz = true;
                                    }
                                }
                                if (!publicClazz) {
                                    throw new IllegalArgumentException(name + " paramTyped element " + paramTypedName + " must be a public class.");
                                }
                            } else {
                                if ("int".equals(paramTypedName)) {
                                    paramTypedName = ParamInt.class.getName();
                                } else if ("java.lang.String".equals(paramTypedName)) {
                                    paramTypedName = ParamString.class.getName();
                                } else if ("boolean".equals(paramTypedName)) {
                                    paramTypedName = ParamBoolean.class.getName();
                                } else if ("double".equals(paramTypedName)) {
                                    paramTypedName = ParamDouble.class.getName();
                                } else if ("float".equals(paramTypedName)) {
                                    paramTypedName = ParamFloat.class.getName();
                                } else if ("long".equals(paramTypedName)) {
                                    paramTypedName = ParamLong.class.getName();
                                } else if ("byte".equals(paramTypedName)) {
                                    paramTypedName = ParamByte.class.getName();
                                } else if ("short".equals(paramTypedName)) {
                                    paramTypedName = ParamShort.class.getName();
                                } else {
                                    throw new IllegalArgumentException(name + " paramTyped element must " +
                                            "be com.wts.router.ParamTyped subclass, " +
                                            "byte.class, short.class, " +
                                            "int.class, String.class, boolean.class, float.class or long.class.");
                                }
                            }


                            builder.addStatement("paramTyped$L.put($S,$L)", i, paramKey[j], paramTypedName + ".class.getName()");
                        }
                    }

                    builder.addStatement("this.$N.put($S,new $T($S,$S,$S,$L,$S,$L,$L,$L))",
                            "mTree",
                            scheme.toLowerCase() + ":" + host.toLowerCase(),
                            router, scheme, host, param,
                            name + ".class.getName()",
                            attach,
                            position.length > 0 ? "value" + i : null,
                            root,
                            paramKey.length > 0 ? "paramTyped" + i : null);
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
        types.add(Route.class.getCanonicalName());// ?????????????????????
        return types;
    }

    private static List<? extends TypeMirror> getParamTyped(Route route) {
        try {
            route.paramTyped(); // this should throw
        } catch (MirroredTypesException mte) {
            return mte.getTypeMirrors();
        }
        throw new RuntimeException(); // can this never happen ??
    }

    private static TypeMirror getAttach(Route route) {
        try {
            route.attach(); // this should throw
        } catch (MirroredTypeException mte) {
            return mte.getTypeMirror();
        }
        throw new RuntimeException(); // can this never happen ??
    }

    /**
     * '/'        ???????????????????????????
     * './' | ''  ??????????????????
     * '../'      ??????????????????
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
