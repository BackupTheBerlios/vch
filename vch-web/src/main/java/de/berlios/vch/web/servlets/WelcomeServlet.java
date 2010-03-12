package de.berlios.vch.web.servlets;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class WelcomeServlet extends BundleContextServlet {
    
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("TITLE", i18n.translate("I18N_WELCOME"));
        String page = templateLoader.loadTemplate("welcome.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

}
