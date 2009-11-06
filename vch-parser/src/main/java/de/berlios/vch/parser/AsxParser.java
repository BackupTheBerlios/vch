package de.berlios.vch.parser;

import java.io.FileNotFoundException;
import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import de.berlios.vch.http.client.HttpUtils;

/**
 * Utility class to parse ASX files
 *
 * @author <a href="mailto:hampelratte@users.berlios.de">hampelratte@users.berlios.de</a>
 */
public class AsxParser {
    
    private static transient Logger logger = LoggerFactory.getLogger(AsxParser.class);
    
    private static final String CHARSET = "utf-8";
    
    /**
     * Get the <code>n</code>-th URI from the file
     * @param asxFile URI of the asx file to parse
     * @param n Index of the desired entry. Counting starts with 0.
     * @return URI of the n-th entry as String
     */
    public static String getUri(String asxFile, int n) {
        String uri = "";

        try {
            // download the file
            String content = HttpUtils.get(asxFile, null, CHARSET);
            
            // dirty hacks for RTL, because RTL is unable to create valid XML, d'oh!
            content = content.replaceAll("&(?!amp;)", "&amp;"); // replace & not followed by amp; with &amp;
            content = content.replaceAll("<[Aa][Ss][Xx](?:\\s+\\w+\\s*=\\s*\"\\d+\\.\\d+\")*\\s*>", "<asx>");
            content = content.replaceAll("</[Aa][Ss][Xx]>", "</asx>");
            
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setValidating(false);
            factory.setFeature("http://xml.org/sax/features/namespaces", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/validation/schema", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));
            
            // get root
            Element rootelement = doc.getDocumentElement();

            // iterate over children
            int count = 0;
            NodeList childs = rootelement.getChildNodes();
            for (int i = 0; i < childs.getLength(); i++) {
                Node child = (Node) childs.item(i);
                if("entry".equalsIgnoreCase(child.getNodeName())) {
                    if(count == n) {
                        NodeList entryChilds = child.getChildNodes();
                        for (int j = 0; j < entryChilds.getLength(); j++) {
                            Node prop = (Node) entryChilds.item(j);
                            if("ref".equalsIgnoreCase(prop.getNodeName())) {
                                for (int k = 0; k < prop.getAttributes().getLength(); k++) {
                                    Node attr = prop.getAttributes().item(k);
                                    if("href".equalsIgnoreCase(attr.getNodeName())) {
                                        return attr.getNodeValue();
                                    }
                                }
                            }
                        }
                    }
                    count++;
                }
            }
        } catch (FileNotFoundException fnfe) {
            logger.warn("ASX file ["+asxFile+"] does not exist on server", fnfe);
        } catch (Exception e) {
            logger.error("Unexpected exception while parsing the asx file ["+asxFile+"]", e);
        }
        return uri;
    }
    
    /**
     * Convenience method. See {@link #getUri(String, int)}
     * @param asxFile URI of the asx file to parse
     * @return URI of the n-th entry as String
     */
    public static String getUri(String asxFile) {
        return getUri(asxFile, 0);
    }
}
