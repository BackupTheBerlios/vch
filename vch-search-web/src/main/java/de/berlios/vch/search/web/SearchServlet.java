package de.berlios.vch.search.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.log.LogService;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.WebPageTitleComparator;
import de.berlios.vch.search.ISearchService;
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
        } else if("show".equals(action)) {
        	
        } 

        // render the page
        String page = templateLoader.loadTemplate("search.ftl", params);
        resp.getWriter().print(page);
    }

    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        post(req, resp);
    }
}
