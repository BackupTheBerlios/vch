package de.berlios.vch.playlist.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class ConfigServlet extends BundleContextServlet {

    public static String PATH = "/config/playlist";
    
    private Preferences prefs;
    
    public ConfigServlet(Preferences prefs) {
        this.prefs = prefs;
    }
    
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        
        if(req.getParameter("save_config") != null) {
            prefs.put("svdrp.host", req.getParameter("svdrp_host"));
            prefs.putInt("svdrp.port", Integer.parseInt(req.getParameter("svdrp_port")));
            
            addNotify(req, new NotifyMessage(TYPE.INFO, i18n.translate("I18N_SETTINGS_SAVED")));
        }
        
        params.put("TITLE", i18n.translate("I18N_PLAYLIST_CONFIG"));
        params.put("SERVLET_URI", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                + req.getServletPath());
        params.put("ACTION", PATH);
        params.put("svdrp_host", prefs.get("svdrp.host", "localhost"));
        params.put("svdrp_port", prefs.get("svdrp.port", "2001"));
        params.put("NOTIFY_MESSAGES", getNotifyMessages(req));
        
        String page = templateLoader.loadTemplate("config.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

}
