package com.wts.router;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import java.util.Map;

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
        return mManager.open(context, url, 0, token);
    }

    public boolean open(Context context, String url, int flags, String... token) {
        return mManager.open(context, url, flags, token);
    }

    public boolean open(Context context, IRoute route, String... token) {
        return mManager.open(context, route, 0, token);
    }

    public boolean open(Context context, IRoute route, int flags, String... token) {
        return mManager.open(context, route, flags, token);
    }

    public boolean open(Context context, Intent intent) {
        return mManager.open(context, intent, null);
    }

    public Intent makeIntent(Context context, String url) {
        return mManager.makeIntent(context, url);
    }

    public Intent makeIntent(Context context, IRoute route) {
        return mManager.makeIntent(context, route);
    }

    public IRoute[] makeRoute(Class<?> ui, Map<String, Object> params) {
        return mManager.makeRoute(ui, params);
    }

    public void replaceTargetIntent(Intent src, Intent dest) {
        mManager.replaceTargetIntent(src, dest);
    }

    public Intent getTargetIntent(Intent intent) {
        return mManager.getTargetIntent(intent);
    }

    public IRoute getRoute(Bundle bundle) {
        return mManager.getRoute(bundle);
    }

    public IRoute getRoute(Intent intent) {
        return mManager.getRoute(intent);
    }

}
