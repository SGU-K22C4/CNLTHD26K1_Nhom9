package com.fashion.common.util;

import java.util.UUID;

public class UuidUtils {
    private UuidUtils() {
    }

    public static String generate() {
        return UUID.randomUUID().toString();
    }
}
