package de.berlios.vch.parser.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IParserService;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;
import de.berlios.vch.web.IWebAction;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class BrowseServlet extends BundleContextServlet {

    public static final String PATH = "/parser";
    
    public static final String STATIC_PATH = PATH + "/static";
    
    private IParserService parserService;
    
    public BrowseServlet(IParserService parserService) {
        this.parserService = parserService;
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        super.setBundleContext(bundleContext);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String parserId = req.getParameter("id");
        IWebParser parser = parserService.getParser(parserId);
        if (parser != null) {
            if ("XMLHttpRequest".equals(req.getHeader("X-Requested-With"))) {
                try {
                    URI vchpage = new URI(req.getParameter("uri"));
                    IWebPage parsedPage = parserService.parse(vchpage);

                    if (parsedPage != null) {
                        String response = "";
                        if (parsedPage instanceof IOverviewPage) {
                            IOverviewPage overview = (IOverviewPage) parsedPage;
                            response = toJSON(overview.getPages());
                        } else {
                            response = toJSON(parsedPage);
                            String actions = actionsToJSON(getWebActions(), parsedPage);
                            
                            response = "{\"video\":" + response + "," 
                                + "\"actions\":" + actions + "}";

                            logger.log(LogService.LOG_INFO, getWebActions().size() + " web actions available");
                            logger.log(LogService.LOG_DEBUG, actions);
                        }
                        resp.setContentType("application/json; charset=utf-8");
                        resp.getWriter().println(response);
                    } else {
                        resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                        resp.getWriter().print("Couldn't load page");
                    }
                } catch (NoSupportedVideoFoundException e) {
                    logger.log(LogService.LOG_WARNING, "Couldn't load page: " + e.getLocalizedMessage());
                    String msg = i18n.translate("no_supported_video_format");
                    error(resp, HttpServletResponse.SC_PRECONDITION_FAILED, msg, true);
                } catch (Exception e) {
                    logger.log(LogService.LOG_ERROR, "Couldn't load page", e);
                    error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage(), true);
                }
            } else {
                logger.log(LogService.LOG_INFO, "Using "+parser.getTitle()+" parser ["+parserId+"]");
                Map<String, Object> params = new HashMap<String, Object>();
                params.put("TITLE", parser.getTitle());
                params.put("SERVLET_URI", req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                        + req.getServletPath());
                params.put("PARSER", parserId);
                
                // add css and javascript for the treeview and for log console
                List<String> css = new ArrayList<String>();
                css.add(BrowseServlet.STATIC_PATH + "/jstree/themes/themeroller/style.css");
                css.add(BrowseServlet.STATIC_PATH + "/parser.css");
                params.put("CSS_INCLUDES", css);
                List<String> js = new ArrayList<String>();
                js.add(BrowseServlet.STATIC_PATH + "/jstree/jquery.tree.js");
                js.add(BrowseServlet.STATIC_PATH + "/jstree/plugins/jquery.tree.themeroller.js");
                params.put("JS_INCLUDES", js);
                
                try {
                    IOverviewPage page = new OverviewPage();
                    page.setTitle(parser.getTitle());
                    page.setParser(parserId);
                    page.setUri(new URI("vchpage://localhost/"+parserId));
                    page.setVchUri(new URI("vchpage://localhost/"+parserId));
                    params.put("PAGE", page);
                } catch (Exception e) {
                    logger.log(LogService.LOG_ERROR, "Couldn't parse root page", e);
                    error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Couldn't parse root page", e);
                    return;
                }
                
                String page = templateLoader.loadTemplate("parser.ftl", params);
                resp.getWriter().print(page);
            }
        } else {
            logger.log(LogService.LOG_ERROR, "Parser with id "+parserId+" is not available");
            error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Parser with id " + parserId + " is not available", 
                    "XMLHttpRequest".equals(req.getHeader("X-Requested-With")));
        }
    }


    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }

    private String toJSON(IWebPage page) throws JSONException {
        // create the data object
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("title", page.getTitle());
        
        // create the attributes object
        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put("id", page.getVchUri());
        
        // set the title
        Map<String, Object> object = new HashMap<String, Object>();
        object.put("data", data);
        object.put("attributes", attributes);
        if(page instanceof IOverviewPage) {
            object.put("state", "closed");
        }
        
        if (page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            if(vpage.getVideoUri() != null) attributes.put("vchvideo", vpage.getVideoUri().toString());
            if(vpage.getUri() != null) attributes.put("vchlink", vpage.getUri().toString());
            if(vpage.getDescription() != null) attributes.put("vchdesc", vpage.getDescription());
            if(vpage.getThumbnail() != null) attributes.put("vchthumb", vpage.getThumbnail().toString());
            if(vpage.getPublishDate() != null) attributes.put("vchpubDate", vpage.getPublishDate().getTimeInMillis());
            if(vpage.getDuration() > 0) attributes.put("vchduration", vpage.getDuration());
            attributes.put("vchisLeaf", true);
            
        }
        return new JSONObject(object).toString();
    }

    private String toJSON(List<IWebPage> pages) throws JSONException {
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

    private String actionsToJSON(List<IWebAction> webActions, IWebPage page) throws UnsupportedEncodingException {
        if (!webActions.isEmpty()) {
            String json = "[";
            for (Iterator<IWebAction> iterator = webActions.iterator(); iterator.hasNext();) {
                IWebAction action = iterator.next();
                json += toJSON(action, page);
                if (iterator.hasNext()) {
                    json += ", ";
                }
            }
            return json += "]";
        } else {
            return "[]";
        }
    }
    
    private String toJSON(IWebAction action, IWebPage page) throws UnsupportedEncodingException {
        Map<String, Object> object = new HashMap<String, Object>();
        object.put("title", action.getTitle());
        object.put("uri", action.getUri(page));
        return new JSONObject(object).toString();
    }
    
    private List<IWebAction> getWebActions() {
        List<IWebAction> actions = new LinkedList<IWebAction>();
        
        ServiceTracker actionsTracker = new ServiceTracker(bundleContext, IWebAction.class.getName(), null); 
        actionsTracker.open();
        Object[] services = actionsTracker.getServices();
        actionsTracker.close();
        
        if(services != null) {
            for (Object object : services) {
                IWebAction action = (IWebAction) object;
                actions.add((IWebAction) action);
            }
        }
        
        return actions;
    }
}
