package de.berlios.vch.parser.rss;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.htmlparser.util.ParserException;
import org.json.JSONObject;
import org.osgi.service.log.LogService;

import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.parser.rss.VdrWikiSuggestions.Feed;
import de.berlios.vch.parser.rss.VdrWikiSuggestions.Group;
import de.berlios.vch.rss.RssParser;
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class ConfigServlet extends BundleContextServlet {

    public static String PATH = "/config/parser/rss";

    private RssFeedParser parser;

    public ConfigServlet(RssFeedParser parser) {
        this.parser = parser;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();

        if (req.getParameter("add_feed") != null) {
            String feedUri = req.getParameter("feed");
            addFeed(feedUri, req);
        } else if (req.getParameter("add_feeds") != null) {
            String[] feeds = req.getParameterValues("feeds");
            if (feeds != null) {
                for (String feedUri : feeds) {
                    addFeed(feedUri, req);
                }
            }
        } else if (req.getParameter("remove_feeds") != null) {
            String[] feeds = req.getParameterValues("feeds");
            if (feeds != null) {
                for (String id : feeds) {
                    parser.removeFeed(id);
                }
            }
        } else if (req.getParameter("get_suggestions") != null) {
            try {
                List<Group> groups = VdrWikiSuggestions.loadSuggestions();
                resp.setContentType("application/json; charset=utf-8");
                resp.getWriter().write(toJSON(groups));
            } catch (ParserException e) {
                logger.log(LogService.LOG_ERROR, "Couldn't load list of suggestions", e);
                error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't load list of suggestions", true);
            }
            return;
        }

        params.put("TITLE", i18n.translate("I18N_RSS_CONFIG"));
        params.put("SERVLET_URI", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                + req.getServletPath());
        params.put("FEEDS", parser.getFeeds());
        params.put("ACTION", PATH);
        params.put("NOTIFY_MESSAGES", getNotifyMessages(req));

        String page = templateLoader.loadTemplate("configRss.ftl", params);
        resp.getWriter().print(page);
    }

    private void addFeed(String feedUri, HttpServletRequest req) {
        try {
            SyndFeed feed = RssParser.parseUri(feedUri);
            parser.addFeed(feed.getTitle(), feedUri);
            addNotify(req, new NotifyMessage(TYPE.INFO, i18n.translate("I18N_FEED_ADDED")));
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't parse feed", e);
            addNotify(req, new NotifyMessage(TYPE.ERROR, i18n.translate("I18N_ERROR_COULDNT_PARSE_FEED"), e));
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    private String toJSON(List<Group> groups) {
        if (!groups.isEmpty()) {
            String json = "[";
            for (Iterator<Group> iterator = groups.iterator(); iterator.hasNext();) {
                Group group = iterator.next();
                json += groupToJSON(group);
                if (iterator.hasNext()) {
                    json += ", ";
                }
            }
            return json += "]";
        } else {
            return "[]";
        }
    }

    private String groupToJSON(Group group) {
        Map<String, Object> object = new HashMap<String, Object>();
        object.put("title", group.title);
        object.put("feeds", feedsToJSON(group.feeds));
        return new JSONObject(object).toString();
    }

    private List<JSONObject> feedsToJSON(List<Feed> feeds) {
        List<JSONObject> result = new ArrayList<JSONObject>();
        if (!feeds.isEmpty()) {
            for (Iterator<Feed> iterator = feeds.iterator(); iterator.hasNext();) {
                Feed feed = iterator.next();
                Map<String, String> feedMap = new HashMap<String, String>();
                feedMap.put("title", feed.title);
                feedMap.put("uri", feed.uri);
                result.add(new JSONObject(feedMap));
            }
        }
        return result;
    }
}
