package de.berlios.vch.osdserver.io.response;

import de.berlios.vch.osdserver.io.Message;

/**
 * Superclass for all responses from the VDR
 * 
 * @author <a href="mailto:henrik.niehaus@gmx.de">Henrik Niehaus</a>
 */
public abstract class Response extends Message {

    /**
     * The response code
     */
    protected int code = 0;

    /**
     * The response message
     */
    protected String message = "";

    /**
     * Creates a new Response with response code and message
     * 
     * @param code
     *            The response code of the response
     * @param message
     *            The message of the response
     */
    public Response(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    /**
     * Gets the message of the response
     * 
     * @return the message of the response
     */
    public String getMessage() {
        return message;
    }

    public boolean isOk() {
        return getCode() >= 200 && getCode() < 300;
    }

    public boolean isEvent() {
        return getCode() >= 300 && getCode() < 400;
    }

    public boolean isError() {
        return getCode() >= 400 && getCode() < 500;
    }
    
    public boolean isSingleLine() {
        return getCode() >= 500 && getCode() < 600;
    }
    
    public boolean isMultiline() {
        return getCode() >= 600 && getCode() < 700;
    }
}