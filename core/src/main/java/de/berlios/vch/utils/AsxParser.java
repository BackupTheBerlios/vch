package de.berlios.vch.utils;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Iterator;

import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
            
            SAXBuilder builder = new SAXBuilder();
            builder.setValidation(false);
            Document doc = builder.build(new StringReader(content));
            
            // get root
            Element rootelement = doc.getRootElement();

            // iterate over children
            int count = 0;
            for (Iterator<?> iterator = rootelement.getChildren().iterator(); iterator.hasNext();) {
                Element child = (Element) iterator.next();
                if("entry".equalsIgnoreCase(child.getName())) {
                    if(count == n) {
                        for (Iterator<?> iter = child.getChildren().iterator(); iter.hasNext();) {
                            Element prop = (Element) iter.next();
                            if("ref".equalsIgnoreCase(prop.getName())) {
                                for (Iterator<?> propIter = prop.getAttributes().iterator(); propIter.hasNext();) {
                                    Attribute attr = (Attribute) propIter.next();
                                    if("href".equalsIgnoreCase(attr.getName())) {
                                        return attr.getValue();
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
