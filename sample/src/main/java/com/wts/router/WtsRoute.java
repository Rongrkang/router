package com.wts.router;

import static com.wts.router.Router.SCHEME_WTS;

public class WtsRoute extends BaseRouter {


    @Override
    public String[] getScheme() {
        return new String[]{SCHEME_WTS};
    }

}
