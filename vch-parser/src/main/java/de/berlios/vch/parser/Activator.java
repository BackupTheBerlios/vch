package de.berlios.vch.parser;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.net.INetworkProtocol;

@Component
@Provides
public class Activator implements ResourceBundleProvider {

    private static transient Logger logger = LoggerFactory.getLogger(Activator.class);
    
    private ResourceBundle resourceBundle;
    
    private BundleContext ctx;
    
    public Activator(BundleContext ctx) {
        this.ctx = ctx;
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
    
    @Validate
    public void start() {
        ctx.registerService(INetworkProtocol.class.getName(), new HTTP(), null);
        ctx.registerService(INetworkProtocol.class.getName(), new MMS(), null);
    }
}