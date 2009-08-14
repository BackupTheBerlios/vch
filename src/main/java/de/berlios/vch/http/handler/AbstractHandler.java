package de.berlios.vch.http.handler;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.htmlparser.util.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedOutput;

import de.berlios.vch.Config;
import de.berlios.vch.http.TemplateLoader;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.utils.StringUtils;

abstract public class AbstractHandler implements HttpHandler {

    private static transient Logger logger = LoggerFactory.getLogger(AbstractHandler.class);
    
    protected HttpExchange exchange;
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            this.exchange = exchange;
            reset();
            doHandle(exchange);
        } catch (Throwable t) {
            try {
                logger.error("Couldn't execute request", t);
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("TITLE", "500 - " + Messages.translate(AbstractHandler.class, "internal_server_error"));
                params.put("MESSAGE", "<pre>" + Translate.encode(StringUtils.stackTraceToString(t)) + "</pre>");
                String html500 = TemplateLoader.loadTemplate("error.ftl", params);
                sendResponse(500, html500, "text/html");
            } catch (RuntimeException e1) {
                logger.error("{}", e1);
            }
        } finally {
            exchange.getResponseBody().close();
        }
        
    }
    
    private void reset() {
        exchange.setAttribute("errors", null);
        exchange.setAttribute("messages", null);
    }

    protected void streamRssFeed(SyndFeed feed) throws IOException {
        exchange.getResponseHeaders().add( "Content-type", "text/xml" );
        exchange.sendResponseHeaders(200, 0);
        
        SyndFeedOutput output = new SyndFeedOutput();
        Writer writer = new OutputStreamWriter(exchange.getResponseBody(), Config.getInstance().getProperty("default.encoding"));
        
        try {
            output.output(feed, writer);
        } catch (Exception e) {
            logger.error("Couldn't stream RSS feed", e);
        } 
    }
    
    protected void debugRssFeed(SyndFeed feed) throws IOException {
        SyndFeedOutput output = new SyndFeedOutput();
        Writer writer = new OutputStreamWriter(System.out, Config.getInstance().getProperty("default.encoding"));
        
        try {
            output.output(feed, writer);
        } catch (Exception e) {
            logger.error("Couldn't print RSS feed", e);
        } 
    }
    
    protected void sendResponse(int code, String body, String type) {
        String encoding = Config.getInstance().getProperty("default.encoding");
        if("text/html".equals(type)) {
            encoding = Config.getInstance().getProperty("html.encoding");
        }
        
        try {
            byte[] content = body.getBytes(encoding);
            this.exchange.getResponseHeaders().add("Content-type", type);
            this.exchange.sendResponseHeaders(code, content.length);
            exchange.getResponseBody().write(content);
        } catch (UnsupportedEncodingException e) {
            logger.error("Encoding not supported " + encoding, e);
        } catch (IOException e) {
            if(e.getMessage().contains("Broken pipe")) {
                logger.warn("Couldn't write http response ", e);
            } else {
                logger.error("Couldn't write http response ", e);
            }
        }
    }

    abstract void doHandle(HttpExchange exchange) throws Exception;
    
    public HandlerDescription getDescription() {
        String url = Config.getInstance().getBaseUrl()
            + Config.getInstance().getHandlerMapping().getPath(getClass());
        String desc = Messages.translate(getClass(), getDescriptionKey());
        return new HandlerDescription(desc, url);
    }
    
    /**
     * This method returns the key for I18N for the handler description.
     * Don't forget to add the description to the language files, when
     * implementing this method
     * @return
     *      I18N key for the description
     */
    protected abstract String getDescriptionKey();
    
    @SuppressWarnings("unchecked")
    protected void addMessage(String msg) {
        List<String> messages = (List<String>) exchange.getAttribute("messages");
        if(messages == null) {
            messages = new ArrayList<String>();
            exchange.setAttribute("messages", messages);
        }
        
        messages.add(msg);
    }
    
    @SuppressWarnings("unchecked")
    protected void addError(String msg) {
        List<String> errors = (List<String>) exchange.getAttribute("errors");
        if(errors == null) {
            errors = new ArrayList<String>();
            exchange.setAttribute("errors", errors);
        }
        
        errors.add(msg);
    }
    
    protected void addError(Exception e) {
        if(e.getMessage() != null && e.getMessage().length() > 0) {
            addError(e.getMessage());
        } else {
            addError(StringUtils.stackTraceToString(e));
        }
    }
}