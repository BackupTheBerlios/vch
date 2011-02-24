package de.berlios.vch.download.webinterface.handler.json;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.webinterface.handler.RequestHandler;

public class JsonDeleteHandler implements RequestHandler {

    private DownloadManager dm;

    public JsonDeleteHandler(DownloadManager dm) {
        super();
        this.dm = dm;
    }

    @Override
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String id = req.getParameter("id");
        dm.cancelDownload(id);
        resp.setContentType("text/plain");
        resp.getWriter().print("OK");
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "delete".equals(action) && json;
    }
}
