package de.berlios.vch.rss;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import com.sun.syndication.feed.module.mediarss.MediaEntryModule;
import com.sun.syndication.feed.module.mediarss.types.MediaContent;
import com.sun.syndication.feed.module.mediarss.types.MediaGroup;
import com.sun.syndication.feed.module.mediarss.types.Thumbnail;
import com.sun.syndication.feed.module.mediarss.types.UrlReference;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

public class RssParser {
    private static transient Logger logger = LoggerFactory.getLogger(RssParser.class);
    
    public static SyndFeed parseUri(String uri) throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL(uri)));
        convertYahooMediaToEnclosure(feed);
        removeNonVideoItems(feed);
        if(feed.getLink() == null) {
            feed.setLink(uri);
        }
        return feed;
    }
    
    public static SyndFeed parse(String rss) throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new InputSource(new StringReader(rss)));
        convertYahooMediaToEnclosure(feed);
        removeNonVideoItems(feed);
        return feed;
    }
    
    
    @SuppressWarnings("unchecked")
    private static void convertYahooMediaToEnclosure(SyndFeed feed) {
        List<SyndEntry> entries = feed.getEntries();
        for (SyndEntry entry : entries) {
            MediaEntryModule module = (MediaEntryModule) entry.getModule( MediaEntryModule.URI );
            if(module == null) {
                continue;
            }
            
            MediaContent[] contents = module.getMediaGroups()[0].getContents();
            if(contents.length > 0) {
                for (int i = 0; i < contents.length; i++) {
                    MediaContent content = contents[i];
                    if(!(content.getReference() instanceof UrlReference)) {
                        continue;
                    }
                        
                    SyndEnclosure enc = new SyndEnclosureImpl();
                    enc.setUrl(content.getReference().toString());
                    if(content.getType() != null) {
                        enc.setType(content.getType());
                    }
                    
                    if(content.getDuration() != null) {
                    	// set duration in foreign markup
                        Element elem = new Element("duration");
                        elem.setText(content.getDuration().toString());
                        ((List<Element>)entry.getForeignMarkup()).add(elem);
                    }
                    
                    if(content.getFileSize() != null) {
                        enc.setLength(content.getFileSize());
                    }
                    
                    if(enc.getUrl().length() > 0) {
                        entry.getEnclosures().add(enc);
                    }
                }
            }
            
            // convert media:thumbnail
            MediaGroup group = module.getMediaGroups()[0];
            if(group.getMetadata() != null && group.getMetadata().getThumbnail() != null) {
                Thumbnail[] thumbs = group.getMetadata().getThumbnail();
                if(thumbs.length > 0) {
                    // TODO add thumbnail
                }
            }
            
        }
    }

    /**
     * Removes all items which don't have an video enclosure
     * @param feed
     */
    @SuppressWarnings("unchecked")
    private static void removeNonVideoItems(SyndFeed feed) {
        for (Iterator iterator = feed.getEntries().iterator(); iterator.hasNext();) {
            SyndEntry entry = (SyndEntry) iterator.next();
            boolean hasVideo = true;
            for (Iterator encIter = entry.getEnclosures().iterator(); encIter.hasNext();) {
                SyndEnclosure enclosure = (SyndEnclosure) encIter.next();
                if(enclosure.getType() != null && !enclosure.getType().startsWith("video")) {
                    hasVideo = false;
                    break;
                }
            }
            if(!hasVideo) {
                logger.debug("Removing item {} from feed {}, because it has no video enclosure", entry.getTitle(), feed.getTitle());
                iterator.remove();
            }
        }
    }
}
