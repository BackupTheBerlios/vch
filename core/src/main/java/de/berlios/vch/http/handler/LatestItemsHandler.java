package de.berlios.vch.http.handler;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ChannelDAO;
import de.berlios.vch.db.dao.ItemDAO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Item;
import de.berlios.vch.utils.ModelToRomeConverter;


public class LatestItemsHandler extends AbstractHandler {

    private static transient Logger logger = LoggerFactory.getLogger(LatestItemsHandler.class);
    
    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();
            
            // get the newest items
            int count = Config.getInstance().getIntValue("latest.items.count");
            List<Item> items = new ItemDAO(conn).listLatest(count);

            // create a RSS feed
            SyndFeed feed = createRssFeed(items);
            
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

    
    private SyndFeed createRssFeed(List<Item> items) throws UnsupportedEncodingException {
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
        for (Item item : items) {
            SyndEntry entry = ModelToRomeConverter.convert(item);
            
            String channelName = "";
            // add the feed name to the title
            Connection con = null;
            try {
                con = ConnectionManager.getInstance().getConnection();
                Channel chan = (Channel) new ChannelDAO(con).findByKey(item.getChannelKey());
                channelName = chan.getTitle() + " - ";
            } catch (SQLException e) {
                logger.warn("Couldn't load channel name", e);
            } finally {
                try {
                    DbUtils.close(con);
                } catch (SQLException e) {
                    logger.error("Couldn't close DB connection", e);
                }
            }
            entry.setTitle(channelName + entry.getTitle());
            
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
