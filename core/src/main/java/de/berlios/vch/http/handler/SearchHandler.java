package de.berlios.vch.http.handler;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ItemDAO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Item;
import de.berlios.vch.utils.ModelToRomeConverter;


public class SearchHandler extends AbstractHandler {

    private static transient Logger logger = LoggerFactory.getLogger(SearchHandler.class);
    
    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // get the search query
            @SuppressWarnings("unchecked")
            Map<String,Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
            String q = (String)params.get("q");
            
            // validate search parameters
            if(q == null) {
                new BadRequestHandler(Messages.translate(getClass(), "error.q_missing")).handle(exchange);
                return;
            } else if(q.length() < 3) {
                new BadRequestHandler(Messages.translate(getClass(), "error.q_too_short")).handle(exchange);
                return;
            }
            
            // create db connection
            conn = ds.getConnection();
            
            // search for items
            List<Item> items = new ItemDAO(conn).findBySearch(q);

            // create a RSS feed
            SyndFeed feed = createRssFeed(items, q);
            
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

    
    private SyndFeed createRssFeed(List<Item> items, String query) throws UnsupportedEncodingException {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        List<String> pathes  = Config.getInstance().getHandlerMapping().getPathes(getClass());
        feed.setLink(pathes.get(0));
        feed.setEncoding(Config.getInstance().getProperty("default.encoding"));
        feed.setTitle(Messages.translate(getClass(), "title") + " - " + query);
        feed.setDescription(Messages.translate(getClass(), "description", new Object[] {items.size()}));
        feed.setAuthor("Vodcatcher Helper");
        feed.setPublishedDate(new Date());
        
        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        if(entries != null) {
            for (Item item : items) {
                SyndEntry entry = ModelToRomeConverter.convert(item);
                entries.add(entry);
            }
            feed.setEntries(entries);
        }
        
        return feed;
    }
    
    @Override
    protected String getDescriptionKey() {
        return "handler_description";
    }
}
