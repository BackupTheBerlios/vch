package de.berlios.vch.osdserver.io.response;

public class GenericResponse extends Response {

    public GenericResponse(int code, String message) {
        super(code, message);
    }

    @Override
    public String toString() {
        return getCode() + " " + getMessage();
    }

}
