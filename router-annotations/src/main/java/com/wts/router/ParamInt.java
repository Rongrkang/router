package com.wts.router;

public final class ParamInt extends ParamTyped {

    public ParamInt(String raw) {
        super(raw);
    }

    @Override
    public Integer getValue() {
        return Integer.parseInt(getRaw());
    }

    @Override
    public Class<?> getType() {
        return int.class;
    }

}
