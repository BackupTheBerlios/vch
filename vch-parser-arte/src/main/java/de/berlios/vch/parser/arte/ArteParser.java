package de.berlios.vch.parser.arte;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.berlios.vch.parser.AsxParser;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

public class ArteParser implements IWebParser, BundleActivator {
    public static final String CHARSET = "UTF-8";
    
    public static final String ID = ArteParser.class.getName();
    
    public static Map<String,String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.2) Gecko/20090821 Gentoo Firefox/3.5.2");
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }
    
    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        
        return page;
    }

    @Override
    public String getTitle() {
        return "Arte+7";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        
        if(page instanceof VideoPage) {
            VideoPage video = (VideoPage) page;
            String videoUri = video.getVideoUri().toString();
            if(videoUri.endsWith("asx")) {
                videoUri = AsxParser.getUri(videoUri);
                video.setVideoUri(new URI(videoUri));
            }
            return page;
        } else {
            return page;
        }
    }

    @Override
    public void start(BundleContext ctx) throws Exception {
        ctx.registerService(IWebParser.class.getName(), this, null);
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
    }

    @Override
    public String getId() {
        return ID;
    }
}