package de.berlios.vch.web;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Map.Entry;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;
import de.berlios.vch.web.servlets.BundleContextServlet;

@Component
@Provides
public class WebInterfaceIPojo implements ResourceBundleProvider {

    private ServiceTracker httpServices;

    private List<String> registeredServlets = new LinkedList<String>();

    private ResourceBundle resourceBundle;
    
    @Requires
    private LogService log;

    @Requires
    private Messages i18n;

    @Requires
    TemplateLoader templateLoader;

    private BundleContext ctx;

    public WebInterfaceIPojo(final BundleContext ctx) throws IOException {
        this.ctx = ctx;

        // load resource bundle and register it
        ResourceBundle rb = ResourceBundleLoader.load(ctx, Locale.getDefault());
        ctx.registerService(ResourceBundle.class.getName(), rb, null);
    }

    @Validate
    public void validate() {
        httpServices = new ServiceTracker(ctx, HttpService.class.getName(), null) {
            @Override
            public void removedService(ServiceReference sr, Object service) {
                unregisterAll((HttpService) service);
                super.removedService(sr, service);
            }

            @Override
            public Object addingService(ServiceReference sr) {
                HttpService http = (HttpService) ctx.getService(sr);
                registerAll(ctx, http);
                return super.addingService(sr);
            }
        };
        httpServices.open();

        log.log(LogService.LOG_DEBUG, "Creating webmenu");
        // register help menu entry
        WebMenuEntry help = new WebMenuEntry();
        //help.setTitle(i18n.translate("I18N_HELP"));
        help.setTitle(getResourceBundle().getString("I18N_HELP"));
        help.setPreferredPosition(Integer.MAX_VALUE);
        help.setLinkUri("#");
        WebMenuEntry content = new WebMenuEntry(getResourceBundle().getString("I18N_CONTENT"));
        content.setLinkUri("/help/" + Locale.getDefault() + "/index.html");
        help.getChilds().add(content);
        ctx.registerService(IWebMenuEntry.class.getName(), help, null);
    }

    @Invalidate
    public void invalidate() throws Exception {
        Object[] services = httpServices.getServices();
        if (services != null && services.length > 0) {
            HttpService http = (HttpService) services[0];
            unregisterAll(http);
        }
    }

    @SuppressWarnings("unchecked")
    private BundleContextServlet createServlet(String className) throws ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        Class handlerClass = Class.forName(className);
        return (BundleContextServlet) handlerClass.newInstance();
    }

    private void registerAll(BundleContext ctx, HttpService http) {
        try {
            log.log(LogService.LOG_INFO, "Registering resource http context");
            ResourceHttpContext httpCtx = new ResourceHttpContext(ctx, log);
            http.registerResources("/", "/htdocs", httpCtx);
            registeredServlets.add("/");
            log.log(LogService.LOG_INFO, "Registering servlets");
            ServletMapping mapping = ServletMapping.getInstance();
            for (Entry<String, String> entry : mapping.entrySet()) {
                BundleContextServlet servlet = null;
                try {
                    servlet = createServlet(entry.getValue());
                } catch (Exception e) {
                    log.log(LogService.LOG_ERROR, "Couldn't instantiate servlet " + entry.getValue(), e);
                    continue;
                }
                servlet.setBundleContext(ctx);
                servlet.setMessages(i18n);
                servlet.setTemplateLoader(templateLoader);
                log.log(LogService.LOG_DEBUG, "Registering " + entry.getKey() + " -> " + entry.getValue());
                http.registerServlet(entry.getKey(), servlet, null, httpCtx);
                registeredServlets.add(entry.getKey());
            }
        } catch (Exception e) {
            log.log(LogService.LOG_ERROR, "Couldn't register servlets", e);
        }
    }

    private void unregisterAll(HttpService service) {
        for (String servlet : registeredServlets) {
            log.log(LogService.LOG_INFO, "Unregistering " + servlet);
            service.unregister(servlet);
        }
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if(resourceBundle == null) {
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
