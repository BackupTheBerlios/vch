package de.berlios.vch.osdserver;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.OverviewMenu;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.service.IParserService;
import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistService;

/**
 * Represents one OSD session. The sessions starts with the request to open the VCH menu and ends when the menu gets
 * closed.
 * 
 * @author <a href="mailto:hampelratte@users.berlios.de">hampelratte@users.berlios.de</a>
 */
// TODO create a logger, which logs to the osd
// TODO i18n
public class OsdSession implements Runnable {

    private static transient Logger logger = LoggerFactory.getLogger(OsdSession.class);

    private Osd osd;

    private boolean running = false;

    private ResourceBundle rb;

    private BundleContext ctx;

    // load the config params
    private String osdserverHost = "localhost";
    private int osdserverPort = 2010;
    private String osdserverEncoding = "UTF-8";

    private Map<String, String> requestPrefs;

    private static Preferences prefs;

    private PlaylistService playlistService;

    public Osd getOsd() {
        return osd;
    }

    public PlaylistService getPlaylistService() {
        return playlistService;
    }

    public ResourceBundle getResourceBundle() {
        return rb;
    }

    public BundleContext getBundleContext() {
        return ctx;
    }

    public OsdSession(BundleContext ctx, ResourceBundle rb, PlaylistService playlistService,
            Map<String, String> requestPrefs) {
        this.rb = rb;
        this.ctx = ctx;
        this.playlistService = playlistService;
        this.requestPrefs = requestPrefs;

        this.osd = new Osd(this);

        ServiceReference sr = ctx.getServiceReference(ConfigService.class.getName());
        if (sr != null) {
            ConfigService config = (ConfigService) ctx.getService(sr);
            if (config != null) {
                prefs = config.getUserPreferences(ctx.getBundle().getSymbolicName());
                osdserverHost = prefs.get("osdserver.host", "localhost");
                osdserverPort = prefs.getInt("osdserver.port", 2010);
                osdserverEncoding = prefs.get("osdserver.encoding", "UTF-8");
            } else {
                logger.error("Preferences service not available falling back to defaults ({},{},{})", new Object[] {
                        osdserverHost, osdserverPort, osdserverEncoding });
            }
        } else {
            logger.error("Preferences service not available falling back to defaults ({},{},{})", new Object[] {
                    osdserverHost, osdserverPort, osdserverEncoding });
        }

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

    @Override
    public void run() {
        running = true;

        // open the connection
        try {
            logger.info("Connecting to {}:{}", osdserverHost, osdserverPort);
            osd.connect(osdserverHost, osdserverPort, 500, osdserverEncoding);
        } catch (Exception e) {
            logger.error("Couldn't open connection to osdserver", e);
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
            logger.error("Couldn't create osd menu", e);
            return;
        }

        while (running) {
            try {
                Menu current = osd.getCurrentMenu();
                if (current != null) {
                    osd.sleepEvent(osd.getCurrentMenu());
                } else {
                    logger.debug("No active menu exists. Ending session");
                    running = false;
                }
            } catch (Exception e) {
                logger.error("Couldn't wait for event", e);
                running = false;
            }
        }
        logger.info("osdserver session ended");
    }

    public void stop() {
        logger.trace("Stopping osd session");
        running = false;
    }

    public void play(Playlist list) throws UnknownHostException, IOException, URISyntaxException {
        playlistService.play(list, requestPrefs);
    }

    private IOverviewPage getParsers() throws Exception {
        IParserService parserService = (IParserService) Activator.parserServiceTracker.getService();
        if (parserService == null) {
            throw new ServiceException("ParserService not available");
        }
        return parserService.getParserOverview();
    }
}
