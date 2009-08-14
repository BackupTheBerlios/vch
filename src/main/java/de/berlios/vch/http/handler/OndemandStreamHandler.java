package de.berlios.vch.http.handler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.parser.OndemandParser;
import de.berlios.vch.streaming.StreamBridge;
import de.berlios.vch.streaming.StreamBridgeFactory;

public class OndemandStreamHandler extends AbstractHandler {

    private static transient Logger logger = LoggerFactory.getLogger(OndemandStreamHandler.class);
    
    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        String provider = (String) params.get("provider");
        String url = (String) params.get("url");
        
        // parse the video page for the video uri
        logger.debug("Trying to stream {} from {} on the fly", url, provider);
        OndemandParser parser = (OndemandParser) Class.forName(provider).newInstance();
        String video = parser.parseOnDemand(url);
        if(video == null) {
            new NotFoundHandler().doHandle(exchange);
            return;
        } 
        
        logger.debug("Trying to stream {}", video);
        streamVideo(exchange, video);
    }

    @Override
    protected String getDescriptionKey() {
        return "description";
    }
    
    private void streamVideo(HttpExchange exchange, String video) throws IOException, URISyntaxException {
        StreamBridge bridge = StreamBridgeFactory.createBridge(exchange, new URI(video));
        bridge.startStream();
    }

    
}
