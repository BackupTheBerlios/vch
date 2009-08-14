package de.berlios.vch.rome.videocast;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.module.ModuleImpl;

public class VideocastImpl extends ModuleImpl implements Videocast {

    private static transient Logger logger = LoggerFactory.getLogger(VideocastImpl.class);
    
    private String image;
    private Streaminfo info;
    private List<Stream> streams;
    private String subfeed;
    
    public VideocastImpl() {
        super(Videocast.class,Videocast.URI);
    }
    
    @Override
    public String getImage() {
        return image;
    }

    @Override
    public Streaminfo getStreaminfo() {
        return info;
    }

    @Override
    public List<Stream> getStreams() {
        return streams;
    }

    @Override
    public String getSubfeed() {
        return subfeed;
    }

    @Override
    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public void setStreaminfo(Streaminfo info) {
        this.info = info;
    }

    @Override
    public void setStreams(List<Stream> streams) {
        this.streams = streams;
    }

    @Override
    public void setSubfeed(String subfeed) {
        this.subfeed = subfeed;
    }

    @Override
    public void copyFrom(Object obj) {
        try {
            Videocast vc = (Videocast) obj;
            setImage(vc.getImage());
            setSubfeed(vc.getSubfeed());
            setStreaminfo((Streaminfo) vc.getStreaminfo().clone());
            
            List<Stream> clonedStreams = new ArrayList<Stream>();
            for (Iterator<Stream> iterator = vc.getStreams().iterator(); iterator.hasNext();) {
                Stream stream = iterator.next();
                Stream clone = (Stream) stream.clone();
                clonedStreams.add(clone);
            }
            setStreams(clonedStreams);
        } catch (CloneNotSupportedException e) {
            logger.error("Couldn't clone Videocast object", e);
        }
    }

    @Override
    public Class<Videocast> getInterface() {
        return Videocast.class;
    }

}
