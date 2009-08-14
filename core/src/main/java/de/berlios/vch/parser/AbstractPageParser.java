package de.berlios.vch.parser;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedOutput;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ChannelDAO;
import de.berlios.vch.db.dao.GroupDAO;
import de.berlios.vch.db.dao.GroupMemberDAO;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Group;
import de.berlios.vch.utils.RomeToModelConverter;

public abstract class AbstractPageParser implements PageParser {
    
    private static transient Logger logger = LoggerFactory.getLogger(AbstractPageParser.class);
    
    @Override
    public synchronized void saveFeed2DB(SyndFeed feed) {
        
        // don't save empty feeds
        if(feed.getEntries() == null || feed.getEntries().size() <= 0) {
            logger.warn("Saving feed {} aborted, because it has no entries", feed.getTitle());
            return;
        }
        
        // convert the SyndFeed to Channel
        Channel channel = RomeToModelConverter.convert(feed);
        
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            
            // save the channel
            new ChannelDAO(conn).saveOrUpdate(channel);
            
            // commit all changes
            conn.commit();
            conn.setAutoCommit(true);
        } catch (SQLException e) {
            logger.error("Couldn't save channel", e);
            if(conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException e1) {
                    logger.error("Couldn't rollback transaction",e);
                }
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
    public synchronized void saveFeed2File(SyndFeed feed) {
        Writer writer = null;
        try {
            String directory = Config.getInstance().getProperty("data.dir");
            File file = new File(directory + File.separator + feed.getTitle().replace(" ", "_") + ".xml");
            FileOutputStream fout = new FileOutputStream(file);
            String encoding = feed.getEncoding() != null ? feed.getEncoding() : Config.getInstance().getProperty("default.encoding");
            writer = new OutputStreamWriter(fout, encoding);
            SyndFeedOutput output = new SyndFeedOutput();
            output.output(feed, writer);
        } catch (Exception e) {
            logger.error("Couldn't store RSS feed", e);
        } finally {
            if(writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.error("Couldn't close RSS feed file", e);
                }
            }
        }
    }
    
    private List<String> rssFeedUrls = new ArrayList<String>();

    @Override
    public synchronized void addRssFeed(String feed) {
        rssFeedUrls.add(feed);
    }
    
    @Override
    public List<String> getRssFeeds() {
        return rssFeedUrls;
    }
    
    protected synchronized void addFeedsToGroup(Group group) {
        logger.info("Adding {} parsed feeds to group {}", getRssFeeds().size(), group.getName());
        Connection con = null;

        // save the group
        try {
            con = ConnectionManager.getInstance().getConnection();
            new GroupDAO(con).saveOrUpdate(group);
        } catch (SQLException e) {
            logger.error("Couldn' save Group " + group.getName());
        } finally {
            try {
                DbUtils.close(con);
            } catch (SQLException e) {
                logger.error("Couldn't close DB connection", e);
            }
        }
        
        // save the group content
        try {
            con = ConnectionManager.getInstance().getConnection();
            GroupMemberDAO gcd = new GroupMemberDAO(con);
            for (Iterator<String> iterator = getRssFeeds().iterator(); iterator.hasNext();) {
                String feedUrl = iterator.next();
                
                // TODO the delete is to avoid duplicate entries
                // this should be checked in the dao with a method like saveOrUpdate
                
                // delete old entries
                gcd.deleteByUserKey(group.getName(), feedUrl);
                // add new entries
                gcd.addChannel(feedUrl, group.getName());
            }
        } catch (SQLException e) {
            logger.error("Couldn' add channel to group", e);
        } finally {
            try {
                DbUtils.close(con);
            } catch (SQLException e) {
                logger.error("Couldn't close DB connection", e);
            }
        }
        
    }
}
