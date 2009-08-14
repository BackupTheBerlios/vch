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
import de.berlios.vch.model.Download;
import de.berlios.vch.model.Enclosure;
import de.berlios.vch.model.Item;

public class ItemDAO extends AbstractDAO {
    
    private static transient Logger logger = LoggerFactory.getLogger(ItemDAO.class);
    
    private static String SQL_FIND_BY_KEY;

    private static String SQL_INSERT_ITEM;
    
    private static String SQL_UPDATE_ITEM;
    
    private static String SQL_DELETE_ITEM;
    
    private static String SQL_FIND_BY_CHANNEL;
    
    private static String SQL_FIND_BY_FIELD;
    
    private static String SQL_LIST_LATEST;
    
    private static String SQL_GET_ALL;
        
    private static String SQL_SEARCH_ITEM;
    
    public ItemDAO(Connection conn) {
        super(conn);
        if(SQL_FIND_BY_KEY == null) {
            String table_name = Config.getInstance().getProperty("db.table.item");
            Properties sqls = loadSqls(table_name);
            SQL_FIND_BY_KEY = sqls.getProperty("SQL_FIND_BY_KEY");
            SQL_INSERT_ITEM = sqls.getProperty("SQL_INSERT_ITEM");
            SQL_UPDATE_ITEM = sqls.getProperty("SQL_UPDATE_ITEM");
            SQL_DELETE_ITEM = sqls.getProperty("SQL_DELETE_ITEM");
            SQL_FIND_BY_CHANNEL = sqls.getProperty("SQL_FIND_BY_CHANNEL");
            SQL_FIND_BY_FIELD = sqls.getProperty("SQL_FIND_BY_PROPERTY");
            SQL_LIST_LATEST = sqls.getProperty("SQL_LIST_LATEST");
            SQL_GET_ALL = sqls.getProperty("SQL_GET_ALL");
            SQL_SEARCH_ITEM = sqls.getProperty("SQL_SEARCH_ITEM");
        }
    }

    public void saveOrUpdate(Object o) throws SQLException {
        checkType(o);
        
        if(!exists(o)) {
            save(o);
        } else {
            update(o);
        }
    }

    public boolean exists(Object o) throws SQLException {
        checkType(o);
        Item item = (Item) o;
        
        Object bean = findByKey(item.getGuid());
        
        return bean != null;
    }
    
    private void checkType(Object o) throws IllegalArgumentException {
        if( !(o instanceof Item) ) {
            throw new IllegalArgumentException("Object is not an Item object");
        }
    }

    @Override
    public Object findByKey(Object o) throws SQLException {
        QueryRunner run = new QueryRunner();
        
        // load the item
        ResultSetHandler h = new BeanHandler(Item.class);
        Item item = (Item) run.query(conn, SQL_FIND_BY_KEY, o, h);
        
        // load the enclosure
        if(item != null) {
            loadDependecies(item);
        }
        
        return item;
    }

    @SuppressWarnings("unchecked")
    public List<Item> findBySearch(String query) throws SQLException {
        List<Item> result = new ArrayList<Item>();
        
        QueryRunner run = new QueryRunner();
        
        // load the items
        ResultSetHandler h = new BeanListHandler(Item.class);
        logger.debug(SQL_SEARCH_ITEM);
        query = "%" + query.toLowerCase() + "%";
        Object[] params = new Object[] {query, query};
        result = (List<Item>) run.query(conn, SQL_SEARCH_ITEM, params, h);
        
        // load the enclosures
        for (Iterator<Item> iterator = result.iterator(); iterator.hasNext();) {
            Item item = iterator.next();
            loadDependecies(item);
        }
        
        return result;
    }
    
    private void loadDependecies(Item item) throws SQLException {
        if(item.getEnclosureKey() != null) {
            Enclosure enclosure = (Enclosure) new EnclosureDAO(conn).findByKey(item.getEnclosureKey());
            item.setEnclosure(enclosure);
        }
    }
    
