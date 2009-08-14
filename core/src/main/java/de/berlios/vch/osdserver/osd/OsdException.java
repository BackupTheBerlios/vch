package de.berlios.vch.osdserver.osd;

import de.berlios.vch.osdserver.io.response.Response;

public class OsdException extends Exception {
    
    private Response res;
    
    public OsdException(Response res) {
        this.res = res;
    }
    
    public int getCode() {
        return res.getCode();
    }
    
    @Override
    public String getMessage() {
        return res.getMessage();
    }
    
    @Override
    public String toString() {
        return res.getCode() + " " + res.getMessage();
    }
}
