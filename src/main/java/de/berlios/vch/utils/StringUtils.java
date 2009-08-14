package de.berlios.vch.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StringUtils {
    public static String stackTraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
