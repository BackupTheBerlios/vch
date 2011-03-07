package de.berlios.vch.download.webinterface.handler.html;

import java.io.IOException;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.servlets.VchHttpServlet;

public class HtmlDeleteHandler extends AbstractHtmlRequestHandler {

    public HtmlDeleteHandler(VchHttpServlet servlet, ResourceBundle rb, DownloadManager dm, TemplateLoader templateLoader) {
        super(servlet, rb, dm, templateLoader);
    }

    @Override
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        dm.cancelDownload(id);
        listDownloads(req, resp);
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "delete".equals(action) && !json;
    }
}
