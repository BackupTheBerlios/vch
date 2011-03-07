package de.berlios.vch.download.webinterface.handler.html;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.webinterface.DownloadsServlet;
import de.berlios.vch.download.webinterface.handler.RequestHandler;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.servlets.VchHttpServlet;

public abstract class AbstractHtmlRequestHandler implements RequestHandler {

    protected ResourceBundle rb;

    protected DownloadManager dm;

    protected TemplateLoader templateLoader;

    protected VchHttpServlet servlet;

    public AbstractHtmlRequestHandler(VchHttpServlet servlet, ResourceBundle rb, DownloadManager dm, TemplateLoader templateLoader) {
        super();
        this.servlet = servlet;
        this.rb = rb;
        this.dm = dm;
        this.templateLoader = templateLoader;
    }

    @Override
    public abstract void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException;

    @Override
    public abstract boolean acceptRequest(String action, boolean json);

    void listDownloads(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("TITLE", rb.getString("I18N_DOWNLOADS"));
        params.put("ACTION", DownloadsServlet.PATH);
        params.put("FILE_PATH", DownloadsServlet.FILE_PATH);
        params.put("STATIC_PATH", DownloadsServlet.STATIC_PATH);
        params.put("DOWNLOADS", dm.getActiveDownloads());
        params.put("FINISHED_DOWNLOADS", dm.getFinishedDownloads());
        params.put("NOTIFY_MESSAGES", servlet.getNotifyMessages(req));

        // additional css
        List<String> css = new ArrayList<String>();
        css.add(DownloadsServlet.STATIC_PATH + "/downloads.css");
        params.put("CSS_INCLUDES", css);

        String page = templateLoader.loadTemplate("downloads.ftl", params);
        resp.getWriter().print(page);
    }

}
