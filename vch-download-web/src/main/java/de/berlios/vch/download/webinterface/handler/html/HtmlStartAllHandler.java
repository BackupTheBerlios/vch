package de.berlios.vch.download.webinterface.handler.html;

import java.io.IOException;
import java.util.ResourceBundle;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.servlets.VchHttpServlet;

public class HtmlStartAllHandler extends AbstractHtmlRequestHandler {

    public HtmlStartAllHandler(VchHttpServlet servlet, ResourceBundle rb, DownloadManager dm, TemplateLoader templateLoader) {
        super(servlet, rb, dm, templateLoader);
    }

    @Override
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dm.startDownloads();
        listDownloads(req, resp);
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "start_all".equals(action) && !json;
    }
}
