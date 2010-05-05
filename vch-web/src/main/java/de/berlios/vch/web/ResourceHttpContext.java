package de.berlios.vch.web;

import java.io.IOException;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpContext;
import org.osgi.service.log.LogService;

public class ResourceHttpContext implements HttpContext {

    private LogService log;
    
    private BundleContext ctx;
    
    public ResourceHttpContext(BundleContext ctx, LogService log) {
        this.ctx = ctx;
        this.log = log;
    }
    
    @Override
    public String getMimeType(String name) {
        if(name != null) {
            if(name.endsWith(".jpg")) {
                return "image/jpg";
            } else if(name.endsWith(".js")) {
                return "application/javascript";
            } else if(name.endsWith(".css")) {
                return "text/css";
            }
        }
        return null;
    }

    @Override
    public URL getResource(String res) {
        log.log(LogService.LOG_DEBUG, "Resolving resource "+res+" in bundle " + ctx.getBundle().getSymbolicName());
        return ctx.getBundle().getResource(res);
    }

    @Override
    public boolean handleSecurity(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        return true;
    }

}
