package de.berlios.vch.download.webinterface.handler.json;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.jaxb.DownloadDTO;
import de.berlios.vch.download.webinterface.handler.RequestHandler;

public class JsonListFinishedHandler implements RequestHandler {

    private DownloadManager dm;

    public JsonListFinishedHandler(DownloadManager dm) {
        super();
        this.dm = dm;
    }

    @Override
    // TODO use org.json
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json; charset=utf-8");
        resp.getWriter().print("[");
        for (Iterator<DownloadDTO> iterator = dm.getFinishedDownloads().iterator(); iterator.hasNext();) {
            DownloadDTO download = iterator.next();
            resp.getWriter().print(
                    "{\"id\":\"" + download.getId() + "\"," + "\"title\":\""
                            + download.getTitle().replaceAll("\"", "\\\\\"") + "\"}");
            if (iterator.hasNext()) {
                resp.getWriter().print(",");
            }
        }
        resp.getWriter().print("]");
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "list_finished".equals(action) && json;
    }
}
