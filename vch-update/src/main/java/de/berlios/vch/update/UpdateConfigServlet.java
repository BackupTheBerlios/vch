package de.berlios.vch.update;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.log.LogService;

import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class UpdateConfigServlet extends BundleContextServlet {

    public static String PATH = "/config/extensions";
    
    private UpdateServlet updateServlet;
    
    public UpdateConfigServlet(UpdateServlet updateServlet) {
        this.updateServlet = updateServlet;
    }
    
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            Map<String, Object> params = new HashMap<String, Object>();
            
            if(req.getParameter("add_obr") != null) {
                String obrUri = req.getParameter("obr");
                try {
                    updateServlet.addOBR(obrUri);
                    addNotify(req, new NotifyMessage(TYPE.INFO, i18n.translate("info.obr_added")));
                } catch (Exception e) {
                    String msg = i18n.translate("error.add_obr");
                    logger.log(LogService.LOG_ERROR, msg, e);
                    addNotify(req, new NotifyMessage(TYPE.ERROR, msg, e));
                }
            } else if(req.getParameter("remove_obrs") != null) {
                String[] obrs = req.getParameterValues("obrs");
                if(obrs != null) {
                    for (String id : obrs) {
                        updateServlet.removeOBR(id);
                    }
                }
            } 
            
            params.put("TITLE", i18n.translate("I18N_CONFIG_TITLE"));
            params.put("SERVLET_URI", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                    + req.getServletPath());
            params.put("OBRS", updateServlet.getOBRs());
            params.put("ACTION", PATH);
            params.put("NOTIFY_MESSAGES", getNotifyMessages(req));
            
            String page = templateLoader.loadTemplate("extensions_config.ftl", params);
            resp.getWriter().print(page);
        } catch (ServiceUnavailableException e) {
            error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getLocalizedMessage());
        } catch (Exception e) {
            error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

}
