package de.berlios.vch.playlist.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;
import de.berlios.vch.web.servlets.VchHttpServlet;

@Component
@Provides
public class ConfigServlet extends VchHttpServlet implements ResourceBundleProvider {

    public static String PATH = "/config/playlist";
    @Requires
    private LogService logger;

    @Requires
    private TemplateLoader templateLoader;

    @Requires
    private HttpService httpService;

    @Requires
    private ConfigService cs;
    private Preferences prefs;

    private BundleContext ctx;

    private ServiceRegistration menuReg;

    private ResourceBundle resourceBundle;

    public ConfigServlet(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();

        if (req.getParameter("save_config") != null) {
            prefs.put("svdrp.host", req.getParameter("svdrp_host"));
            prefs.putInt("svdrp.port", Integer.parseInt(req.getParameter("svdrp_port")));

            addNotify(req, new NotifyMessage(TYPE.INFO, getResourceBundle().getString("I18N_SETTINGS_SAVED")));
        }

        params.put("TITLE", getResourceBundle().getString("I18N_PLAYLIST_CONFIG"));
        params.put("SERVLET_URI",
                req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getServletPath());
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

    @Validate
    public void start() {
        prefs = cs.getUserPreferences("de.berlios.vch.playlist");

        registerConfigServlet();
    }

    private void registerConfigServlet() {
        try {
            // register the servlet
            httpService.registerServlet(ConfigServlet.PATH, this, null, null);

            // register web interface menu
            IWebMenuEntry menu = new WebMenuEntry(getResourceBundle().getString("I18N_CONFIGURATION"));
            menu.setPreferredPosition(Integer.MAX_VALUE - 1);
            menu.setLinkUri("#");
            SortedSet<IWebMenuEntry> childs = new TreeSet<IWebMenuEntry>();
            IWebMenuEntry entry = new WebMenuEntry();
            entry.setTitle(getResourceBundle().getString("I18N_PLAYLIST"));
            entry.setLinkUri(ConfigServlet.PATH);
            childs.add(entry);
            menu.setChilds(childs);
            menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't register playlist config servlet", e);
        }
    }

    @Invalidate
    public void stop() {
        // unregister the config servlet
        if (httpService != null) {
            httpService.unregister(ConfigServlet.PATH);
        }

        // unregister the web menu
        if (menuReg != null) {
            menuReg.unregister();
        }
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            try {
                logger.log(LogService.LOG_DEBUG, "Loading resource bundle for " + getClass().getSimpleName());
                resourceBundle = ResourceBundleLoader.load(ctx, Locale.getDefault());
            } catch (IOException e) {
                logger.log(LogService.LOG_ERROR, "Couldn't load resource bundle", e);
            }
        }
        return resourceBundle;
    }

}
