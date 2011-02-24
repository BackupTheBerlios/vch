package de.berlios.vch.download.webinterface.handler.html;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.servlets.VchHttpServlet;

public class HtmlStopAllHandler extends AbstractHtmlRequestHandler {

    public HtmlStopAllHandler(VchHttpServlet servlet, Messages i18n, DownloadManager dm, TemplateLoader templateLoader) {
        super(servlet, i18n, dm, templateLoader);
    }

    @Override
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dm.stopDownloads();
        listDownloads(req, resp);
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "stop_all".equals(action) && !json;
    }
}
