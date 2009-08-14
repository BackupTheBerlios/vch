package de.berlios.vch.http.handler;

import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.http.TemplateLoader;
import de.berlios.vch.i18n.Messages;

public class NotFoundHandler extends AbstractHandler {

    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("TITLE", "404 - " + Messages.translate(getClass(), "not_found"));
        params.put("MESSAGE", "");
        String html404 = TemplateLoader.loadTemplate("error.ftl", params);
        sendResponse(404, html404, "text/html");
    }
    
    @Override
    protected String getDescriptionKey() {
        return "description";
    }

}
