package de.berlios.vch.search.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPageTitleComparator;
import de.berlios.vch.search.ISearchService;
import de.berlios.vch.web.IWebAction;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class SearchServlet extends BundleContextServlet {

    public static final String PATH = "/search";

	public static final String STATIC_PATH = PATH + "/static";
    
    private Activator activator;
    
    public SearchServlet(Activator activator) {
        this.activator = activator;
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
    	Map<String, Object> params = new HashMap<String, Object>();
    	params.put("TITLE", i18n.translate("I18N_SEARCH"));
    	params.put("ACTION", PATH);
    	params.put("NOTIFY_MESSAGES", getNotifyMessages(req));
    	List<String> css = new ArrayList<String>();
        css.add(SearchServlet.STATIC_PATH + "/search.css");
        params.put("CSS_INCLUDES", css);
        
    	String action = req.getParameter("action");
        if("search".equals(action)) {
        	// execute the search
        	search(params, req, resp);
        } else if("parse".equals(action)) {
        	try {
				IWebPage page = parse(req, resp);
				if(page instanceof IVideoPage) {
					resp.setContentType("application/json; charset=utf-8");
					String video = toJSON(page);
					String actions = actionsToJSON(getWebActions(), page);
					String response = "{\"video\":" + video + ",\"actions\":" + actions + "}";
		    		resp.getWriter().print(response);
		    	} else {
		    		resp.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
		    		resp.setContentType("text/plain");
		    		resp.getWriter().print("Nested search results are not yet implemented");
		    	}
				return;
			} catch (Exception e) {
				throw new ServletException(e);
			}
        } 

        // render the page
        String page = templateLoader.loadTemplate("search.ftl", params);
        resp.getWriter().print(page);
    }
    
    private String toJSON(IWebPage page) {
    	Map<String, Object> video = new HashMap<String, Object>();
    	video.put("title", page.getTitle());
    	video.put("id", page.getVchUri());
                
        if (page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            if(vpage.getVideoUri() != null) video.put("vchvideo", vpage.getVideoUri().toString());
            if(vpage.getUri() != null) video.put("vchlink", vpage.getUri().toString());
            if(vpage.getDescription() != null) video.put("vchdesc", vpage.getDescription());
            if(vpage.getThumbnail() != null) video.put("vchthumb", vpage.getThumbnail().toString());
            if(vpage.getPublishDate() != null) video.put("vchpubDate", vpage.getPublishDate().getTimeInMillis());
            if(vpage.getDuration() > 0) video.put("vchduration", vpage.getDuration());
            video.put("vchisLeaf", true);
        }
        return new JSONObject(video).toString();
    }

    private IWebPage parse(HttpServletRequest req, HttpServletResponse resp) throws Exception {
    	// check, if the search service is available
    	ISearchService searchService = activator.getSearchService();
    	if(searchService == null) {
    		error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, activator.getResourceBundle().getString("I18N_SEARCH_SERVICE_MISSING"));
    		return null;
    	}
    	
    	String id = req.getParameter("id");
    	String uri = req.getParameter("uri");
    	String isVideoPage = req.getParameter("isVideoPage");
    	IWebPage page;
    	if("true".equals(isVideoPage)) {
    		page = new VideoPage();
    	} else {
    		page = new OverviewPage();
    	}
    	page.setParser(id);
    	page.setUri(new URI(uri));
    	return searchService.parse(page);
	}

	private void search(Map<String, Object> params, HttpServletRequest req, HttpServletResponse resp) throws IOException {
    	String q = req.getParameter("q");
    	params.put("Q", q);
    	
    	// check, if the search service is available
    	ISearchService searchService = activator.getSearchService();
    	if(searchService == null) {
    		error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, activator.getResourceBundle().getString("I18N_SEARCH_SERVICE_MISSING"));
    		return;
    	}
    	
    	// use the search service to search with different providers
    	IOverviewPage results = searchService.search(q);
    	try {
    		Collections.sort(results.getPages(), new WebPageTitleComparator());
    	} catch (Exception e1) {
    		logger.log(LogService.LOG_WARNING, "Couldn't sort providers by name", e1);
    	}
    	
    	params.put("RESULTS", results);
    	try {
    		int resultCount = 0;
    		for (IWebPage page : results.getPages()) {
    			if(page instanceof IOverviewPage) {
    				resultCount += ((IOverviewPage)page).getPages().size();
    			}
    		}
    		params.put("COUNT", resultCount);
    	} catch(Exception e) {
    		logger.log(LogService.LOG_ERROR, "Couldn't determine search result count", e);
    	}
	}

	@Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        post(req, resp);
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
	private String actionsToJSON(List<IWebAction> webActions, IWebPage page) throws UnsupportedEncodingException, URISyntaxException {
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
    
    private String toJSON(IWebAction action, IWebPage page) throws UnsupportedEncodingException, URISyntaxException {
        Map<String, Object> object = new HashMap<String, Object>();
        object.put("title", action.getTitle());
        object.put("uri", action.getUri(page));
        return new JSONObject(object).toString();
    }
}
