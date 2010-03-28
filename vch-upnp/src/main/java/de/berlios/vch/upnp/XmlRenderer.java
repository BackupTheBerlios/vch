package de.berlios.vch.upnp;

import java.io.StringWriter;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;

public class XmlRenderer {

    public static String renderOverview(IOverviewPage page, String vchPath) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        doc.setXmlVersion("1.0");
        
        Element root = doc.createElement("DIDL-Lite");
        root.setAttribute("xmlns", "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/");
        root.setAttribute("xmlns:dc", "http://purl.org/dc/elements/1.1/");
        root.setAttribute("xmlns:upnp", "urn:schemas-upnp-org:metadata-1-0/upnp/");
        
        for (IWebPage subpage : page.getPages()) {
            Element container = doc.createElement("container");
            if("vchpage".equalsIgnoreCase(subpage.getUri().getScheme())) {
                container.setAttribute("id", subpage.getUri().toString());
            } else {
                container.setAttribute("id", vchPath + "/" + md5(subpage.getUri().toString()));
            }
            container.setAttribute("parentID", vchPath);
            container.setAttribute("restricted", "1");
            root.appendChild(container);
            
            Element title = doc.createElement("dc:title");
            title.setTextContent(subpage.getTitle());
            container.appendChild(title);
            
            if(subpage instanceof IVideoPage) {
                IVideoPage video = (IVideoPage) subpage;
                if(video.getPublishDate() != null) {
                    Element date = doc.createElement("dc:date");
                    date.setTextContent(new SimpleDateFormat("yyyy-MM-dd").format(video.getPublishDate().getTime()));
                    container.appendChild(date);
                }
            }
            
            Element upnpClass = doc.createElement("upnp:class");
            upnpClass.setTextContent("object.container");
            container.appendChild(upnpClass);
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(root), new StreamResult(sw));

        return sw.toString();
    }

    public static String renderVideo(IVideoPage page, URI videoUri, String vchPath) throws TransformerFactoryConfigurationError, DOMException, NoSuchAlgorithmException, ParserConfigurationException, TransformerException {
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        doc.setXmlVersion("1.0");
        
        Element root = doc.createElement("DIDL-Lite");
        root.setAttribute("xmlns", "urn:schemas-upnp-org:metadata-1-0/DIDL-Lite/");
        root.setAttribute("xmlns:dc", "http://purl.org/dc/elements/1.1/");
        root.setAttribute("xmlns:upnp", "urn:schemas-upnp-org:metadata-1-0/upnp/");

        Element item = doc.createElement("item");
        if("vchpage".equalsIgnoreCase(page.getUri().getScheme())) {
            item.setAttribute("id", page.getUri().toString());
        } else {
            item.setAttribute("id", vchPath + "/" + md5(page.getUri().toString()));
        }
        item.setAttribute("parentID", vchPath);
        item.setAttribute("restricted", "1");
        root.appendChild(item);
        
        Element title = doc.createElement("dc:title");
        title.setTextContent(page.getTitle());
        item.appendChild(title);
        
        if(page.getDescription() != null && page.getDescription().length() > 0) {
            Element desc = doc.createElement("dc:description");
            desc.setTextContent(page.getDescription());
            item.appendChild(desc);
        }
        
        Element upnpClass = doc.createElement("upnp:class");
        upnpClass.setTextContent("object.item.videoItem");
        item.appendChild(upnpClass);
        
        Element res = doc.createElement("res");
        
        res.setAttribute("protocolInfo", getProtocol(videoUri) + ":*:" + getMimeTipe(videoUri) + ":*");
        res.setAttribute("resolution", "0x0");
        
        if(page.getDuration() > 0) {
            long hours = page.getDuration() / 3600;
            long minutes = page.getDuration() % 3600 / 60;
            long seconds = page.getDuration() % 60;
            NumberFormat nf = new DecimalFormat("00");
            res.setAttribute("duration", hours + ":" + nf.format(minutes) + ":" + nf.format(seconds));
        }
        
        res.setTextContent(videoUri.toString());
        item.appendChild(res);

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        StringWriter sw = new StringWriter();
        transformer.transform(new DOMSource(root), new StreamResult(sw));

        return sw.toString();
    }
    
    private static String getProtocol(URI uri) {
        if(uri.getScheme().equals("http")) {
           return "http-get"; 
        } else {
            return uri.getScheme();
        }
    }

    // TODO create method renderError

    private static String getMimeTipe(URI uri) {
        if(uri.toString().endsWith(".flv")) {
            return "video/x-flv";
        } else if(uri.toString().endsWith(".mp4")) {
            return "video/mp4";
        } else if(uri.toString().endsWith(".wmv") || uri.getScheme().equals("mms") || uri.getScheme().equals("mmst")) {
            return "video/x-ms-wmv";
        } else if(uri.toString().endsWith(".avi")) {
            return "video/x-msvideo";
        } else {
            return "video";
        }
    }
    
    public static String md5(String s) throws NoSuchAlgorithmException {
        String digest = "";

        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] b = s.getBytes();
        md.update(b, 0, b.length);
        byte[] md5Bytes = md.digest();

        StringBuffer hexValue = new StringBuffer();
        for (int i = 0; i < md5Bytes.length; i++) {
            int val = ((int) md5Bytes[i]) & 0xff;
            if (val < 16)
                hexValue.append("0");
            hexValue.append(Integer.toHexString(val));
        }
        digest = hexValue.toString();

        return digest;
    }
}
