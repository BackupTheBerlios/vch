package de.berlios.vch.web;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class ServletMapping extends HashMap<String, String> {

    private static transient Logger logger = LoggerFactory.getLogger(ServletMapping.class);

    private InputStream in = ServletMapping.class.getResourceAsStream("/servlet_mapping.xml");
    
    private static ServletMapping instance;
    
    private ServletMapping() {
        loadMapping(in);
    }
    
    private ServletMapping(InputStream in) {
        loadMapping(in);
    }
    
    public static synchronized ServletMapping getInstance() {
        if(instance == null) {
            instance = new ServletMapping();
        }
        return instance;
    }
    
    @SuppressWarnings("unchecked")
    private void loadMapping(InputStream in) {
        try {
            DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = fac.newDocumentBuilder();
            Document doc = builder.parse(in);
            Element e = doc.getDocumentElement();
            NodeList mappings = e.getElementsByTagName("mapping");
            for (int i = 0; i < mappings.getLength(); i++) {
                Element mapping = (Element) mappings.item(i);
                NodeList pathes = mapping.getElementsByTagName("path");
                NodeList servlet = mapping.getElementsByTagName("servlet");
                for (int j = 0; j < pathes.getLength(); j++) {
                    Element path = (Element) pathes.item(j);
                    put(path.getTextContent(), servlet.item(0).getTextContent());
                }
            }
        } catch (Exception e) {
            logger.error("Couldn't load servlet mapping", e);
            System.exit(1);
        } 
        
        // debug
        for (Iterator iterator = keySet().iterator(); iterator.hasNext();) {
            String path = (String) iterator.next();
            logger.debug(path + " -> " + get(path));
        }
    }
    
    public List<String> getPathes(Class<? extends HttpServlet> handler) {
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
    
    public String getPath(Class<? extends HttpServlet> handler) {
        List<String> pathes = getPathes(handler);
        if(pathes.size() > 0) {
            return pathes.get(0);
        } 
        
        return null;
    }
}
