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
import java.util.Map;

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
import de.berlios.vch.db.dao.GroupDAO;
import de.berlios.vch.db.dao.GroupMemberDAO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Group;
import de.berlios.vch.rome.videocast.Videocast;
import de.berlios.vch.rome.videocast.VideocastImpl;
import de.berlios.vch.utils.comparator.ChannelTitleComparator;

public class GroupMemberHandler extends AbstractHandler {

    private static transient Logger logger = LoggerFactory.getLogger(RssIndexHandler.class);
    
    @Override
    public void doHandle(HttpExchange exchange) throws Exception {
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();
            
            @SuppressWarnings("unchecked")
            Map<String,Object> params = (Map<String,Object>) exchange.getAttribute("parameters");
            String group = (String) params.get("group_name");
            
            // get all channels
            long start = System.currentTimeMillis();
            List<Channel> channels = new GroupMemberDAO(conn).findByKey(group);
            long stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop-start) + " ms");

            // order by title
            Collections.sort(channels, new ChannelTitleComparator());
            
            // get the group
            start = System.currentTimeMillis();
            Group grp = (Group) new GroupDAO(conn).findByKey(group);
            
            // group not found
            if(grp == null) {
                throw new RuntimeException(Messages.translate(Messages.class, "entity_not_found", new Object[] {"GroupMember", group}));
            }
            
            stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop-start) + " ms");
            
            
            // create a RSS feed
            SyndFeed feed = createRssFeed(channels, grp);
            
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
    private SyndFeed createRssFeed(List<Channel> channels, Group grp) throws UnsupportedEncodingException {
        SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("rss_2.0");
        List<String> pathes  = Config.getInstance().getHandlerMapping().getPathes(getClass());
        feed.setLink(pathes.get(0));
        feed.setEncoding(Config.getInstance().getProperty("default.encoding"));
        feed.setTitle(grp.getName());
        feed.setDescription(grp.getDescription() != null ? grp.getDescription() : Messages.translate(getClass(), "description"));
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
