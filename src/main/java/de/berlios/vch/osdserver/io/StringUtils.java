package de.berlios.vch.osdserver.io;


public class StringUtils {
    public static String escape(String s) {
        return s == null ? "" : s.replaceAll("\\\\", "\\\\\\\\")
            .replaceAll("\"", "\\\\\"")
            .replaceAll("'", "\\\\'")
            .replaceAll("\t", "\\\\t")
            .replaceAll("\r", "\\\\r")
            .replaceAll("\n", "\\\\n");
    }
}
