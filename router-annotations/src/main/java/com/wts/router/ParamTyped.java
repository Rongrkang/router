package com.wts.router;

public abstract class ParamTyped {

    private final String raw;

    public ParamTyped(String raw) {
        this.raw = raw;
    }

    public abstract Object getValue();

    public abstract Class<?> getType();

    protected final String getRaw() {
        return raw;
    }

    static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

}
