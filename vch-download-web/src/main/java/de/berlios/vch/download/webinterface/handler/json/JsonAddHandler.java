package de.berlios.vch.download.webinterface.handler.json;

import java.io.IOException;
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.log.LogService;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.PlaylistFileFoundException;
import de.berlios.vch.download.webinterface.handler.RequestHandler;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.uri.IVchUriResolveService;

public class JsonAddHandler implements RequestHandler {

    private IVchUriResolveService uriResolver;

    private DownloadManager dm;

    private LogService logger;

    public JsonAddHandler(IVchUriResolveService uriResolver, DownloadManager dm, LogService logger) {
        super();
        this.uriResolver = uriResolver;
        this.dm = dm;
        this.logger = logger;
    }

    @Override
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            URI vchuri = new URI(req.getParameter("vchuri"));
            IWebPage page = uriResolver.resolve(vchuri);
            // TODO check, if the video is not null and we support the format
            // fail gracefully otherwise
            if(page instanceof IVideoPage) {
                try {
                    dm.downloadItem((IVideoPage) page);
                } catch (PlaylistFileFoundException e) {
                    logger.log(LogService.LOG_WARNING, "Playlist file found. Retrying to download the video file");
                    dm.downloadItem((IVideoPage) page);
                }
            }

            resp.setContentType("text/plain");
            resp.getWriter().print("OK");
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "add".equals(action) && json;
    }
}
