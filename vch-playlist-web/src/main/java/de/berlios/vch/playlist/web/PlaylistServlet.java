package de.berlios.vch.playlist.web;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.osgi.service.log.LogService;

import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.playlist.Playlist;
import de.berlios.vch.playlist.PlaylistEntry;
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class PlaylistServlet extends BundleContextServlet {

    public static final String PATH = "/playlist";

    private Activator activator;

    public PlaylistServlet(Activator activator) {
        this.activator = activator;
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        HttpSession session = req.getSession();
        session.setMaxInactiveInterval(-1);

        Playlist pl = activator.getPlaylistService().getPlaylist();
        boolean json = "XMLHttpRequest".equals(req.getHeader("X-Requested-With"));

        String action = req.getParameter("action");
        if ("add".equalsIgnoreCase(action)) {
            String uri = req.getParameter("uri");
            try {
                IWebPage page = activator.getUriResolverService().resolve(new URI(uri));
                if (page instanceof IVideoPage) {
                    PlaylistEntry entry = new PlaylistEntry((IVideoPage) page);
                    pl.add(entry);

                    if (json) {
                        resp.setContentType("text/plain");
                        resp.getWriter().println("OK");
                        return;
                    }
                } else {
                    String msg = i18n.translate("not_a_video");
                    addNotify(req, new NotifyMessage(TYPE.ERROR, msg));
                    logger.log(LogService.LOG_ERROR, msg);
                    if (json) {
                        error(resp, HttpServletResponse.SC_BAD_REQUEST, msg, true);
                        return;
                    }
                }
            } catch (Exception e) {
                addNotify(req, new NotifyMessage(TYPE.ERROR, e.getLocalizedMessage()));
                logger.log(LogService.LOG_ERROR, e.getLocalizedMessage(), e);
                if (json) {
                    error(resp, HttpServletResponse.SC_BAD_REQUEST, e.getLocalizedMessage(), true);
                    return;
                }
            }
        } else if ("play".equals(action)) {
            if (activator.getPlaylistService() != null) {
                try {
                    activator.getPlaylistService().play(pl, null);
                    if (json) {
                        resp.setContentType("text/plain");
                        resp.getWriter().println("OK");
                        return;
                    }
                } catch (Exception e) {
                    addNotify(req, new NotifyMessage(TYPE.ERROR, e.getLocalizedMessage()));
                    logger.log(LogService.LOG_ERROR, e.getLocalizedMessage(), e);
                    if (json) {
                        String msg = i18n.translate("couldnt_start_playlist", e.getLocalizedMessage());
                        error(resp, HttpServletResponse.SC_OK, msg, true);
                        return;
                    }
                }
            } else {
                addNotify(req, new NotifyMessage(TYPE.ERROR, i18n.translate("playlist_service_missing")));
                logger.log(LogService.LOG_ERROR, i18n.translate("playlist_service_missing"));
            }
        } else if ("reorder".equals(action)) {
            String[] order = req.getParameterValues("pe[]");
            Playlist newPl = new Playlist();
            for (int i = 0; i < order.length; i++) {
                String id = order[i];
                for (PlaylistEntry playlistEntry : pl) {
                    if (id.equals(playlistEntry.getId())) {
                        newPl.add(playlistEntry);
                    }
                }
            }
            pl = newPl;
            activator.getPlaylistService().setPlaylist(pl);
            resp.getWriter().println("OK");
            return;
        } else if ("clear".equals(action)) {
            pl.clear();
            if (json) {
                resp.setContentType("text/plain");
                resp.getWriter().println("OK");
                return;
            }
        } else if ("list".equals(action)) {
            resp.setContentType("application/json; charset=utf-8");
            resp.getWriter().print("[");
            for (Iterator<PlaylistEntry> iterator = pl.iterator(); iterator.hasNext();) {
                PlaylistEntry entry = iterator.next();
                resp.getWriter().print(
                        "{\"id\":\"" + entry.getId() + "\",\"title\":\""
                                + entry.getVideo().getTitle().replaceAll("\"", "\\\\\"") + "\"}");
                if (iterator.hasNext()) {
                    resp.getWriter().print(",");
                }
            }
            resp.getWriter().print("]");
            return;
        } else if ("remove".equals(action)) {
            String id = req.getParameter("id");
            for (Iterator<PlaylistEntry> iterator = pl.iterator(); iterator.hasNext();) {
                PlaylistEntry playlistEntry = iterator.next();
                if (id.equals(playlistEntry.getId())) {
                    iterator.remove();
                    resp.getWriter().println("OK");
                    return;
                }
            }
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Playlist entry not found"); // TODO i18n, response as
                                                                                            // notification
        }

        // now display the playlist
        displayPlaylist(req, resp, pl);
    }

    private void displayPlaylist(HttpServletRequest req, HttpServletResponse resp, Playlist pl) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("TITLE", i18n.translate("I18N_PLAYLIST"));
        params.put("ACTION", PATH);
        params.put("PLAYLIST", pl);
        params.put("NOTIFY_MESSAGES", getNotifyMessages(req));

        String page = templateLoader.loadTemplate("playlist.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        post(req, resp);
    }
}
