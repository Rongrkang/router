package com.wts.router;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wts.router.BaseRouter.parserQuery;

final class RouteImpl implements IRoute, Parcelable {

    private final String scheme;
    private final String host;
    private String param;
    private final String attach;
    private final String self;
    private final int[] position;
    private final boolean root;

    RouteImpl(String scheme, String host, String param,
              String self, String attach, int[] position,
              boolean root) {
        this.scheme = scheme;
        this.host = host;
        this.param = param;
        this.attach = attach;
        this.self = self;
        this.position = position;
        this.root = root;
    }

    private RouteImpl(RouteImpl routeImpl) {
        this.scheme = routeImpl.scheme;
        this.host = routeImpl.host;
        this.param = routeImpl.param;
        this.attach = routeImpl.attach;
        this.self = routeImpl.self;
        this.position = routeImpl.position;
        this.root = routeImpl.root;
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
    public RouteImpl appendParam(String key, String value) {
        if (TextUtils.isEmpty(param)) {
            this.param += (key + "=" + URLEncoder.encode(value));
        } else {
            this.param += ("&" + key + "=" + URLEncoder.encode(value));
        }
        return this;
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

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(scheme);
        out.writeString(host);
        out.writeString(param);
        out.writeString(attach);
        out.writeString(self);
        out.writeIntArray(position);
        out.writeByte((byte) (root ? 1 : 0));
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
            RouteImpl routeImpl = new RouteImpl(scheme, host, param, attach, self, route, isRoot);
            return routeImpl;
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
        try {
            return (IRoute) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new RuntimeException(ex);
        }
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
    @Override
    public Bundle toParamBundle() {
        Bundle args = new Bundle();
        String param = this.param;
        try {
            Map<String, String> defParams = parserQuery(param);
            for (String str : defParams.keySet()) {
                parseAndPutParams(args, str, defParams.get(str));
            }
        } catch (Exception ignored) {
        }
        return args;
    }

    private static int parseInt(String text) throws NumberFormatException {
        return Integer.parseInt(text);
    }

    private static boolean parseBoolean(String text) {
        if (!"true".equalsIgnoreCase(text) && !"false".equalsIgnoreCase(text)) {
            throw new RuntimeException();
        }
        return Boolean.parseBoolean(text);
    }

    private static short parseShort(String text) throws NumberFormatException {
        return Short.parseShort(text);
    }

    private static long parseLong(String text) throws NumberFormatException {
        return Long.parseLong(text);
    }

    private static float parseFloat(String text) throws NumberFormatException {
        return Float.parseFloat(text);
    }

    private static double parseDouble(String text) throws NumberFormatException {
        return Double.parseDouble(text);
    }

    private static byte parseByte(String text) throws NumberFormatException {
        return Byte.parseByte(text);
    }

    static void parseAndPutParams(Bundle bundle, String key, String param) {
        try {
            Pattern pattern = getParamPattern();
            Matcher matcher = pattern.matcher(param);
            if (matcher.find()) {
                final String value = param.substring(param.indexOf(")") + 1);
                final String defValue = bundle.getString(key);
                if (defValue == null) {
                    bundle.remove(key);
                }
                if (param.startsWith("(byte)")) {
                    byte pv = parseByte(value);
                    try {
                        bundle.putByte(key, parseByte(defValue));
                    } catch (Exception ex) {
                        bundle.putByte(key, pv);
                    }
                } else if (param.startsWith("(short)")) {
                    short pv = parseShort(value);
                    try {
                        bundle.putShort(key, parseShort(defValue));
                    } catch (Exception ex) {
                        bundle.putShort(key, pv);
                    }
                } else if (param.startsWith("(int)")) {
                    int pv = parseInt(value);
                    try {
                        bundle.putInt(key, parseInt(defValue));
                    } catch (Exception ex) {
                        bundle.putInt(key, pv);
                    }
                } else if (param.startsWith("(long)")) {
                    long pv = parseLong(value);
                    try {
                        bundle.putLong(key, parseLong(defValue));
                    } catch (Exception ex) {
                        bundle.putLong(key, pv);
                    }
                } else if (param.startsWith("(float)")) {
                    float pv = parseFloat(value);
                    try {
                        bundle.putFloat(key, parseFloat(defValue));
                    } catch (Exception ex) {
                        bundle.putFloat(key, pv);
                    }
                } else if (param.startsWith("(double)")) {
                    double pv = parseDouble(value);
                    try {
                        bundle.putDouble(key, parseDouble(defValue));
                    } catch (Exception ex) {
                        bundle.putDouble(key, pv);
                    }
                } else if (param.startsWith("(boolean)")) {
                    boolean pv = parseBoolean(value);
                    try {
                        bundle.putBoolean(key, parseBoolean(defValue));
                    } catch (Exception ex) {
                        bundle.putBoolean(key, pv);
                    }
                } else if (param.startsWith("(String)")) {
                    if (!bundle.containsKey(key)) {
                        bundle.putString(key, value);
                    }
                }
            } else {
                if (!bundle.containsKey(key)) {
                    bundle.putString(key, param);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            if (!bundle.containsKey(key)) {
                bundle.putString(key, param);
            }
        }
    }

    private static Pattern mParamPattern;

    private static Pattern getParamPattern() {
        synchronized (RouteImpl.class) {
            if (mParamPattern == null) {
                String regex = "(^\\(byte\\)[-]?[0-9]{1,2}$)|" +
                        "(^\\(short\\)[-]?[0-9]{1,4}$)|" +
                        "(^\\(int\\)[-]?[0-9]+$)|" +
                        "(^\\(long\\)[-]?[0-9]+$)|" +
                        "(^\\(float\\)[-]?[0-9]+([.]{1}[0-9]+){0,1}$)|" +
                        "(^\\(double\\)[-]?[0-9]+([.]{1}[0-9]+){0,1}$)|" +
                        "(^\\(boolean\\)(([tT][rR][uU][eE])|([fF][aA][lL][sS][eE]))$)|" +
                        "(^\\(String\\))";
                mParamPattern = Pattern.compile(regex);
            }
            return mParamPattern;
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
