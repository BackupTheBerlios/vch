package de.berlios.vch.parser.youtube;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.rss.RssParser;
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;
import de.berlios.vch.web.servlets.VchHttpServlet;

@Component
public class ConfigServlet extends VchHttpServlet {

    public static String PATH = "/config/parser/youtube";

    @Requires
    private FeedConfiguration feedConfig;

    @Requires(filter = "(instance.name=vch.parser.youtube)")
    private IWebParser parser;

    @Requires(filter = "(instance.name=vch.parser.youtube)")
    private ResourceBundleProvider rbp;

    @Requires
    private LogService logger;

    @Requires
    private TemplateLoader templateLoader;

    @Requires
    private HttpService httpService;

    @Requires
    private ConfigService cs;
    private Preferences prefs;

    private BundleContext ctx;

    private ServiceRegistration menuReg;

    public ConfigServlet(BundleContext ctx) {
        this.ctx = ctx;
    }

    public ConfigServlet(YoutubeParser parser, Preferences prefs) {
        this.feedConfig = parser;
        this.prefs = prefs;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Map<String, Object> params = new HashMap<String, Object>();

        if (req.getParameter("add_feed") != null) {
            String feedUri = req.getParameter("feed");
            try {
                SyndFeed feed = RssParser.parseUri(feedUri);
                feedConfig.addFeed(feed.getTitle(), feedUri);
                addNotify(req, new NotifyMessage(TYPE.INFO, rbp.getResourceBundle().getString("I18N_FEED_ADDED")));
            } catch (Exception e) {
                logger.log(LogService.LOG_ERROR, rbp.getResourceBundle().getString("I18N_ERROR_COULDNT_PARSE_FEED"), e);
                addNotify(req,
                        new NotifyMessage(TYPE.ERROR, rbp.getResourceBundle()
                                .getString("I18N_ERROR_COULDNT_PARSE_FEED"), e));
            }
        } else if (req.getParameter("remove_feeds") != null) {
            String[] feeds = req.getParameterValues("feeds");
            if (feeds != null) {
                for (String id : feeds) {
                    feedConfig.removeFeed(id);
                }
            }
        } else if (req.getParameter("save_config") != null) {
            prefs.put("video.quality", req.getParameter("quality"));
            prefs.put("user", req.getParameter("own_account"));
        }

        params.put("TITLE", rbp.getResourceBundle().getString("I18N_YOUTUBE_CONFIG"));
        params.put("SERVLET_URI",
                req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort() + req.getServletPath());
        params.put("FEEDS", feedConfig.getFeeds());
        params.put("ACTION", PATH);
        params.put("QUALITY", prefs.getInt("video.quality", 34));
        params.put("OWN_ACCOUNT", prefs.get("user", ""));
        params.put("NOTIFY_MESSAGES", getNotifyMessages(req));

        String page = templateLoader.loadTemplate("configYoutube.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    @Validate
    public void start() throws ServletException, NamespaceException {
        prefs = cs.getUserPreferences(ctx.getBundle().getSymbolicName());

        registerServlet();
    }

    private void registerServlet() throws ServletException, NamespaceException {
        // register the servlet
        httpService.registerServlet(PATH, this, null, null);

        // register web interface menu
        IWebMenuEntry menu = new WebMenuEntry(rbp.getResourceBundle().getString("I18N_BROWSE"));
        menu.setPreferredPosition(Integer.MIN_VALUE);
        menu.setLinkUri("#");
        SortedSet<IWebMenuEntry> childs = new TreeSet<IWebMenuEntry>();
        IWebMenuEntry entry = new WebMenuEntry();
        entry.setTitle(parser.getTitle());
        entry.setLinkUri("/parser?id=" + getClass().getName());
        childs.add(entry);
        menu.setChilds(childs);
        childs = new TreeSet<IWebMenuEntry>();
        IWebMenuEntry config = new WebMenuEntry();
        config.setTitle(rbp.getResourceBundle().getString("I18N_CONFIGURATION"));
        config.setLinkUri(ConfigServlet.PATH);
        config.setPreferredPosition(Integer.MAX_VALUE);
        childs.add(config);
        entry.setChilds(childs);
        menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
    }

    @Invalidate
    public void stop() {
        prefs = null;

        // unregister the config servlet
        if (httpService != null) {
            httpService.unregister(PATH);
        }

        // unregister the web menu
        if (menuReg != null) {
            menuReg.unregister();
        }
    }
}
