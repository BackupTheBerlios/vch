package de.berlios.vch.rome.videocast;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jdom.Element;
import org.jdom.Namespace;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.io.ModuleGenerator;

public class VideocastGenerator implements ModuleGenerator {

    private static final Namespace VIDEOCAST_NS  = Namespace.getNamespace("videocast", Videocast.URI);

    @Override
    public String getNamespaceUri() {
        return Videocast.URI;
    }

    private static final Set<Namespace> NAMESPACES;

    static {
        Set<Namespace> nss = new HashSet<Namespace>();
        nss.add(VIDEOCAST_NS);
        NAMESPACES = Collections.unmodifiableSet(nss);
    }

    @Override
    public Set<Namespace> getNamespaces() {
        return NAMESPACES;
    }
    
    @Override
    public void generate(Module module, Element element) {
        Videocast vc = (Videocast) module;
        
        // image tag
        if(vc.getImage() != null) {
            Element e = new Element("image", VIDEOCAST_NS);
            e.setAttribute("url", vc.getImage());
            element.addContent(e);
        }
        
        // streaminfo
        if(vc.getStreaminfo() != null) {
            Element e = new Element("streaminfo", VIDEOCAST_NS);
            if(vc.getStreaminfo().getLength() != null) {
                e.setAttribute("length", vc.getStreaminfo().getLength());
            }
            if(vc.getStreaminfo().getSubtitle() != null) {
                e.setAttribute("subtitle", vc.getStreaminfo().getSubtitle());
            }
            element.addContent(e);
        }
        
        // subfeed
        if(vc.getSubfeed() != null) {
            Element e = new Element("subfeed", VIDEOCAST_NS);
            e.setAttribute("url", vc.getSubfeed());
            element.addContent(e);
        }
        
        // streams
        if(vc.getStreams() != null && vc.getStreams().size() > 0) {
            for (Iterator<Stream> iterator = vc.getStreams().iterator(); iterator.hasNext();) {
                Stream stream = iterator.next();
                Element e = new Element("stream", VIDEOCAST_NS);
                if(stream.getUrl() != null) {
                    e.setAttribute("url", stream.getUrl());
                }
                if(stream.getQuality() != null) {
                    e.setAttribute("quality", stream.getQuality());
                }
                element.addContent(e);
            }
        }
    }
}
