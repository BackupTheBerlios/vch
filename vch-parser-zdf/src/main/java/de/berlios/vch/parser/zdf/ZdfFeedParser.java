package de.berlios.vch.parser.zdf;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.AsxParser;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.rss.RssParser;

public class ZdfFeedParser {
    private static transient Logger logger = LoggerFactory.getLogger(ZdfFeedParser.class);

    private Comparator<SyndEnclosure> comparator = new SyndEnclosureComparator();

    @SuppressWarnings("unchecked")
    public SyndFeed parse(IWebPage page) throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
        String feedUri = page.getUri().toString();

        logger.info("Parsing rss feed {}", feedUri);
        String rss = HttpUtils.get(feedUri, null, "UTF-8");
        SyndFeed feed = RssParser.parse(rss);
        feed.setLink(feedUri);
        feed.setTitle(page.getTitle());

        for (Iterator iterator = feed.getEntries().iterator(); iterator.hasNext();) {
            SyndEntry entry = (SyndEntry) iterator.next();
            // sort enclosures, so that the best quality is enclosure[0],
            Collections.sort(entry.getEnclosures(), comparator);
            Collections.reverse(entry.getEnclosures());

            // if the first enclosure is an asx file, parse the asx file
            if(entry.getEnclosures().size() > 0) {
                SyndEnclosure first = (SyndEnclosure) entry.getEnclosures().get(0);
                if ("video/x-ms-asf".equals(first.getType())) {
                    String uri = AsxParser.getUri(first.getUrl());
                    first.setUrl(uri);
                    first.setType("video/wmv");
                }
            } else {
                iterator.remove();
            }
        }
        
        return feed;
    }
}
