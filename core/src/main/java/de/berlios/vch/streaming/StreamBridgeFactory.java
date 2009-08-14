package de.berlios.vch.streaming;

import java.net.URI;

import com.sun.net.httpserver.HttpExchange;

public class StreamBridgeFactory {
    public static StreamBridge createBridge(HttpExchange exchange, URI uri) {
        String scheme = uri.getScheme();
        StreamBridge bridge = null;        
        if("mms".equals(scheme)) {
            bridge = new MmsHttpBridge(uri, exchange);
        } else if("http".equals(scheme)) {
            bridge = new HttpHttpBridge(uri, exchange);
        //} else if("rtmp".equals(scheme)) {
        //    bridge = new RtmpHttpBridge(uri, exchange);
        }
        return bridge;
    }
}
