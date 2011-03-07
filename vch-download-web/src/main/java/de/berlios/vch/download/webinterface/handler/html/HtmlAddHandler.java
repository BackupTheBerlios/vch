package de.berlios.vch.download.webinterface.handler.html;

import java.io.IOException;
import java.net.URI;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.log.LogService;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.PlaylistFileFoundException;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.uri.IVchUriResolveService;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.servlets.VchHttpServlet;

public class HtmlAddHandler extends AbstractHtmlRequestHandler {

    private IVchUriResolveService uriResolver;

    private LogService logger;

    public HtmlAddHandler(VchHttpServlet servlet, ResourceBundle rb, DownloadManager dm, TemplateLoader templateLoader,
            IVchUriResolveService uriResolver, LogService logger) {
        super(servlet, rb, dm, templateLoader);
        this.uriResolver = uriResolver;
        this.logger = logger;
    }

    @Override
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            URI vchuri = new URI(req.getParameter("vchuri"));
            IWebPage page = uriResolver.resolve(vchuri);
            // TODO check, if the video is not null and we support the format
            // fail gracefully otherwise
            if (page instanceof IVideoPage) {
                try {
                    dm.downloadItem((IVideoPage) page);
                } catch (PlaylistFileFoundException e) {
                    logger.log(LogService.LOG_WARNING, "Playlist file found. Retrying to download the video file");
                    dm.downloadItem((IVideoPage) page);
                }
            }

            listDownloads(req, resp);
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "add".equals(action) && !json;
    }

}
