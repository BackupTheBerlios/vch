package de.berlios.vch.osdserver;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.OverviewMenu;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.service.IParserService;
import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistService;
import de.berlios.vch.uri.IVchUriResolveService;

/**
 * Represents one OSD session. The sessions starts with the request to open the VCH menu and ends when the menu gets closed.
 * 
 * @author <a href="mailto:hampelratte@users.berlios.de">hampelratte@users.berlios.de</a>
 */
// TODO create a logger, which logs to the osd
// TODO i18n
@Component
public class OsdSession implements Runnable {

    @Requires
    private LogService logger;

    private Osd osd;

    private boolean running = false;

    @Requires(filter = "(instance.name=vch.osd)")
    private ResourceBundleProvider rbp;

    private BundleContext ctx;

    // load the config params
    private String osdserverHost = "localhost";
    private int osdserverPort = 2010;
    private String osdserverEncoding = "UTF-8";

    private Map<String, String> requestPrefs;

    @Requires
    private ConfigService cs;
    private static Preferences prefs;

    @Requires
    private PlaylistService playlistService;

    @Requires
    private IParserService parserService;

    @Requires
    private IVchUriResolveService resolverService;

    private List<OsdSessionListener> sessionListeners = new ArrayList<OsdSession.OsdSessionListener>();

    public OsdSession(BundleContext ctx) {
        this.ctx = ctx;
    }

    public Osd getOsd() {
        return osd;
    }

    public PlaylistService getPlaylistService() {
        return playlistService;
    }

    public IVchUriResolveService getResolverService() {
        return resolverService;
    }

    public ResourceBundle getResourceBundle() {
        return rbp.getResourceBundle();
    }

    public LogService getLogger() {
        return logger;
    }

    public BundleContext getBundleContext() {
        return ctx;
    }

    public void setRequestPreferences(Map<String, String> requestPrefs) {
        this.requestPrefs = requestPrefs;
        if (requestPrefs != null) {
            if (requestPrefs.containsKey("osdhost")) {
                osdserverHost = requestPrefs.get("osdhost");
            }
            if (requestPrefs.containsKey("osdport")) {
                osdserverPort = Integer.parseInt(requestPrefs.get("osdport"));
            }
            if (requestPrefs.containsKey("encoding")) {
                osdserverEncoding = requestPrefs.get("encoding");
            }
        }
    }

    private void loadConfig() {
        prefs = cs.getUserPreferences(ctx.getBundle().getSymbolicName());
        osdserverHost = prefs.get("osdserver.host", "localhost");
        osdserverPort = prefs.getInt("osdserver.port", 2010);
        osdserverEncoding = prefs.get("osdserver.encoding", "UTF-8");
    }

    @Override
    public void run() {
        running = true;

        // open the connection
        try {
            logger.log(LogService.LOG_INFO, "Connecting to " + osdserverHost + ":" + osdserverPort);
            osd.connect(osdserverHost, osdserverPort, 500, osdserverEncoding);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't open connection to osdserver", e);
            return;
        }

        try {
            Menu menu;
            // if (osd.getCurrentMenu() != null) {
            // logger.debug("Found previous menu");
            // menu = osd.getCurrentMenu();
            // } else {
            menu = new OverviewMenu(this, getParsers());
            // }
            osd.createMenu(menu);
            osd.show(menu);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't create osd menu", e);
            return;
        }

        while (running) {
            try {
                Menu current = osd.getCurrentMenu();
                if (current != null) {
                    osd.sleepEvent(osd.getCurrentMenu());
                } else {
                    logger.log(LogService.LOG_DEBUG, "No active menu exists. Ending session");
                    running = false;
                }
            } catch (Exception e) {
                logger.log(LogService.LOG_ERROR, "Couldn't wait for event", e);
                running = false;
            }
        }

        logger.log(LogService.LOG_INFO, "osdserver session ended");
        for (OsdSessionListener l : sessionListeners) {
            l.sessionEnded();
        }
    }

    public void stop() {
        logger.log(LogService.LOG_DEBUG, "Stopping osd session");
        running = false;
    }

    public void play(Playlist list) throws UnknownHostException, IOException, URISyntaxException {
        playlistService.play(list, requestPrefs);
    }

    private IOverviewPage getParsers() throws Exception {
        if (parserService == null) {
            throw new ServiceException("ParserService not available");
        }
        return parserService.getParserOverview();
    }

    @Validate
    public void validate() {
        loadConfig();
        this.osd = new Osd(this);
    }

    public void addOsdSessionListener(OsdSessionListener l) {
        sessionListeners.add(l);
    }

    public void removeOsdSessionListener(OsdSessionListener l) {
        sessionListeners.remove(l);
    }

    public interface OsdSessionListener {
        public void sessionEnded();
    }
}
