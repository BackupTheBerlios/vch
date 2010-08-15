package de.berlios.vch.osgi.hotswap;

import java.io.IOException;
import java.net.ServerSocket;
import org.osgi.framework.*;

public class Activator implements BundleActivator {

    private ServerSocket httpd;
    private int port;
    private BundleContext ctx;
    
    public Activator() {
        port = 8765;
    }

    public void start(BundleContext ctx) throws Exception {
        this.ctx = ctx;
        startServer();
    }

    public void stop(BundleContext ctx) throws Exception {
        httpd.close();
    }

    private void startServer() {
        Thread server = new Thread() {
            public void run() {
                try {
                    httpd = new ServerSocket(port);
                    do {
                        java.net.Socket socket = httpd.accept();
                        (new ClientHandlerThread(ctx, socket)).start();
                    } while (true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        server.setName((new StringBuilder(String.valueOf(ctx.getBundle().getSymbolicName()))).append(" Server").toString());
        server.start();
    }
}
