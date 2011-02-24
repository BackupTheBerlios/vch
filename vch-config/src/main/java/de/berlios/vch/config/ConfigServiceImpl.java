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
public class ConfigServiceImpl implements ConfigService, ResourceBundleProvider {

    @Requires
    private LogService logger;

    private Preferences userPrefs;

    private BundleContext ctx;

    private ResourceBundle resourceBundle;

    public ConfigServiceImpl(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Validate
    public void start() {
        userPrefs = Preferences.userNodeForPackage(ConfigService.class);
    }

    @Invalidate
    public void stop() {
        try {
            userPrefs.flush();
        } catch (BackingStoreException e) {
            logger.log(LogService.LOG_ERROR, "Couldn't save preferences", e);
        }
    }

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
