package de.berlios.vch.download;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.prefs.Preferences;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.log.LogService;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.download.osd.DownloadAction;
import de.berlios.vch.download.osd.OpenDownloadsAction;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.osdserver.osd.menu.actions.OverviewAction;
import de.berlios.vch.playlist.PlaylistService;

@Component
public class Activator {

    private BundleContext ctx;

    @Requires(filter = "(instance.name=vch.download.i18n)")
    private ResourceBundleProvider rbp;

    @Requires
    private LogService logger;

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
            DownloadAction action = new DownloadAction(rbp.getResourceBundle(), dm, logger);
            ServiceRegistration sr = ctx.registerService(ItemDetailsAction.class.getName(), action, null);
            serviceRegs.add(sr);
            OpenDownloadsAction oda = new OpenDownloadsAction(rbp.getResourceBundle(), dm, logger, prefs,
                    playlistService);
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
}
