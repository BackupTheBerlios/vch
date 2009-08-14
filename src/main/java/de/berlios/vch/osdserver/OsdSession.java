package de.berlios.vch.osdserver;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;

import org.hampelratte.svdrp.Command;
import org.hampelratte.svdrp.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.download.AbstractDownload;
import de.berlios.vch.model.Download;
import de.berlios.vch.model.Item;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.io.svdrp.CheckMplayerSvdrpInterface;
import de.berlios.vch.osdserver.io.svdrp.CheckXineliboutputSvdrpInterface;
import de.berlios.vch.osdserver.osd.Menu;
import de.berlios.vch.osdserver.osd.Osd;
import de.berlios.vch.osdserver.osd.OsdException;
import de.berlios.vch.osdserver.osd.menu.GroupsMenu;

/**
 * TODO create a logger, which logs to the osd
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
    
    @Override
    public void run() {
        running = true;
        osd = Osd.getInstance();

        String host = Config.getInstance().getProperty("osdserver.host", "localhost");
        int port = Integer.parseInt(Config.getInstance().getProperty("osdserver.port", "2010"));
        String encoding = Config.getInstance().getProperty("default.encoding");
        try {
            osd.connect(host, port, 500, encoding);
        } catch (Exception e) {
            logger.error("Couldn't open connection to osdserver", e);
        }

        try {
            Menu groupMenu = new GroupsMenu();
            osd.createMenu(groupMenu);
            try {
                osd.setColorKeyText(groupMenu, "Downloads", Event.KEY_BLUE);
                groupMenu.registerEvent(new Event(groupMenu.getId(), Event.KEY_BLUE, null));
            } catch (Exception e) {
                logger.error("Couldn't create color button", e);
            }
            osd.show(groupMenu);
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
        
        if(o instanceof AbstractDownload) {
            AbstractDownload ad = (AbstractDownload) o;
            playlistEntry = ad.getLocalFile();
            title = ad.getItem().getTitle();
        } else if(o instanceof Download) {
            Download d = (Download) o;
            playlistEntry = d.getLocalFile();
            title = d.getItem().getTitle();
        } else if(o instanceof Item) {
            Item item = (Item) o;
            playlistEntry = item.getEnclosure().getLink();
            title = item.getTitle();
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
}
