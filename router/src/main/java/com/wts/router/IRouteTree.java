package com.wts.router;

import java.net.URI;
import java.util.Set;

public interface IRouteTree {

    boolean matches(URI action);

    IRoute get(String scheme, String host);

    String[] getSchemes();

    int size();

    boolean isEmpty();

    boolean contains(String scheme, String host);

    IRoute get(String host);

    Set<String> keySet();


    class Provider {
        static IRouteTree obtain() {
            try {
                return (IRouteTree) Class.forName("com.wts.router.RouteTree").newInstance();
            } catch (Exception ex) {
                return null;
            }
        }
    }

}
