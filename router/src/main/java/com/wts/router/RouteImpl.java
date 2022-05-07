package com.wts.router;

import android.os.Binder;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Size;
import android.util.SizeF;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Map;

final class RouteImpl implements IRoute, Parcelable {

    private final String scheme;
    private final String host;
    private final String attach;
    private final String self;
    private final int[] position;
    private final boolean root;

    private String param;
    private ArrayMap<String, String> paramTyped;

    RouteImpl(String scheme, String host, String param,
              String self, String attach, int[] position,
              boolean root, ArrayMap<String, String> paramTyped) {
        this.scheme = scheme;
        this.host = host;
        this.param = param;
        this.attach = attach;
        this.self = self;
        this.position = position;
        this.root = root;
        this.paramTyped = paramTyped;
    }

    private RouteImpl(RouteImpl routeImpl) {
        this.scheme = routeImpl.scheme;
        this.host = routeImpl.host;
        this.param = routeImpl.param;
        this.attach = routeImpl.attach;
        this.self = routeImpl.self;
        if (routeImpl.position != null) {
            this.position = new int[routeImpl.position.length];
            if (this.position.length > 0) {
                System.arraycopy(routeImpl.position, 0, this.position, 0, this.position.length);
            }
        } else {
            this.position = null;
        }
        this.root = routeImpl.root;
        if (routeImpl.paramTyped != null) {
            this.paramTyped = new ArrayMap<>(routeImpl.paramTyped);
        }
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getAttach() {
        return attach;
    }

    public String getSelf() {
        return self;
    }

    @Override
    public int[] getPosition() {
        return position;
    }

    @Override
    public boolean isRoot() {
        return root;
    }

    @Override
    public String getParam() {
        return param;
    }

    @Override
    public RouteImpl appendParam(String param) {
        if (TextUtils.isEmpty(this.param)) {
            this.param += param;
        } else {
            this.param += ("&" + param);
        }
        return this;
    }

    @Override
    public IRoute appendParam(String key, String value) {
        if (TextUtils.isEmpty(param)) {
            this.param += (key + "=" + URLEncoder.encode(value));
        } else {
            this.param += ("&" + key + "=" + URLEncoder.encode(value));
        }
        return this;
    }

    @Override
    public IRoute appendParam(String key, String value, Class<? extends ParamTyped> typed) {
        this.appendParam(key, value);
        addParamTyped(key, typed);
        return this;
    }

    private void addParamTyped(String key, Class<? extends ParamTyped> typed) {
        if (this.paramTyped == null) {
            this.paramTyped = new ArrayMap<>();
        }
        this.paramTyped.put(key, typed.getName());
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(scheme);
        out.writeString(host);
        out.writeString(param);
        out.writeString(attach);
        out.writeString(self);
        out.writeIntArray(position);
        out.writeByte((byte) (root ? 1 : 0));
        if (paramTyped == null) {
            out.writeStringList(null);
            out.writeStringList(null);
        } else {
            out.writeStringList(new ArrayList<>(paramTyped.keySet()));
            out.writeStringList(new ArrayList<>(paramTyped.values()));
        }
    }

    public static final Creator<RouteImpl> CREATOR = new Creator<RouteImpl>() {

        public RouteImpl createFromParcel(Parcel in) {
            final String scheme = in.readString();
            final String host = in.readString();
            final String param = in.readString();
            final String attach = in.readString();
            final String self = in.readString();
            int[] route = in.createIntArray();
            final boolean isRoot = in.readByte() != 0;

            ArrayMap<String, String> paramTyped = null;

            ArrayList<String> paramTypedKey = in.createStringArrayList();
            ArrayList<String> paramTypedValue = in.createStringArrayList();
            if (paramTypedKey != null && paramTypedValue != null) {
                paramTyped = new ArrayMap<>();
                for (int i = 0; i < paramTypedKey.size(); i++) {
                    paramTyped.put(paramTypedKey.get(i), paramTypedValue.get(i));
                }
            }

            return new RouteImpl(scheme, host, param, attach, self, route, isRoot, paramTyped);
        }

        public RouteImpl[] newArray(int size) {
            return new RouteImpl[size];
        }
    };

    public int describeContents() {
        return 0;
    }

    @Override
    @NonNull
    public IRoute clone() {
        return new RouteImpl(this);
    }

    @Override
    public String toAction() {
        StringBuilder action = new StringBuilder(scheme);
        action.append("://").append(host);
        if (!TextUtils.isEmpty(param)) {
            action.append("?").append(param);
        }
        return action.toString();
    }

    @NonNull
    public Bundle toParamBundle() {
        Bundle args = new Bundle();
        String param = this.param;
        Map<String, String> params = parserQuery(param);
        for (String key : params.keySet()) {
            if (this.paramTyped != null && this.paramTyped.size() > 0) {
                putParams(args, key, params.get(key), this.paramTyped);
            } else {
                args.putString(key, params.get(key));
            }
        }
        return args;
    }

//    @NonNull
//    @Override
//    public Bundle toParamBundle(@Nullable Bundle params) {
//        Bundle args = toParamBundle();
//        if (params != null && params.containsKey(ROUTE_EXTRA_PARAM)) {
//            Bundle extra = params.getBundle(ROUTE_EXTRA_PARAM);
//            params.remove(ROUTE_EXTRA_PARAM);
//            if (extra != null) {
//                args.putAll(extra);
//            }
//        }
//        return args;
//    }

    static Map<String, String> parserQuery(String query) {
        ArrayMap<String, String> map = new ArrayMap<>();
        try {
            if (!TextUtils.isEmpty(query)) {
                String[] split = query.split("&");
                for (String str : split) {
                    if (!str.contains("=")) {
                        continue;
                    }
                    int index = str.indexOf("=");
                    String value = str.length() >= index + 1 ? str.substring(index + 1) : "";
                    String key = str.substring(0, index);
                    try {
                        value = URLDecoder.decode(value, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        //nothing
                    }
                    map.put(key, value);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return map;
    }

    static void putParams(Bundle bundle, String key, String value, Map<String, String> paramTyped) {
        try {
            String typedName = paramTyped.get(key);
            if (TextUtils.isEmpty(typedName)) {
                bundle.putString(key, value);
            } else {
                Param param = new Param(key, value, typedName);
                Object typedValue = param.getValue();
                if (param.getType().isArray()) {
                    if (param.getType() == String[].class) {
                        bundle.putStringArray(param.getKey(), (String[]) typedValue);
                    } else if (param.getType() == int[].class) {
                        bundle.putIntArray(param.getKey(), (int[]) typedValue);
                    } else if (param.getType() == boolean[].class) {
                        bundle.putBooleanArray(param.getKey(), (boolean[]) typedValue);
                    } else if (param.getType() == long[].class) {
                        bundle.putLongArray(param.getKey(), (long[]) typedValue);
                    } else if (param.getType() == double[].class) {
                        bundle.putDoubleArray(param.getKey(), (double[]) typedValue);
                    } else if (param.getType() == float[].class) {
                        bundle.putFloatArray(param.getKey(), (float[]) typedValue);
                    } else if (param.getType() == short[].class) {
                        bundle.putShortArray(param.getKey(), (short[]) typedValue);
                    } else if (param.getType() == byte[].class) {
                        bundle.putByteArray(param.getKey(), (byte[]) typedValue);
                    } else if (param.getType() == char[].class) {
                        bundle.putCharArray(param.getKey(), (char[]) typedValue);
                    } else if (param.getType() == CharSequence[].class) {
                        bundle.putCharSequenceArray(param.getKey(), (CharSequence[]) typedValue);
                    } else if (Parcelable[].class.isAssignableFrom(param.getType())) {
                        bundle.putParcelableArray(param.getKey(), (Parcelable[]) typedValue);
                    }
                } else {
                    if (param.getType() == String.class) {
                        bundle.putString(param.getKey(), (String) typedValue);
                    } else if (param.getType() == int.class || param.getType() == Integer.class) {
                        bundle.putInt(param.getKey(), (int) typedValue);
                    } else if (param.getType() == boolean.class || param.getType() == Boolean.class) {
                        bundle.putBoolean(param.getKey(), (boolean) typedValue);
                    } else if (param.getType() == long.class || param.getType() == Long.class) {
                        bundle.putLong(param.getKey(), (Long) typedValue);
                    } else if (param.getType() == double.class || param.getType() == Double.class) {
                        bundle.putDouble(param.getKey(), (Double) typedValue);
                    } else if (param.getType() == float.class || param.getType() == Float.class) {
                        bundle.putFloat(param.getKey(), (Float) typedValue);
                    } else if (param.getType() == short.class || param.getType() == Short.class) {
                        bundle.putShort(param.getKey(), (Short) typedValue);
                    } else if (param.getType() == byte.class || param.getType() == Byte.class) {
                        bundle.putByte(param.getKey(), (Byte) typedValue);
                    } else if (param.getType() == char.class || param.getType() == Character.class) {
                        bundle.putChar(param.getKey(), (Character) typedValue);
                    } else if (param.getType() == CharSequence.class) {
                        bundle.putCharSequence(param.getKey(), (CharSequence) typedValue);
                    } else if (param.getType() == Size.class) {
                        bundle.putSize(param.getKey(), (Size) typedValue);
                    } else if (param.getType() == SizeF.class) {
                        bundle.putSizeF(param.getKey(), (SizeF) typedValue);
                    } else if (param.getType() == Bundle.class) {
                        bundle.putBundle(param.getKey(), (Bundle) typedValue);
                    } else if (param.getType() == Binder.class) {
                        bundle.putBinder(param.getKey(), (Binder) typedValue);
                    } else if (Parcelable.class.isAssignableFrom(param.getType())) {
                        bundle.putParcelable(param.getKey(), (Parcelable) typedValue);
                    } else if (Serializable.class.isAssignableFrom(param.getType())) {
                        bundle.putSerializable(param.getKey(), (Serializable) typedValue);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static class Param {

        private final String key;
        private final String value;
        private final ParamTyped typed;

        Param(String key, String value) {
            this.key = key;
            this.value = value;
            this.typed = new ParamString(value);
        }

        Param(String key, String value, String typed) {
            this.key = key;
            this.value = value;
            Class<?> clazz = null;
            try {
                clazz = Class.forName(typed);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            if (clazz != null && ParamTyped.class.isAssignableFrom(clazz)) {
                try {
                    Constructor<?> constructor = clazz.getConstructor(String.class);
                    this.typed = (ParamTyped) constructor.newInstance(value);
                } catch (Exception ex) {
                    throw new IllegalArgumentException(ex);
                }
            } else {
                this.typed = new ParamString(value);
            }
        }

        public final String getKey() {
            return key;
        }

        public final Class<?> getType() {
            return typed.getType();
        }

        public final Object getValue() {
            return typed.getValue();
        }

    }

    public static void main(String[] args) {
//        String regex = "^\\(byte\\)[-]?[0-9]{1,2}$";
//        String regex = "^\\(short\\)[-]?[0-9]{1,4}$";
//        String regex = "^\\(int\\)[-]?[0-9]+$";
//        String regex = "^\\(long\\)[-]?[0-9]+$";
//        String regex = "^\\(float\\)[-]?[0-9]+([.]{1}[0-9]+){0,1}$";
//        String regex = "^\\(double\\)[-]?[0-9]+([.]{1}[0-9]+){0,1}$";
//        String regex = "^\\(boolean\\)(([tT][rR][uU][eE])|([fF][aA][lL][sS][eE]))$";
//        Pattern p = Pattern.compile(regex);
//        String check = "(boolean)tRue";
//        System.out.println(p.matcher(check).find());

//        String params = "a=(int)11&b=(short)1&c=(String)&d=(float)-0.1";
//        Map<String, String> map = parserQuery(params);
//        Pattern p = getParamPattern();
//        for (String key : map.keySet()) {
//            String param = map.get(key);
//            if (p.matcher(param).find()) {
//                String value = param.substring(param.indexOf(")") + 1);
//                System.out.println("key:" + key + " value:" + value);
//            } else {
//                System.out.println("key:" + key + " value:" + param);
//            }
//        }

        String action = "http://share?type=1&link=https%3A//bx.wts9999.net/index.html%23/personal/policyMyList&title=%u4E2A%u4EBA%u4E2D%u5FC3-%u68A7%u6850%u6811%u4FDD%u9669%u7F51&content=%u68A7%u6850%u6811%u4FDD%u9669%u7F51%u4E3A%u5BA2%u6237%u63D0%u4F9B%u5BB6%u5EAD%u4FDD%u9669%u89C4%u5212%u3001%u4FDD%u9669%u65B9%u6848%u5B9A%u5236%u3001%u5BB6%u5EAD%u98CE%u9669%u7BA1%u7406%u3001%u5BB6%u5EAD%u8D22%u52A1%u89C4%u5212%u3001%u7406%u8D54%u670D%u52A1%u3002%u68A7%u6850%u6811%u4FDD%u9669%u7F51%uFF0C%u60A8%u8EAB%u8FB9%u7684%u5BB6%u5EAD%u4FDD%u9669%u914D%u7F6E%u4E13%u5BB6%u300224%u5C0F%u65F6%u5BA2%u6237%u670D%u52A1%u70ED%u7EBF400-9955-788%u3002&logo=https%3A//bx.wts999.com/m/lib/images/logo-3.jpg";
        String substring = action.substring(0, 91);
        System.out.println(substring);
        String substring1 = action.substring(91, action.length());
        System.out.println(substring1);
        try {
            String aa = "http://share?type=1&link=https%3A//bx.wts9999.net/index.html%23/personal/policyMyList&title=个人中心-梧桐树保险网&content=梧桐树保险网为客户提供家庭保险规划、保险方案定制、家庭风险管理、家庭财务规划、理赔服务。梧桐树保险网，您身边的家庭保险配置专家。24小时客户服务热线400-9955-788。";
            URI uri = URI.create(aa);
            URL url = new URL(action);
            System.out.println("1111111");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


}
