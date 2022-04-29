package com.wts.router;

public final class ParamBoolean extends ParamTyped {

    public ParamBoolean(String raw) {
        super(raw);
    }

    @Override
    public Boolean getValue() {
        if (isEmpty(getRaw())) {
            return false;
        }
        return Boolean.parseBoolean(getRaw());
    }

    @Override
    public Class<?> getType() {
        return boolean.class;
    }

}
