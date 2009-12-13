package de.berlios.vch.osdserver;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.IWebParser;

@Component
@Provides
public class Activator implements ResourceBundleProvider {
    
    private static transient Logger logger = LoggerFactory.getLogger(Activator.class);
    
    public static ServiceTracker parserTracker;
    
    private BundleContext ctx;
    
    @Requires
    private Messages i18n;
    
    @Requires
    private HttpService httpService;
    
    private ResourceBundle resourceBundle;
    
    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Validate
    public void start() {
        parserTracker = new ServiceTracker(ctx, IWebParser.class.getName(), null);
        parserTracker.open();
        
        try {
            httpService.registerServlet("/osdserver", new ActivatorServlet(ctx, i18n), null, null);
        } catch (Exception e) {
            logger.error("Couldn't register osdserver startup servlet", e);
        }
    }

    @Invalidate
    public void stop() {
        parserTracker.close();
        parserTracker = null;
        
        httpService.unregister("/osdserver");
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if(resourceBundle == null) {
            try {
                logger.debug("Loading resource bundle for {}", getClass().getSimpleName());
                resourceBundle = ResourceBundleLoader.load(ctx, Locale.getDefault());
            } catch (IOException e) {
                logger.error("Couldn't load resource bundle", e);
            }
        }
        return resourceBundle;
    }
}
