package de.berlios.vch.download.webinterface.handler.json;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.webinterface.handler.RequestHandler;

public class JsonStopAllHandler implements RequestHandler {

    private DownloadManager dm;

    public JsonStopAllHandler(DownloadManager dm) {
        super();
        this.dm = dm;
    }

    @Override
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        dm.stopDownloads();
        resp.setContentType("text/plain");
        resp.getWriter().print("OK");
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "stop_all".equals(action) && json;
    }
}
