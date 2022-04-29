package com.wts.router;

public final class ParamShort extends ParamTyped {

    public ParamShort(String raw) {
        super(raw);
    }

    @Override
    public Short getValue() {
        return Short.parseShort(getRaw());
    }

    @Override
    public Class<?> getType() {
        return short.class;
    }

}
