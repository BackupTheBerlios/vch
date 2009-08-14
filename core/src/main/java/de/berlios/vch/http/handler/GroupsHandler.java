package de.berlios.vch.http.handler;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.GroupDAO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.rome.videocast.Videocast;
import de.berlios.vch.rome.videocast.VideocastImpl;

public class GroupsHandler extends AbstractHandler  {
	
	private static transient Logger logger = LoggerFactory.getLogger(GroupsHandler.class);
	@Override
	void doHandle(HttpExchange exchange) throws Exception {
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();
            
            // get all channels
            long start = System.currentTimeMillis();
            List<Group> groups = new GroupDAO(conn).getAll(true);
            long stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop-start) + " ms");

            // order by title
            //Collections.sort(channels, new ChannelTitleComparator());
            
            // create a RSS feed
            SyndFeed feed = createRssFeed(groups);
            
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
    private SyndFeed createRssFeed(List<Group> groups) throws UnsupportedEncodingException {
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
        for (Iterator<Group> iterator = groups.iterator(); iterator.hasNext();) {
        	
        	Group group = iterator.next();
            SyndEntry entry = new SyndEntryImpl();
            entry.setTitle(group.getName());
            entry.setPublishedDate(new Date());
            SyndContent content = new SyndContentImpl();
            content.setType("text/plain");
            content.setValue(group.getDescription());
            entry.setDescription(content);
            String link = Config.getInstance().getBaseUrl()
            + "/group?group_name="
            + URLEncoder.encode(group.getName(), Config.getInstance().getProperty("default.encoding"));
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
