package de.berlios.vch.osdserver;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.playlist.PlaylistService;
import de.berlios.vch.web.servlets.VchHttpServlet;

@Component
public class ActivatorServlet extends VchHttpServlet {

    public final static String PATH = "/osdserver";

    @Requires
    private Messages i18n;

    @Requires
    private PlaylistService playlistService;

    @Requires
    private HttpService httpService;

    private BundleContext ctx;

    public ActivatorServlet(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, String> requestPrefs = new HashMap<String, String>();
        Enumeration<?> prefNames = req.getParameterNames();
        while (prefNames.hasMoreElements()) {
            String name = (String) prefNames.nextElement();
            if (name != null) {
                String v = req.getParameter(name);
                if (v != null) {
                    requestPrefs.put(name, v);
                }
            }
        }

        Thread t = new Thread(new OsdSession(ctx, i18n, playlistService, requestPrefs));
        t.setName("Osdserver Session");
        t.start();
        resp.getWriter().println("Osdserver session started");
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    @Validate
    public void start() throws ServletException, NamespaceException {
        // register the servlet
        httpService.registerServlet(ActivatorServlet.PATH, this, null, null);
    }

    @Invalidate
    public void stop() {
        // unregister the config servlet
        if (httpService != null) {
            httpService.unregister(ActivatorServlet.PATH);
        }
    }
}
