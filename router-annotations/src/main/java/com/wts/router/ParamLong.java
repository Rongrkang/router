package com.wts.router;

public final class ParamLong extends ParamTyped {

    public ParamLong(String raw) {
        super(raw);
    }

    @Override
    public Long getValue() {
        return Long.parseLong(getRaw());
    }

    @Override
    public Class<?> getType() {
        return long.class;
    }

}
