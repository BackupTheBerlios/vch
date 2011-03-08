package de.berlios.vch.android.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

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
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.web.ResourceHttpContext;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;
import de.berlios.vch.web.servlets.VchHttpServlet;

@Component
@Provides
public class Servlet extends VchHttpServlet implements ResourceBundleProvider {

    private static final long serialVersionUID = 2L;

    public static final String PATH = "/android";

    public static final String STATIC_PATH = PATH + "/static";

    @Requires
    private LogService logger;

    @Requires
    private TemplateLoader templateLoader;

    @Requires
    private HttpService httpService;

    private ServiceRegistration menuReg;

    private BundleContext ctx;

    private ResourceBundle resourceBundle;

    public Servlet(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("TITLE", "VCH Android Interface");
        params.put("STATIC_PATH", STATIC_PATH);
        params.put("NOTIFY_MESSAGES", getNotifyMessages(req));
        String page = templateLoader.loadTemplate("android.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    @Validate
    public void start() {
        try {
            // register downloads and configuration servlet
            registerServlets();

            // register web interface menu
            WebMenuEntry menu = new WebMenuEntry();
            menu.setTitle("Android");
            menu.setPreferredPosition(Integer.MAX_VALUE);
            menu.setLinkUri(Servlet.PATH);
            menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't start android web servlet", e);
        }
    }

    @Invalidate
    public void stop() {
        // unregister the servlet
        unregisterServlets();

        // unregister web menu etc
        unregisterMenu();
    }

    private void unregisterMenu() {
        if (menuReg != null) {
            menuReg.unregister();
        }
    }

    private void unregisterServlets() {
        if (httpService != null) {
            httpService.unregister(Servlet.PATH);
            httpService.unregister(Servlet.STATIC_PATH);
        }
    }

    private void registerServlets() throws ServletException, NamespaceException {
        // register servlet
        httpService.registerServlet(PATH, this, null, null);

        // register resource context for static files
        ResourceHttpContext resourceHttpContext = new ResourceHttpContext(ctx, logger);
        httpService.registerResources(Servlet.STATIC_PATH, "/htdocs", resourceHttpContext);
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
