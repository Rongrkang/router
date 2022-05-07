package com.wts.router;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.wts.router.BaseRouter.ROUTE_PARAM;

public abstract class AbsRouterManager {

    final static String ROOT_SHORTCUT_PARAM = "route_root_shortcut_param";

    private final IRouteTree mTree = IRouteTree.Provider.obtain();

    private final List<BaseRouter> mRouterScheme;

    public AbsRouterManager() {
        mRouterScheme = new ArrayList<>();
    }

    public boolean open(Context context, String url, int flags, String... token) {
        Intent intent = makeIntent(context, url, token);
        if (intent != null) {
            intent.setFlags(flags);
            return open(context, intent, null);
        }
        return false;
    }

    public boolean open(Context context, IRoute route, int flags, String... token) {
        Intent intent = makeIntent(context, route, token);
        if (intent != null) {
            intent.setFlags(flags);
            return open(context, intent, null);
        }
        return false;
    }

    public final boolean open(Context context, Intent intent, @Nullable Bundle options) {
        if (intent != null) {
            Intent startIntent;
            IRoute route = intent.getParcelableExtra(ROUTE_PARAM);
            if (route != null && (route.getPosition() == null || route.getPosition().length == 0)) {
                intent.removeExtra(ROUTE_PARAM);
                Bundle args = route.toParamBundle();
                intent.putExtras(args);
            }

            if (route != null && !route.isRoot()) {
                if (!hasAliveActivity()) {
                    startIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
                    if (startIntent != null) {
                        startIntent.putExtra(ROOT_SHORTCUT_PARAM, intent);
                    } else {
                        startIntent = intent;
                    }
                } else {
                    startIntent = intent;
                }
            } else {
                startIntent = intent;
            }

            if (!(context instanceof Activity)) {
                startIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            }
            try {
                context.startActivity(startIntent, options);
                return true;
            } catch (ActivityNotFoundException ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public final Intent makeIntent(Context context, String url, String... token) {
        if (url == null) {
            return null;
        }
        url = url.trim();
        if (url.length() == 0) {
            return null;
        }
        int si = url.indexOf(":");
        if (si <= 0) {
            return null;
        }

        if (token == null || token.length == 0) {
            token = getDefaultToken();
        }

        String s = url.substring(0, si);

        //step 1: 不在token中的都不处理
        if (!Arrays.asList(token).contains(s)) {
            return null;
        }

        //step 2:让那些不能转化URI的先执行一遍如果可以返回Intent就不继续执行了
        for (BaseRouter routerScheme : mRouterScheme) {
            if (!Arrays.asList(routerScheme.getScheme()).contains(s)) continue;
            Intent intent = routerScheme.getIntent(context, url);
            if (intent != null) {
                return intent;
            }
        }

        //step 3: 将url转成URI再去解析
        URI uri = makeURI(url);
        if (uri == null) return null;
        for (BaseRouter routerScheme : mRouterScheme) {
            if (!routerScheme.filter(uri)) continue;
            return routerScheme.getIntent(context, uri);
        }
        Intent intent = getErrorIntent(context);
        if (intent != null) {
            return intent;
        }
        throw new RuntimeException("Not find error route.");
    }

    public final Intent makeIntent(Context context, IRoute route, String... token) {
        if (route == null) return null;
        if (token == null || token.length == 0) {
            token = getDefaultToken();
        }

        boolean checkCorrect = false;
        for (String scheme : token) {
            if (route.getScheme().equalsIgnoreCase(scheme)) {
                checkCorrect = true;
                break;
            }
        }

        if (!checkCorrect) return null;

        for (BaseRouter routerScheme : mRouterScheme) {
            if (!routerScheme.filter(route)) continue;
            return routerScheme.getIntent(context, route);
        }
        return null;
    }

    public final IRoute[] makeRoute(Class<?> ui, @Nullable Map<String, Object> params) {
        if (ui == null) return null;
        String className = ui.getName();
        StringBuilder builder = new StringBuilder();
        if (params != null) {
            for (String key : params.keySet()) {
                try {
                    String value = URLEncoder.encode(params.get(key) + "", "UTF-8");
                    builder.append(key).append("=").append(value).append("&");
                } catch (Exception ignored) {
                }
            }
        }
        int length = builder.length();
        if (length > 0) {
            builder.delete(length - 1, length);
        }
        List<IRoute> routers = new ArrayList<>();
        for (String schemehost : mTree.keySet()) {
            IRoute iRoute = mTree.get(schemehost);
            if (TextUtils.equals(iRoute.getSelf(), className)) {
                routers.add(iRoute.appendParam(builder.toString()));
            }
        }
        return routers.toArray(new IRoute[0]);
    }

    protected void addRouter(BaseRouter router) {
        router.attachRouteTree(mTree);
        mRouterScheme.add(router);
        Collections.sort(mRouterScheme);
    }

    public void replaceTargetIntent(Intent src, Intent dest) {
        if (src == null || dest == null) {
            return;
        }
        Parcelable route = src.getParcelableExtra(ROOT_SHORTCUT_PARAM);
        if (route != null) {
            dest.putExtra(ROOT_SHORTCUT_PARAM, route);
        }
    }

    public boolean hasTargetIntent(Intent intent) {
        if (intent == null) return false;
        return intent.hasExtra(ROOT_SHORTCUT_PARAM);
    }

    @Nullable
    public Intent getTargetIntent(Intent intent) {
        if (hasTargetIntent(intent)) {
            Intent result = intent.getParcelableExtra(ROOT_SHORTCUT_PARAM);
            intent.removeExtra(ROOT_SHORTCUT_PARAM);
            return result;
        }
        return null;
    }

    public boolean hasRoute(Bundle bundle) {
        if (bundle == null) return false;
        return bundle.containsKey(ROUTE_PARAM);
    }

    public boolean hasRoute(Intent intent) {
        if (intent == null) return false;
        return intent.hasExtra(ROUTE_PARAM);
    }

    @Nullable
    public IRoute getRoute(Bundle bundle) {
        if (hasRoute(bundle)) {
            IRoute route = bundle.getParcelable(ROUTE_PARAM);
            bundle.remove(ROUTE_PARAM);
            return route;
        }
        return null;
    }

    @Nullable
    public IRoute getRoute(Intent intent) {
        if (hasRoute(intent)) {
            IRoute route = intent.getParcelableExtra(ROUTE_PARAM);
            intent.removeExtra(ROUTE_PARAM);
            return route;
        }
        return null;
    }

    public abstract String[] getSchemes();

    public abstract Intent getErrorIntent(Context context);

    public abstract boolean hasAliveActivity();

    public abstract String[] getDefaultToken();


    private URI makeURI(String action) {
        try {
            return URI.create(action);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
