package de.berlios.vch.android.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.web.servlets.BundleContextServlet;

public class Servlet extends BundleContextServlet {

    private static final long serialVersionUID = 1L;

    public static final String PATH = "/android";
    
    public static final String STATIC_PATH = PATH + "/static";
    
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("TITLE", "VCH Android Interface");
        params.put("STATIC_PATH", STATIC_PATH);
        params.put("NOTIFY_MESSAGES", getNotifyMessages(req));
        String page = templateLoader.loadTemplate("android.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }
}
