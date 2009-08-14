package de.berlios.vch.http.handler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ChannelDAO;
import de.berlios.vch.db.dao.GroupDAO;
import de.berlios.vch.db.dao.GroupMemberDAO;
import de.berlios.vch.db.dao.UserFeedDAO;
import de.berlios.vch.http.HandlerMapping;
import de.berlios.vch.http.TemplateLoader;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Group;
import de.berlios.vch.model.UserFeed;

public class ConfigGroupsHandler extends AbstractHandler {
	
    private static transient Logger logger = LoggerFactory.getLogger(ConfigGroupsHandler.class);
    
    private HandlerMapping mapping = Config.getInstance().getHandlerMapping();

	@Override
	void doHandle(HttpExchange exchange) throws Exception {

		@SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        String action = (String)params.get("action");
        //action = action == null ? "edit" : action;
        String group_name = (String)params.get("group_name");
		
        Connection conn = null; 
        try {
            conn = ConnectionManager.getInstance().getConnection();
            if(group_name == null) {
	            Map<String, Object> tplParams = new HashMap<String, Object>();
	            String path = exchange.getRequestURI().getPath();
	            tplParams.put("ACTION", path);
	            tplParams.put("TITLE", Messages.translate(getClass(),"configuration"));
	            tplParams.put("I18N_GROUP_MEMBER", getClass().getName() + ".member");
	            tplParams.put("I18N_EDIT", getClass().getName() + ".edit");
	            tplParams.put("I18N_DELETE", getClass().getName() + ".delete");
	            tplParams.put("I18N_ADD", getClass().getName() + ".add");
	            tplParams.put("I18N_GROUP_NOT_EMPTY", getClass().getName() + ".group_not_empty_confirm");
	            
	            // enable ajax?
	            tplParams.put("AJAX_ENABLED", Config.getInstance().getBoolValue("ajax.enabled"));
	            
	            // add errors and messages
	            tplParams.put("ERRORS", exchange.getAttribute("errors"));
	            tplParams.put("MESSAGES", exchange.getAttribute("messages"));
	            
	            List <String> handlermap = mapping.getPathes(de.berlios.vch.http.handler.ConfigGroupMemberHandler.class);
	            Iterator<String> mapit = handlermap.iterator();
	            while (mapit.hasNext()) {
		            tplParams.put("MEMBER_CONFIG_PAGE", (String) mapit.next());
	            }
	            // get all groups
	            long start = System.currentTimeMillis();
	            List<Group> groups = new GroupDAO(conn).getAll(true);
	            long stop = System.currentTimeMillis();
	            logger.trace("DB access took " + (stop-start) + " ms");
	            tplParams.put("GROUPLIST", groups);
	            
                String template = TemplateLoader.loadTemplate("configGroups.ftl", tplParams);
                sendResponse(200, template, "text/html");
            } else if(group_name != null && action.equals("addgroup")) {
            	Group new_group = new Group();
            	new_group.setName(group_name);
            	new_group.setDescription("");
            	new GroupDAO(conn).save(new_group);
            	
            	addMessage(Messages.translate(getClass(), "msg_group_added"));
                params.put("group_name", null);
                doHandle(exchange);
            } else if(group_name != null && action.equals("delete")) {
	        	Group del_group = new Group();
	        	del_group.setName(group_name);
	        	List<Channel> groupMembers = new GroupMemberDAO(conn).findByKey(group_name);
	        	if(groupMembers.size() > 0) {
	        	    // delete group entries, if wanted
	        	    boolean deleteEntries = Boolean.parseBoolean((String)params.get("delete_entries"));
	        	    if(deleteEntries) {
	                    List<Channel> channels = new GroupMemberDAO(conn).findByKey(group_name);
	                    ChannelDAO cdao = new ChannelDAO(conn);
	                    UserFeedDAO ufdao = new UserFeedDAO(conn);
	                    for (Channel channel : channels) {
	                        UserFeed ufeed = (UserFeed) ufdao.findByChannel(channel);
	                        if(ufeed != null) {
	                            // delete the user feed
	                            ufdao.delete(ufeed);
	                        } else {
	                            // delete the channel
	                            cdao.delete(channel);
	                        }
                        }
	                    addMessage(Messages.translate(getClass(), "feeds_have_been_deleted", new Object[] {channels.size()}));
	        	    } else {
	        	        throw new Exception(Messages.translate(getClass(), "group_not_empty"));
	        	    }
	        	    
	        	    // delete group from groups_inter_channel
                    Group group = new Group();
                    group.setName(group_name);
                    new GroupMemberDAO(conn).deleteGroup(group);
	        	}
	        	
	        	// delete group
        	    new GroupDAO(conn).delete(del_group);
                addMessage(Messages.translate(getClass(), "msg_group_deleted"));
                params.put("group_name", null);
                doHandle(exchange);
            } else if(group_name != null && action.equals("group_count")) {
                List<Channel> groupMembers = new GroupMemberDAO(conn).findByKey(group_name);
                sendResponse(200, Integer.toString(groupMembers.size()), "text/plain");
            } else {
                String msg = Messages.translate(getClass(), "error_action");
            	addError(msg);
            	params.put("group_name", null);
            	doHandle(exchange);
            }
        } catch (Exception e) {
        	logger.error("Couldn't execute requested action", e);
        	addError(e.getMessage());
            params.put("group_name", null);
            doHandle(exchange);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close DB connection", e);
            }
        }
	}
	
	@Override
    protected String getDescriptionKey() {
        return "description";
    }
}