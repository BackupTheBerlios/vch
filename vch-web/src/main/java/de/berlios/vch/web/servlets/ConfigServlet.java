package de.berlios.vch.web.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.ServiceReference;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigServlet extends BundleContextServlet {
    
    private static transient Logger logger = LoggerFactory.getLogger(ConfigServlet.class);
    
    @SuppressWarnings("unchecked")
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        PrintWriter pw = resp.getWriter();

        String action = req.getParameter("action");
        action = action == null ? "edit" : action;
        
        Preferences prefs;
        
        // lookup preferences service
        ServiceReference sr = bundleContext.getServiceReference(PreferencesService.class.getName());
        if(sr != null) {
            PreferencesService prefsService = (PreferencesService) bundleContext.getService(sr);
            prefs = prefsService.getSystemPreferences();
            System.out.println(prefs.toString());
        } else {
            error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Preferences service not available");
            return;
        }
        
        if("edit".equals(action)) {
            Map<String, Object> tplParams = new HashMap<String, Object>();
            String path = req.getRequestURI();
            tplParams.put("ACTION", path);
            tplParams.put("TITLE", i18n.translate("I18N_CONFIGURATION"));
            
            // add errors and messages
            tplParams.put("ERRORS", req.getAttribute("errors"));
            tplParams.put("MESSAGES", req.getAttribute("messages"));
            
            tplParams.put("CONFIG_PARAMS", new LinkedList<Entry<?, ?>>()); // TODO implement
//            Enumeration<?> keys = props.keys();
//            List<Entry<Object, Object>> entryList = new ArrayList<Entry<Object,Object>>(entries);
//
//            // sort by parameter name
//            Collections.sort(entryList, new Comparator<Entry<Object, Object>>() {
//                @Override
//                public int compare(Entry<Object, Object> o1, Entry<Object, Object> o2) {
//                    return o1.getKey().toString().compareTo(o2.getKey().toString());
//                }
//            });
//            
//            tplParams.put("CONFIG_PARAMS", entryList);
            String template = templateLoader.loadTemplate("configEdit.ftl", tplParams);
            pw.println(template);
        } else if("save".equals(action)) {
            Enumeration<String> paramNames = req.getParameterNames();
            while(paramNames.hasMoreElements()) {
                String param = (String) paramNames.nextElement();
                if(param.startsWith("param_")) {
                    param = param.substring(6);
                    logger.debug("{}={}", param, req.getParameter(param));
                }
            }
            
//            for (Iterator iterator = params.keySet().iterator(); iterator.hasNext();) {
//                String key = (String) iterator.next();
//                // only save params, which exist in Config
//                if(Config.getInstance().containsKey(key)) {
//                    Config.getInstance().setProperty(key, (String)params.get(key));
//                }
//            }
//            Config.getInstance().save();
//            
//            params.put("action", "edit");
//            String msg = Messages.translate(getClass(), "successfully_saved");
//            addMessage(msg);
//            doHandle(exchange);
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        get(req, resp);
    }
}
