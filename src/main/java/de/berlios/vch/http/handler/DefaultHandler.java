package de.berlios.vch.http.handler;

import java.io.IOException;
import java.net.InetSocketAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.berlios.vch.Config;
import de.berlios.vch.http.HandlerMapping;

public class DefaultHandler implements HttpHandler {
    
    private static transient Logger logger = LoggerFactory.getLogger(DefaultHandler.class);
    
    private HandlerMapping mapping = Config.getInstance().getHandlerMapping();
    
    private NotFoundHandler notFoundHandler = new NotFoundHandler();
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        long start = System.currentTimeMillis();
        String requestedURI = exchange.getRequestURI().toString();
        logger.debug("Requested URI: {}", requestedURI);
        
        // save local ip address of this connection as fallback,
        // if InetAddress.getLocalHost().getHostAddress()
        InetSocketAddress addr = exchange.getLocalAddress();
        String baseUrl = "http://" + addr.getAddress().getHostAddress() + ":" + addr.getPort();
        Config.getInstance().setBaseUrl(baseUrl);

        HttpHandler handler = mapping.findHandler(requestedURI);
        if(handler != null) {
            handler.handle(exchange);
        } else {
            notFoundHandler.handle(exchange);
        }
        long stop = System.currentTimeMillis();
        logger.trace("Request [{}] took {} ms", requestedURI, (stop-start));
    }
}