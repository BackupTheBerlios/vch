package de.berlios.vch.parser.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPage;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;
import de.berlios.vch.web.servlets.BundleContextServlet;

// TODO videos tatsächlich erst bei bedarf parsen und in json objekte umwandeln
public class BrowseServlet extends BundleContextServlet {

    public static String PATH = "/parser";
    
    private ServiceTracker st;

    private static transient Logger logger = LoggerFactory.getLogger(BrowseServlet.class);

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        super.setBundleContext(bundleContext);
        st = new ServiceTracker(bundleContext, IWebParser.class.getName(), null);
        st.open();
    }

    public IWebParser getParser(String id) {
        Object[] parsers = st.getServices();
        for (Object o : parsers) {
            IWebParser parser = (IWebParser) o;
            if (parser.getId().equals(id)) {
                return parser;
            }
        }

        return null;
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String parserId = req.getParameter("id");
        IWebParser parser = getParser(parserId);
        if (parser != null) {
            if ("XMLHttpRequest".equals(req.getHeader("X-Requested-With"))) {
                try {
                    IWebPage page = createPage(req);
                    page.setParser(parser.getId());
                    page.setUri(new URI(req.getParameter("uri")));
                    IWebPage parsedPage = null;
                    if("vchpage".equals(page.getUri().getScheme())) {
                        parsedPage = parser.getRoot();
                    } else {
                        parsedPage = parser.parse(page);
                    }

                    if (parsedPage != null) {
                        String response = "{\"ResultSet\":{\"Result\":";
                        if (parsedPage instanceof IOverviewPage) {
                            IOverviewPage overview = (IOverviewPage) parsedPage;
                            response += toJSON(overview.getPages());
                        } else {
                            response += toJSON(parsedPage);
                        }
                        response += "}}";
                        resp.setContentType("application/json; charset=utf-8"); // TODO config param?
                        resp.getWriter().println(response);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().print("Couldn't load page");
                    }
                } catch (NoSupportedVideoFoundException e) {
                    logger.warn("Couldn't load page: {}", e.getLocalizedMessage());
                    String msg = i18n.translate("no_supported_video_format");
                    error(resp, HttpServletResponse.SC_PRECONDITION_FAILED, msg, true);
                } catch (Exception e) {
                    logger.error("Couldn't load page", e);
                    error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), true);
                }
            } else {
                logger.info("Using {} parser [{}]", parser.getTitle(), parserId);
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("TITLE", parser.getTitle());
                params.put("SERVLET_URI", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                        + req.getServletPath());
                params.put("PARSER", parserId);
                
                // add css and javascript for the treeview and for log console
                List<String> css = new ArrayList<String>();
                css.add("http://yui.yahooapis.com/2.7.0/build/treeview/assets/skins/sam/treeview.css");
                css.add("http://yui.yahooapis.com/2.7.0/build/logger/assets/skins/sam/logger.css");
                params.put("CSS_INCLUDES", css);
                List<String> js = new ArrayList<String>();
                js.add("http://yui.yahooapis.com/2.7.0/build/connection/connection-min.js");
                js.add("http://yui.yahooapis.com/2.7.0/build/treeview/treeview-min.js");
                js.add("http://yui.yahooapis.com/2.8.0r4/build/logger/logger-min.js");
                params.put("JS_INCLUDES", js);
                
                try {
                    // IOverviewPage page = parser.getRoot();
                    IOverviewPage page = new OverviewPage();
                    page.setTitle(parser.getTitle());
                    page.setUri(new URI("vchpage://root"));
                    page.setParser(parserId);
                    params.put("PAGE", page);
                } catch (Exception e) {
                    logger.error("Couldn't parse root page", e);
                    error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't parse root page", e);
                    return;
                }
                String page = templateLoader.loadTemplate("parser.ftl", params);

                resp.getWriter().print(page);
            }
        } else {
            logger.error("Parser with id {} is not available", parserId);
            error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Parser with id " + parserId + " is not available", 
                    "XMLHttpRequest".equals(req.getHeader("X-Requested-With")));
        }
    }

    @SuppressWarnings("unchecked")
    private IWebPage createPage(HttpServletRequest req) throws URISyntaxException {
        String type = req.getParameter("node.data.type");
        IWebPage page;
        if (IOverviewPage.class.getSimpleName().equals(type)) {
            page = new OverviewPage();
        } else if (IVideoPage.class.getSimpleName().equals(type)) {
            page = new VideoPage();
        } else {
            page = new WebPage();
        }
        Enumeration paramNames = req.getParameterNames();
        while(paramNames.hasMoreElements()) {
            String key = (String) paramNames.nextElement(); 
            if( key.startsWith("node.data.") ) {
                page.getUserData().put(key.substring(10), req.getParameter(key));
                if("video".equals(key.substring(10))) {
                    ((IVideoPage)page).setVideoUri(new URI(req.getParameter(key)));
                }
            }
        }
        page.setTitle(req.getParameter("title"));
        return page;
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    private String toJSON(IWebPage page) {
        Map<String, Object> object = new HashMap<String, Object>();
        // copy the values, which were transmitted from the browser
        for (Entry<String, Object> entry : page.getUserData().entrySet()) {
            object.put(entry.getKey(), entry.getValue().toString());
        }
        
        // set the title
        object.put("label", page.getTitle());
        if (page.getUri() != null) {
            object.put("href", page.getUri().toString());
        }
        
        if (page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            if(vpage.getVideoUri() != null) object.put("video", vpage.getVideoUri().toString());
            if(object.get("desc") == null && vpage.getDescription() != null) object.put("desc", vpage.getDescription());
            if(object.get("thumb") == null && vpage.getThumbnail() != null) object.put("thumb", vpage.getThumbnail().toString());
            if(object.get("pubDate") == null && vpage.getPublishDate() != null) object.put("pubDate", vpage.getPublishDate().getTimeInMillis());
            if(object.get("duration") == null && vpage.getDuration() > 0) object.put("duration", vpage.getDuration());
            object.put("isLeaf", true);
        }
        
        if (object.get("type") == null) {
            if(page instanceof IVideoPage) {
                object.put("type", IVideoPage.class.getSimpleName());
            } else if(page instanceof IOverviewPage) {
                object.put("type", IOverviewPage.class.getSimpleName());
            } else {
                object.put("type", IWebPage.class.getSimpleName());
            }
        }
        return new JSONObject(object).toString();
    }

    private String toJSON(List<IWebPage> pages) {
        if (!pages.isEmpty()) {
            String json = "[";
            for (Iterator<IWebPage> iterator = pages.iterator(); iterator.hasNext();) {
                IWebPage page = iterator.next();
                json += toJSON(page);
                if (iterator.hasNext()) {
                    json += ", ";
                }
            }
            return json += "]";
        } else {
            return "[]";
        }
    }
}
