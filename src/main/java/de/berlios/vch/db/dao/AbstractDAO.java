package de.berlios.vch.db.dao;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.utils.PropertiesLoader;

abstract public class AbstractDAO {
    
    private static transient Logger logger = LoggerFactory.getLogger(AbstractDAO.class);
    
    protected Connection conn;
    
    public AbstractDAO(Connection conn) {
        this.conn = conn;
    }
    
    /**
     * Saves an object, which doesn't exist in the database or updates
     * an object, which already exists
     * @param o
     *      the object to be persisted
     * @throws SQLException
     */
    abstract public void saveOrUpdate(Object o) throws SQLException;
    
    /**
     * Saves a given object to the database
     * 
     * @param o
     *      the object to be persisted
     * @throws SQLException
     * @see #saveOrUpdate(Object)
     */
    abstract public void save(Object o) throws SQLException;
    
    /**
     * Updates a given object 
     * 
     * @param o
     *      the changed object
     * @throws SQLException
     * @see #saveOrUpdate(Object)
     */
    abstract public void update(Object o) throws SQLException;
    
    /**
     * Checks, if an object exists in the database or not
     * 
     * @param o
     *      The object to be checked
     * @return
     *      true if the object exists in the database
     * @throws SQLException
     */
    abstract public boolean exists(Object o) throws SQLException;
    
    /**
     * Retrieves an object from the database by a given key
     * 
     * @param o
     *      the key of the desired Object
     * @throws
     *      SQLException
     * @return
     *      The desired object or <code>null</code> if the object couldn't be found 
     */
    abstract public Object findByKey(Object o) throws SQLException;
    
    /**
     * Deletes an object from the database
     * @param o
     *      the object that shall be deleted
     * @throws SQLException
     */
    abstract public void delete(Object o) throws SQLException;

    /**
     * Loads the sql statements for the DAO
     * @param table_name The name of the table in the db
     */
    @SuppressWarnings("unchecked")
    protected Properties loadSqls(String table_name) {
        String driver = Config.getInstance().getProperty("db.connection.driver");
        String name = this.getClass().getSimpleName();
        String sqlPropsFile = "/sql/" + driver + "/" + name + ".sql.props";
        logger.debug("Trying to load sql statements from " + sqlPropsFile);
        Properties sqls = null;
        try {
            sqls = PropertiesLoader.loadFromJar(sqlPropsFile);
        } catch (IOException e) {
            logger.error("Couldn't load sql statements from " + sqlPropsFile, e);
            System.exit(1);
        }
        for (Iterator iterator = sqls.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            String value = sqls.getProperty(key);
            // replace the table placeholders
            String CHANNEL_TABLE = Config.getInstance().getProperty("db.table.channel");
            String ITEM_TABLE = Config.getInstance().getProperty("db.table.item");
            String ENCLOSURE_TABLE = Config.getInstance().getProperty("db.table.enclosure");
            String GROUPS_TABLE = Config.getInstance().getProperty("db.table.groups");
            String GROUPS_INTER_CHANNEL_TABLE = Config.getInstance().getProperty("db.table.groups_inter_channel");
            String USER_FEED_TABLE = Config.getInstance().getProperty("db.table.userfeed");
            value = value.replaceAll("\\{CHANNEL_TABLE\\}", CHANNEL_TABLE);
            value = value.replaceAll("\\{GROUPS_TABLE\\}", GROUPS_TABLE);
            value = value.replaceAll("\\{ITEM_TABLE\\}", ITEM_TABLE);
            value = value.replaceAll("\\{ENCLOSURE_TABLE\\}", ENCLOSURE_TABLE);
            value = value.replaceAll("\\{GROUPS_INTER_CHANNEL_TABLE\\}", GROUPS_INTER_CHANNEL_TABLE);
            value = value.replaceAll("\\{USER_FEED_TABLE\\}", USER_FEED_TABLE);
            value = value.replaceAll("\\{TABLE_NAME\\}", table_name);
            sqls.setProperty(key, value);
        }
        return sqls;
    }
}