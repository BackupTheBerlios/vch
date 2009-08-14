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

import de.berlios.vch.model.Config;

public class ConfigDAO extends AbstractDAO {

    private static transient Logger logger = LoggerFactory.getLogger(ConfigDAO.class);
    
    private static String SQL_FIND_BY_KEY;

    private static String SQL_INSERT_CONFIG;

    private static String SQL_UPDATE_CONFIG;
    
    private static String SQL_DELETE_CONFIG;
    
    private static String SQL_LIST_ALL;
    
    public ConfigDAO(Connection conn) {
        super(conn);
        if(SQL_FIND_BY_KEY == null) {
            String table_name = de.berlios.vch.Config.getInstance().getProperty("db.table.config");
            Properties sqls = loadSqls(table_name);
            SQL_FIND_BY_KEY = sqls.getProperty("SQL_FIND_BY_KEY");
            SQL_INSERT_CONFIG = sqls.getProperty("SQL_INSERT_CONFIG");
            SQL_UPDATE_CONFIG = sqls.getProperty("SQL_UPDATE_CONFIG");
            SQL_DELETE_CONFIG = sqls.getProperty("SQL_DELETE_CONFIG");
            SQL_LIST_ALL = sqls.getProperty("SQL_LIST_ALL");
        }
    }
    
    @Override
    public boolean exists(Object o) throws SQLException {
        checkType(o);
        Config config = (Config) o;

        Object bean = findByKey(config.getParameterKey());

        return bean != null;
    }

    private void checkType(Object o) throws IllegalArgumentException {
        if( !(o instanceof Config) ) {
            throw new IllegalArgumentException("Object is not an Config object");
        }
    }

    @Override
    public Object findByKey(Object o) throws SQLException {
        QueryRunner run = new QueryRunner();
        
        ResultSetHandler h = new BeanHandler(Config.class);

        return run.query(conn, SQL_FIND_BY_KEY, o, h);
    }

    @Override
    public void save(Object o) throws SQLException {
        checkType(o);
        Config config = (Config) o;
        logger.debug("Saving new config parameter ["+config.getParameterKey()+"]");
        
        QueryRunner run = new QueryRunner();

        // save the enclosure
        Object[] params = {
            config.getParameterKey(),
            config.getParameterValue()
        };
        run.update(conn, SQL_INSERT_CONFIG, params);
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
        Config config = (Config) o;
        logger.debug("Updating config parameter ["+config.getParameterKey()+"] value ["+config.getParameterValue()+"]");
        
        QueryRunner run = new QueryRunner();

        // save the enclosure
        Object[] params = {
            config.getParameterValue(),
            config.getParameterKey()
        };
        run.update(conn, SQL_UPDATE_CONFIG, params);
    }

    @Override
    public void delete(Object o) throws SQLException {
        checkType(o);
        Config config = (Config) o;
        
        QueryRunner run = new QueryRunner();
        
        run.update(conn, SQL_DELETE_CONFIG, config.getParameterKey());
    }
    
    @SuppressWarnings("unchecked")
    public List<Config> getAll() throws SQLException {
        List<Config> result = new ArrayList<Config>();
        
        QueryRunner run = new QueryRunner();
        
        // load the channels
        ResultSetHandler h = new BeanListHandler(Config.class);
        logger.debug("Executing SQL: " + SQL_LIST_ALL);
        result = (List<Config>) run.query(conn, SQL_LIST_ALL, h);
        
        return result;
    }
}
