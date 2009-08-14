package de.berlios.vch.parser;

import java.util.List;

import com.sun.syndication.feed.synd.SyndFeed;

/**
 * An interface for the webpage parsers. To write a new
 * parser, extend {@link AbstractPageParser} (recommended) or implement 
 * this interface.
 * To activate the parser, add it to {@link RSSFeedCatcher}<code>.parserClasses</code>.
 * If the parser isn't explicitly disabled in the configuration, it will be started.
 * To disable a parser, add an entry to the configuration with the format
 * <nobr><code>&lt;Parser class name&gt;.enabled=false</code></nobr>
 * e.g.
 * <nobr><code>de.berlios.vch.parser.zdfmediathek.ZDFMediathekParser.enabled=false</code></nobr>
 *
 * @author <a href="mailto:hampelratte@users.berlios.de">hampelratte@users.berlios.de</a>
 */
public interface PageParser extends Runnable {
    /**
     * Stores a RSS feed in the database
     * @param feed
     */
    public void saveFeed2DB(SyndFeed feed);
    
    /**
     * Stores a RSS feed in the data directory
     * @param feed
     */
    public void saveFeed2File(SyndFeed feed);
    
    /**
     * Add a feed to the list of feeds, 
     * which are provided by this parser 
     * and will be printed to stdout
     * @param feed
     */
    public void addRssFeed(String feed);
    
    /**
     * Returns a list of feeds, which will be printed to stdout
     * @see #addRssFeed(String)
     */
    public List<String> getRssFeeds();
}