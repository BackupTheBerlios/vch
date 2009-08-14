package de.berlios.vch.osdserver.io.response;

public class NotImplemented extends Response {

    public NotImplemented(int code, String message) {
        super(code, message);
    }

    @Override
    public String toString() {
        return "Not implemented - " + getCode() + " " + getMessage();
    }

}
