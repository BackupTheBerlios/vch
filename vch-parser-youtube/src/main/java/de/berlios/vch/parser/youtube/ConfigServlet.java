package de.berlios.vch.parser.youtube;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.log.LogService;

import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.rss.RssParser;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class ConfigServlet extends BundleContextServlet {

    public static String PATH = "/config/parser/youtube";
    
    private YoutubeParser parser;
    
    private Preferences prefs;
    
    public ConfigServlet(YoutubeParser parser, Preferences prefs) {
        this.parser = parser;
        this.prefs = prefs;
    }
    
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();
        
        if(req.getParameter("add_feed") != null) {
            String feedUri = req.getParameter("feed");
            try {
                SyndFeed feed = RssParser.parseUri(feedUri);
                parser.addFeed(feed.getTitle(), feedUri);
            } catch (Exception e) {
                logger.log(LogService.LOG_ERROR, "Couldn't parse feed", e);
                error(resp, HttpServletResponse.SC_BAD_REQUEST, "Couldn't parse feed");
                return;
            }
        } else if(req.getParameter("remove_feeds") != null) {
            String[] feeds = req.getParameterValues("feeds");
            if(feeds != null) {
                for (String id : feeds) {
                    parser.removeFeed(id);
                }
            }
        } else if(req.getParameter("save_config") != null) {
            prefs.put("video.quality", req.getParameter("quality"));
        }
        
        params.put("TITLE", i18n.translate("I18N_YOUTUBE_CONFIG"));
        params.put("SERVLET_URI", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                + req.getServletPath());
        params.put("FEEDS", parser.getFeeds());
        params.put("ACTION", PATH);
        params.put("QUALITY", prefs.getInt("video.quality", 34));
        
        String page = templateLoader.loadTemplate("configYoutube.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

}
