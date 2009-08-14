package de.berlios.vch.download;

import java.net.URI;

public class DownloadFactory {
    public static AbstractDownload createDownload(URI uri) {
        String scheme = uri.getScheme();
        AbstractDownload d = null;
        
        if("mms".equals(scheme)) {
            d = new MMSDownload(uri);
        } else if("http".equals(scheme)) {
            d = new HttpDownload(uri);
        } else if("rtmp".equals(scheme)) {
            d = new RtmpDownload(uri);
        }
        
        if(d != null) {
            d.setId(uri.toString());
        }
        
        return d;
    }
}
