package de.berlios.vch.parser.exceptions;

import java.util.List;

public class NoSupportedVideoFoundException extends Exception {

    private String uri;
    private List<String> formats;
    
    public NoSupportedVideoFoundException(String uri, List<String> formats) {
        this.uri = uri;
        this.formats = formats;
    }

    @Override
    public String getMessage() {
        return "No supported video format found on page";
    }
    
    public String getUri() {
        return uri;
    }
    
    public List<String> getFormats() {
        return formats;
    }
}
