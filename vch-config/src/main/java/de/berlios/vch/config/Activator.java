package de.berlios.vch.config;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;

@Component
@Provides
public class Activator implements ConfigService, ResourceBundleProvider {

    @Requires
    private LogService logger;
    
//    @Requires
//    private HttpService httpService;
//    
//    @Requires
//    private Messages messages;
//    
//    @Requires
//    private TemplateLoader templateLoader;
    
    private Preferences userPrefs;
    
    private BundleContext ctx;
    
    private ResourceBundle resourceBundle;
    
    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Validate
    public void start() {
        userPrefs = Preferences.userNodeForPackage(ConfigService.class);
        
//        try {
//            registerServlet();
//        } catch (Exception e) {
//            logger.log(LogService.LOG_ERROR, "Couldn't register config servlet", e);
//        }
    }
    
//    private void registerServlet() throws ServletException, NamespaceException {
//        ConfigServlet servlet = new ConfigServlet();
//        servlet.setBundleContext(ctx);
//        servlet.setMessages(messages);
//        servlet.setTemplateLoader(templateLoader);
//        httpService.registerServlet(ConfigServlet.PATH, servlet, null, null);
//        
//        // register web interface menu
//        WebMenuEntry config = new WebMenuEntry();
//        config.setTitle(getResourceBundle().getString("I18N_CONFIGURATION"));
//        config.setPreferredPosition(Integer.MAX_VALUE-1);
//        config.setLinkUri("#");
//        WebMenuEntry content = new WebMenuEntry(getResourceBundle().getString("I18N_CONFIGURATION"));
//        content.setLinkUri(ConfigServlet.PATH);
//        config.getChilds().add(content);
//        ctx.registerService(IWebMenuEntry.class.getName(), config, null);
//    }

    @Invalidate
    public void stop() {
        try {
            userPrefs.flush();
        } catch (BackingStoreException e) {
            logger.log(LogService.LOG_ERROR, "Couldn't save preferences", e);
        }
        
//        unregisterServlet();
    }

//    private void unregisterServlet() {
//        if(httpService != null) {
//            httpService.unregister(ConfigServlet.PATH);
//        }
//    }

    @Override
    public Preferences getUserPreferences(String node) {
        return userPrefs.node(node);
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if(resourceBundle == null) {
            try {
                logger.log(LogService.LOG_INFO, "Loading resource bundle for " + getClass().getSimpleName());
                resourceBundle = ResourceBundleLoader.load(ctx, Locale.getDefault());
            } catch (IOException e) {
                logger.log(LogService.LOG_ERROR, "Couldn't load resource bundle", e);
            }
        }
        return resourceBundle;
    }
}
