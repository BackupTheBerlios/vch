package de.berlios.vch.download.webinterface.handler;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface RequestHandler {
    void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;

    boolean acceptRequest(String action, boolean json);
}
