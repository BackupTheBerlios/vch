package de.berlios.vch.parser.zdfmediathek;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RSSLinkGetter {

    private static transient Logger logger = LoggerFactory.getLogger(RSSLinkGetter.class);
    
    private String link = "http://www.zdf.de/ZDFmediathek/inhalt";

    private Set<String> links = new HashSet<String>();

    public Set<String> getLinks() {
        return links;
    }

    public RSSLinkGetter() {

        try {

            // Öffne link
            URL url = new URL(link);
            URLConnection con = url.openConnection();
            con.addRequestProperty("Accept-Encoding", "gzip");

            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            if("gzip".equalsIgnoreCase(encoding)) {
                in = new GZIPInputStream(in);
            }

            // Parse HTML
            SAXBuilder builder = new SAXBuilder(false);
            builder.setFeature("http://xml.org/sax/features/validation", false);
            builder.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            Document doc = builder.build(in);

            // Get Root
            Element rootelement = doc.getRootElement();

            // Get Body
            Element body = (Element) rootelement.getContent(3);

            // Descent the xml tree
            Element div = (Element) body.getContent(3);
            div = (Element) div.getContent(8);
            div = (Element) div.getContent(3);
            div = (Element) div.getContent(1);
            div = (Element) div.getContent(1);

            // dei linke Spalte mit Sendungen ereicht
            Element ul = (Element) div.getContent(3);
            // dei mittlere Spalte mit Sendungen ereicht
            // Element ul2 = (Element) div.getContent(5);
            // dei rechte Spalte mit Sendungen ereicht
            // Element ul3 = (Element) div.getContent(7);

            // durch die linke tablle loopen
            @SuppressWarnings("unchecked")
            List<Element> sendungsliste = ul.getChildren();
            Iterator<Element> it = sendungsliste.iterator();

            while (it.hasNext()) {
                Element current_channel = (Element) it.next();
                // Warum es hier zwei tifen gibt weiß ich nicht
                if (current_channel.getContentSize() == 9) {
                    Element tmp = (Element) current_channel.getContent(7);
                    tmp = (Element) tmp.getContent(1);
                    links.add(tmp.getAttributeValue("href"));
                } else {
                    Element tmp = (Element) current_channel.getContent(5);
                    tmp = (Element) tmp.getContent(1);
                    links.add(tmp.getAttributeValue("href"));
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }

    }

}
