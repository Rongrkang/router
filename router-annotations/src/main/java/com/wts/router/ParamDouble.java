package com.wts.router;

public final class ParamDouble extends ParamTyped {

    public ParamDouble(String raw) {
        super(raw);
    }

    @Override
    public Double getValue() {
        return Double.parseDouble(getRaw());
    }

    @Override
    public Class<?> getType() {
        return double.class;
    }

}
