package com.wts.router;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.wts.router.BaseRouter.SHORTCUT_PARAM;

public abstract class AbsRouterManager {

    final static String ROOT_SHORTCUT_PARAM = "router_root_shortcut_param";

    private final IRouteTree mTree = IRouteTree.Provider.obtain();

    private final List<BaseRouter> mRouterScheme;

    public AbsRouterManager() {
        mRouterScheme = new ArrayList<>();
    }

    public boolean open(Context context, String url, String... token) {
        return open(context, makeIntent(context, url, token));
    }

    public boolean open(Context context, IRoute router, String... token) {
        return open(context, makeIntent(context, router, token));
    }

    public boolean open(Context context, String url, Intent params, String... token) {
        return open(context, makeIntent(context, url, params, token));
    }

    private boolean open(Context context, Intent intent) {
        if (intent != null) {
            Intent startIntent;
            IRoute router = intent.getParcelableExtra(SHORTCUT_PARAM);
            if (router.getPosition().length == 0) {
                intent.removeExtra(SHORTCUT_PARAM);
                Bundle args = router.toParamBundle();
                if (args != null) {
                    intent.putExtras(args);
                }
            }

            if (!router.isRoot()) {
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
                context.startActivity(startIntent);
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    public Intent makeIntent(Context context, String url, Intent params, String... token) {
        if (url == null) {
            return null;
        }
        url = url.trim();
        if (url.length() == 0) {
            return null;
        }
        int si = url.indexOf("://");
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
                if (params != null) {
                    intent.putExtras(params);
                    intent.setFlags(params.getFlags());
                }
                return intent;
            }
        }

        //step 3: 将url转成URI再去解析
        URI uri = makeURI(url);
        if (uri == null) return null;
        for (BaseRouter routerScheme : mRouterScheme) {
            if (!routerScheme.filter(uri)) continue;
            Intent intent = routerScheme.getIntent(context, uri);
            if (params != null) {
                intent.putExtras(params);
                intent.setFlags(params.getFlags());
            }
            return intent;
        }
        Intent intent = getErrorIntent(context);
        if (intent != null) {
            return intent;
        }
        throw new RuntimeException("Not find error route.");
    }

    public Intent makeIntent(Context context, IRoute router, String... token) {
        if (router == null) return null;
        if (token == null || token.length == 0) {
            token = getDefaultToken();
        }

        boolean checkCorrect = false;
        for (String scheme : token) {
            if (router.getScheme().equalsIgnoreCase(scheme)) {
                checkCorrect = true;
                break;
            }
        }

        if (!checkCorrect) return null;

        for (BaseRouter routerScheme : mRouterScheme) {
            if (!routerScheme.filter(router)) continue;
            return routerScheme.getIntent(context, router);
        }
        return null;
    }

    public Intent makeIntent(Context context, String url, String... token) {
        return makeIntent(context, url, null, token);
    }

    public IRoute[] makeRoute(Class<?> ui, Map<String, Object> params) {
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
        return routers.toArray(new IRoute[routers.size()]);
    }

    protected void addRouter(BaseRouter router) {
        router.attachRouterTree(mTree);
        mRouterScheme.add(router);
        Collections.sort(mRouterScheme);
    }

    public void addRootRouteIntent(Intent src, Intent dest) {
        if (src == null || dest == null) {
            return;
        }
        Parcelable route = src.getParcelableExtra(ROOT_SHORTCUT_PARAM);
        if (route != null) {
            dest.putExtra(ROOT_SHORTCUT_PARAM, route);
        }
    }

    @Nullable
    public IRoute getAndRemoveRoute(Intent intent) {
        if (intent == null) return null;
        IRoute route = intent.getParcelableExtra(SHORTCUT_PARAM);
        if (route != null) {
            intent.removeExtra(SHORTCUT_PARAM);
        }
        return route;
    }


    private URI makeURI(String action) {
        try {
            return URI.create(action);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public abstract String[] getSchemes();

    public abstract Intent getErrorIntent(Context context);

    public abstract boolean hasAliveActivity();

    public abstract String[] getDefaultToken();

}
