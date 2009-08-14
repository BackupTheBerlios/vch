package de.berlios.vch.osdserver.osd.menu;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ItemDAO;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Item;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Menu;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.listener.ItemListener;

public class ItemsMenu extends Menu {

    private static transient Logger logger = LoggerFactory.getLogger(ItemsMenu.class);
    
    private ItemListener itemListener = new ItemListener();
    
    public ItemsMenu(Channel chan) throws SQLException {
        super("items", chan.getTitle());
        
        Connection conn = null;
        try {
            // create db connection
            conn = ConnectionManager.getInstance().getConnection();
            
            // get all channels entries
            List<Item> items = new ItemDAO(conn).findByChannel(chan);

            // create menu entries
            for (int i = 0; i < items.size(); i++) {
                Item item = items.get(i);
                String itemId = "item"+i;
                OsdItem osditem = new OsdItem(itemId, item.getTitle());
                osditem.registerEvent(new Event(itemId, Event.KEY_OK, null));
                osditem.registerEvent(new Event(getId(), Event.KEY_GREEN, null));
                osditem.registerEvent(new Event(getId(), Event.KEY_BLUE, null));
                osditem.setUserData(item);
                osditem.addEventListener(itemListener);
                addOsdItem(osditem);
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
