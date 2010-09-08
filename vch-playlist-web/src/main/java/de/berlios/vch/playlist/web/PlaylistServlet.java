package de.berlios.vch.playlist.web;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.log.LogService;

import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistEntry;
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class PlaylistServlet extends BundleContextServlet {

    public static final String PATH = "/playlist";
    
    private static final String PLAYLIST = "playlist";
    
    private Activator activator;
    
    public PlaylistServlet(Activator activator) {
        this.activator = activator;
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        HttpSession session = req.getSession();
        session.setMaxInactiveInterval(-1);
        
        // get the playlist from the session or create a new one
        Playlist pl = (Playlist) session.getAttribute(PLAYLIST);
        if(pl == null) {
            logger.log(LogService.LOG_INFO, "Adding new playlist to session");
            pl = new Playlist();
            session.setAttribute(PLAYLIST, pl);
        }
        
        String action = req.getParameter("action");
        if("add".equalsIgnoreCase(action)) {
            String title = req.getParameter("title");
            String uri = req.getParameter("uri");
            PlaylistEntry entry = new PlaylistEntry(title, uri);
            pl.add(entry);
        } else if("play".equals(action)) {
            if(activator.getPlaylistService() != null) {
                activator.getPlaylistService().play(pl);
            } else {
                addNotify(req, new NotifyMessage(TYPE.ERROR, i18n.translate("playlist_service_missing")));
            }
        } else if("reorder".equals(action)) {
            String[] order = req.getParameterValues("pe[]");
            Playlist newPl = new Playlist();
            for (int i = 0; i < order.length; i++) {
                String id = order[i];
                for (PlaylistEntry playlistEntry : pl) {
                    if(id.equals(playlistEntry.getId())) {
                        newPl.add(playlistEntry);
                    }
                }
            }
            pl = newPl;
            session.setAttribute(PLAYLIST, pl);
            resp.getWriter().println("OK");
            return;
        } else if("clear".equals(action)) {
            pl.clear();
        } else if("remove".equals(action)) {
            String id = req.getParameter("id");
            for (Iterator<PlaylistEntry> iterator = pl.iterator(); iterator.hasNext();) {
                PlaylistEntry playlistEntry = iterator.next();
                if(id.equals(playlistEntry.getId())) {
                    iterator.remove();
                    resp.getWriter().println("OK");
                    return;
                }
            }
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Playlist entry not found");
        }
        
        // now display the playlist
        displayPlaylist(req, resp, pl);
    }

    private void displayPlaylist(HttpServletRequest req, HttpServletResponse resp, Playlist pl) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("TITLE", i18n.translate("I18N_PLAYLIST"));
        params.put("ACTION", PATH);
        params.put("PLAYLIST", pl);

        String page = templateLoader.loadTemplate("playlist.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        post(req, resp);
    }
}
