package de.berlios.vch.http.handler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.Config;
import de.berlios.vch.http.TemplateLoader;
import de.berlios.vch.i18n.Messages;

public class ConfigHandler extends AbstractHandler {

    @SuppressWarnings("unchecked")
    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");

        String action = (String)params.get("action");
        action = action == null ? "edit" : action;
        
        if("edit".equals(action)) {
            Map<String, Object> tplParams = new HashMap<String, Object>();
            String path = exchange.getRequestURI().getPath();
            tplParams.put("ACTION", path);
            tplParams.put("TITLE", Messages.translate(getClass(), "configuration"));
            tplParams.put("I18N_HELP", getClass().getName() + ".help");
            tplParams.put("I18N_SAVE", getClass().getName() + ".save");
            
            // add errors and messages
            tplParams.put("ERRORS", exchange.getAttribute("errors"));
            tplParams.put("MESSAGES", exchange.getAttribute("messages"));
            
            // create help link
            List<String> pathes = Config.getInstance().getHandlerMapping().getPathes(FileHandler.class); 
            String helpLink = Config.getInstance().getBaseUrl()
                + pathes.get(0)
                + "/help/"
                + Locale.getDefault()
                + "/index.html#configuration";
            tplParams.put("LINK_HELP", helpLink);
            
            Set<Entry<Object, Object>> entries = Config.getInstance().entrySet();
            List<Entry<Object, Object>> entryList = new ArrayList<Entry<Object,Object>>(entries);

            // sort by parameter name
            Collections.sort(entryList, new Comparator<Entry<Object, Object>>() {
                @Override
                public int compare(Entry<Object, Object> o1, Entry<Object, Object> o2) {
                    return o1.getKey().toString().compareTo(o2.getKey().toString());
                }
            });
            
            tplParams.put("CONFIG_PARAMS", entryList);
            String template = TemplateLoader.loadTemplate("configEdit.ftl", tplParams);
            sendResponse(200, template, "text/html");
        } else if("save".equals(action)) {
            for (Iterator iterator = params.keySet().iterator(); iterator.hasNext();) {
                String key = (String) iterator.next();
                // only save params, which exist in Config
                if(Config.getInstance().containsKey(key)) {
                    Config.getInstance().setProperty(key, (String)params.get(key));
                }
            }
            Config.getInstance().save();
            
            params.put("action", "edit");
            String msg = Messages.translate(getClass(), "successfully_saved");
            addMessage(msg);
            doHandle(exchange);
        }
    }
    
    @Override
    protected String getDescriptionKey() {
        return "description";
    }
}
