package de.berlios.vch.db.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.model.Enclosure;

public class EnclosureDAO extends AbstractDAO {

    private static transient Logger logger = LoggerFactory.getLogger(EnclosureDAO.class);
    
    private static String SQL_FIND_BY_KEY;

    private static String SQL_INSERT_ENCLOSURE;

    private static String SQL_UPDATE_ENCLOSURE;
    
    private static String SQL_DELETE_ENCLOSURE;
    
    private static String SQL_DELETE_ORPHAN_ENCLOSURE;
    
    public EnclosureDAO(Connection conn) {
        super(conn);
        if(SQL_FIND_BY_KEY == null) {
            String table_name = Config.getInstance().getProperty("db.table.enclosure");
            Properties sqls = loadSqls(table_name);
            SQL_FIND_BY_KEY = sqls.getProperty("SQL_FIND_BY_KEY");
            SQL_INSERT_ENCLOSURE = sqls.getProperty("SQL_INSERT_ENCLOSURE");
            SQL_UPDATE_ENCLOSURE = sqls.getProperty("SQL_UPDATE_ENCLOSURE");
            SQL_DELETE_ENCLOSURE = sqls.getProperty("SQL_DELETE_ENCLOSURE");
            SQL_DELETE_ORPHAN_ENCLOSURE = sqls.getProperty("SQL_DELETE_ORPHAN_ENCLOSURE");
        }
    }
    
    @Override
    public boolean exists(Object o) throws SQLException {
        checkType(o);
        Enclosure enclosure = (Enclosure) o;

        Object bean = findByKey(enclosure.getLink());

        return bean != null;
    }

    private void checkType(Object o) throws IllegalArgumentException {
        if( !(o instanceof Enclosure) ) {
            throw new IllegalArgumentException("Object is not an Enclosure object");
        }
    }

    @Override
    public Object findByKey(Object o) throws SQLException {
        QueryRunner run = new QueryRunner();
        
        ResultSetHandler h = new BeanHandler(Enclosure.class);

        return run.query(conn, SQL_FIND_BY_KEY, o, h);
    }

    @Override
    public void save(Object o) throws SQLException {
        checkType(o);
        Enclosure enclosure = (Enclosure) o;
        logger.debug("Saving new enclosure ["+enclosure.getLink()+"]");
        
        QueryRunner run = new QueryRunner();

        // save the enclosure
        Object[] params = {
            enclosure.getLink(),
            enclosure.getType(),
            enclosure.getLength(),
            enclosure.getDuration(),
            enclosure.isOndemand()
        };
        run.update(conn, SQL_INSERT_ENCLOSURE, params);
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
        Enclosure enclosure = (Enclosure) o;
        logger.debug("Updating enclosure ["+enclosure.getLink()+"]");
        
        QueryRunner run = new QueryRunner();

        // save the enclosure
        Object[] params = {
            enclosure.getType(),
            enclosure.getLength(),
            enclosure.getDuration(),
            enclosure.isOndemand(),
            enclosure.getLink()
        };
        run.update(conn, SQL_UPDATE_ENCLOSURE, params);
    }

    @Override
    public void delete(Object o) throws SQLException {
        checkType(o);
        Enclosure enclosure = (Enclosure) o;
        
        QueryRunner run = new QueryRunner();
        
        run.update(conn, SQL_DELETE_ENCLOSURE, enclosure.getLink());
    }
    
    /**
     * Deletes all enclosures, which are not assigned to an item.
     * 
     * @throws SQLException
     */
    public void deleteOrphan() throws SQLException {
        QueryRunner run = new QueryRunner();
        int deleted = run.update(conn, SQL_DELETE_ORPHAN_ENCLOSURE);
        logger.debug("Deleted " + deleted + " orphan enclosures");
    }
}
