package de.berlios.vch.http.handler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ChannelDAO;
import de.berlios.vch.model.Channel;
import de.berlios.vch.utils.ModelToRomeConverter;

public class RssFeedHandler extends AbstractHandler {

    private static transient Logger logger = LoggerFactory.getLogger(RssFeedHandler.class);
    
    @SuppressWarnings("unchecked")
    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();
            
            Map<String,Object> params = (Map<String,Object>) exchange.getAttribute("parameters");
            String link = (String) params.get("link");
            
            if(link != null) {
                Channel chan = (Channel) new ChannelDAO(conn).findByKey(link);
                if(chan == null) {
                    new NotFoundHandler().handle(exchange);
                    return;
                }

                logger.debug("Trying to stream feed ["+link+"]");
                SyndFeed feed = ModelToRomeConverter.convert(chan);
                
                // stream the feed to the client
                streamRssFeed(feed);
            } else {
                new NotFoundHandler().handle(exchange);
            }
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }
    
    @Override
    protected String getDescriptionKey() {
        return "handler_description";
    }
}
