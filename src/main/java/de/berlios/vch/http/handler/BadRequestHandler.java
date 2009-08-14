package de.berlios.vch.http.handler;

import java.util.HashMap;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.http.TemplateLoader;
import de.berlios.vch.i18n.Messages;

public class BadRequestHandler extends AbstractHandler {

    private String msg;
    
    public BadRequestHandler(String msg) {
        this.msg = msg;
    }
    
    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("MESSAGE", msg);
        params.put("TITLE", "400 - " + Messages.translate(getClass(), "bad_request"));
        String html400 = TemplateLoader.loadTemplate("error.ftl", params);
        sendResponse(400, html400, "text/html");
    }

    @Override
    protected String getDescriptionKey() {
        return "description";
    }
}
