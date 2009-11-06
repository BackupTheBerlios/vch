package de.berlios.vch.osdserver.osd;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.osdserver.IEventDispatcher;
import de.berlios.vch.osdserver.io.Connection;
import de.berlios.vch.osdserver.io.StringUtils;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.io.response.Response;
import de.berlios.vch.osdserver.osd.menu.Menu;

public class Osd implements IEventDispatcher, IEventListener {
    
    private static transient Logger logger = LoggerFactory.getLogger(Osd.class);
    
    private Connection conn;
    
    private static Osd instance;
    
    private Stack<Map<String, OsdObject>> contextStack = new Stack<Map<String,OsdObject>>();
    
    private Stack<Menu> menuStack = new Stack<Menu>();
    
    private Map<String, OsdObject> context = new HashMap<String, OsdObject>();

    private Osd() {}

    public synchronized static Osd getInstance() {
        if (instance == null) {
            instance = new Osd();
        }
        return instance;
    }
    
    public void connect(String host, int port, int timeout, String encoding) throws UnknownHostException, IOException, OsdException {
        conn = new Connection(host, port, timeout, encoding);
        conn.setEventDispatcher(this);
    }
    
    public Connection getConnection() {
        return conn;
    }
    
    public void createMenu(Menu menu) throws IOException, OsdException {
        // create menu
        conn.send(menu.getId() + " = new menu '" + StringUtils.escape(menu.getTitle()) + "'");

        // register events for the menu
        StringBuilder sb = new StringBuilder(menu.getId()+".enableevent");
        for (Event event : menu.getRegisteredEvents()) {
            sb.append(' ').append(event.getType());
        }
        sb.append(" close");
        conn.send(sb.toString());
        context.put(menu.getId(), menu);
        
        // create all menu items
        for (OsdItem item : menu.getItems()) {
            createOsdItem(menu, item);
        }
        
        menu.addEventListener(this);
    }
    
    public void createOsdItem(Menu menu, OsdItem item) throws IOException, OsdException {
        // create osd item
        StringBuilder sb = new StringBuilder(item.getId());
        sb.append(" = ").append(menu.getId()).append(".AddNew OsdItem");
        if(!item.isSelectable()) {
            sb.append(" -unselectable");
        }
        sb.append(" '").append(StringUtils.escape(item.getTitle())).append("'");
        conn.send(sb.toString());

        // register events for the item
        sb = new StringBuilder(item.getId()+".enableevent");
        for (Event event : item.getRegisteredEvents()) {
            sb.append(' ').append(event.getType());
        }
        conn.send(sb.toString());
        context.put(item.getId(), item);
    }
    
    public void enterLocal() throws IOException, OsdException {
        conn.send("enterlocal");
        contextStack.push(context);
        context = new HashMap<String, OsdObject>();
    }
    
    public void leaveLocal() throws IOException, OsdException {
        conn.send("leavelocal");
        context = contextStack.pop();
    }
    
    public void showMessage(OsdMessage msg) throws IOException, OsdException {
        conn.send(msg);
    }
    
    public void showMessageSilent(OsdMessage msg) {
        try {
            conn.send(msg);
        } catch (Exception e) {}
    }
    
    public void show(Menu menu) throws IOException, OsdException {
        conn.send(menu.getId() + ".show");
        
        /* show() must be called to update the osd after changes.
         * To avoid having a wrong menu hierarchy, we have to make
         * sure, that the menu on top of the stack is not given one */
        if(menuStack.isEmpty() || menuStack.peek() != menu) {
            menuStack.push(menu);
        }
    }
    
    public void sleepEvent(String objectId) throws IOException, OsdException {
        conn.send(objectId + ".sleepevent");
    }
    
    public void sleepEvent(Menu menu) throws IOException, OsdException {
        sleepEvent(menu.getId());
    }
    
    public void appendToFocus(Menu menu) throws IOException, OsdException {
        conn.send("_focus.addsubmenu " + menu.getId());
    }
    
    public void appendTo(Menu parent, Menu child) throws IOException, OsdException {
        conn.send(parent.getId() + ".addsubmenu " + child.getId());
    }
    
    public void sendState(String state) throws IOException, OsdException {
        conn.send(menuStack.peek().getId() + ".sendstate " + state);
    }
    
    public void setText(OsdItem item, String text) throws IOException, OsdException {
        conn.send(item.getId() + ".settext '" + StringUtils.escape(text) + "'");
    }

    @Override
    public void dispatchEvent(Event event) {
        String srcId = event.getSourceId();
        OsdObject oo = context.get(srcId);
        if(oo != null) {
            event.setSource(oo);
            if(oo instanceof IEventBased) {
                IEventBased ieb = (IEventBased) oo;
                for (IEventListener l : ieb.getEventListeners()) {
                    l.eventHappened(event);
                }
            }
        } else {
            logger.warn("Event source {} not found in context", srcId);
        }
    }
    
    /*
     * Implemented to automatically catch menu close events.
     * For each closed menu we send a delete command to free the memory 
     */
    @Override
    public void eventHappened(Event event) {
        if(event.getType().equals(Event.CLOSE)) {
            Menu current = menuStack.pop(); // first pop the current menu.
            try {
                conn.send("delete " + current.getId());
            } catch (Exception e) {
                logger.error("Couldn't delete menu {}", event.getType());
            }
            logger.trace("Menu stack: {} {}", menuStack.size(), menuStack);
        }
    }
    
    public Menu getCurrentMenu() {
        if(menuStack.isEmpty()) {
            return null;
        }
        
        return menuStack.peek(); 
    }
    
    public void setColorKeyText(Menu menu, String text, String key) throws IOException, OsdException {
        StringBuilder sb = new StringBuilder(menu.getId());
        sb.append(".setcolorkeytext ");
        if(key.equals(Event.KEY_RED)) {
            sb.append(" -red '");
        } else if(key.equals(Event.KEY_GREEN)) {
            sb.append(" -green '");
        } else if(key.equals(Event.KEY_YELLOW)) {
            sb.append(" -yellow '");
        } else if(key.equals(Event.KEY_BLUE)) {
            sb.append(" -blue '");
        }
        sb.append(StringUtils.escape(text)).append("'");
        conn.send(sb.toString());
    }
    
    public OsdItem getCurrentItem() throws IOException, OsdException {
        List<Response> list = (List<Response>) conn.send(menuStack.peek().getId() + ".getcurrent");
        for (Response response : list) {
            if(response.getCode() == 302) {
                String[] tmp = response.getMessage().split(" ");
                if(tmp.length == 2) {
                    String objectId = tmp[1];
                    return (OsdItem) context.get(objectId);
                }
            }
        }
        return null;
    }
    
    public OsdObject getObjectById(String id) {
        return context.get(id);
    }
    
    public void refreshMenu(Menu menu) throws Exception {
        Menu current = menuStack.pop();
        if(!current.getId().equals(menu.getId())) {
            throw new Exception("Can't refresh menu. Menu IDs differ");
        }
        
        createMenu(menu);
        appendTo(menuStack.peek(), menu);
        show(menu);
    }
}