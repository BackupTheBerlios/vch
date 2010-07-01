package de.berlios.vch.osdserver;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.playlist.PlaylistService;

public class ActivatorServlet extends HttpServlet {
    
    private Messages i18n;
    
    private BundleContext ctx;
    
    private PlaylistService playlistService;
    
    public ActivatorServlet(BundleContext ctx, Messages i18n, PlaylistService playlistService) {
        this.i18n = i18n;
        this.ctx = ctx;
        this.playlistService = playlistService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Thread t = new Thread(new OsdSession(ctx, i18n, playlistService));
        t.setName("Osdserver Session");
        t.start();
        resp.getWriter().println("Osdserver session started");
    }
}
