package com.aaax.core.id;

import java.util.UUID;

/** ID helpers (core foundation). */
public final class Ids {

    private Ids() {
    }

    public static String uuid() {
        return UUID.randomUUID().toString();
    }
}
