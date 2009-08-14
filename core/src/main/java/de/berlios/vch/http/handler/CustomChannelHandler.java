package de.berlios.vch.http.handler;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.dbutils.DbUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ChannelDAO;
import de.berlios.vch.db.dao.EnclosureDAO;
import de.berlios.vch.db.dao.ItemDAO;
import de.berlios.vch.db.dao.UserFeedDAO;
import de.berlios.vch.http.TemplateLoader;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Channel;
import de.berlios.vch.model.Enclosure;
import de.berlios.vch.model.Item;
import de.berlios.vch.model.UserFeed;
import de.berlios.vch.parser.rss.RssParser;
import de.berlios.vch.utils.RomeToModelConverter;
import de.berlios.vch.utils.comparator.ChannelTitleComparator;

public class CustomChannelHandler extends AbstractHandler {

    private static transient Logger logger = LoggerFactory.getLogger(CustomChannelHandler.class);

    @Override
    void doHandle(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        String action = (String)params.get("action");
       
        if (params.get("edit_channel") !=  null) {
            editChannel(exchange);
        } else if ("save_item".equals(action)) {
            saveItem(exchange);
        } else if ("delete_item".equals(action)) {
            deleteItem(exchange);
        } else if ("add_item".equals(action)) {
            addItem(exchange);
        } else if ("edit_item".equals(action)) {
            editItem(exchange);
        } else if (params.get("delete_channel") != null) {
            deleteChannel(exchange);
        } else if ("add_channel".equals(action)) {
            addChannel(exchange);
        } else {
            listChannels(exchange);
        }
    }

    private void editItem(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        
        Map<String, Object> tplParams = new HashMap<String, Object>();
        String path = exchange.getRequestURI().getPath();
        tplParams.put("ACTION", path);
        tplParams.put("TITLE", Messages.translate(getClass(), "customchannel"));
        tplParams.put("I18N_ITEM", getClass().getName() + ".item");
        tplParams.put("I18N_SAVE", getClass().getName() + ".save");
        tplParams.put("I18N_TITLE", getClass().getName() + ".title");
        tplParams.put("I18N_DESCRIPTION", getClass().getName() + ".description");
        tplParams.put("I18N_ENCLOSURE_LINK", getClass().getName() + ".enclosure_link");
        
        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            if(params.get("guid") == null) {
                throw new Exception("No item selected");
            }
            
            // create db connection
            conn = ds.getConnection();

            // load the item
            Item item = (Item) new ItemDAO(conn).findByKey(params.get("guid"));
            tplParams.put("item", item);
            String page = TemplateLoader.loadTemplate("customChannelItemEdit.ftl", tplParams);
            sendResponse(200, page, "text/html");
        } catch (Exception e) {
            logger.error("Couldn't add channel", e);
            addError(e);
            params.put("action", "edit");
            doHandle(exchange);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }

