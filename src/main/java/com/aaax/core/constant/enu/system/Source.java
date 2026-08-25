package com.aaax.core.constant.enu.system;

import java.util.Arrays;

public enum Source {
    SYSTEM,
    CLIENT
    ;

    public static Source get(String source) {
        for (Source value : Source.values()) {
            if (source.equalsIgnoreCase(value.name())){
                return value;
            }
        }
        throw new IllegalArgumentException("Wrong Source value. [" +source+ "] not in->" +  Arrays.asList(Source.values()));
    }
}
