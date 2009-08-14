package de.berlios.vch.http.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.Config;
import de.berlios.vch.http.HandlerMapping;

/**
 * Handler for classic http file serving.
 * The handler looks for the file in the jar file under /htdocs 
 * and if it is not found there, in the directory specified 
 * by the config parameter "docroot"
 */
public class FileHandler extends AbstractHandler {

    private static transient Logger logger = LoggerFactory.getLogger(FileHandler.class);
    
    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        URI requestedURI = exchange.getRequestURI();
        String requestedPath = requestedURI.getPath();
        HandlerMapping mapping = Config.getInstance().getHandlerMapping();
        List<String> pathes = mapping.getPathes(getClass());
        for (Iterator<String> iterator = pathes.iterator(); iterator.hasNext();) {
            String path = iterator.next();
            if(path.endsWith("/*")) {
                path = path.substring(0, path.length()-1);
            }
            
            if(requestedPath.startsWith(path)) {
                requestedPath = requestedPath.substring(path.length());
                logger.debug("Requested file {}", requestedPath);
                break;
            }
        }
        
        if(requestedPath.endsWith("/")) {
            requestedPath += "index.html";
        }
        
        InputStream in = loadFromClasspath(requestedPath);
        if(in != null) {
            sendFile(in);
            return;
        } else {
            in = loadFromFilesystem(requestedPath);
            if(in != null) {
                // add content length to the response headers
                File file = new File(requestedPath);
                this.exchange.getResponseHeaders().add("Content-length", Long.toString(file.length()));
                
                // stream the file
                sendFile(in);
                return;
            }
        }
        
        // file couldn't be found
        new NotFoundHandler().handle(exchange);
    }
    
    private InputStream loadFromFilesystem(String requestedPath) throws IOException {
        List<String> locations = new ArrayList<String>();
        locations.add(Config.getInstance().getProperty("docroot"));
        locations.add(Config.getInstance().getProperty("video.cache.dir"));

        for (String location : locations) {
            File file = new File(location, requestedPath);
            logger.debug("Looking for {}", file.getAbsolutePath());
            if(file.exists()) {
                String canonicalPath = file.getCanonicalPath();
                if(canonicalPath.startsWith(location)) {
                    logger.debug("Found requested file {} on disk", canonicalPath);
                    return new FileInputStream(file);
                } else {
                    logger.warn("docroot breakout detected. {} ist not in an allowed directory", canonicalPath);
                }
            }
        }

        logger.debug("{} not found", requestedPath);
        return null;
    }

    private InputStream loadFromClasspath(String requestedPath) throws IOException {
        String jarPath = "/htdocs" + requestedPath;
        logger.debug("Looking for {} with Classloader", jarPath);
        URL url = FileHandler.class.getResource(jarPath);
        InputStream in = null;
        if(url != null) {
            logger.debug("Found requested file {}", url);
            in = url.openStream();
        }
        return in;
    }

    private void sendFile(InputStream in) throws IOException {
        this.exchange.getResponseHeaders().add("Cache-Control", "max-age=" + TimeUnit.HOURS.toSeconds(6));
        this.exchange.sendResponseHeaders(200, 0);
        int length = -1;
        byte[] buffer = new byte[1024];
        while( (length = in.read(buffer)) > 0 ) {
            exchange.getResponseBody().write(buffer, 0, length);
        }
    }
    
    @Override
    protected String getDescriptionKey() {
        return "description";
    }
}
