package de.berlios.vch.db.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.commons.dbutils.handlers.BeanListHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Group;

public class GroupMemberDAO extends AbstractDAO {
	
    private static transient Logger logger = LoggerFactory.getLogger(GroupMemberDAO.class);

    private static String SQL_FIND_MEMBER_BY_KEY;
    
//    private static String SQL_FIND_MEMBER_BY_USERKEY;

//    private static String SQL_INSERT_GROUP_MEMBER;

//    private static String SQL_UPDATE_GROUP_MEMBER;
    
    private static String SQL_DELETE_GROUP_MEMBER;
    
    private static String SQL_DELETE_GROUP_MEMBER_BY_USER_KEY;
    
    private static String SQL_GROUP_MEMBER_ADD;
    
    private static String SQL_DELETE_CHANNEL;
    
    private static String SQL_DELETE_GROUP;
    
//    private static String SQL_LIST_ALL;
    
    public GroupMemberDAO(Connection conn) {
        super(conn);
        if(SQL_FIND_MEMBER_BY_KEY == null) {
            String table_name = Config.getInstance().getProperty("db.table.groups");
            Properties sqls = loadSqls(table_name);
            SQL_FIND_MEMBER_BY_KEY = sqls.getProperty("SQL_FIND_MEMBER_BY_KEY");
//            SQL_FIND_MEMBER_BY_USERKEY = sqls.getProperty("SQL_FIND_MEMBER_BY_USERKEY");
//            SQL_INSERT_GROUP_MEMBER = sqls.getProperty("SQL_INSERT_GROUP_MEMBER");
//            SQL_UPDATE_GROUP_MEMBER = sqls.getProperty("SQL_UPDATE_GROUP_MEMBER");
            SQL_DELETE_GROUP_MEMBER = sqls.getProperty("SQL_DELETE_GROUP_MEMBER");
            SQL_DELETE_GROUP_MEMBER_BY_USER_KEY = sqls.getProperty("SQL_DELETE_GROUP_MEMBER_BY_USER_KEY");
            SQL_GROUP_MEMBER_ADD = sqls.getProperty("SQL_GROUP_MEMBER_ADD");
//            SQL_LIST_ALL = sqls.getProperty("SQL_LIST_ALL");
            SQL_DELETE_CHANNEL = sqls.getProperty("SQL_DELETE_CHANNEL");
            SQL_DELETE_GROUP = sqls.getProperty("SQL_DELETE_GROUP");
        }
    }

	@Override
	public void delete(Object o) throws SQLException {
		logger.debug("Deleting Group entry from Group");
        QueryRunner run = new QueryRunner();
        String id = (String) o;
        run.update(conn, SQL_DELETE_GROUP_MEMBER, id);
	}

	@Override
	public boolean exists(Object o) throws SQLException {
		// TODO Auto-generated method stub
		return false;
	}

	@SuppressWarnings("unchecked")
    @Override
	public List<Channel> findByKey(Object o) throws SQLException {
		
        logger.debug("Looking Channels in Group with name [" + o + "]");
        
        List<Channel> result = new ArrayList<Channel>();
        
        QueryRunner run = new QueryRunner();
        
        // load the channels
        ResultSetHandler h = new BeanListHandler(Channel.class);
        
        Object[] params = {
        		o
            };
        
        // load the channels
        logger.debug("Executing SQL: " + SQL_FIND_MEMBER_BY_KEY);
        result = (List<Channel>) run.query(conn, SQL_FIND_MEMBER_BY_KEY, params, h);;
                
        return result;
        
	}

	@Override
	public void save(Object o) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void saveOrUpdate(Object o) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void update(Object o) throws SQLException {
		// TODO Auto-generated method stub
		
	}

	public void addChannel (String new_channel, String group_name) throws SQLException {

        logger.debug("Adding new Channel[" + new_channel + "] to Group [" + group_name + "]");
        
        QueryRunner run = new QueryRunner();

        // save the group
        Object[] params = {
        		group_name,
        		new_channel
        };
        logger.debug(SQL_GROUP_MEMBER_ADD);
        
        run.update(conn, SQL_GROUP_MEMBER_ADD, params);
	}
	
    
	public void deleteByUserKey(String group, String link) throws SQLException {
        
		logger.debug("Deleting Group entry from Group");
        
        QueryRunner run = new QueryRunner();
        
    	Object[] queryparams = {
    			group,
    			link
    	};

        run.update(conn, SQL_DELETE_GROUP_MEMBER_BY_USER_KEY, queryparams);
  
	}
	
	public void deleteChannel(Channel chan) throws SQLException {
        
        logger.debug("Deleting channel {} from all groups", chan.getTitle());
        
        QueryRunner run = new QueryRunner();
        
        Object[] queryparams = {
                chan.getLink()
        };

        run.update(conn, SQL_DELETE_CHANNEL, queryparams);
    }
	
	public void deleteGroup(Group group) throws SQLException {
        logger.debug("Deleting group {}", group.getName());
        
        QueryRunner run = new QueryRunner();
        
        Object[] queryparams = {
                group.getName()
        };

        run.update(conn, SQL_DELETE_GROUP, queryparams);
    }
}
