package de.berlios.vch.upnp.services.contentdirectory.actions;

import java.net.URI;
import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.cache.Cache;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.WebPageTitleComparator;
import de.berlios.vch.upnp.Activator;
import de.berlios.vch.upnp.XmlRenderer;
import de.berlios.vch.upnp.services.contentdirectory.variables.BrowseFlag;
import de.berlios.vch.upnp.services.contentdirectory.variables.Count;
import de.berlios.vch.upnp.services.contentdirectory.variables.Filter;
import de.berlios.vch.upnp.services.contentdirectory.variables.Index;
import de.berlios.vch.upnp.services.contentdirectory.variables.ObjectID;
import de.berlios.vch.upnp.services.contentdirectory.variables.Result;
import de.berlios.vch.upnp.services.contentdirectory.variables.SortCriteria;
import de.berlios.vch.upnp.services.contentdirectory.variables.UpdateID;

public class Browse implements UPnPAction {

    private static transient Logger logger = LoggerFactory.getLogger(Browse.class);
    
    // variables
    private ObjectID objectID = new ObjectID();
    private BrowseFlag browseFlag = new BrowseFlag();
    private Filter filter = new Filter();
    private Index index = new Index();
    private Count count = new Count();
    private SortCriteria sortCriteria = new SortCriteria();
    private Result result = new Result();
    private UpdateID updateID = new UpdateID();
    private Map<String, UPnPStateVariable> variables = new HashMap<String, UPnPStateVariable>();
    
    private BundleContext ctx = Activator.context;
    private ServiceTracker parserTracker = new ServiceTracker(ctx, IWebParser.class.getName(), null);
    private ServiceTracker i18nTracker = new ServiceTracker(ctx, Messages.class.getName(), null);
    
    private long updateId = 0;
    
    private Cache<String, IWebPage> cache = new Cache<String, IWebPage>(1000, 5, TimeUnit.MINUTES);
    
    public Browse() {
        variables.put("ObjectID", objectID);
        variables.put("BrowseFlag", browseFlag);
        variables.put("Filter", filter);
        variables.put("StartingIndex", index);
        variables.put("RequestedCount", count);
        variables.put("SortCriteria", sortCriteria);
        variables.put("Result", result);
        variables.put("NumberReturned", count);
        variables.put("TotalMatches", count);
        variables.put("UpdateID", updateID);
        
        parserTracker.open();
        i18nTracker.open(); 
    }
    
