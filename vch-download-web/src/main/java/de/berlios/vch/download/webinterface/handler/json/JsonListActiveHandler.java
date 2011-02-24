package de.berlios.vch.download.webinterface.handler.json;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import de.berlios.vch.download.Download;
import de.berlios.vch.download.DownloadManager;
import de.berlios.vch.download.webinterface.handler.RequestHandler;

public class JsonListActiveHandler implements RequestHandler {

    private DownloadManager dm;

    public JsonListActiveHandler(DownloadManager dm) {
        super();
        this.dm = dm;
    }

    @Override
    // TODO use org.json
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json; charset=utf-8");
        resp.getWriter().print("[");
        for (Iterator<Download> iterator = dm.getActiveDownloads().iterator(); iterator.hasNext();) {
            Download download = iterator.next();
            resp.getWriter().print("{\"id\":\"" + download.getId()+ "\","
                    + "\"title\":\"" + download.getVideoPage().getTitle().replaceAll("\"", "\\\\\"")+"\","
                    + "\"progress\":" + download.getProgress()+","
                    + "\"status\":\"" + download.getStatus()+"\","
                    + "\"throughput\":" + download.getSpeed()+"}");
            if(iterator.hasNext()) {
                resp.getWriter().print(",");
            }
        }
        resp.getWriter().print("]");
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "list_active".equals(action) && json;
    }
}
