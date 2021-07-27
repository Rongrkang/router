package com.wts.router;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.Nullable;

import java.util.Map;

import static com.wts.router.IRoute.INTENT_ROUTER_HOST;

public final class Router {

    public final static String SCHEME_WTS = "wts";

    private static Router mInstance;

    private final RouterManager mManager;

    private Router() {
        mManager = new RouterManager();
    }

    public static Router getInstance() {
        if (mInstance == null) {
            mInstance = new Router();
        }
        return mInstance;
    }

    public boolean open(Context context, String url, String... token) {
        return mManager.open(context, url, token);
    }

    public boolean open(Context context, IRoute route, String... token) {
        return mManager.open(context, route, token);
    }

    public boolean open(Context context, String url, Intent params, String... token) {
        return mManager.open(context, url, params, token);
    }

    public Intent makeIntent(Context context, String url) {
        return mManager.makeIntent(context, url);
    }

    public Intent makeIntent(Context context, String url, Intent params) {
        return mManager.makeIntent(context, url, params);
    }

    public Intent makeIntent(Context context, IRoute route) {
        return mManager.makeIntent(context, route);
    }

    public IRoute[] makeRoute(Class<?> ui, Map<String, Object> params) {
        return mManager.makeRoute(ui, params);
    }

    public void addRootRouteIntent(Intent src, Intent dest) {
        mManager.addRootRouteIntent(src, dest);
    }

    public String getRouteHost(Intent intent) {
        return intent.getStringExtra(INTENT_ROUTER_HOST);
    }

    @Nullable
    public IRoute getAndRemoveRoute(Intent intent) {
        return mManager.getAndRemoveRoute(intent);
    }

}
