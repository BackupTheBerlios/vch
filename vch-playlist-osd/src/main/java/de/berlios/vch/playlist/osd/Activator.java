package de.berlios.vch.playlist.osd;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.playlist.PlaylistService;

@Component
public class Activator implements ResourceBundleProvider {

    @Requires
    private LogService logger;

    @Requires
    private Messages i18n;

    @Requires
    private PlaylistService playlistService;

    private BundleContext ctx;

    private ResourceBundle resourceBundle;

    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Validate
    public void start() throws ServletException, NamespaceException {
        // register translation
        i18n.addProvider(this);
    }

    @Invalidate
    public void stop() {
        i18n.removeProvider(this);
    }
    
    public PlaylistService getPlaylistService() {
        return playlistService;
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if(resourceBundle == null) {
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
