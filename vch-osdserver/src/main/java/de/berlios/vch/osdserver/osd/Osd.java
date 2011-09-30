package de.berlios.vch.osdserver.osd;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.osdserver.IEventDispatcher;
import de.berlios.vch.osdserver.OsdSession;
import de.berlios.vch.osdserver.io.Connection;
import de.berlios.vch.osdserver.io.StringUtils;
import de.berlios.vch.osdserver.io.command.OsdMessage;
import de.berlios.vch.osdserver.io.response.Event;
import de.berlios.vch.osdserver.io.response.Response;
import de.berlios.vch.osdserver.osd.menu.Menu;
import de.berlios.vch.osdserver.osd.menu.actions.IOsdAction;

public class Osd implements IEventDispatcher {

    private static transient Logger logger = LoggerFactory.getLogger(Osd.class);

    private Connection conn;

    private Stack<Map<String, OsdObject>> contextStack = new Stack<Map<String, OsdObject>>();

    private Stack<Menu> menuStack = new Stack<Menu>();

    private Map<String, OsdObject> context = new HashMap<String, OsdObject>();

    private OsdSession session;

    public Osd(OsdSession session) {
        this.session = session;
    }

    public void connect(String host, int port, int timeout, String encoding) throws UnknownHostException, IOException, OsdException {
        conn = new Connection(host, port, timeout, encoding);
        conn.setEventDispatcher(this);
    }

    public Connection getConnection() {
        return conn;
    }

    public void createMenu(Menu menu) throws IOException, OsdException {
        // add menu close action:
        // automatically remove a menu from the stack and
        // delete it on the osdserver, if a close event happens
        menu.registerAction(new IOsdAction() {
            @Override
            public String getName() {
                return "close menu";
            }

            @Override
            public String getModifier() {
                return null;
            }

            @Override
            public String getEvent() {
                return Event.CLOSE;
            }

            @Override
            public void execute(OsdSession session, OsdObject oo) {
                closeMenu();
            }
        });

        // create menu
        conn.send(menu.getId() + " = new menu '" + StringUtils.escape(menu.getTitle()) + "'");

        // register events for the menu
        registerEvents(menu, null);

        context.put(menu.getId(), menu);

        // create all menu items
        for (OsdItem item : menu.getItems()) {
            createOsdItem(menu, item);
        }
    }

    public void closeMenu() {
        Menu menu = menuStack.peek();
        try {
            // send the delete command
            conn.send("delete " + menu.getId());

            // if everything went fine we have to pop the menu from the menu stack
            menuStack.pop();
        } catch (Exception e) {
            logger.error("Couldn't delete menu {}", menu.getId());
        }
        logger.trace("Menu stack: {} {}", menuStack.size(), menuStack);
    }

    public void createOsdItem(Menu menu, OsdItem item) throws IOException, OsdException {
        if (item instanceof StringEditOsdItem) {
            createStringEditItem(menu, (StringEditOsdItem) item);
        } else {
            createStaticOsdItem(menu, item);
        }

    }

    private void createStringEditItem(Menu menu, StringEditOsdItem item) throws IOException, OsdException {
        // an edit item needs columns
        setColumns(menu, 20);

        // now create the edit item
        String addToMenu = item.getId() + "=" + menu.getId() + ".addNew EditStrItem '" + item.getTitle() + "' '" + item.getDefaultValue() + "'";
        conn.send(addToMenu);

        // register events for the item
        registerEvents(item, null);

        context.put(item.getId(), item);
    }

    // TODO multiline response is possible in 600 response
    public String getText(StringEditOsdItem stringEditOsdItem, boolean quoted) throws IOException, OsdException {
        String msg = stringEditOsdItem.getId() + ".GETVALUE";
        if (quoted) {
            msg += " -quoted";
        }
        Response response = conn.send(msg).get(0);
        String text = "";
        if (response.getCode() == 500 || response.getCode() == 600) {
            text = response.getMessage();
        }
        return text;
    }

