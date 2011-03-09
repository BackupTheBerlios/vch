package de.berlios.vch.download;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;

@Component
@Provides
public class DownloadResourceBundleProvider implements ResourceBundleProvider {

    @Requires
    private LogService logger;

    private ResourceBundle resourceBundle;

    private BundleContext ctx;

    public DownloadResourceBundleProvider(BundleContext ctx) {
        this.ctx = ctx;
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