    private void listChannels(HttpExchange exchange) throws SQLException {
        Map<String, Object> tplParams = new HashMap<String, Object>();
        String path = exchange.getRequestURI().getPath();
        tplParams.put("ACTION", path);
        tplParams.put("TITLE", Messages.translate(getClass(), "customchannel"));
        tplParams.put("I18N_NEW_FEED", getClass().getName() + ".new_feed");
        tplParams.put("I18N_ADD", getClass().getName() + ".add");
        tplParams.put("I18N_EDIT", getClass().getName() + ".edit");
        tplParams.put("I18N_DELETE", getClass().getName() + ".delete");

        // add errors and messages
        tplParams.put("ERRORS", exchange.getAttribute("errors"));
        tplParams.put("MESSAGES", exchange.getAttribute("messages"));

        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();

            // get all channels
            long start = System.currentTimeMillis();
            List<Channel> channels = new ChannelDAO(conn).getAll(false);
            long stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop - start) + " ms");
            Collections.sort(channels, new ChannelTitleComparator());
            
            tplParams.put("CHANNELS", channels);
            String template = TemplateLoader.loadTemplate("customChannel.ftl", tplParams);
            sendResponse(200, template, "text/html");

        } catch (Exception e) {
            logger.error("Couldn't load channels", e);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }

    private void addChannel(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        Map<String, String> tplParams = new HashMap<String, String>();
        String path = exchange.getRequestURI().getPath();
        tplParams.put("ACTION", path);
        tplParams.put("TITLE", Messages.translate(getClass(), "customchannel"));

        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            String feedUri = (String) params.get("channel");
            SyndFeed feed = RssParser.parse(feedUri);
            feed.getPublishedDate();
            Channel chan = RomeToModelConverter.convert(feed);
            UserFeed ufeed = new UserFeed(feedUri, chan);
            
            conn = ds.getConnection();
            new UserFeedDAO(conn).saveOrUpdate(ufeed);
            
            addMessage(Messages.translate(getClass(), "msg_channel_added"));
        } catch (Exception e) {
            logger.error("Couldn't add channel", e);
            addError(e);
        } finally {
            params.remove("action");
            doHandle(exchange);
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close DB connection", e);
            }
        }
    }

    private void deleteChannel(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        Map<String, String> tplParams = new HashMap<String, String>();
        String path = exchange.getRequestURI().getPath();
        tplParams.put("ACTION", path);
        tplParams.put("TITLE", Messages.translate(getClass(), "customchannel"));

        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();

            // get the channel
            if(params.get("channel") == null) {
                throw new Exception(Messages.translate(getClass(), "error_no_feed_selected"));
            }
            long start = System.currentTimeMillis();
            Channel chan = (Channel) new ChannelDAO(conn).findByKey(params.get("channel"));
            if(chan == null) {
                throw new Exception(Messages.translate(getClass(), "error_feed_not_found"));
            }
            UserFeed ufeed = (UserFeed) new UserFeedDAO(conn).findByChannel(chan);
            long stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop - start) + " ms");
            
            // delete the user feed
            if(ufeed != null) {
                new UserFeedDAO(conn).delete(ufeed);
            } else {
                // delete the channel
                new ChannelDAO(conn).delete(chan);
            }

            addMessage(Messages.translate(getClass(), "msg_channel_deleted"));
            params.remove("delete_channel");
            doHandle(exchange);
        } catch (Exception e) {
            logger.error("Couldn't delete channel", e);
            addError(e.getMessage());
            params.remove("delete_channel");
            doHandle(exchange);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close DB connection", e);
            }
        }
    }

    private void addItem(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        Map<String, String> tplParams = new HashMap<String, String>();
        String path = exchange.getRequestURI().getPath();
        tplParams.put("ACTION", path);
        tplParams.put("TITLE", Messages.translate(getClass(), "customchannel"));

        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {

            // create db connection
            conn = ds.getConnection();

            //get the channel
            long start = System.currentTimeMillis();
            Channel chan = (Channel) new ChannelDAO(conn).findByKey(params.get("channel"));
            long stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop - start) + " ms");

            Item item = new Item();
            item.setTitle((String)params.get("title"));
            item.setLink((String)params.get("enc_link"));
            item.setDescription((String)params.get("desc"));
            item.setPubDate(new Date());
            item.setGuid((String)params.get("enc_link"));
            item.setChannelKey(chan.getLink());
            item.setEnclosureKey((String)params.get("enc_link"));
            item.setChannel(chan);

            Enclosure enc = new Enclosure();
            enc.setLink((String)params.get("enc_link"));
            enc.setDuration(0);
            enc.setLength(0);
            enc.setType("video/wmv");

            item.setEnclosure(enc);

            new ItemDAO(conn).save(item);

            addMessage(Messages.translate(getClass(), "msg_item_added"));
            params.put("action", "edit");
            params.put("channel", item.getChannelKey());
            doHandle(exchange);
        } catch (Exception e) {
            logger.error("Couldn't add item", e);
            addError(e.getMessage());
            params.put("action", "edit");
            doHandle(exchange);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close DB connection", e);
            }
        }
    }

    private void deleteItem(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        Map<String, String> tplParams = new HashMap<String, String>();
        String path = exchange.getRequestURI().getPath();
        tplParams.put("ACTION", path);
        tplParams.put("TITLE", Messages.translate(getClass(), "customchannel"));

        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();

            // get item
            long start = System.currentTimeMillis();
            Item item = (Item) new ItemDAO(conn).findByKey(params.get("guid"));
            long stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop - start) + " ms");

            new ItemDAO(conn).delete(item);

            addMessage(Messages.translate(getClass(), "msg_item_deleted"));
            params.put("edit_channel", "true");
            params.put("channel", item.getChannelKey());
            doHandle(exchange);
        } catch (Exception e) {
            logger.error("Couldn't delete item", e);
            addError(e.getMessage());
            params.put("action", "edit");
            doHandle(exchange);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close DB connection", e);
            }
        }
    }

    private void saveItem(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        Map<String, String> tplParams = new HashMap<String, String>();
        String path = exchange.getRequestURI().getPath();
        tplParams.put("ACTION", path);
        tplParams.put("TITLE", Messages.translate(getClass(), "customchannel"));

        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();

            // get item
            long start = System.currentTimeMillis();
            Item item = (Item) new ItemDAO(conn).findByKey(params.get("guid"));
            long stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop - start) + " ms");

            // get channel for item
            start = System.currentTimeMillis();
            Channel chan = (Channel) new ChannelDAO(conn).findByKey(item.getChannelKey());
            stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop - start) + " ms");

            item.setDescription((String)params.get("desc"));
            item.setTitle((String)params.get("title"));

            Enclosure old_enc = (Enclosure) item.getEnclosure();
            //Copy the object
            Enclosure new_enc = new Enclosure();
            new_enc.setLink(old_enc.getLink());
            new_enc.setType(old_enc.getType());
            new_enc.setLength(old_enc.getLength());
            new_enc.setDuration(old_enc.getDuration());
            
            // save new enclosure link
            item.setEnclosureKey((String)params.get("enc_link"));
            new_enc.setLink((String)params.get("enc_link"));
            item.setEnclosure(new_enc);
            item.setChannel(chan);

            //Save the new item
            new ItemDAO(conn).saveOrUpdate(item);
            
            //Delete old enclosure
            if(!old_enc.getLink().equals(new_enc.getLink())) {
                new EnclosureDAO(conn).delete(old_enc);
            }

            addMessage(Messages.translate(getClass(), "msg_item_saved"));
            params.put("edit_channel", "true");
            params.put("channel", item.getChannelKey());
            doHandle(exchange);
        } catch (Exception e) {
            logger.error("Couldn't save item", e);
            addError(e.getMessage());
            params.put("action", "edit");
            doHandle(exchange);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }

    private void editChannel(HttpExchange exchange) throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) exchange.getAttribute("parameters");
        
        Map<String, Object> tplParams = new HashMap<String, Object>();
        String path = exchange.getRequestURI().getPath();
        tplParams.put("ACTION", path);
        String downloadHandlerPath = Config.getInstance().getHandlerMapping().getPath(DownloadHandler.class);
        tplParams.put("DOWNLOAD_ACTION", downloadHandlerPath);
        tplParams.put("TITLE", Messages.translate(getClass(), "customchannel"));
        tplParams.put("I18N_ADD", getClass().getName() + ".add");
        tplParams.put("I18N_EDIT", getClass().getName() + ".edit");
        tplParams.put("I18N_DELETE", getClass().getName() + ".delete");
        tplParams.put("I18N_DOWNLOAD", getClass().getName() + ".download");
        tplParams.put("I18N_ITEMS", getClass().getName() + ".items");
        tplParams.put("I18N_OPTIONS", getClass().getName() + ".options");
        tplParams.put("I18N_TITLE", getClass().getName() + ".title");
        tplParams.put("I18N_DESCRIPTION", getClass().getName() + ".description");
        tplParams.put("I18N_ENCLOSURE_LINK", getClass().getName() + ".enclosure_link");
        tplParams.put("I18N_ADD_NEW_ITEM", getClass().getName() + ".add_new_item");
        tplParams.put("I18N_FEED", getClass().getName() + ".feed");
        tplParams.put("I18N_NEW_ITEM", getClass().getName() + ".new_item");
        tplParams.put("I18N_NEW_DESCRIPTION", getClass().getName() + ".new_description");
        tplParams.put("I18N_NEW_VIDEO_URL", getClass().getName() + ".new_video_url");

        ConnectionManager ds = ConnectionManager.getInstance();
        Connection conn = null;
        try {
            // create db connection
            conn = ds.getConnection();

            //get the channel
            if(params.get("channel") == null) {
                throw new Exception(Messages.translate(getClass(), "error_no_feed_selected"));
            }
            long start = System.currentTimeMillis();
            Channel chan = (Channel) new ChannelDAO(conn).findByKey(params.get("channel"));
            long stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop - start) + " ms");

            // get all Items
            start = System.currentTimeMillis();
            List<Item> items = new ItemDAO(conn).findByChannel(chan);
            stop = System.currentTimeMillis();
            logger.trace("DB access took " + (stop - start) + " ms");

            tplParams.put("ITEMS", items);
            tplParams.put("CHANNEL_LINK", chan.getLink());
            tplParams.put("CHANNEL_TITLE", chan.getTitle());
            tplParams.put("CHANNEL_IMAGE", chan.getThumbnail());
            
            // add errors and messages
            tplParams.put("ERRORS", exchange.getAttribute("errors"));
            tplParams.put("MESSAGES", exchange.getAttribute("messages"));
            
            String template = TemplateLoader.loadTemplate("customChannelItems.ftl", tplParams);
            
            sendResponse(200, template, "text/html");
        } catch (Exception e) {
            logger.error("Couldn't load channel", e);
            addError(e);
            params.clear();
            doHandle(exchange);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
    }

    @Override
    protected String getDescriptionKey() {
        return "handler_description";
    }
}