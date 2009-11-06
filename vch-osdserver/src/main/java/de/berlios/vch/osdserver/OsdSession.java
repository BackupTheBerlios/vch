package de.berlios.vch.osdserver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collections;

import org.hampelratte.svdrp.Command;
import org.hampelratte.svdrp.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.svdrp.CheckMplayerSvdrpInterface;
import de.berlios.vch.osdserver.io.svdrp.CheckXineliboutputSvdrpInterface;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdException;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.OverviewMenu;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.WebPageTitleComparator;

/**
 * TODO create a logger, which logs to the osd
 * TODO i18n
 * Represents one OSD session. The sessions starts with the request to open the VCH menu
 * and ends when, the menu gets closed. 
 *
 * @author <a href="mailto:hampelratte@users.berlios.de">hampelratte@users.berlios.de</a>
 */
public class OsdSession implements Runnable {

    private static transient Logger logger = LoggerFactory.getLogger(OsdSession.class);
    
    private Osd osd;
    
    private static boolean running = false;
    
    public static enum MediaPlayer {MPLAYER, XINELIBOUTPUT};
    
    public static MediaPlayer player = null;
    
    private Messages i18n;
    
    public OsdSession(Messages i18n) {
        this.i18n = i18n;
    }

    @Override
    public void run() {
        running = true;
        osd = Osd.getInstance();

        // TODO config params
//        String host = Config.getInstance().getProperty("osdserver.host", "localhost");
//        int port = Integer.parseInt(Config.getInstance().getProperty("osdserver.port", "2010"));
//        String encoding = Config.getInstance().getProperty("default.encoding");
        String host = "localhost";
        int port = 2010;
        String encoding = "UTF-8";
        
        try {
            logger.info("Connecting to {}:{}", host, port);
            osd.connect(host, port, 500, encoding);
        } catch (Exception e) {
            logger.error("Couldn't open connection to osdserver", e);
        }

        try {
            Menu menu;
            if (osd.getCurrentMenu() != null) {
                logger.debug("Found previous menu");
                menu = osd.getCurrentMenu();
            } else {
                menu = new OverviewMenu(getParsers(), i18n);
            }
            osd.createMenu(menu);
            // TODO activate for downloads
//            try {
//                osd.setColorKeyText(siteMenu, "Downloads", Event.KEY_BLUE);
//                siteMenu.registerEvent(new Event(siteMenu.getId(), Event.KEY_BLUE, null));
//            } catch (Exception e) {
//                logger.error("Couldn't create color button", e);
//            }
            
            osd.show(menu);
        } catch (Exception e) {
            logger.error("Couldn't create osd menu", e);
        }
        
        while(running) {
            try {
                Menu current = osd.getCurrentMenu();
                if(current != null) {
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
    
    public static void play(Object o) throws IOException {
        String playlistEntry = null;
        String title = null;
        
        if(o instanceof IVideoPage) {
            IVideoPage page = (IVideoPage) o;
            title = page.getTitle();
            playlistEntry = page.getVideoUri().toString();
//        if(o instanceof AbstractDownload) {
//            AbstractDownload ad = (AbstractDownload) o;
//            playlistEntry = ad.getLocalFile();
//            title = ad.getItem().getTitle();
//        } else if(o instanceof Download) {
//            Download d = (Download) o;
//            playlistEntry = d.getLocalFile();
//            title = d.getItem().getTitle();
//        } else if(o instanceof Item) {
//            Item item = (Item) o;
//            playlistEntry = item.getEnclosure().getLink();
//            title = item.getTitle();
        } else {
            throw new IOException("Couldn't determine enclosure URI");
        }
        
        logger.debug("Requested playback of video {}", playlistEntry);
        String host = "localhost";
        int port = 2001;
        Osd.getInstance().showMessageSilent(new OsdMessage("Wiedergabe wird gestartet. Bitte warten...", OsdMessage.STATUS));
        logger.info("Starting media player plugin with SVDRP on {}:{} for {}", new Object[] {host, port, playlistEntry});
        org.hampelratte.svdrp.Connection svdrp = null;
        try {
            svdrp = new org.hampelratte.svdrp.Connection(host, port);
            Command playCmd = getPlayCommand(svdrp);
            Osd.getInstance().showMessageSilent(new OsdMessage("", OsdMessage.STATUSCLEAR));
            Osd.getInstance().showMessageSilent(new OsdMessage("Wiedergabeliste wird erstellt...", OsdMessage.STATUS));
            FileWriter fw = new FileWriter(new File("/tmp/vch.pls"));
            if(player == MediaPlayer.MPLAYER) {
                fw.write(playlistEntry);
            } else if(player == MediaPlayer.XINELIBOUTPUT) {
                fw.write("File1="+URLDecoder.decode(playlistEntry, "utf-8")+"\n");
                fw.write("Title1="+title);
            }
            fw.close();
            
            org.hampelratte.svdrp.Response resp = svdrp.send(playCmd);
            Osd.getInstance().showMessageSilent(new OsdMessage("", OsdMessage.STATUSCLEAR));
            logger.debug("SVDRP response: {} {}", resp.getCode(), resp.getMessage());
            if( resp.getCode() < 900 || resp.getCode() > 999 ) {
                Osd.getInstance().showMessageSilent(new OsdMessage(resp.getMessage().trim(), OsdMessage.ERROR));
            } else {
                try {
                    Osd.getInstance().sendState("osEnd");
                } catch (OsdException e) {}
            }
        } finally {
            if(svdrp != null) {
                svdrp.close();
            }
        }
    }
    
    private static Command getPlayCommand(org.hampelratte.svdrp.Connection svdrp) throws IOException {
        Response res = svdrp.send(new CheckMplayerSvdrpInterface());
        if(res.getCode() == 214) {
            logger.trace("Using MPlayer to play the file");
            player = MediaPlayer.MPLAYER;
            return new Command() {
                @Override
                public String getCommand() {
                    return "plug mplayer play /tmp/vch.pls";
                }
                @Override
                public String toString() {
                    return "MPlayer PLAY";
                }
            };
        } else {
            res = svdrp.send(new CheckXineliboutputSvdrpInterface());
            if(res.getCode() == 214) {
                logger.trace("Using xineliboutput to play the file");
                player = MediaPlayer.XINELIBOUTPUT;
                return new Command() {
                    @Override
                    public String getCommand() {
                        return "plug xineliboutput pmda /tmp/vch.pls";
                    }
                    @Override
                    public String toString() {
                        return "Xineliboutput PMDA";
                    }
                };
            } else {
                throw new IOException("No media player plugin available");
            }
        }
    }
    
    public static void stop() {
        logger.trace("Stopping osd session");
        running = false;
    }
    
    private IOverviewPage getParsers() throws Exception {
        Object[] parsers = Activator.parserTracker.getServices();
        IOverviewPage overview = new OverviewPage();
        overview.setTitle(i18n.translate("sites"));
        if (parsers != null && parsers.length > 0) {
            for (Object o : parsers) {
                IWebParser parser = (IWebParser) o;
                IOverviewPage parserPage = new OverviewPage();
                parserPage.setTitle(parser.getTitle());
                parserPage.setUri(new URI("vchpage://root"));
                parserPage.setParser(parser.getId());
                overview.getPages().add(parserPage);
            }
        }
        
        Collections.sort(overview.getPages(), new WebPageTitleComparator());
        return overview;
    }
}