    /**
     * Return all Items of a given channel
     * @param chan
     * @return
     *      a {@link java.util.List} of {@link Item}
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public List<Item> findByChannel(Channel chan) throws SQLException {
        List<Item> result = new ArrayList<Item>();
        
        QueryRunner run = new QueryRunner();
        
        // load the items
        ResultSetHandler h = new BeanListHandler(Item.class);
        result = (List<Item>) run.query(conn, SQL_FIND_BY_CHANNEL, chan.getLink(), h);
        
        // load the enclosures
        for (Iterator<Item> iterator = result.iterator(); iterator.hasNext();) {
            Item item = iterator.next();
            loadDependecies(item);
        }
        
        return result;
    }

    @Override
    public void save(Object o) throws SQLException {
        checkType(o);
        Item item = (Item) o;
        logger.debug("Saving new item ["+item.getTitle()+"]");
        
        QueryRunner run = new QueryRunner();

        // save the enclosure
        if(item.getEnclosure() != null) {
            new EnclosureDAO(conn).saveOrUpdate(item.getEnclosure());
            
            // save the item
            Object[] params = {
                item.getTitle(),
                item.getLink(),
                item.getDescription(),
                new SimpleDateFormat(Constants.SQL_DATE_FORMAT).format(item.getPubDate()),
                item.getThumbnail(),
                item.getGuid(),
                item.getEnclosure() != null ? item.getEnclosure().getLink() : "",
                item.getChannel().getLink()
            };
            run.update(conn, SQL_INSERT_ITEM, params);
        }
    }

    @Override
    public void update(Object o) throws SQLException {
        checkType(o);
        Item item = (Item) o;
        logger.debug("Updating item ["+item.getTitle()+"]");
        
        
        QueryRunner run = new QueryRunner();

        // save the enclosure
        if(item.getEnclosure() != null) {
            new EnclosureDAO(conn).saveOrUpdate(item.getEnclosure());
        }
        
        // save the item
        Object[] params = {
            item.getTitle(),
            item.getLink(),
            item.getDescription(),
            item.getThumbnail(),
            new SimpleDateFormat(Constants.SQL_DATE_FORMAT).format(item.getPubDate()),
            item.getEnclosure() != null ? item.getEnclosure().getLink() : "",
            item.getChannel().getLink(),
            item.getGuid()
        };
        run.update(conn, SQL_UPDATE_ITEM, params);
    }

    @Override
    public void delete(Object o) throws SQLException {
        checkType(o);
        Item item = (Item) o;

        // delete associated download (files will stay on disk)
        DownloadDAO ddao = new DownloadDAO(conn);
        Download download = (Download) ddao.findByKey(item.getGuid());
        if(download != null) {
            ddao.delete(download);
        }
        
        // delete the item
        QueryRunner run = new QueryRunner();
        run.update(conn, SQL_DELETE_ITEM, item.getGuid());
        
        // delete the enclosure
        if(item.getEnclosure() != null) {
            Item remainingItem = findByProperty("ENCLOSUREKEY", item.getEnclosureKey());
            if(remainingItem == null) {
                new EnclosureDAO(conn).delete(item.getEnclosure());
            }
        }
    }

    @SuppressWarnings("unchecked")
    public List<Item> listLatest(int count) throws SQLException {
        List<Item> result = new ArrayList<Item>();
        
        QueryRunner run = new QueryRunner();
        
        // load the items
        ResultSetHandler h = new BeanListHandler(Item.class);
        result = (List<Item>) run.query(conn, SQL_LIST_LATEST, count, h);
        
        // load the enclosures
        for (Iterator<Item> iterator = result.iterator(); iterator.hasNext();) {
            Item item = iterator.next();
            loadDependecies(item);
        }
        
        return result;
    }
    
    @SuppressWarnings("unchecked")
    public List<Item> getAll() throws SQLException {
        QueryRunner run = new QueryRunner();
        
        //List<Item> items = new ArrayList<Item>();
        
        // load the items
        ResultSetHandler h = new BeanListHandler(Item.class);
        logger.debug("Getting all items");
        List<Item> items = (List<Item>) run.query(conn, SQL_GET_ALL, h);

        // load the enclosure
        for (Item item : items) {
            loadDependecies(item);
        }
        
        return items;
    }

    public Item findByProperty(String prop, String value) throws SQLException {
        QueryRunner run = new QueryRunner();
        
        // load the item
        ResultSetHandler h = new BeanHandler(Item.class);
        Item item = (Item) run.query(conn, SQL_FIND_BY_FIELD.replaceAll("%prop%", prop), value, h);
        
        // load the enclosure
        if(item != null) {
            loadDependecies(item);
        }
        
        return item;
    }
}