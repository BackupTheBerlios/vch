package de.berlios.vch.osdserver;

import java.util.UUID;

public class ID {
    public static String randomId() {
        return "a" + UUID.randomUUID().toString().replaceAll("-", "");
    }
}
