package de.berlios.vch.osgi.hotswap;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

class ClientHandlerThread extends Thread {
    
    private BundleContext ctx;
    private Socket socket;
    private PrintStream out;
    private InputStream in;

    public ClientHandlerThread(BundleContext ctx, Socket socket) {
        this.ctx = ctx;
        this.socket = socket;
    }

    public void run() {
        try {
            out = new PrintStream(socket.getOutputStream());
            in = socket.getInputStream();
            List<String> request = readRequest();
            String symbolicName = getParameter(request, "Bundle-SymbolicName");
            String url = getParameter(request, "URL");
            try {
                restartBundle(symbolicName, url);
                sendResponse("HTTP/1.0 200 OK");
            } catch (BundleException e) {
                e.printStackTrace();
                out.print("HTTP/1.0 500 Internal Server Error\r\n\r\n");
                e.printStackTrace(out);
                out.print("\r\n\r\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private List<String> readRequest() throws IOException {
        List<String> request = new ArrayList<String>(10);
        StringBuffer sb = new StringBuffer(100);
        int c;
        while ((c = in.read()) != -1) {
            if (c == 13)
                continue;
            if (c == 10) {
                if (sb.length() <= 0)
                    break;
                request.add(sb.toString());
                sb = new StringBuffer(100);
            } else {
                sb.append((char) c);
            }
        }
        String s = (String) request.get(0);
        int pos = s.indexOf(' ');
        if (pos != -1) {
            s = s.substring(pos + 1);
            pos = s.indexOf(' ');
            if (pos != -1)
                s = s.substring(pos + 1);
        }
        return request;
    }

    private void restartBundle(String symbolicName, String url) throws BundleException {
        Bundle abundle[];
        int j = (abundle = ctx.getBundles()).length;
        Bundle bundle;
        for (int i = 0; i < j; i++) {
            bundle = abundle[i];
            if (symbolicName.equals(bundle.getSymbolicName()))
                bundle.uninstall();
        }

        bundle = ctx.installBundle(url);
        bundle.start();
    }

    private String getParameter(List<String> request, String key) {
        for (Iterator<String> iterator = request.iterator(); iterator.hasNext();) {
            String header = iterator.next();
            if (header.startsWith((new StringBuilder(String.valueOf(key))).append(": ").toString()))
                return header.split(": ")[1];
        }

        return null;
    }

    private void sendResponse(String response) {
        out.println((new StringBuilder(String.valueOf(response))).append("\r\n\r\n").toString());
    }
}