    @Override
    public String[] getInputArgumentNames() {
        return new String[] {"ObjectID", "BrowseFlag", "Filter", "StartingIndex", "RequestedCount", "SortCriteria"};
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String[] getOutputArgumentNames() {
        return new String[] {"Result", "NumberReturned", "TotalMatches", "UpdateID"};
    }

    @Override
    public String getReturnArgumentName() {
        return null;
    }

    @Override
    public UPnPStateVariable getStateVariable(String argumentName) {
        return variables.get(argumentName);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Dictionary invoke(Dictionary args) throws Exception {
        logger.debug("Browse({})", args);
        String objectId = (String) args.get("ObjectID");

        // TODO implement the following params? are there devices, which use these params?
//        String brosweFlag = (String) args.get("BrowseFlag");
//        String filter = (String) args.get("Filter");
//        String start = (String) args.get("StartingIndex");
//        String count = (String) args.get("RequestedCount");
//        String sort = (String) args.get("SortCriteria");
        
        IWebPage page = null;
        if("0".equals(objectId)) {
            objectId = "vchpage://localhost";
            page = getParsers();
        } else {
            URI vchpage = new URI(objectId);
            String path = vchpage.getPath();
            
            // lookup page in cache 
            page = lookup(path);
            
            if(page != null) {
                // parse the page, if it is not the root page of the parser
                String rootUri = "vchpage://localhost/" + page.getParser();
                if(rootUri.equals(page.getUri().toString())) {
                    page = getParser(page.getParser()).getRoot();
                } else {
                    page = parsePage(page);
                }
            } else {
                String msg = "Page not found in cache";
                logger.error(msg);
                throw new Exception(msg);
            }
        }
        
        Hashtable<String, Object> result = new Hashtable<String, Object>();
        result.put("UpdateID", updateId++);
        
        if(!page.getUri().getScheme().equals("vchpage")) {
            cache.put(XmlRenderer.md5(page.getUri().toString()), page);
        }
        
        if(page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            // add pages to cache
            for (IWebPage subpage : opage.getPages()) {
                if(!subpage.getUri().getScheme().equals("vchpage")) {
                    cache.put(XmlRenderer.md5(subpage.getUri().toString()), subpage);
                }
            }
            result.put("Result", XmlRenderer.renderOverview(opage, objectId));
            result.put("NumberReturned", opage.getPages().size());
            result.put("TotalMatches", opage.getPages().size());
        } else {
            result.put("Result", XmlRenderer.renderVideo((IVideoPage) page, objectId));
            result.put("NumberReturned", 1);
            result.put("TotalMatches", 1);
        }

        logger.debug("Returning {}", result);
        return result;
    }

    private IWebPage lookup(String path) throws Exception {
        if(!path.isEmpty()) {
            Scanner scanner = new Scanner(path).useDelimiter("/");
            String parserId = scanner.next();
            IWebParser parser = getParser(parserId);
            IWebPage parent = null;
            if(scanner.hasNext()) {
                while(scanner.hasNext()) {
                    String md5Uri = scanner.next();
                    IWebPage page = cache.get(md5Uri);
                    if(page == null) {
                        // the current page is not in the cache, we have to 
                        // parse the parent page and then add it to the cache
                        if(parent == null) {
                            parent = parser.getRoot();
                        } else {
                            parent = parser.parse(parent);
                        }
                        // we have parsed the parent page, now we can add all
                        // subpages to the cache. the desired page will then be
                        // in the cache, too
                        if(parent instanceof IOverviewPage) {
                            IOverviewPage opage = (IOverviewPage) parent;
                            for (IWebPage subpage : opage.getPages()) {
                                cache.put(XmlRenderer.md5(subpage.getUri().toString()), subpage);
                            }
                            
                            // now we can retrieve the desired page from the cache
                            page = cache.get(md5Uri);
                        } else {
                            throw new Exception("Parent page "+md5Uri+" is part of the path, but seems to be an IVideoPage");
                        }
                    } 
    
                    // we have found the page. if it is the last element in
                    // the path, we can return it, otherwise we have to continue
                    // with the next part
                    if(scanner.hasNext()) {
                        parent = page;
                    } else {
                        return page;
                    }
                }
            } else {
                IWebPage page = cache.get(parserId);
                if(page == null) {
                    page = parser.getRoot();
                }
                return page;
            }
        }
        return null;
    }

    private IWebPage parsePage(IWebPage page) throws Exception {
        IWebParser parser = getParser(page.getParser());
        IWebPage parsedPage = null;
        if(page.getUri().getPath().equalsIgnoreCase(parser.getId())) {
            parsedPage = parser.getRoot();
        } else {
            parsedPage = parser.parse(page);
        }
        parsedPage.setParser(parser.getId());
        return parsedPage;
    }
    
    public IWebParser getParser(String id) {
        Object[] parsers = parserTracker.getServices();
        for (Object o : parsers) {
            IWebParser parser = (IWebParser) o;
            if (parser.getId().equals(id)) {
                return parser;
            }
        }

        return null;
    }

    private IOverviewPage getParsers() throws Exception {
        Object[] parsers = parserTracker.getServices();
        IOverviewPage overview = new OverviewPage();
        overview.setUri(new URI("vchpage://localhost"));
        Messages i18n = (Messages) i18nTracker.getService();
        overview.setTitle(i18n.translate("sites"));
        cache.put("localhost", overview);
        if (parsers != null && parsers.length > 0) {
            for (Object o : parsers) {
                IWebParser parser = (IWebParser) o;
                IOverviewPage parserPage = new OverviewPage();
                parserPage.setTitle(parser.getTitle());
                parserPage.setUri(new URI("vchpage://localhost/"+parser.getId()));
                parserPage.setParser(parser.getId());
                overview.getPages().add(parserPage);
                cache.put(parser.getId(), parserPage);
            }
        }
        
        Collections.sort(overview.getPages(), new WebPageTitleComparator());
        return overview;
    }
}
