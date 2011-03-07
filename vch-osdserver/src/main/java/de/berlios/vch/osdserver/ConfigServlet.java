package de.berlios.vch.osdserver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
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
import org.osgi.service.http.NamespaceException;

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

    public static String PATH = "/config/osdserver";

    @Requires(filter = "(instance.name=vch.osd)")
    private ResourceBundleProvider rbp;

    @Requires
    private ConfigService cs;
    private Preferences prefs;

    @Requires
    private TemplateLoader templateLoader;

    @Requires
    private HttpService httpService;

    private BundleContext ctx;

    private ServiceRegistration menuReg;

    public ConfigServlet() {
    }

    public ConfigServlet(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();

        if (req.getParameter("save_config") != null) {
            prefs.put("osdserver.host", req.getParameter("osdserver_host"));
            prefs.putInt("osdserver.port", Integer.parseInt(req.getParameter("osdserver_port")));
            prefs.put("osdserver.encoding", req.getParameter("osdserver_encoding"));
            addNotify(req, new NotifyMessage(TYPE.INFO, rbp.getResourceBundle().getString("I18N_SETTINGS_SAVED")));
        }

        params.put("TITLE", rbp.getResourceBundle().getString("I18N_OSDSERVER_CONFIG"));
        params.put("SERVLET_URI",
                req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getServletPath());
        params.put("ACTION", PATH);
        params.put("osdserver_host", prefs.get("osdserver.host", "localhost"));
        params.put("osdserver_port", prefs.get("osdserver.port", "2010"));
        params.put("osdserver_encoding", prefs.get("osdserver.encoding", "UTF-8"));
        params.put("NOTIFY_MESSAGES", getNotifyMessages(req));

        String page = templateLoader.loadTemplate("configOsdserver.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    @Validate
    public void start() throws ServletException, NamespaceException {
        prefs = cs.getUserPreferences(ctx.getBundle().getSymbolicName());
        registerConfigServlet();
    }

    private void registerConfigServlet() throws ServletException, NamespaceException {
        // register the servlet
        httpService.registerServlet(PATH, this, null, null);

        // register web interface menu
        IWebMenuEntry menu = new WebMenuEntry(rbp.getResourceBundle().getString("I18N_CONFIGURATION"));
        menu.setPreferredPosition(Integer.MAX_VALUE - 1);
        menu.setLinkUri("#");
        SortedSet<IWebMenuEntry> childs = new TreeSet<IWebMenuEntry>();
        IWebMenuEntry entry = new WebMenuEntry();
        entry.setTitle("Osdserver");
        entry.setLinkUri(ConfigServlet.PATH);
        childs.add(entry);
        menu.setChilds(childs);
        menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);

    }

    @Invalidate
    public void stop() {
        // unregister the config servlet
        if (httpService != null) {
            httpService.unregister(PATH);
        }

        // unregister the web menu
        if (menuReg != null) {
            menuReg.unregister();
        }
    }
}
