package de.berlios.vch.osdserver.osd.menu;

import java.util.StringTokenizer;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.osdserver.ID;
import de.berlios.vch.osdserver.osd.OsdItem;
import de.berlios.vch.osdserver.osd.menu.actions.IOsdAction;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.osdserver.osd.menu.actions.PlayAction;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.playlist.PlaylistService;

public class ItemDetailsMenu extends Menu {

    private int index = 0;
    
    public ItemDetailsMenu(BundleContext ctx, IVideoPage page, Messages i18n, PlaylistService playlistService) {
        super(ID.randomId(), page.getTitle());
        
        if(page.getVideoUri() != null && !page.getVideoUri().toString().isEmpty()) {
            // register play action
            registerAction(new PlayAction(ctx, i18n, playlistService));
            
            // register actions from other osgi bundles
            Object[] actions = getOsdActions(ctx);
            if(actions != null) {
                for (Object a : actions) {
                    IOsdAction action = (IOsdAction) a;
                    registerAction(action);
                }
            }
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
    
    private Object[] getOsdActions(BundleContext ctx) {
        ServiceTracker st = new ServiceTracker(ctx, ItemDetailsAction.class.getName(), null);
        st.open();
        Object[] actions = st.getServices();
        st.close();
        return actions;
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
