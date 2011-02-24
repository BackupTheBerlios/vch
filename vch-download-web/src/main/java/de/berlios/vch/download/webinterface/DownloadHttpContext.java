package de.berlios.vch.download.webinterface;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.prefs.Preferences;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.http.HttpContext;
import org.osgi.service.log.LogService;

public class DownloadHttpContext implements HttpContext {

    private LogService logger;

    private Preferences prefs;

    public DownloadHttpContext(Preferences prefs, LogService logger) {
        this.prefs = prefs;
        this.logger = logger;
    }

    @Override
    public String getMimeType(String name) {
        if(name.endsWith(".flv")) {
            return "video/x-flv";
        } else if(name.endsWith(".wmv")) {
            return "video/x-ms-wmv";
        }
        return null;
    }

    @Override
    public URL getResource(String name) {
        try {
            name = URLDecoder.decode(name, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            logger.log(LogService.LOG_ERROR, "Couldn't decode file name", e);
            return null;
        }

        File dataDir = new File(prefs.get("data.dir", "data"));
        File file = new File(dataDir, name);
        if(file.exists()) {
            try {
                return file.toURI().toURL();
            } catch (MalformedURLException e) {
                logger.log(LogService.LOG_WARNING, "Couldn't create URL for file " + file, e);
                return null;
            }
        } else {
            logger.log(LogService.LOG_WARNING, "File not found: " + file.getAbsolutePath());
        }
        return null;
    }

    @Override
    public boolean handleSecurity(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // allow all requests
        return true;
    }

}
