package de.berlios.vch.osdserver.osd.menu;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.GroupMemberDAO;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Group;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Menu;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.listener.ChannelClickListener;

public class ChannelsMenu extends Menu {

    private static transient Logger logger = LoggerFactory.getLogger(ChannelsMenu.class);
    
    private ChannelClickListener channelClickListener = new ChannelClickListener();
    
    public ChannelsMenu(Group g) throws SQLException {
        super("channels", g.getName());
        
        Connection conn = null;
        try {
            // create db connection
            conn = ConnectionManager.getInstance().getConnection();
            
            // get all channels
            List<Channel> channels = new GroupMemberDAO(conn).findByKey(g.getName());

            // create menu entries
            for (int i = 0; i < channels.size(); i++) {
                Channel chan = channels.get(i);
                String channelId = "channel"+i;
                OsdItem item = new OsdItem(channelId, chan.getTitle());
                item.registerEvent(new Event(channelId, Event.KEY_OK, null));
                item.setUserData(chan);
                item.addEventListener(channelClickListener);
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
