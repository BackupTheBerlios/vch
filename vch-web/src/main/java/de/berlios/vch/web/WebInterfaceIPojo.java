package de.berlios.vch.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;
import de.berlios.vch.web.servlets.WelcomeServlet;

@Component
public class WebInterfaceIPojo implements ResourceBundleProvider {
    private ResourceBundle resourceBundle;

    @Requires
    private LogService log;

    @Requires
    TemplateLoader templateLoader;

    @Requires
    private HttpService httpService;

    @Requires
    private Messages i18n;

    private BundleContext ctx;

    private static final String STATIC_PATH = "/static";

    private List<ServiceRegistration> menuRegs = new ArrayList<ServiceRegistration>();

    public WebInterfaceIPojo(final BundleContext ctx) throws IOException {
        this.ctx = ctx;
    }

    @Validate
    public void validate() {
        i18n.addProvider(this);
        registerHttpContext();
        registerMenu();
    }

    private void registerMenu() {
        log.log(LogService.LOG_DEBUG, "Creating webmenu");
        // register help menu entry
        WebMenuEntry help = new WebMenuEntry();
        help.setTitle(getResourceBundle().getString("I18N_HELP"));
        help.setPreferredPosition(Integer.MAX_VALUE);
        help.setLinkUri("#");

        WebMenuEntry content = new WebMenuEntry(getResourceBundle().getString("I18N_CONTENT"));
        content.setLinkUri("http://vdr-wiki.de/wiki/index.php/Vodcatcher_Helper");
        help.getChilds().add(content);

        WebMenuEntry developer = new WebMenuEntry(getResourceBundle().getString("I18N_DEVELOPER"));
        developer.setLinkUri("http://vdr-wiki.de/wiki/index.php/Vodcatcher_Helper/Entwickler");
        developer.setPreferredPosition(Integer.MAX_VALUE);
        help.getChilds().add(developer);

        menuRegs.add(ctx.registerService(IWebMenuEntry.class.getName(), help, null));
    }

    @Invalidate
    public void invalidate() throws Exception {
        unregisterHttpContext(httpService);
        for (ServiceRegistration reg : menuRegs) {
            unregisterService(reg);
        }
        i18n.removeProvider(this);
    }

    private void unregisterService(ServiceRegistration sr) {
        if (sr != null) {
            sr.unregister();
        }
    }

    private void registerHttpContext() {
        try {
            log.log(LogService.LOG_INFO, "Registering resource http context");
            ResourceHttpContext httpCtx = new ResourceHttpContext(ctx, log);
            httpService.registerResources(STATIC_PATH, "/htdocs", httpCtx);
            httpService.createDefaultHttpContext();

            // register welcome servlet
            WelcomeServlet welcome = new WelcomeServlet();
            welcome.setBundleContext(ctx);
            welcome.setTemplateLoader(templateLoader);
            welcome.setMessages(i18n);
            httpService.registerServlet(WelcomeServlet.PATH, welcome, null, httpCtx);
        } catch (Exception e) {
            log.log(LogService.LOG_ERROR, "Couldn't register servlets", e);
        }
    }

    private void unregisterHttpContext(HttpService service) {
        if (httpService != null) {
            httpService.unregister(STATIC_PATH);
            httpService.unregister(WelcomeServlet.PATH);
        }
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            try {
                log.log(LogService.LOG_DEBUG, "Loading resource bundle for " + getClass().getSimpleName());
                resourceBundle = ResourceBundleLoader.load(ctx, Locale.getDefault());
            } catch (IOException e) {
                log.log(LogService.LOG_ERROR, "Couldn't load resource bundle", e);
            }
        }
        return resourceBundle;
    }
}
