package de.berlios.vch.download.webinterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;
import de.berlios.vch.web.servlets.VchHttpServlet;

@Component
public class ConfigServlet extends VchHttpServlet {

    public static String PATH = "/config/downloads";

    @Requires
    private HttpService httpService;

    @Requires
    private ConfigService cs;

    @Requires(filter = "(instance.name=vch.web.download.servlet)")
    private ResourceBundleProvider rbp;

    @Requires
    private TemplateLoader templateLoader;

    @Requires
    private LogService logger;

    private Preferences prefs;

    private BundleContext ctx;

    public ConfigServlet(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();

        if (req.getParameter("save_config") != null) {
            prefs.put("data.dir", req.getParameter("data_dir"));
            prefs.putInt("concurrent_downloads", Integer.parseInt(req.getParameter("concurrent_downloads")));
            addNotify(req, new NotifyMessage(TYPE.INFO, rbp.getResourceBundle().getString("I18N_SETTINGS_SAVED")));
        }

        params.put("TITLE", rbp.getResourceBundle().getString("I18N_DOWNLOADS_CONFIG"));
        params.put("SERVLET_URI", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getServletPath());
        params.put("ACTION", PATH);
        params.put("data_dir", prefs.get("data.dir", "data"));
        params.put("concurrent_downloads", prefs.getInt("concurrent_downloads", 2));
        params.put("NOTIFY_MESSAGES", getNotifyMessages(req));

        String page = templateLoader.loadTemplate("configDownloads.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    @Validate
    public void start() {
        // initialize the preferences
        prefs = cs.getUserPreferences("de.berlios.vch.download");

        registerServlet();
        registerMenu();
    }

    private void registerMenu() {
        // register web interface menu
        WebMenuEntry downloads = new WebMenuEntry();
        downloads.setTitle(rbp.getResourceBundle().getString("I18N_DOWNLOADS"));
        downloads.setPreferredPosition(DownloadsServlet.MENU_POS);
        downloads.setLinkUri("#");
        WebMenuEntry config = new WebMenuEntry(rbp.getResourceBundle().getString("I18N_CONFIGURATION"));
        config.setLinkUri(ConfigServlet.PATH);
        downloads.getChilds().add(config);
        ServiceRegistration sr = ctx.registerService(IWebMenuEntry.class.getName(), downloads, null);
        serviceRegs.add(sr);
    }

    private void registerServlet() {
        // register the config servlet
        try {
            httpService.registerServlet(PATH, this, null, null);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't register servlet", e);
        }
    }

    @Invalidate
    public void stop() {
        // unregister config servlet
        httpService.unregister(PATH);

        // unregister all manually made registrations
        for (Iterator<ServiceRegistration> iterator = serviceRegs.iterator(); iterator.hasNext();) {
            ServiceRegistration sr = iterator.next();
            unregisterService(sr);
            iterator.remove();
        }
    }
}
