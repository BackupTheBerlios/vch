package de.berlios.vch.rome.videocast;

import java.util.List;

import com.sun.syndication.feed.module.Module;

public interface Videocast extends Module {
    public static final String URI = "http://vch.berlios.de/schema/videocast/1.0";
    
    public String getSubfeed();
    public void setSubfeed(String subfeed);
    
    public String getImage();
    public void setImage(String image);
    
    public List<Stream> getStreams();
    public void setStreams(List<Stream> streams);
    
    public Streaminfo getStreaminfo();
    public void setStreaminfo(Streaminfo info);
    
    
}
