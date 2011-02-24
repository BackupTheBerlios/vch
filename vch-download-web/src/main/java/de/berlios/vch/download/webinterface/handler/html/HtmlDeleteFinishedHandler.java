package de.berlios.vch.download.webinterface.handler.html;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.servlets.VchHttpServlet;

public class HtmlDeleteFinishedHandler extends AbstractHtmlRequestHandler {

    public HtmlDeleteFinishedHandler(VchHttpServlet servlet, Messages i18n, DownloadManager dm,
            TemplateLoader templateLoader) {
        super(servlet, i18n, dm, templateLoader);
    }

    @Override
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        dm.deleteDownload(id);
        servlet.addNotify(req, new NotifyMessage(TYPE.INFO, i18n.translate("I18N_DL_FILE_DELETED")));
        listDownloads(req, resp);
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "delete_finished".equals(action) && !json;
    }
}
