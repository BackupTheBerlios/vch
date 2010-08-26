package de.berlios.vch.download.webinterface;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.parser.IParserService;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class DownloadsServlet extends BundleContextServlet {

    private static final long serialVersionUID = 1L;

    public static final String PATH = "/downloads";
    
    public static final String FILE_PATH = PATH + "/files";
    
    public static final String STATIC_PATH = PATH + "/static";
    
    private DownloadManager dm;

    private IParserService parserService;
    
    public DownloadsServlet(DownloadManager dm, IParserService parserService) {
        this.dm = dm;
        this.parserService = parserService;
    }
    
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // get the action
        String action = req.getParameter("action");
        action = action == null ? "list" : action;
        
        if("stop".equals(action)) {
            String id = req.getParameter("id");
            dm.stopDownload(id);
        } else if("start".equals(action)) {
            String id = req.getParameter("id");
            dm.startDownload(id);
        } else if("delete".equals(action)) {
            String id = req.getParameter("id");
            dm.cancelDownload(id);
        } else if("start_all".equals(action)) {
            dm.startDownloads();
        } else if("stop_all".equals(action)) {
            dm.stopDownloads();
        } else if ("download_item".equals(action)) {
            //dm.downloadItem("bla");
        } else if ("delete_finished".equals(action)) {
            String id = req.getParameter("id");
            dm.deleteDownload(id);
            addNotify(req, new NotifyMessage(TYPE.INFO, i18n.translate("I18N_DL_FILE_DELETED")));
        } else if ("add".equals(action)) {
            String vchuri = req.getParameter("vchuri");
            try {
                URI uri = new URI(vchuri);
                IWebPage page = parserService.parse(uri);
                // TODO check, if the video is not null and we support the format
                // fail gracefully otherwise
                if(page instanceof IVideoPage) {
                    dm.downloadItem((IVideoPage) page);
                }
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }
            
        listDownloads(req, resp);
    }

    private void listDownloads(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("TITLE", i18n.translate("I18N_DOWNLOADS"));
        params.put("ACTION", PATH);
        params.put("FILE_PATH", FILE_PATH);
        params.put("STATIC_PATH", STATIC_PATH);
        params.put("DOWNLOADS", dm.getActiveDownloads());
        params.put("FINISHED_DOWNLOADS", dm.getFinishedDownloads());
        params.put("AJAX_ENABLED", true);
        params.put("NOTIFY_MESSAGES", getNotifyMessages(req));
        
        // additional css
        List<String> css = new ArrayList<String>();
        css.add(STATIC_PATH + "/downloads.css");
        params.put("CSS_INCLUDES", css);
        
        String page = templateLoader.loadTemplate("downloads.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }
}
