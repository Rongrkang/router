package com.wts.router;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;

public abstract class BaseRouter implements Comparable<BaseRouter> {

    final static String ROUTE_PARAM = "route_shortcut_param";

    IRouteTree mTree;

    final void attachRouteTree(IRouteTree tree) {
        this.mTree = tree;
    }

    public final IRouteTree getRouteTree() {
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
        IRoute route = mTree.get(uri.getScheme(), uri.getHost().toLowerCase());
        String query = uri.getRawQuery();
        if (!TextUtils.isEmpty(query)) {
            route.appendParam(query);
        }
        String fragment = uri.getFragment();
        if (!TextUtils.isEmpty(fragment)) {
            route.appendParam("#", fragment);
        }
        return getIntent(context, route);
    }

    @NonNull
    public Intent getIntent(Context context, @NonNull IRoute route) {
        try {
            Intent intent = new Intent();
            Class<?> clazz = Class.forName(route.getAttach());
            intent.setClass(context, clazz);
            intent.putExtra(ROUTE_PARAM, route);
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

    public static boolean isEmpty(CharSequence str) {
        return str == null || str.length() == 0;
    }
}
