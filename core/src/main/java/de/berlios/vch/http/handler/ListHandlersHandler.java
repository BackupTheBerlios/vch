package de.berlios.vch.http.handler;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.Config;
import de.berlios.vch.http.HandlerMapping;
import de.berlios.vch.http.TemplateLoader;
import de.berlios.vch.i18n.Messages;

public class ListHandlersHandler extends AbstractHandler {

    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        HandlerMapping mapping = Config.getInstance().getHandlerMapping();
        Set<HandlerDescription> handlers = new HashSet<HandlerDescription>();
        for (Iterator<String> iterator = mapping.values().iterator(); iterator.hasNext();) {
            String handlerName = iterator.next();
            AbstractHandler handler = (AbstractHandler) Class.forName(handlerName).newInstance();
            handlers.add(handler.getDescription());
        }
        Map<String,Object> params = new HashMap<String, Object>();
        params.put("TITLE", Messages.translate(getClass(), "title"));
        params.put("I18N_TH_URL", getClass().getName() + ".th_url");
        params.put("I18N_TH_DESC", getClass().getName() + ".th_desc");
        params.put("HANDLER_LIST", handlers);
        String body = TemplateLoader.loadTemplate("listHandlers.ftl", params);
        sendResponse(200, body, "text/html");
    }

    @Override
    protected String getDescriptionKey() {
        return "description";
    }
}
