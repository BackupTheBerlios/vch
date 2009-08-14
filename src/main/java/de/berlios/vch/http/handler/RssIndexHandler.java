package de.berlios.vch.http.handler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ChannelDAO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Channel;
import de.berlios.vch.rome.videocast.Videocast;
import de.berlios.vch.rome.videocast.VideocastImpl;
import de.berlios.vch.utils.comparator.ChannelTitleComparator;

public class RssIndexHandler extends AbstractHandler {

    private static transient Logger logger = LoggerFactory.getLogger(RssIndexHandler.class);
    
    @Override
    public void doHandle(HttpExchange exchange) throws Exception {
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();
            
            // get all channels
            long start = System.currentTimeMillis();
            List<Channel> channels = new ChannelDAO(conn).getAll(false);
            long stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop-start) + " ms");

            // order by title
            Collections.sort(channels, new ChannelTitleComparator());
            
            // create a RSS feed
            SyndFeed feed = createRssFeed(channels);
            
            // stream the feed to the client
            streamRssFeed(feed);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private SyndFeed createRssFeed(List<Channel> channels) throws UnsupportedEncodingException {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        List<String> pathes  = Config.getInstance().getHandlerMapping().getPathes(getClass());
        feed.setLink(pathes.get(0));
        feed.setEncoding(Config.getInstance().getProperty("default.encoding"));
        feed.setTitle(Messages.translate(getClass(), "title"));
        feed.setDescription(Messages.translate(getClass(), "description"));
        feed.setAuthor("Vodcatcher Helper");
        feed.setPublishedDate(new Date());
        
        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        for (Iterator<Channel> iterator = channels.iterator(); iterator.hasNext();) {
            Channel chan = iterator.next();
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(chan.getTitle());
            entry.setPublishedDate(chan.getPubDate());
            SyndContent content = new SyndContentImpl();
            content.setType("text/plain");
            content.setValue(chan.getDescription());
            entry.setDescription(content);
            List<SyndCategory> categories = new ArrayList<SyndCategory>();
            SyndCategory cat = new SyndCategoryImpl();
            cat.setName(chan.getTitle());
            entry.setCategories(categories);
            String link = Config.getInstance().getBaseUrl()
                + Config.getInstance().getHandlerMapping().getFeedPath()
                + "?link="
                + URLEncoder.encode(chan.getLink(), Config.getInstance().getProperty("default.encoding"));
            Videocast vc = new VideocastImpl();
            vc.setSubfeed(link);
            entry.getModules().add(vc);
            entry.setLink(link);
            entries.add(entry);
        }
        feed.setEntries(entries);
        
        return feed;
    }
    
    @Override
    protected String getDescriptionKey() {
        return "handler_description";
    }
}
