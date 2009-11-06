package de.berlios.vch.web.menu;

import java.util.SortedSet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MenuBuilder {

    private static transient Logger logger = LoggerFactory.getLogger(MenuBuilder.class);
    
    private ServiceTracker menuTracker;
    
    private IWebMenuEntry root;
    
    private boolean menuHasChanged = true;
    
    public MenuBuilder(final BundleContext ctx) {
        menuTracker = new ServiceTracker(ctx, IWebMenuEntry.class.getName(), null) {
            @Override
            public void removedService(ServiceReference sr, Object service) {
                menuHasChanged = true;
                logger.debug("Menu entry removed {}", ((IWebMenuEntry)service).getTitle());
                super.removedService(sr, service);
            }
            
            @Override
            public void modifiedService(ServiceReference sr, Object service) {
                menuHasChanged = true;
                super.modifiedService(sr, service);
            }
            
            @Override
            public Object addingService(ServiceReference sr) {
                menuHasChanged = true;
                return super.addingService(sr);
            }
        };
        menuTracker.open();
    }

    public IWebMenuEntry getMenu() {
        if(menuHasChanged) {
            rebuild();
        }
        return root;
    }
    
    private void rebuild() {
        logger.info("Rebuilding navigation menu");
        root = new WebMenuEntry();
        
        Object[] services = menuTracker.getServices();
        if(services != null) {
            for (Object service : services) {
                IWebMenuEntry entry = ((IWebMenuEntry) service).clone(); // clone this object, so that the original menu entry stays untouched
                insertMenuItems(root, entry);
            }
        }
        
        menuHasChanged = false;
    }
    
    private void insertMenuItems(IWebMenuEntry menu, IWebMenuEntry item) {
        SortedSet<IWebMenuEntry> childs = menu.getChilds();
        if(childs.contains(item)) {
            for (IWebMenuEntry entry : childs) {
                if(entry.equals(item)) {
                    for (IWebMenuEntry itemChild : item.getChilds()) {
                        insertMenuItems(entry, itemChild);
                    }
                }
            }
        } else {
            childs.add(item);
        }
    }
}
