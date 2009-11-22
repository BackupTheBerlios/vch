package de.berlios.vch.web;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;

@Component
@Provides
public class WebInterfaceIPojo implements ResourceBundleProvider {

    private List<String> registeredServlets = new LinkedList<String>();

    private ResourceBundle resourceBundle;
    
    @Requires
    private LogService log;

    @Requires
    TemplateLoader templateLoader;
    
    @Requires
    private HttpService httpService;

    private BundleContext ctx;

    public WebInterfaceIPojo(final BundleContext ctx) throws IOException {
        this.ctx = ctx;

        // load resource bundle and register it
        ResourceBundle rb = ResourceBundleLoader.load(ctx, Locale.getDefault());
        ctx.registerService(ResourceBundle.class.getName(), rb, null);
    }

    @Validate
    public void validate() {
        registerAll(ctx, httpService);

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
        if(httpService != null) {
            unregisterAll(httpService);
        }
    }

    private void registerAll(BundleContext ctx, HttpService http) {
        try {
            log.log(LogService.LOG_INFO, "Registering resource http context");
            ResourceHttpContext httpCtx = new ResourceHttpContext(ctx, log);
            http.registerResources("/", "/htdocs", httpCtx);
            registeredServlets.add("/");
        } catch (Exception e) {
            log.log(LogService.LOG_ERROR, "Couldn't register servlets", e);
        }
    }

    private void unregisterAll(HttpService service) {
        for (String servlet : registeredServlets) {
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
