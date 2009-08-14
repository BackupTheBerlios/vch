package de.berlios.vch.osdserver.osd.menu;

import java.util.StringTokenizer;

import de.berlios.vch.model.Item;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.osd.Menu;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.listener.ItemListener;

public class ItemDetailsMenu extends Menu {

    private int index = 0;
    
    public ItemDetailsMenu(Item item) {
        super("item", item.getTitle());
        registerEvent(new Event(getId(), Event.KEY_GREEN, null));
        registerEvent(new Event(getId(), Event.KEY_BLUE, null));
        
        int rowlen = 50;
        String desc = item.getDescription();
        desc = desc != null ? desc : item.getTitle();
        StringTokenizer st = new StringTokenizer(desc);
        StringBuilder line;
        if(st.hasMoreTokens()) {
            line = new StringBuilder(st.nextToken());
            while(st.hasMoreTokens()) {
                String word = st.nextToken();
                if( (line.length() + word.length() + 1) < rowlen ) {
                    line.append(" ").append(word);
                } else {
                    addLine(line.toString(), item);
                    line = new StringBuilder(word);
                }
            }
            addLine(line.toString(), item);
        }
        
        addEventListener(new ItemListener());
    }
    
    private void addLine(String line, Item item) {
        OsdItem description = new OsdItem("item_desc" + index++, line.toString());
        description.setSelectable(false);
        description.setUserData(item); // this is a workaround, so that we get
                                       // the item, when we ask for the current 
                                       // item on the details page
        addOsdItem(description);
    }
}
