package com.wts.router;

import android.content.Context;
import android.content.Intent;

import static com.wts.router.Router.SCHEME_WTS;

class RouterManager extends AbsRouterManager {


    RouterManager() {
        addRouter(new WtsRoute());
    }

    @Override
    public String[] getSchemes() {
        return new String[]{SCHEME_WTS};
    }

    @Override
    public boolean hasAliveActivity() {
        return true;
    }

    @Override
    public Intent getErrorIntent(Context context) {
        return new Intent(context, RouteNotFindActivity.class);
    }

    @Override
    public String[] getDefaultToken() {
        return new String[]{SCHEME_WTS};
    }

}
