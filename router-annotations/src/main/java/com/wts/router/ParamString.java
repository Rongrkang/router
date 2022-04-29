package com.wts.router;

public final class ParamString extends ParamTyped {

    public ParamString(String raw) {
        super(raw);
    }

    @Override
    public String getValue() {
        return getRaw();
    }

    @Override
    public Class<?> getType() {
        return String.class;
    }

}
