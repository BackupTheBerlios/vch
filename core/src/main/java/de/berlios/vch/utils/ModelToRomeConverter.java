package de.berlios.vch.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Element;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndImageImpl;

import de.berlios.vch.Config;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Enclosure;
import de.berlios.vch.model.Item;
import de.berlios.vch.rome.videocast.VideocastImpl;

public class ModelToRomeConverter {
    public static SyndFeed convert(Channel chan) {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        feed.setEncoding(Config.getInstance().getProperty("default.encoding"));
        feed.setTitle(chan.getTitle());
        feed.setDescription(chan.getDescription());
        if(chan.getThumbnail() != null && chan.getThumbnail().length() > 0) {
            SyndImageImpl image = new SyndImageImpl();
            image.setTitle(chan.getThumbnail());
            image.setUrl(chan.getThumbnail());
            feed.setImage(image);
        }
        feed.setLink(chan.getLink());
        feed.setPublishedDate(chan.getPubDate());
        feed.setLanguage(chan.getLanguage());
        feed.setCopyright(chan.getCopyright());
        
        if(chan.getItems().size() > 0) {
            List<SyndEntry> entries = new ArrayList<SyndEntry>();
            for (Iterator<Item> iterator = chan.getItems().iterator(); iterator.hasNext();) {
                Item item = iterator.next();
                SyndEntry entry = convert(item);
                entries.add(entry);
			}
            feed.setEntries(entries);
        }
        
        return feed;
    }
    
    @SuppressWarnings("unchecked")
    public static SyndEntry convert(Item item) {
        SyndEntry entry = new SyndEntryImpl();
        entry.setTitle(item.getTitle());
        entry.setLink(item.getLink());
        SyndContent content = new SyndContentImpl();
        content.setType("text/plain");
        content.setValue(item.getDescription());
    	VideocastImpl myvidcast = new VideocastImpl();
        myvidcast.setImage(item.getThumbnail());
        entry.getModules().add(myvidcast);
        entry.setDescription(content);
        entry.setPublishedDate(item.getPubDate());
        
        // add guid to foreign markup
        Element elem = new Element("guid");
        elem.setText(item.getGuid());
        ((List<Element>)entry.getForeignMarkup()).add(elem);
        
        if(item.getEnclosure() != null) {
            List<SyndEnclosure> enclosures = new ArrayList<SyndEnclosure>();
            enclosures.add(convert(item.getEnclosure()));
            entry.setEnclosures(enclosures);
        }
        
        return entry;
    }

    public static SyndEnclosure convert(Enclosure enclosure) {
        SyndEnclosure encl = new SyndEnclosureImpl();
        encl.setLength(enclosure.getLength());
        encl.setType(enclosure.getType());
        encl.setUrl(enclosure.getLink());
        return encl;
    }
}
