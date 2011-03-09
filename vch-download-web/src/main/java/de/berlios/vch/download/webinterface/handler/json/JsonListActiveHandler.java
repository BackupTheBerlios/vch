package de.berlios.vch.download.webinterface.handler.json;

import java.io.IOException;
import java.util.Iterator;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

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
    public void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json; charset=utf-8");
        resp.getWriter().print("[");
        for (Iterator<Download> iterator = dm.getActiveDownloads().iterator(); iterator.hasNext();) {
            Download download = iterator.next();
            try {
                JSONObject json = new JSONObject();
                json.put("id", download.getId());
                json.put("title", download.getVideoPage().getTitle());
                json.put("progress", download.getProgress());
                json.put("status", download.getStatus());
                json.put("throughput", download.getSpeed());
                resp.getWriter().print(json.toString());
                if (iterator.hasNext()) {
                    resp.getWriter().print(",");
                }
            } catch (JSONException e) {
                throw new ServletException("Couldn't encode download as json", e);
            }
        }
        resp.getWriter().print("]");
    }

    @Override
    public boolean acceptRequest(String action, boolean json) {
        return "list_active".equals(action) && json;
    }
}
