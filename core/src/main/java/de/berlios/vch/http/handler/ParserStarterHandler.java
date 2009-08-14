package de.berlios.vch.http.handler;

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.Config;
import de.berlios.vch.RSSFeedCatcher;
import de.berlios.vch.http.TemplateLoader;
import de.berlios.vch.i18n.Messages;

public class ParserStarterHandler extends AbstractHandler {

    private String baseUrl = Config.getInstance().getBaseUrl()
        + Config.getInstance().getHandlerMapping().getFeedPath() 
        + "?link=";
    private String encoding = Config.getInstance().getProperty("default.encoding");
    
    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        List<String> links = RSSFeedCatcher.getInstance().startParsers();
        if( links != null ) {
            StringBuilder sb = new StringBuilder(Messages.translate(getClass(), "parser_finished"));
            for (Iterator<String> iterator = links.iterator(); iterator.hasNext();) {
                String link = iterator.next();
                sb.append(baseUrl);
                sb.append(URLEncoder.encode(link, encoding));
                sb.append("\n");
            }
            sendResponse(200, sb.toString(), "text/plain");
        } else {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("TITLE", "503 - " + Messages.translate(getClass(), "not_available"));
            params.put("MESSAGE", Messages.translate(getClass(), "parser_already_running"));
            String html = TemplateLoader.loadTemplate("error.ftl", params);
            sendResponse(503, html, "text/html");
        }
    }
    
    @Override
    protected String getDescriptionKey() {
        return "description";
    }
}