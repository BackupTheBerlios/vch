package de.berlios.vch.db.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
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
import de.berlios.vch.Constants;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Item;

public class ChannelDAO extends AbstractDAO {

    private static transient Logger logger = LoggerFactory.getLogger(ChannelDAO.class);

    private static String SQL_FIND_BY_KEY;
    
    private static String SQL_FIND_BY_NAME;
    
    private static String SQL_INSERT_CHANNEL;
    
    private static String SQL_UPDATE_CHANNEL;
    
    private static String SQL_DELETE_CHANNEL;
    
    private static String SQL_LIST_ALL;
    
    public ChannelDAO(Connection conn) {
        super(conn);
        if(SQL_FIND_BY_KEY == null) {
            String table_name = Config.getInstance().getProperty("db.table.channel");
            Properties sqls = loadSqls(table_name);
            SQL_FIND_BY_KEY = sqls.getProperty("SQL_FIND_BY_KEY");
            SQL_FIND_BY_NAME = sqls.getProperty("SQL_FIND_BY_NAME");
            SQL_INSERT_CHANNEL = sqls.getProperty("SQL_INSERT_CHANNEL");
            SQL_UPDATE_CHANNEL = sqls.getProperty("SQL_UPDATE_CHANNEL");
            SQL_DELETE_CHANNEL = sqls.getProperty("SQL_DELETE_CHANNEL");
            SQL_LIST_ALL = sqls.getProperty("SQL_LIST_ALL");
        }
    }

    @Override
    public void saveOrUpdate(Object o) throws SQLException {
        checkType(o);
        
        if(exists(o)) {
            update(o);
        } else {
            save(o);
        }
    }
    
    @Override
    public void save(Object o) throws SQLException {
        checkType(o);
        Channel chan = (Channel) o;
        logger.debug("Saving new channel ["+chan.getTitle()+"]");
        
        QueryRunner run = new QueryRunner();

        // save the channel
        Object[] params = {
            chan.getTitle(),
            chan.getLink(),
            chan.getDescription(),
            chan.getThumbnail(), 
            chan.getCopyright(),
            new SimpleDateFormat(Constants.SQL_DATE_FORMAT).format(chan.getPubDate()),
            chan.getLanguage()
        };
        run.update(conn, SQL_INSERT_CHANNEL, params);
        
        // save or update the items
        for (Iterator<Item> iterator = chan.getItems().iterator(); iterator.hasNext();) {
            Item item = iterator.next();
            item.setChannel(chan);
            new ItemDAO(conn).saveOrUpdate(item);
        }
    }

    @Override
    public void update(Object o) throws SQLException {
        checkType(o);
        Channel chan = (Channel) o;
        logger.debug("Updating channel ["+chan.getTitle()+"]");
        
        Object[] params = {
            chan.getTitle(),
            chan.getDescription(),
            chan.getThumbnail(), 
            chan.getCopyright(),
            new SimpleDateFormat(Constants.SQL_DATE_FORMAT).format(chan.getPubDate()),
            chan.getLanguage(),
            chan.getLink()
        };
        
        QueryRunner run = new QueryRunner();
        run.update(conn, SQL_UPDATE_CHANNEL, params);
        
        // save or update the items
        for (Iterator<Item> iterator = chan.getItems().iterator(); iterator.hasNext();) {
            Item item = iterator.next();
            item.setChannel(chan);
            new ItemDAO(conn).saveOrUpdate(item);
        }
    }

    @Override
    public boolean exists(Object o) throws SQLException {
        checkType(o);
        Channel chan = (Channel) o;
        
        Object bean = findByKey(chan.getLink());

        return bean != null;
    }
    
    
    private void checkType(Object o) throws IllegalArgumentException {
        if( !(o instanceof Channel) ) {
            throw new IllegalArgumentException("Object is not a Channel object");
        }
    }

    /**
     * Retrieves a channel object by the "link" property
     * 
     * @param o
     *      the link property of the desired channel
     */
    @Override
    public Object findByKey(Object o) throws SQLException {
        logger.debug("Looking for Channel with link [" + o + "]");
        
        QueryRunner run = new QueryRunner();
        
        // load the channel
        ResultSetHandler h = new BeanHandler(Channel.class);
        Channel chan = (Channel) run.query(conn, SQL_FIND_BY_KEY, o, h);
        
        // load the items
        if(chan != null) {
            List<Item> items = new ItemDAO(conn).findByChannel(chan);
            chan.setItems(items);
        }
        
        return chan;
    }

    @Override
    public void delete(Object o) throws SQLException {
        checkType(o);
        Channel chan = (Channel) o;

        // delete channel from groups
        new GroupMemberDAO(conn).deleteChannel(chan);
        
        // load items, if not done before
        if(chan.getItems() == null) {
            List<Item> items = new ItemDAO(conn).findByChannel(chan);
            chan.setItems(items);
        }
        
        // delete the items
        if(chan.getItems().size() > 0) {
            for (Iterator<Item> iterator = chan.getItems().iterator(); iterator.hasNext();) {
                Item item = iterator.next();
                new ItemDAO(conn).delete(item);
            }
        }
        
        // delete the channel
        QueryRunner run = new QueryRunner();
        run.update(conn, SQL_DELETE_CHANNEL, chan.getLink());
    }
    
    @SuppressWarnings("unchecked")
    public List<Channel> getAll(boolean cascade) throws SQLException {
        List<Channel> result = new ArrayList<Channel>();
        
        QueryRunner run = new QueryRunner();
        
        // load the channels
        ResultSetHandler h = new BeanListHandler(Channel.class);
        logger.debug("Executing SQL: " + SQL_LIST_ALL);
        result = (List<Channel>) run.query(conn, SQL_LIST_ALL, h);
        
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
    
    public Channel findByName(Object o) throws SQLException {
        logger.debug("Looking for Channel with name[" + o + "]");
        
        QueryRunner run = new QueryRunner();
        
        // load the channel
        ResultSetHandler h = new BeanHandler(Channel.class);
        Channel chan = (Channel) run.query(conn, SQL_FIND_BY_NAME, o, h);
                
        // load the items
        if(chan != null) {
            List<Item> items = new ItemDAO(conn).findByChannel(chan);
            chan.setItems(items);
        }
        
        return chan;
    }
    
    public List<Channel> getAll() throws SQLException {
        return getAll(false);
    }
}