package de.berlios.vch.rome.videocast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.jdom.Namespace;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleParser;

public class VideocastParser implements ModuleParser {

    private static final Namespace VIDECAST_NS  = Namespace.getNamespace("sample", Videocast.URI);
    
    @Override
    public String getNamespaceUri() {
        return Videocast.URI;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Module parse(Element element) {
        boolean foundSomething = false;
        Videocast fm = new VideocastImpl();

        Element e = element.getChild("image", VIDECAST_NS);
        if (e != null) {
            foundSomething = true;
            fm.setImage(e.getAttributeValue("url"));
        }
        List<Element> eList = element.getChildren("stream", VIDECAST_NS);
        if (eList.size() > 0) {
            foundSomething = true;
            fm.setStreams(parseStreams(eList));
        }
        e = element.getChild("subfeed", VIDECAST_NS);
        if (e != null) {
            foundSomething = true;
            fm.setSubfeed(e.getAttributeValue("url"));
        }
        e = element.getChild("streaminfo", VIDECAST_NS);
        if (e != null) {
            foundSomething = true;
            Streaminfo info = new Streaminfo();
            info.setLength(e.getAttributeValue("length"));
            info.setSubtitle(e.getAttributeValue("subtitle"));
            fm.setStreaminfo(info);
        }
        return (foundSomething) ? fm : null;
    }

    private List<Stream> parseStreams(List<Element> list) {
        List<Stream> streams = new ArrayList<Stream>();
        for (Iterator<Element> iterator = list.iterator(); iterator.hasNext();) {
            Element elem = (Element) iterator.next();
            Stream stream = new Stream();
            stream.setQuality(elem.getAttributeValue("quality"));
            stream.setUrl(elem.getAttributeValue("url"));
            streams.add(stream);
        }
        return streams;
    }
}
