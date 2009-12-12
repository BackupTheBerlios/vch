package de.berlios.vch.web;

import java.io.PrintWriter;
import java.io.StringWriter;

public class NotifyMessage {
    public enum TYPE {
        INFO,
        WARNING,
        ERROR
    }
    
    private TYPE type;
    
    private String message;
    
    private Throwable exception;
    
    public NotifyMessage(TYPE type, String message) {
        this(type, message, null);
    }

    public NotifyMessage(TYPE type, String message, Throwable exception) {
        super();
        this.type = type;
        this.message = message;
        this.exception = exception;
    }

    public TYPE getType() {
        return type;
    }

    public void setType(TYPE type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }

    public static String stackTraceToString(Throwable e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
    
    public String getStackTrace() {
        return stackTraceToString(exception).replaceAll("\n", "<br/>");
    }
}
