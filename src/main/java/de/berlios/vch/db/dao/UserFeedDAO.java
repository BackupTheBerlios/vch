package de.berlios.vch.db.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Item;
import de.berlios.vch.model.UserFeed;

public class UserFeedDAO extends AbstractDAO {
	
    private static transient Logger logger = LoggerFactory.getLogger(UserFeedDAO.class);

    private static String SQL_FIND_BY_KEY;
    
    private static String SQL_FIND_BY_CHANNEL;
    
    private static String SQL_INSERT;
    
    private static String SQL_DELETE;
    
    private static String SQL_LIST_ALL;
    
    public UserFeedDAO(Connection conn) {
        super(conn);
        if(SQL_FIND_BY_KEY == null) {
            String table_name = Config.getInstance().getProperty("db.table.userfeed");
            Properties sqls = loadSqls(table_name);
            SQL_FIND_BY_KEY = sqls.getProperty("SQL_FIND_BY_KEY");
            SQL_FIND_BY_CHANNEL = sqls.getProperty("SQL_FIND_BY_CHANNEL");
            SQL_INSERT = sqls.getProperty("SQL_INSERT");
            SQL_DELETE = sqls.getProperty("SQL_DELETE");
            SQL_LIST_ALL = sqls.getProperty("SQL_LIST_ALL");
        }
    }
    
    @Override
    public void saveOrUpdate(Object o) throws SQLException {
        checkType(o);
        
        if(!exists(o)) {
            save(o);
        } else {
            update(o);
        }
    }
    
    @Override
    public void save(Object o) throws SQLException {
    	
    	checkType(o);
    	UserFeed feed = (UserFeed) o;
        logger.debug("Saving new user feed [" + feed.getTitle() + "]");

        new ChannelDAO(conn).saveOrUpdate(feed);
        
        QueryRunner run = new QueryRunner();

        // save the group
        Object[] params = {
        		feed.getFeedUri(),
        		feed.getLink()
        };
        logger.debug(SQL_INSERT);
        
        run.update(conn, SQL_INSERT, params);
    }
    
    @Override
    public void update(Object o) throws SQLException {
        checkType(o);
        UserFeed feed = (UserFeed) o;
        logger.debug("Updating user feed ["+feed.getTitle() +"]");
        
        new ChannelDAO(conn).update(feed);
    }
    
    @Override
    public boolean exists(Object o) throws SQLException {
        checkType(o);
        UserFeed feed = (UserFeed) o;
        
        Object bean = findByKey(feed.getFeedUri());

        return bean != null;
    }
    
    private void checkType(Object o) throws IllegalArgumentException {
    	
        if( !(o instanceof UserFeed) ) {
            throw new IllegalArgumentException("Object is not an UserFeed object");
        }
    	
    }
    
    @Override
    public Object findByKey(Object uri) throws SQLException {
        logger.debug("Looking for UserFeed with URI [" + uri + "]");
        
        QueryRunner run = new QueryRunner();
        
        // load the channel
        ResultSetHandler h = new BeanHandler(UserFeed.class);
        
        logger.debug(SQL_FIND_BY_KEY);
    
        UserFeed feed = (UserFeed) run.query(conn, SQL_FIND_BY_KEY, uri, h);
        
        return feed;
    }
    
    public Object findByChannel(Channel chan) throws SQLException {
        logger.debug("Looking for UserFeed by channel [" + chan + "]");
        
        QueryRunner run = new QueryRunner();
        
        // load the channel
        ResultSetHandler h = new BeanHandler(UserFeed.class);
        
        logger.debug(SQL_FIND_BY_CHANNEL);
    
        UserFeed feed = (UserFeed) run.query(conn, SQL_FIND_BY_CHANNEL, chan.getLink(), h);
        
        return feed;
    }
    
    @SuppressWarnings("unchecked")
    public List<UserFeed> getAll(boolean cascade) throws SQLException {
        List<UserFeed> result = new ArrayList<UserFeed>();
        
        QueryRunner run = new QueryRunner();
        
        // load the channels
        ResultSetHandler h = new BeanListHandler(UserFeed.class);
        logger.debug("Executing SQL: " + SQL_LIST_ALL);
        result = (List<UserFeed>) run.query(conn, SQL_LIST_ALL, h);
        
        // load the items
        if(cascade) {
            for (Iterator iterator = result.iterator(); iterator.hasNext();) {
                Channel channel = (Channel) iterator.next();
                List<Item> items = new ItemDAO(conn).findByChannel(channel);
                channel.setItems(items);
            }
        }
                
        return result;
    }
    
    public List<UserFeed> getAll() throws SQLException {
        return getAll(false);
    }
    
    @Override
    public void delete(Object o) throws SQLException {
        checkType(o);
        UserFeed feed = (UserFeed) o;
        
        QueryRunner run = new QueryRunner();
        logger.debug("Executing SQL: {} with parameter [{}]", SQL_DELETE, feed.getFeedUri());
        run.update(conn, SQL_DELETE, feed.getFeedUri());
        
        new ChannelDAO(conn).delete(feed);
    }
    
    public List<String> getFeedUris() throws SQLException {
        List<UserFeed> feeds = getAll();
        List<String> uris = new ArrayList<String>();
        for (UserFeed feed : feeds) {
            uris.add(feed.getFeedUri());
        }
        return uris;
    }
}