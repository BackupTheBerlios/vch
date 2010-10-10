package de.berlios.vch.playlist;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.prefs.Preferences;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.hampelratte.svdrp.Command;
import org.hampelratte.svdrp.Response;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.playlist.io.svdrp.CheckMplayerSvdrpInterface;
import de.berlios.vch.playlist.io.svdrp.CheckXineliboutputSvdrpInterface;

@Component
@Provides
public class PlaylistServiceImpl implements PlaylistService {

    private Playlist playlist = new Playlist();
    
    public static enum MediaPlayer {MPLAYER, XINELIBOUTPUT};
    
    public static MediaPlayer player = null;
    
    @Requires
    private LogService logger;
    
    @Requires
    private ConfigService config;
    
    private BundleContext ctx;
    
    private Preferences prefs;
    
    public PlaylistServiceImpl(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Override
    public Playlist getPlaylist() {
        return playlist;
    }

    @Override
    public void play(Playlist playlist) throws UnknownHostException, IOException {
        String svdrpHost = prefs.get("svdrp.host", "localhost");
        int svdrpPort = prefs.getInt("svdrp.port", 2001);
        logger.log(LogService.LOG_INFO, "Starting media player plugin with SVDRP on "+svdrpHost+":"+svdrpPort);
        org.hampelratte.svdrp.Connection svdrp = null;
        FileWriter fw = null;
        try {
            svdrp = new org.hampelratte.svdrp.Connection(svdrpHost, svdrpPort);
            Command playCmd = getPlayCommand(svdrp);
            fw = new FileWriter(new File("/tmp/vch.pls"));
            if(player == MediaPlayer.MPLAYER) {
                for (PlaylistEntry playlistEntry : playlist) {
                    fw.write(playlistEntry.getUrl() + '\n');
                }
            } else if(player == MediaPlayer.XINELIBOUTPUT) {
                for (int i = 0; i < playlist.size(); i++) {
                    fw.write("File"+(i+1)+"="+URLDecoder.decode(playlist.get(i).getUrl(), "utf-8")+"\n");
                    fw.write("Title"+(i+1)+"="+playlist.get(i).getTitle()+'\n');
                }
            }
            fw.close();
            
            org.hampelratte.svdrp.Response resp = svdrp.send(playCmd);
            if(resp != null) {
                logger.log(LogService.LOG_DEBUG, "SVDRP response: " + resp.getCode() + " " + resp.getMessage());
                if( resp.getCode() < 900 || resp.getCode() > 999 ) {
                    throw new IOException(resp.getMessage().trim());
                }
            }
        } finally {
            if(svdrp != null) {
                svdrp.close();
            }
            if(fw != null) {
                fw.close();
            }
        }
    }
    
    private Command getPlayCommand(org.hampelratte.svdrp.Connection svdrp) throws IOException {
        Response res = svdrp.send(new CheckMplayerSvdrpInterface());
        if(res.getCode() == 214) {
            logger.log(LogService.LOG_DEBUG, "Using MPlayer to play the file");
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
                logger.log(LogService.LOG_DEBUG, "Using xineliboutput to play the file");
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

    @Override
    public void setPlaylist(Playlist playlist) {
        this.playlist = playlist;
    }
    
    @Validate
    public void start() {
        prefs = config.getUserPreferences(ctx.getBundle().getSymbolicName());
    }

}
