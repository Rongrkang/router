package com.wts.router;

public final class ParamFloat extends ParamTyped {

    public ParamFloat(String raw) {
        super(raw);
    }

    @Override
    public Float getValue() {
        return Float.parseFloat(getRaw());
    }

    @Override
    public Class<?> getType() {
        return float.class;
    }

}
