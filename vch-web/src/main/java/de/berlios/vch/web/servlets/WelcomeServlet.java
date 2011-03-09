package de.berlios.vch.web.servlets;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.json.JSONObject;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.web.TemplateLoader;

@Component
public class WelcomeServlet extends VchHttpServlet {

    public static final String PATH = "/vch";

    @Requires(filter = "(instance.name=vch.web)")
    private ResourceBundleProvider rbp;

    @Requires
    private TemplateLoader templateLoader;

    @Requires
    private HttpService httpService;

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (req.getParameter("news") != null) {
            // load news
            try {
                SyndFeed feed = loadNewsFeed();
                resp.setContentType("application/json; charset=utf-8");
                resp.getWriter().print('[');
                for (Iterator<?> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
                    SyndEntry entry = (SyndEntry) iterator.next();
                    if (entry.getCategories().contains("VCH")) {
                        JSONObject json = new JSONObject();
                        json.put("title", entry.getTitle());
                        json.put("date", entry.getPublishedDate());
                        json.put("text", entry.getDescription().getValue());
                        json.put("link", entry.getLink());
                        resp.getWriter().print(json.toString());
                        if (iterator.hasNext()) {
                            resp.getWriter().print(',');
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
    }

    @Invalidate
    public void stop() {
        // unregister the servlet
        httpService.unregister(PATH);
    }

    private SyndFeed loadNewsFeed() throws IllegalArgumentException, MalformedURLException, FeedException, IOException {
        SyndFeedInput input = new SyndFeedInput();
        SyndFeed feed = input.build(new XmlReader(new URL("http://www.hampelratte.org/blog/?feed=rss2")));
        return feed;
    }

}
