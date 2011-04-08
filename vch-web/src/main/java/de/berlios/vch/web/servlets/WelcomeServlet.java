package de.berlios.vch.web.servlets;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.sun.syndication.feed.synd.SyndCategory;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.WebInterface;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;

@Component
public class WelcomeServlet extends VchHttpServlet {

    public static final String PATH = "/news";

    @Requires(filter = "(instance.name=vch.web)")
    private ResourceBundleProvider rbp;

    @Requires
    private TemplateLoader templateLoader;

    @Requires
    private HttpService httpService;

    private BundleContext ctx;

    public WelcomeServlet(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("news") != null) {
            // load news
            try {
                SyndFeed feed = loadNewsFeed();
                resp.setContentType("application/json; charset=utf-8");
                resp.getWriter().print('[');
                boolean first = true;
                for (Iterator<?> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
                    SyndEntry entry = (SyndEntry) iterator.next();
                    List<?> categories = entry.getCategories();
                    for (Iterator<?> cats = categories.iterator(); cats.hasNext();) {
                        SyndCategory category = (SyndCategory) cats.next();
                        if ("vch".equalsIgnoreCase(category.getName())) {
                            if (!first) {
                                resp.getWriter().print(',');
                            }
                            JSONObject json = new JSONObject();
                            json.put("title", entry.getTitle());
                            json.put("date", entry.getPublishedDate());
                            json.put("text", entry.getDescription().getValue());
                            json.put("link", entry.getLink());
                            resp.getWriter().print(json.toString());
                            first = false;
                            break;
                        }
                    }
                }
                resp.getWriter().print(']');
            } catch (Exception e) {
                throw new ServletException("Couldn't load news feed", e);
            }
        } else {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("TITLE", rbp.getResourceBundle().getString("I18N_WELCOME"));
            params.put("SERVLET_URI", PATH);

            // add additional css
            List<String> css = new ArrayList<String>();
            css.add(WebInterface.STATIC_PATH + "/news.css");
            params.put("CSS_INCLUDES", css);

            String page = templateLoader.loadTemplate("welcome.ftl", params);
            resp.getWriter().print(page);
        }
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    @Validate
    public void start() throws ServletException, NamespaceException {
        // register the servlet
        httpService.registerServlet(PATH, this, null, null);

        registerMenu();
    }

    private void registerMenu() {
        // register web interface menu
        WebMenuEntry news = new WebMenuEntry();
        news.setTitle(rbp.getResourceBundle().getString("I18N_NEWS"));
        news.setPreferredPosition(Integer.MIN_VALUE);
        news.setLinkUri(WelcomeServlet.PATH);
        ServiceRegistration sr = ctx.registerService(IWebMenuEntry.class.getName(), news, null);
        serviceRegs.add(sr);
    }

    @Invalidate
    public void stop() {
        // unregister the servlet
        httpService.unregister(PATH);

        // unregister the menu entry
        unregisterServices();
    }

    private SyndFeed loadNewsFeed() throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL("http://www.hampelratte.org/blog/?feed=rss2")));
        return feed;
    }

}
