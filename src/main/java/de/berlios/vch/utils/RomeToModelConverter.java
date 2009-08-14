package de.berlios.vch.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.module.Module;
import com.sun.syndication.feed.module.mediarss.MediaEntryModule;
import com.sun.syndication.feed.module.mediarss.types.Metadata;
import com.sun.syndication.feed.module.mediarss.types.Thumbnail;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Enclosure;
import de.berlios.vch.model.Item;
import de.berlios.vch.rome.videocast.Videocast;

public class RomeToModelConverter {
    private static transient Logger logger = LoggerFactory.getLogger(RomeToModelConverter.class);
    
    @SuppressWarnings("unchecked")
    public static Channel convert(SyndFeed feed) {
        Channel channel = new Channel();
        channel.setTitle(feed.getTitle().trim());
        channel.setDescription(feed.getDescription());
        if(feed.getImage() != null) {
            channel.setThumbnail(feed.getImage().getUrl());
        }
        channel.setLink(feed.getLink());
        if (feed.getPublishedDate() == null) {
        	channel.setPubDate(new Date());
        } else {
            channel.setPubDate(feed.getPublishedDate());
        }
        channel.setCopyright(feed.getCopyright());
        channel.setLanguage(feed.getLanguage());

        List<Item> items = new ArrayList<Item>();
        for (Iterator<SyndEntry> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
            SyndEntry entry = iterator.next();
            Item item = convert(entry, channel);
            
            // add Item to the Item list
            items.add(item);
        }
        
        // add the items to the channel
        channel.setItems(items);
        return channel;
    }

    @SuppressWarnings("unchecked")
    public static Item convert(SyndEntry entry, Channel channel) {
        Item item = new Item();
        
        // set channel
        item.setChannel(channel);
        
        // set title
        item.setTitle(entry.getTitle());
        
        // set description
        if(entry.getDescription() != null) {
            item.setDescription(entry.getDescription().getValue());
        }
        
        // set image
        convertImage(item, entry);
        
        // set pubdate
        item.setPubDate(entry.getPublishedDate());
        
        // set link
        item.setLink(entry.getLink());
        
        // convert enclosures
        List<SyndEnclosure> enclosures = entry.getEnclosures();
        if(enclosures.size() > 0) {
            SyndEnclosure encl = enclosures.get(0);
            Enclosure enclosure = convert(encl);
            
            // look in foreign markup, if we have a duration
            String duration = getForeignMarkupValue("duration", entry);
            if(duration != null) {
                enclosure.setDuration(Long.parseLong(duration));
            } else {
                // maybe there is an itunes:duration
                duration = getForeignMarkupValue("itunes", "duration", entry);
                if(duration != null) {
                    Pattern p = Pattern.compile("(\\d+):(\\d+):(\\d+)");
                    Matcher m = p.matcher(duration);
                    if(m.matches()) {
                        int hours = Integer.parseInt(m.group(1));
                        int mins = Integer.parseInt(m.group(2));
                        int secs = Integer.parseInt(m.group(3));
                        enclosure.setDuration(hours * 3600 + mins * 60 + secs);
                    } else {
                        p = Pattern.compile("(\\d+):(\\d+)");
                        m = p.matcher(duration);
                        if(m.matches()) {
                            int mins = Integer.parseInt(m.group(1));
                            int secs = Integer.parseInt(m.group(2));
                            enclosure.setDuration(mins * 60 + secs);
                        }
                    }
                }
            }
            
            // look in foreign markup, if this enclosure has to be parsed ondemand
            String ondemand = getForeignMarkupValue("ondemand" ,entry);
            if(ondemand != null) {
                enclosure.setOndemand(true);
            }
            
            item.setEnclosure(enclosure);
        }
        
        // set default guid
        // create a guid from channel url + enclosure link (or pubDate, if enclosure is null)
        StringBuffer sb = new StringBuffer();
        if(channel.getLink() != null) {
            sb.append(channel.getLink());
        }
        if(item.getEnclosure() != null) {
            sb.append(item.getEnclosure().getLink());
        } else if(item.getPubDate() != null) {
            sb.append(item.getPubDate());
        }
        item.setGuid(sb.toString());
        
        // set the guid from foreignMarkup
        // some parsers abuse foreignMarkup to store the guid as <guid> element
        // e.g. YoutubeParser and ArteParser
        String guid = getForeignMarkupValue("guid", entry);
        if(guid != null) {
            item.setGuid(guid);
        }
        
        return item;
    }
    
    public static String getForeignMarkupValue(String key, SyndEntry entry) {
        return getForeignMarkupValue("", key, entry);
    }
    
    @SuppressWarnings("unchecked")
    public static String getForeignMarkupValue(String namespace, String key, SyndEntry entry) {
        if(entry.getForeignMarkup()!= null) {
            List elements = (List) entry.getForeignMarkup();
            for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
                Element elem = (Element) iterator.next();
                if(namespace.equals(elem.getNamespacePrefix()) && key.equals(elem.getName())) {
                    logger.debug("Found {} in foreign markup {}", key, elem.getText());
                    return elem.getText();
                }
            }
        }
        return null;
    }

    private static void convertImage(Item item, SyndEntry entry) {
        // try to find an image in the videocast namespace 
        Videocast vc = (Videocast) entry.getModule("http://vch.berlios.de/schema/videocast/1.0");
        if (vc != null ) {
            item.setThumbnail(vc.getImage());
            return;
        } 
        
        // try to find an image in the media namespace <media:thumbnail>
        Module module = entry.getModule(MediaEntryModule.URI);
        if(module != null) {
            MediaEntryModule media = (MediaEntryModule) module;

            if(media.getMediaGroups() != null && media.getMediaGroups().length > 0) {
                Metadata meta = media.getMediaGroups()[0].getMetadata();
                for (int i = 0; i < meta.getThumbnail().length; i++) {
                    Thumbnail thumb = meta.getThumbnail()[i];
                    logger.debug(thumb.getUrl().toString());
                    if(thumb.getUrl().toString().endsWith("0.jpg")) {
                        item.setThumbnail(thumb.getUrl().toString());
                    }
                }
            }
        }
    }

    public static Enclosure convert(SyndEnclosure encl) {
        Enclosure enclosure = new Enclosure();
        enclosure.setLink(encl.getUrl());
        enclosure.setType(encl.getType());
        enclosure.setLength(encl.getLength());
        return enclosure;
    }
}
