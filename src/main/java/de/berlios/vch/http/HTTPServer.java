package de.berlios.vch.http;

import java.net.InetSocketAddress;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;

import de.berlios.vch.Config;
import de.berlios.vch.db.DBSetup;
import de.berlios.vch.http.filter.ParameterParser;
import de.berlios.vch.http.handler.DefaultHandler;

public class HTTPServer {

    private static transient Logger logger = LoggerFactory.getLogger(HTTPServer.class);
    
    private static HTTPServer instance;
    
    private HttpServer server;

    private HTTPServer() {}

    public static synchronized HTTPServer getInstance() {
        if (instance == null) {
            instance = new HTTPServer();
        }
        return instance;
    }
    
    public void start() {
        // run DBSetup the first time
        logger.info("Checking database");
        DBSetup setup = new DBSetup();
        setup.runSetupIfNecessary();
        
        try {
            String lang = Config.getInstance().getProperty("locale.language");
            String country = Config.getInstance().getProperty("locale.country");
            if(country != null) {
                Locale.setDefault(new Locale(lang, country));
            } else {
                Locale.setDefault(new Locale(lang));
            }
            logger.info("Switched to locale " + Locale.getDefault());
            int port = Config.getInstance().getIntValue("webserver.listenport");
            server = HttpServer.create(new InetSocketAddress( port ), 0);
            HttpContext context = server.createContext("/", new DefaultHandler());
            logger.info("Webserver listening at {}", port);
            context.getFilters().add(new ParameterParser());
            ExecutorService executor = Executors.newFixedThreadPool(5);
            server.setExecutor(executor);
            server.start();
            logger.info("Yippie ya yay! VCH {} (revision: {}) is ready", 
                    Config.getInstance().getManifestProperty("VCH-Version"),
                    Config.getInstance().getManifestProperty("VCH-Revision"));
        } catch (Exception e) {
            logger.error("An unexpected error occured", e);
        }
    }
    
    public void stop() {
        logger.info("Stopping VCH");
        server.stop(0);
    }
    
    public static void main(String[] args) {
        HTTPServer.getInstance().start();
    }
}
