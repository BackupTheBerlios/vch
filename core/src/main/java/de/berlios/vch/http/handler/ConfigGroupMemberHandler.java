package de.berlios.vch.http.handler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ChannelDAO;
import de.berlios.vch.db.dao.GroupDAO;
import de.berlios.vch.db.dao.GroupMemberDAO;
import de.berlios.vch.http.TemplateLoader;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Group;
import de.berlios.vch.utils.comparator.ChannelTitleComparator;

public class ConfigGroupMemberHandler extends AbstractHandler {
	
    private static transient Logger logger = LoggerFactory.getLogger(ConfigGroupMemberHandler.class);

	@SuppressWarnings("unchecked")
    @Override
	void doHandle(HttpExchange exchange) throws Exception {
		
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        String action = (String)params.get("action");
		
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        
        try {
            // create db connection
            conn = ds.getConnection();
            
            if ("edit".equals(action)) {
                Map<String, Object> tplParams = new HashMap<String, Object>();
                String path = exchange.getRequestURI().getPath();
                tplParams.put("ACTION", path);
                tplParams.put("I18N_GROUP", getClass().getName() + ".configuration");
                tplParams.put("I18N_GROUP_MEMBER", getClass().getName() + ".member");
                tplParams.put("I18N_ALL_CHANNEL", getClass().getName() + ".all");
                tplParams.put("I18N_ADD", getClass().getName() + ".add");
                tplParams.put("I18N_SAVE", getClass().getName() + ".save");
                tplParams.put("I18N_DELETE", getClass().getName() + ".delete");
                tplParams.put("I18N_DESCRIPTION", getClass().getName() + ".channel_description");

                // add errors and messages
                tplParams.put("ERRORS", exchange.getAttribute("errors"));
                tplParams.put("MESSAGES", exchange.getAttribute("messages"));
                
                // get group
                long start = System.currentTimeMillis();
                Group group = (Group) new GroupDAO(conn).findByKey(params.get("group_name"));
                long stop = System.currentTimeMillis();
                logger.trace("DB access took " + (stop-start) + " ms");
                tplParams.put("GROUP_NAME", group.getName());
                tplParams.put("DESC", group.getDescription());
                
            	// get all channels
                start = System.currentTimeMillis();
                List<Channel> allchannl = new ChannelDAO(conn).getAll(false);
                Collections.sort(allchannl, new ChannelTitleComparator());
                stop = System.currentTimeMillis();
                logger.trace("DB access took " + (stop-start) + " ms");
                tplParams.put("AVAILCHANNELLIST", allchannl);
                
                // get group member
                start = System.currentTimeMillis();
                List<Channel> channels = new GroupMemberDAO(conn).findByKey(params.get("group_name"));
                stop = System.currentTimeMillis();
                logger.trace("DB access took " + (stop-start) + " ms");
                allchannl.removeAll(channels); // remove the group channels from all channels list
                Collections.sort(channels, new ChannelTitleComparator());
                tplParams.put("CHANNELLIST", channels);
                String template = TemplateLoader.loadTemplate("configGroupMember.ftl", tplParams);
                sendResponse(200, template, "text/html");
            } else if ("save_desc".equals(action)) {
                Group save_group = (Group) new GroupDAO(conn).findByKey(params.get("group_name"));
                save_group.setDescription((String)params.get("desc"));
                new GroupDAO(conn).update(save_group);
                String mesg = Messages.translate(getClass(), "msg_save_desc");
                addMessage(mesg);
                params.put("action", "edit");
                doHandle(exchange);
            } else if (params.get("submit_add") != null) {
                GroupMemberDAO gmp = new GroupMemberDAO(conn);
                Object value = params.get("channels");
                if(value instanceof List) {
                    List<String> channels = (List<String>) value;
                    for (Iterator<String> iterator = channels.iterator(); iterator.hasNext();) {
                        String link = iterator.next();
                        Channel channel = (Channel) new ChannelDAO(conn).findByKey(link);
                        gmp.addChannel(channel.getLink(), (String)params.get("group_name"));
                    }
                } else if(value instanceof String) {
                    Channel channel = (Channel) new ChannelDAO(conn).findByKey(value.toString());
                    gmp.addChannel(channel.getLink(), (String)params.get("group_name"));
                }
                
            	String mesg = Messages.translate(getClass(), "msg_add_channel");
                addMessage(mesg);
                params.put("action", "edit");
                doHandle(exchange);
            } else if (params.get("submit_delete") != null) {
                GroupMemberDAO gmp = new GroupMemberDAO(conn);
                Object value = params.get("members");
                if(value instanceof List) {
                    List<String> channels = (List<String>) value;
                    for (Iterator<String> iterator = channels.iterator(); iterator.hasNext();) {
                        String link = iterator.next();
                        Channel channel = (Channel) new ChannelDAO(conn).findByKey(link);
                        gmp.deleteByUserKey((String)params.get("group_name"),channel.getLink());
                    }
                } else if(value instanceof String) {
                    Channel channel = (Channel) new ChannelDAO(conn).findByKey((String)value);
                    gmp.deleteByUserKey((String)params.get("group_name"),channel.getLink());
                }
                
            	String mesg = Messages.translate(getClass(), "msg_delete_channel");
                addMessage(mesg);
                params.put("action", "edit");
                doHandle(exchange);
            }
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