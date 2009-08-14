package de.berlios.vch.http.handler;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.utils.enclosurechecker.EnclosureChecker;

/**
 * Handler, which starts a database sweep
 */
public class SweepHandler extends AbstractHandler {

    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        final EnclosureChecker ec = EnclosureChecker.getInstance();
        if(ec.isRunning()) {
            sendResponse(503, "Sweep is running already. " +
                    "Progress: " + ec.getNumberOfProcessed() + " / " + ec.getNumberOfEnclosures() + " " +
                    "<br>Broken: " + ec.getBrokenCount() + "<br>" +
                    ec.getProgress() + "%", "text/html");
        } else {
            new Thread() {
                @Override
                public void run() {
                    ec.start();
                }
            }.start();
            sendResponse(200, "Sweep started", "text/html");
        }
    }
    
    @Override
    protected String getDescriptionKey() {
        return "description";
    }
}
