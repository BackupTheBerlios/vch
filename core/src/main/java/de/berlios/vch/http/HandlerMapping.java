package de.berlios.vch.http;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpHandler;

import de.berlios.vch.Config;
import de.berlios.vch.http.handler.BadRequestHandler;
import de.berlios.vch.http.handler.RssFeedHandler;

public class HandlerMapping extends HashMap<String, String> {

    private static transient Logger logger = LoggerFactory.getLogger(HandlerMapping.class);

    private InputStream in = Config.class.getResourceAsStream("/handler_mapping.xml");
    
    public HandlerMapping() {
        loadMapping(in);
    }
    
    public HandlerMapping(InputStream in) {
        loadMapping(in);
    }
    
    public HttpHandler findHandler(String uriString) {
        URI uri = null;
        try {
            uri = new URI(uriString);
        } catch (URISyntaxException e1) {
            logger.error("URI Syntax error", e1);
            return new BadRequestHandler("URI Syntax error ["+uriString+"]");
        }
        
        String path = uri.getPath();
        String handlerName = null;
        HttpHandler handler = null;
        for (Iterator<String> iterator = keySet().iterator(); iterator.hasNext();) {
            String key = iterator.next();
            if(path.equals(key)) {
                handlerName = get(key);
                break;
            } else if(key.endsWith("/*")) {
                String _key = key.substring(0, key.length() - 1);
                if(path.startsWith(_key)) {
                    handlerName = get(key);
                    break;
                }
            }
        }
        
        try {
            handler = createHandler(handlerName);
        } catch (Exception e) {
            logger.warn("Couldn't load handler for URI [" + uri + "]", e);
        }
        
        return handler;
    }
    
    public String getFeedPath() {
        List<String> pathes = getPathes(RssFeedHandler.class);
        return pathes.get(0);
    }
    
    public List<String> getPathes(Class<? extends HttpHandler> handler) {
        List<String> pathes = new ArrayList<String>();
        for (Iterator<String> iterator = keySet().iterator(); iterator.hasNext();) {
            String path = (String) iterator.next();
            String handlerName = get(path);
            if(handler.getName().equals(handlerName)) {
                if(path.endsWith("/*")) {
                    path = path.substring(0, path.length()-2);
                }
                pathes.add(path);
            } 
        }
        return pathes;
    }
    
    public String getPath(Class<? extends HttpHandler> handler) {
        List<String> pathes = getPathes(handler);
        if(pathes.size() > 0) {
            return pathes.get(0);
        } 
        
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private HttpHandler createHandler(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        Class handlerClass = Class.forName(className);
        return (HttpHandler) handlerClass.newInstance();
    }
    
    @SuppressWarnings("unchecked")
    private void loadMapping(InputStream in) {
        SAXBuilder builder = new SAXBuilder();
        try {
            Document doc = builder.build(in);
            Element e = doc.getRootElement();
            List<Element> mappings = e.getChildren("mapping");
            for (Iterator iterator = mappings.iterator(); iterator.hasNext();) {
                Element mapping = (Element) iterator.next();
                List pathes = mapping.getChildren("path");
                Element handler = mapping.getChild("handler");
                for (Iterator iter = pathes.iterator(); iter.hasNext();) {
                    Element path = (Element) iter.next();
                    put(path.getText(), handler.getText());
                }
            }
        } catch (Exception e) {
            logger.error("Couldn't load handler mapping", e);
            System.exit(1);
        } 
        
        // debug
        for (Iterator iterator = keySet().iterator(); iterator.hasNext();) {
            String path = (String) iterator.next();
            logger.debug(path + " -> " + get(path));
        }
    }
}
