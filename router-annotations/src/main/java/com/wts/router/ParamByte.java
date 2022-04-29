package com.wts.router;

public final class ParamByte extends ParamTyped {

    public ParamByte(String raw) {
        super(raw);
    }

    @Override
    public Byte getValue() {
        return Byte.parseByte(getRaw());
    }

    @Override
    public Class<?> getType() {
        return byte.class;
    }

}
