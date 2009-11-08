package de.berlios.vch.osdserver.osd.menu;

import java.util.StringTokenizer;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.ID;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.actions.PlayAction;
import de.berlios.vch.parser.IVideoPage;

public class ItemDetailsMenu extends Menu {

    private int index = 0;
    
    public ItemDetailsMenu(IVideoPage page, Messages i18n) {
        super(ID.randomId(), page.getTitle());
        
        if(page.getVideoUri() != null && !page.getVideoUri().toString().isEmpty()) {
            registerAction(new PlayAction(i18n));
        }
        
        int rowlen = 50;
        String desc = page.getDescription();
        desc = desc != null ? (desc.isEmpty() ? page.getTitle() : desc) : page.getTitle();
        StringTokenizer lines = new StringTokenizer(desc, "\n");
        while(lines.hasMoreTokens()) {
            StringTokenizer words = new StringTokenizer(lines.nextToken());
            StringBuilder line;
            if(words.hasMoreTokens()) {
                line = new StringBuilder(words.nextToken());
                while(words.hasMoreTokens()) {
                    String word = words.nextToken();
                    if( (line.length() + word.length() + 1) < rowlen ) {
                        line.append(" ").append(word);
                    } else {
                        addLine(line.toString(), page);
                        line = new StringBuilder(word);
                    }
                }
                addLine(line.toString(), page);
            }
        }
    }
    
    private void addLine(String line, IVideoPage page) {
        OsdItem description = new OsdItem("item_desc" + index++, line.toString());
        description.setSelectable(false);
        description.setUserData(page); // this is a workaround, so that we get
                                       // the page, when we ask for the current 
                                       // page on the details page
        addOsdItem(description);
    }
}
