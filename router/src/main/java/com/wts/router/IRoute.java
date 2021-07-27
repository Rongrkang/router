package com.wts.router;

import android.os.Bundle;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface IRoute extends Cloneable, Parcelable {

    String ROUTE_HOST = "route_host";

    String getScheme();

    String getHost();

    String getParam();

    String getAttach();

    String getSelf();

    int[] getPosition();

    boolean isRoot();

    @NonNull
    IRoute clone();

    IRoute appendParam(String key, String value);

    IRoute appendParam(@Nullable String param);

    String toAction();

    @NonNull
    Bundle toParamBundle();

}
