package com.wts.router;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.ArrayMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.wts.router.IRoute.INTENT_ROUTER_HOST;

public abstract class BaseRouter implements Comparable<BaseRouter> {

    private final static String TAG = "IRouterScheme";

    protected final static String SHORTCUT_PARAM = "route_shortcut_param";

    IRouteTree mTree;

    final void attachRouterTree(IRouteTree tree) {
        this.mTree = tree;
    }

    public final IRouteTree getRouterTree() {
        return mTree;
    }

    public int getPriority() {
        return 1;
    }

    public abstract String[] getScheme();

    @Nullable
    public Intent getIntent(Context context, @Nullable String url) {
        return null;
    }

    @NonNull
    public Intent getIntent(Context context, @NonNull URI uri) {
        IRoute router = mTree.get(uri.getScheme(), uri.getHost().toLowerCase());
        Intent intent = getIntent(context, router);
        String query = uri.getRawQuery();
        try {
            Map<String, String> map = parserQuery(query);
            for (String str : map.keySet()) {
                intent.putExtra(str, map.get(str));
            }
        } catch (Exception ignored) {
        }
        if (!TextUtils.isEmpty(query)) {
            router.appendParam(query);
        }
        return intent;
    }

    @NonNull
    public Intent getIntent(Context context, @NonNull IRoute router) {
        try {
            Intent intent = new Intent();
            Class<?> clazz = Class.forName(router.getAttach());
            intent.setClass(context, clazz);
            intent.putExtra(INTENT_ROUTER_HOST, router.getHost());
            intent.putExtra(SHORTCUT_PARAM, router);
//            if (router.getPosition().length > 0) {
//                intent.putExtra(SHORTCUT_PARAM, router);
//            } else {
//                Bundle bundle = router.toParamBundle();
//                intent.putExtras(bundle);
//            }

            return intent;
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    final boolean filter(@Nullable URI uri) {
        if (uri == null || TextUtils.isEmpty(uri.getScheme())) return false;
        for (String scheme : getScheme()) {
            if (uri.getScheme().equalsIgnoreCase(scheme)) {
                return mTree.matches(uri);
            }
        }
        return false;
    }

    final boolean filter(@Nullable IRoute router) {
        if (router == null || TextUtils.isEmpty(router.getScheme())) return false;
        for (String scheme : getScheme()) {
            if (router.getScheme().equalsIgnoreCase(scheme)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int compareTo(BaseRouter o) {
        return Integer.compare(o.getPriority(), getPriority());
    }

    static Map<String, String> parserQuery(String query) {
        ArrayMap<String, String> map = new ArrayMap<>();
        if (!isEmpty(query)) {
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
        return map;
    }

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
}
