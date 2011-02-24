package de.berlios.vch.download.webinterface;

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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
import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.webinterface.handler.RequestHandler;
import de.berlios.vch.download.webinterface.handler.html.HtmlAddHandler;
import de.berlios.vch.download.webinterface.handler.html.HtmlDeleteFinishedHandler;
import de.berlios.vch.download.webinterface.handler.html.HtmlDeleteHandler;
import de.berlios.vch.download.webinterface.handler.html.HtmlListHandler;
import de.berlios.vch.download.webinterface.handler.html.HtmlStartAllHandler;
import de.berlios.vch.download.webinterface.handler.html.HtmlStartHandler;
import de.berlios.vch.download.webinterface.handler.html.HtmlStopAllHandler;
import de.berlios.vch.download.webinterface.handler.html.HtmlStopHandler;
import de.berlios.vch.download.webinterface.handler.json.JsonAddHandler;
import de.berlios.vch.download.webinterface.handler.json.JsonDeleteFinishedHandler;
import de.berlios.vch.download.webinterface.handler.json.JsonDeleteHandler;
import de.berlios.vch.download.webinterface.handler.json.JsonListActiveHandler;
import de.berlios.vch.download.webinterface.handler.json.JsonListFinishedHandler;
import de.berlios.vch.download.webinterface.handler.json.JsonStartAllHandler;
import de.berlios.vch.download.webinterface.handler.json.JsonStartHandler;
import de.berlios.vch.download.webinterface.handler.json.JsonStopAllHandler;
import de.berlios.vch.download.webinterface.handler.json.JsonStopHandler;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.uri.IVchUriResolveService;
import de.berlios.vch.web.ResourceHttpContext;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;
import de.berlios.vch.web.servlets.VchHttpServlet;

@Component
public class DownloadsServlet extends VchHttpServlet {

    private static final long serialVersionUID = 2L;

    public static final String PATH = "/downloads";

    public static final String FILE_PATH = PATH + "/files";

    public static final String STATIC_PATH = PATH + "/static";

    @Requires
    private DownloadManager dm;

    @Requires
    private IVchUriResolveService uriResolver;

    @Requires
    private HttpService httpService;

    @Requires
    private Messages i18n;

    @Requires
    private TemplateLoader templateLoader;

    @Requires
    private LogService logger;

    @Requires
    private ConfigService cs;

    private Preferences prefs;

    private BundleContext ctx;

    private Set<RequestHandler> handlers = new HashSet<RequestHandler>();

    public DownloadsServlet(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // get the action
        String action = req.getParameter("action");
        action = action == null ? "list" : action;
        boolean isJsonRequest = "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));

        RequestHandler handler = getHandler(action, isJsonRequest);
        handler.get(req, resp);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    private RequestHandler getHandler(String action, boolean json) throws ServletException {
        for (RequestHandler handler : handlers) {
            if (handler.acceptRequest(action, json)) {
                return handler;
            }
        }

        throw new ServletException("Request handler for " + action + " json:" + json + " not found");
    }

    @Validate
    public void start() {
        // initialize the preferences
        prefs = cs.getUserPreferences("de.berlios.vch.download");

        registerServlet();
        registerMenu();
        createHandlers();
    }

    private void createHandlers() {
        // add the handlers for json request
        handlers.add(new JsonAddHandler(uriResolver, dm, logger));
        handlers.add(new JsonDeleteFinishedHandler(dm));
        handlers.add(new JsonDeleteHandler(dm));
        handlers.add(new JsonListActiveHandler(dm));
        handlers.add(new JsonListFinishedHandler(dm));
        handlers.add(new JsonStartAllHandler(dm));
        handlers.add(new JsonStartHandler(dm));
        handlers.add(new JsonStopAllHandler(dm));
        handlers.add(new JsonStopHandler(dm));

        // add the handlers for html requests
        handlers.add(new HtmlAddHandler(this, i18n, dm, templateLoader, uriResolver, logger));
        handlers.add(new HtmlDeleteFinishedHandler(this, i18n, dm, templateLoader));
        handlers.add(new HtmlDeleteHandler(this, i18n, dm, templateLoader));
        handlers.add(new HtmlListHandler(this, i18n, dm, templateLoader));
        handlers.add(new HtmlStartAllHandler(this, i18n, dm, templateLoader));
        handlers.add(new HtmlStartHandler(this, i18n, dm, templateLoader));
        handlers.add(new HtmlStopAllHandler(this, i18n, dm, templateLoader));
        handlers.add(new HtmlStopHandler(this, i18n, dm, templateLoader));

    }

    private void registerMenu() {
        // register web interface menu
        WebMenuEntry downloads = new WebMenuEntry();
        downloads.setTitle(i18n.translate("I18N_DOWNLOADS"));
        downloads.setPreferredPosition(Integer.MAX_VALUE - 2);
        downloads.setLinkUri("#");
        WebMenuEntry manage = new WebMenuEntry(i18n.translate("I18N_MANAGE"));
        manage.setLinkUri(DownloadsServlet.PATH);
        downloads.getChilds().add(manage);
        ServiceRegistration sr = ctx.registerService(IWebMenuEntry.class.getName(), downloads, null);
        serviceRegs.add(sr);
    }

    private void registerServlet() {
        try {
            // register the main servlet
            httpService.registerServlet(PATH, this, null, null);

            // register resource context to make downloads available
            DownloadHttpContext downloadHttpContext = new DownloadHttpContext(prefs, logger);
            httpService.registerResources(DownloadsServlet.FILE_PATH, "", downloadHttpContext);

            // register resource context for static files
            ResourceHttpContext resourceHttpContext = new ResourceHttpContext(ctx, logger);
            httpService.registerResources(DownloadsServlet.STATIC_PATH, "/htdocs", resourceHttpContext);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't register servlet", e);
        }
    }

    @Invalidate
    public void stop() {
        handlers.clear();

        httpService.unregister(PATH);

        // unregister all manually made registrations
        for (Iterator<ServiceRegistration> iterator = serviceRegs.iterator(); iterator.hasNext();) {
            ServiceRegistration sr = iterator.next();
            unregisterService(sr);
            iterator.remove();
        }
    }
}
