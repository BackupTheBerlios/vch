package de.berlios.vch.http.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.osdserver.OsdSession;


public class OsdserverHandler extends AbstractHandler {

    private static transient Logger logger = LoggerFactory.getLogger(OsdserverHandler.class);
    
    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        logger.info("Starting osdserver session");
        Thread t = new Thread(new OsdSession());
        t.setName("Osdserver Session");
        t.start();
        sendResponse(200, "Osdserver session started\n", "text/plain");
    }
    
    @Override
    protected String getDescriptionKey() {
        return "handler_description";
    }
}
