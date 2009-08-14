package de.berlios.vch.streaming;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import com.sun.net.httpserver.HttpExchange;

public class HttpHttpBridge implements StreamBridge {

    private URI video;
    
    private HttpExchange exchange;
    
    public HttpHttpBridge(URI video, HttpExchange exchange) {
        this.video = video;
        this.exchange = exchange;
    }
    
    public void startStream() throws IOException {
        URL url = video.toURL();
        URLConnection con = url.openConnection();
        InputStream in = con.getInputStream();

        // try to forward response headers
        String contentType = con.getContentType();
        int length = con.getContentLength();
        if(contentType != null) {
            exchange.getResponseHeaders().add("Content-type", contentType);
        }
                
        // send response headers
        exchange.sendResponseHeaders(200, length > 0 ? length : 0);
        
        length = -1;
        byte[] b = new byte[10240];
        while( (length = in.read(b)) > 0 ) {
            exchange.getResponseBody().write(b, 0, length);
        }
        in.close();
    }
}