    private void createStaticOsdItem(Menu menu, OsdItem item) throws IOException, OsdException {
        StringBuilder sb = new StringBuilder(item.getId());
        sb.append(" = ").append(menu.getId()).append(".AddNew OsdItem");
        if (!item.isSelectable()) {
            sb.append(" -unselectable");
        }
        sb.append(" '").append(StringUtils.escape(item.getTitle())).append("'");
        conn.send(sb.toString());

        // register events for the item
        registerEvents(item, null);

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
        } catch (Exception e) {
        }
    }

    public void show(Menu menu) throws IOException, OsdException {
        conn.send(menu.getId() + ".show");

        /*
         * show() must be called to update the osd after changes. To avoid having a wrong menu hierarchy, we have to make sure, that the menu on top of the
         * stack is not the given one
         */
        if (menuStack.isEmpty() || menuStack.peek() != menu) {
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
    public synchronized void dispatchEvent(Event event) {
        logger.debug("Trying to dispatch event {} {} {}", new Object[] { event.getCode(), event.getType(), event.getSourceId() });
        String srcId = event.getSourceId();
        OsdObject oo = context.get(srcId);
        if (oo != null) {
            event.setSource(oo);
            if (oo instanceof IEventBased) {
                IEventBased ieb = (IEventBased) oo;
                IOsdAction actionToExecute = null;
                for (Iterator<IOsdAction> iterator = ieb.getRegisteredActions().iterator(); iterator.hasNext();) {
                    IOsdAction action = iterator.next();
                    if (action.getEvent().equals(event.getType())) {
                        actionToExecute = action;
                        break;
                    }
                }
                if (actionToExecute != null) {
                    try {
                        logger.debug("Dispatching event {}-{} to action {}", new Object[] { event.getCode(), event.getType(), actionToExecute.getName() });
                        actionToExecute.execute(session, oo);
                    } catch (Exception e) {
                        logger.error("Couldn't execute action [" + actionToExecute.getName() + "] " + e.getLocalizedMessage(), e);
                        showMessageSilent(new OsdMessage("", OsdMessage.STATUSCLEAR));
                        showMessageSilent(new OsdMessage(e.getLocalizedMessage(), OsdMessage.ERROR));
                    }
                } else {
                    logger.warn("Couldn't dispatch event {} {} {}", new Object[] { event.getCode(), event.getType(), event.getSourceId() });
                }
            }
        } else {
            logger.warn("Event source {} not found in context", srcId);
        }
    }

    public Menu getCurrentMenu() {
        if (menuStack.isEmpty()) {
            return null;
        }

        return menuStack.peek();
    }

    private void registerEvents(InteractiveOsdObject ioo, String[] additionalEvents) throws IOException, OsdException {
        if (ioo.getRegisteredActions().size() > 0 || (additionalEvents != null && additionalEvents.length > 0)) {
            StringBuilder sb = new StringBuilder(ioo.getId() + ".enableevent");
            for (IOsdAction action : ioo.getRegisteredActions()) {
                sb.append(' ').append(action.getEvent());
                if (ioo instanceof Menu
                        && (action.getEvent().equals(Event.KEY_RED) || action.getEvent().equals(Event.KEY_GREEN) || action.getEvent().equals(Event.KEY_YELLOW) || action
                                .getEvent().equals(Event.KEY_BLUE))) {
                    setColorKeyText((Menu) ioo, action.getName(), action.getEvent());
                }
            }
            if (additionalEvents != null) {
                for (String event : additionalEvents) {
                    sb.append(' ').append(event);
                }
            }
            conn.send(sb.toString());
        }
    }

    public void registerEvent(InteractiveOsdObject ioo, IOsdAction action) throws IOException, OsdException {
        StringBuilder sb = new StringBuilder(ioo.getId() + ".enableevent");
        sb.append(' ').append(action.getEvent());
        if (ioo instanceof Menu
                && (action.getEvent().equals(Event.KEY_RED) || action.getEvent().equals(Event.KEY_GREEN) || action.getEvent().equals(Event.KEY_YELLOW) || action
                        .getEvent().equals(Event.KEY_BLUE))) {
            setColorKeyText((Menu) ioo, action.getName(), action.getEvent());
        }
        conn.send(sb.toString());
    }

    public void setColorKeyText(Menu menu, String text, String key) throws IOException, OsdException {
        StringBuilder sb = new StringBuilder(menu.getId());
        sb.append(".setcolorkeytext ");
        if (key.equals(Event.KEY_RED)) {
            sb.append(" -red '");
        } else if (key.equals(Event.KEY_GREEN)) {
            sb.append(" -green '");
        } else if (key.equals(Event.KEY_YELLOW)) {
            sb.append(" -yellow '");
        } else if (key.equals(Event.KEY_BLUE)) {
            sb.append(" -blue '");
        }
        sb.append(StringUtils.escape(text)).append("'");
        conn.send(sb.toString());
    }

    /**
     * 
     * @return the current selected OsdItem or null
     * @throws IOException
     * @throws OsdException
     */
    public OsdItem getCurrentItem() throws IOException, OsdException {
        List<Response> list = conn.send(menuStack.peek().getId() + ".getcurrent");
        for (Response response : list) {
            if (response.getCode() == 302) {
                String[] tmp = response.getMessage().split(" ");
                if (tmp.length == 2) {
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
        if (!current.getId().equals(menu.getId())) {
            throw new Exception("Can't refresh menu. Menu IDs differ");
        }

        createMenu(menu);
        appendTo(menuStack.peek(), menu);
        show(menu);
    }

    public String getStringItemValue(StringEditOsdItem item) throws IOException, OsdException {
        List<Response> list = conn.send(item.getId() + ".getvalue");
        for (Response response : list) {
            if (response.getCode() == 600) {
                return response.getMessage();
            }
        }
        return null;
    }

    public void quit() throws IOException, OsdException {
        conn.send("QUIT");
    }

    public void setColumns(Menu menu, int columns) throws IOException, OsdException {
        conn.send(menu.getId() + ".SetColumns " + columns);
    }
}
