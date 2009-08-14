package de.berlios.vch.db.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.model.Download;
import de.berlios.vch.model.Item;

public class DownloadDAO extends AbstractDAO {

    private static transient Logger logger = LoggerFactory.getLogger(DownloadDAO.class);
    
    private static String SQL_FIND_BY_KEY;

    private static String SQL_INSERT_DOWNLOAD;

    private static String SQL_UPDATE_DOWNLOAD;
    
    private static String SQL_DELETE_DOWNLOAD;
    
    private static String SQL_LIST_ALL;
    
    public DownloadDAO(Connection conn) {
        super(conn);
        if(SQL_FIND_BY_KEY == null) {
            String table_name = de.berlios.vch.Config.getInstance().getProperty("db.table.downloads");
            Properties sqls = loadSqls(table_name);
            SQL_FIND_BY_KEY = sqls.getProperty("SQL_FIND_BY_KEY");
            SQL_INSERT_DOWNLOAD = sqls.getProperty("SQL_INSERT_DOWNLOAD");
            SQL_UPDATE_DOWNLOAD = sqls.getProperty("SQL_UPDATE_DOWNLOAD");
            SQL_DELETE_DOWNLOAD = sqls.getProperty("SQL_DELETE_DOWNLOAD");
            SQL_LIST_ALL = sqls.getProperty("SQL_LIST_ALL");
        }
    }

    private void checkType(Object o) throws IllegalArgumentException {
        if( !(o instanceof Download) ) {
            throw new IllegalArgumentException("Object is not an Download object");
        }
    }
    
    @Override
    public void delete(Object o) throws SQLException {
        checkType(o);
        Download download = (Download) o;
        
        QueryRunner run = new QueryRunner();
        
        run.update(conn, SQL_DELETE_DOWNLOAD, download.getItemKey());
    }

    @Override
    public boolean exists(Object o) throws SQLException {
        checkType(o);
        Download download = (Download) o;

        Object bean = findByKey(download.getItemKey());

        return bean != null;
    }

    @Override
    public Object findByKey(Object o) throws SQLException {
        QueryRunner run = new QueryRunner();
        
        ResultSetHandler h = new BeanHandler(Download.class);

        return run.query(conn, SQL_FIND_BY_KEY, o, h);
    }

    @Override
    public void save(Object o) throws SQLException {
        checkType(o);
        Download download = (Download) o;
        logger.debug("Saving new download [{}, {}]", download.getItemKey(), download.getLocalFile());
        
        QueryRunner run = new QueryRunner();

        Object[] params = {
            download.getItemKey(),
            download.getLocalFile()
        };
        run.update(conn, SQL_INSERT_DOWNLOAD, params);
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
    public void update(Object o) throws SQLException {
        checkType(o);
        Download download = (Download) o;
        logger.debug("Updating download [{}, {}]", download.getItemKey(), download.getLocalFile());
        
        QueryRunner run = new QueryRunner();

        Object[] params = {
            download.getItemKey(),
            download.getLocalFile()
        };
        run.update(conn, SQL_UPDATE_DOWNLOAD, params);
    }
    
    /**
     * 
     * @param cascade if set to true, all the items will be loaded, too
     * @return
     * @throws SQLException
     */
    @SuppressWarnings("unchecked")
    public List<Download> getAll(boolean cascade) throws SQLException {
        List<Download> result = new ArrayList<Download>();
        
        QueryRunner run = new QueryRunner();
        
        // load the downloads
        ResultSetHandler h = new BeanListHandler(Download.class);
        logger.debug("Executing SQL: " + SQL_LIST_ALL);
        result = (List<Download>) run.query(conn, SQL_LIST_ALL, h);
        
        // load the items
        if(cascade) {
            for (Download download : result) {
                Item item = (Item) new ItemDAO(conn).findByKey(download.getItemKey());
                download.setItem(item);
            }
        }
        
        return result;
    }
}
