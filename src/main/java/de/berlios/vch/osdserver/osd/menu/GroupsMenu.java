package de.berlios.vch.osdserver.osd.menu;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.GroupDAO;
import de.berlios.vch.http.handler.GroupsHandler;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.IEventListener;
import de.berlios.vch.osdserver.osd.Menu;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.listener.GroupClickListener;
import de.berlios.vch.osdserver.osd.listener.OpenDownloadsListener;

public class GroupsMenu extends Menu {

    private static transient Logger logger = LoggerFactory.getLogger(GroupsMenu.class);
    
    private GroupClickListener groupClickListener = new GroupClickListener();
    
    private OpenDownloadsListener openDownloadsListener;
    
    public GroupsMenu() throws SQLException {
        super("groups", Messages.translate(GroupsHandler.class, "title"));
        openDownloadsListener = new OpenDownloadsListener(this);
        
        registerEvent(new Event(getId(), Event.KEY_BLUE, null));
        addEventListener(openDownloadsListener);
        addEventListener(new IEventListener() {
            @Override
            public void eventHappened(Event event) {
                if(event.getId().equals(Event.CLOSE)) {
                    OsdSession.stop();
                }
            }
        });
        
        Connection conn = null;
        try {
            // create db connection
            conn = ConnectionManager.getInstance().getConnection();
            
            // get all groups
            List<Group> groups = new GroupDAO(conn).getAll(true);

            // create group menu entries
            for (int i = 0; i < groups.size(); i++) {
                Group group = groups.get(i);
                String groupId = "group"+i;
                OsdItem item = new OsdItem(groupId, group.getName());
                item.registerEvent(new Event(groupId, Event.KEY_OK, null));
                item.setUserData(group);
                item.addEventListener(groupClickListener);
                addOsdItem(item);
            }
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }
}
