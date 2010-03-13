package de.berlios.vch.web;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
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
@Provides
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
    
    private ServiceRegistration menuReg;
    
    public WebInterfaceIPojo(final BundleContext ctx) throws IOException {
        this.ctx = ctx;
    }

    @Validate
    public void validate() {
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
        menuReg = ctx.registerService(IWebMenuEntry.class.getName(), help, null);        
    }

    @Invalidate
    public void invalidate() throws Exception {
        unregisterHttpContext(httpService);
        unregisterService(menuReg);
    }
    
    private void unregisterService(ServiceRegistration sr) {
        if(sr != null) {
            sr.unregister();
        }
    }

    private void registerHttpContext() {
        try {
            log.log(LogService.LOG_INFO, "Registering resource http context");
            ResourceHttpContext httpCtx = new ResourceHttpContext(ctx, log);
            httpService.registerResources("/static", "/htdocs", httpCtx);
            
            // register welcome servlet
            WelcomeServlet welcome = new WelcomeServlet();
            welcome.setBundleContext(ctx);
            welcome.setTemplateLoader(templateLoader);
            welcome.setMessages(i18n);
            httpService.registerServlet("/", welcome, null, httpCtx);
        } catch (Exception e) {
            log.log(LogService.LOG_ERROR, "Couldn't register servlets", e);
        }
    }

    private void unregisterHttpContext(HttpService service) {
        if(httpService != null) {
            httpService.unregister("/static");
            httpService.unregister("/");
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
