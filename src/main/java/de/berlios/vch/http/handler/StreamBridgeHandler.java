package de.berlios.vch.http.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.streaming.StreamBridge;
import de.berlios.vch.streaming.StreamBridgeFactory;

public class StreamBridgeHandler extends AbstractHandler {

    void doHandle(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        String uri = (String) params.get("uri");
        if(uri != null) {
            streamVideo(exchange, uri);
        } else {
            new BadRequestHandler("Missing parameter uri").handle(exchange);
            return;
        }
    }

    private void streamVideo(HttpExchange exchange, String video) throws IOException, URISyntaxException {
        StreamBridge bridge = StreamBridgeFactory.createBridge(exchange, new URI(video));
        bridge.startStream();
    }

    @Override
    protected String getDescriptionKey() {
        return "description";
    }

}
