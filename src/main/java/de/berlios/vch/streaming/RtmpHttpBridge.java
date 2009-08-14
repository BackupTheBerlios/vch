package de.berlios.vch.streaming;

import java.io.IOException;
import java.net.URI;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.download.RtmpDownload;

public class RtmpHttpBridge implements StreamBridge {

    private URI video;
    
    private HttpExchange exchange;
    
    public RtmpHttpBridge(URI video, HttpExchange exchange) {
        this.video = video;
        this.exchange = exchange;
    }
    
    public void startStream() throws IOException {
        // send response headers with content-length 0, because we don't know the size
        exchange.getResponseHeaders().add("Content-type", "video/flv");
        exchange.sendResponseHeaders(200, 0);
        
        RtmpDownload d = new RtmpDownload(video);
        d.setOutputStream(exchange.getResponseBody());
        d.run();
    }
}
