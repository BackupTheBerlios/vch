package de.berlios.vch.download;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.download.osd.DownloadAction;
import de.berlios.vch.download.osd.OpenDownloadsAction;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.osdserver.osd.menu.actions.OverviewAction;
import de.berlios.vch.playlist.PlaylistService;

@Component
@Provides
public class Activator implements ResourceBundleProvider {

    private BundleContext ctx;

    private ResourceBundle resourceBundle;

    @Requires
    private LogService logger;

    @Requires
    private Messages messages;

    @Requires
    private DownloadManager dm;

    @Requires
    private ConfigService cs;

    private Preferences prefs;

    @Requires
    private PlaylistService playlistService;

    private List<ServiceRegistration> serviceRegs = new LinkedList<ServiceRegistration>();

    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Validate
    public void start() {
        prefs = cs.getUserPreferences(ctx.getBundle().getSymbolicName());
        setDefaults(prefs);
        try {
            // register osd actions
            DownloadAction action = new DownloadAction(messages, dm, logger);
            ServiceRegistration sr = ctx.registerService(ItemDetailsAction.class.getName(), action, null);
            serviceRegs.add(sr);
            OpenDownloadsAction oda = new OpenDownloadsAction(messages, dm, logger, prefs, playlistService);
            sr = ctx.registerService(OverviewAction.class.getName(), oda, null);
            serviceRegs.add(sr);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't start download manager", e);
        }
    }

    static void setDefaults(Preferences prefs) {
        setIfEmpty(prefs, "data.dir", "data");
    }

    private static void setIfEmpty(Preferences prefs, String key, String value) {
        prefs.put(key, prefs.get(key, value));
    }

    @Invalidate
    public void stop() {
        // stop the download manager
        dm.stop();

        // unregister osd actions and web menu etc
        for (Iterator<ServiceRegistration> iterator = serviceRegs.iterator(); iterator.hasNext();) {
            ServiceRegistration sr = iterator.next();
            unregisterService(sr);
            iterator.remove();
        }
    }

    private void unregisterService(ServiceRegistration sr) {
        if (sr != null) {
            sr.unregister();
        }
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
