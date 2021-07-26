package com.wts.router;

import android.text.TextUtils;

import java.io.Serializable;
import java.net.URI;
import java.util.HashMap;
import java.util.Set;

final class RouteTree implements Cloneable, Serializable, IRouteTree {
    private transient HashMap<String, IRoute> mTree;

    RouteTree() {
        this.mTree = new HashMap<>(66);
        init();
    }

    private void init() {
    }

    public int size() {
        return mTree.size();
    }

    public boolean isEmpty() {
        return mTree.isEmpty();
    }

    public boolean contains(String scheme, String host) {
        return mTree.containsKey(scheme + ":" + host);
    }

    public IRoute get(String host) {
        IRoute router = mTree.get(host);
        if (router != null) {
            return router.clone();
        }
        return null;
    }

    public IRoute get(String scheme, String host) {
        IRoute router = mTree.get(scheme + ":" + host);
        if (router != null) {
            return router.clone();
        }
        return null;
    }


    public Set<String> keySet() {
        return mTree.keySet();
    }

    @Override
    public boolean matches(URI action) {
        if (action != null) {
            for (String scheme : getSchemes()) {
                if (scheme.equalsIgnoreCase(action.getScheme())
                        && !TextUtils.isEmpty(action.getHost())
                        && contains(scheme.toLowerCase(), action.getHost().toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String[] getSchemes() {
        return new String[0];
    }

}
