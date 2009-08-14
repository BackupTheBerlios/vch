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

import de.berlios.vch.Config;
import de.berlios.vch.model.Group;

public class GroupDAO extends AbstractDAO {
	
    private static transient Logger logger = LoggerFactory.getLogger(GroupDAO.class);

    private static String SQL_FIND_BY_KEY;
    
    private static String SQL_INSERT_GROUPS;
    
    private static String SQL_UPDATE_GROUPS;
    
    private static String SQL_DELETE_GROUPS;
    
    private static String SQL_LIST_ALL;
    
    public GroupDAO(Connection conn) {
        super(conn);
        if(SQL_FIND_BY_KEY == null) {
            String table_name = Config.getInstance().getProperty("db.table.groups");
            Properties sqls = loadSqls(table_name);
            SQL_FIND_BY_KEY = sqls.getProperty("SQL_FIND_BY_KEY");
            SQL_INSERT_GROUPS = sqls.getProperty("SQL_INSERT_GROUPS");
            SQL_UPDATE_GROUPS = sqls.getProperty("SQL_UPDATE_GROUPS");
            SQL_DELETE_GROUPS = sqls.getProperty("SQL_DELETE_GROUPS");
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
    	Group group = (Group) o;
        logger.debug("Saving new group [" + group.getName() + "] with Desc [" + group.getDescription() + "]");
        
        QueryRunner run = new QueryRunner();

        // save the group
        Object[] params = {
        		group.getName(),
        		group.getDescription()
        };
        logger.debug(SQL_INSERT_GROUPS);
        
        run.update(conn, SQL_INSERT_GROUPS, params);

    }
    
    @Override
    public void update(Object o) throws SQLException {
    	
        checkType(o);
        Group group = (Group) o;
        logger.debug("Updating channel ["+group.getName() +"]");
        
        Object[] params = {
            group.getName(),
            group.getDescription(),
            group.getName()
        };
        
        QueryRunner run = new QueryRunner();
        run.update(conn, SQL_UPDATE_GROUPS, params);
    	
    }
    
    @Override
    public boolean exists(Object o) throws SQLException {
        checkType(o);
        Group group = (Group) o;
        
        Object bean = findByKey(group.getName());

        return bean != null;
    }
    
    private void checkType(Object o) throws IllegalArgumentException {
    	
        if( !(o instanceof Group) ) {
            throw new IllegalArgumentException("Object is not an Groups object");
        }
    	
    }
    
    @Override
    public Object findByKey(Object o) throws SQLException {
    	
        logger.debug("Looking for Group with name [" + o + "]");
        
        QueryRunner run = new QueryRunner();
        
        // load the channel
        ResultSetHandler h = new BeanHandler(Group.class);
        
        logger.debug(SQL_FIND_BY_KEY);
        
        Group group = (Group) run.query(conn, SQL_FIND_BY_KEY, o, h);
               
        return group;
    	
    }
    
    @Override
    public void delete(Object o) throws SQLException {
    	
        checkType(o);
        Group group = (Group) o;
        
        QueryRunner run = new QueryRunner();
        
        run.update(conn, SQL_DELETE_GROUPS, group.getName());
    	
    }
    
    @SuppressWarnings("unchecked")
    public List<Group> getAll(boolean cascade) throws SQLException {
    	
        List<Group> result = new ArrayList<Group>();
        
        QueryRunner run = new QueryRunner();
        
        // load the channels
        ResultSetHandler h = new BeanListHandler(Group.class);
        logger.debug("Executing SQL: " + SQL_LIST_ALL);
        result = (List<Group>) run.query(conn, SQL_LIST_ALL, h);
        
        return result;
    	
    }
    
    public List<Group> getAll() throws SQLException {
        return getAll(false);
    }

}
