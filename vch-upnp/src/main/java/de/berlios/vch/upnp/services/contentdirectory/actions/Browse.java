package de.berlios.vch.upnp.services.contentdirectory.actions;

import java.net.URI;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IParserService;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
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
    private ServiceTracker parserTracker = new ServiceTracker(ctx, IParserService.class.getName(), null);
    private ServiceTracker i18nTracker = new ServiceTracker(ctx, Messages.class.getName(), null);
    private ServiceTracker protos = new ServiceTracker(ctx, INetworkProtocol.class.getName(), null);;
    
    private long updateId = 0;
    
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
        protos.open();
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
    public Dictionary invoke(Dictionary args) throws Exception {
        String objectId = (String) args.get("ObjectID");
        IParserService parserService = (IParserService) parserTracker.getService();
        if(parserService == null) {
            throw new ServiceException("ParserService not available");
        }

        // TODO implement the following params? are there devices, which use these params?
//        String brosweFlag = (String) args.get("BrowseFlag");
//        String filter = (String) args.get("Filter");
//        String start = (String) args.get("StartingIndex");
//        String count = (String) args.get("RequestedCount");
//        String sort = (String) args.get("SortCriteria");
        
        if("0".equals(objectId)) {
            objectId = "vchpage://localhost";
        }
        
        URI vchpage = new URI(objectId);
        IWebPage page = parserService.parse(vchpage);
        
        Hashtable<String, Object> result = new Hashtable<String, Object>();
        result.put("UpdateID", updateId++);
        
        if(page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            result.put("Result", XmlRenderer.renderOverview(opage, objectId));
            result.put("NumberReturned", opage.getPages().size());
            result.put("TotalMatches", opage.getPages().size());
        } else {
            IVideoPage videoPage = (IVideoPage) page;
            URI video = videoPage.getVideoUri();
            Object[] protocols = protos.getServices();
            for (Object object : protocols) {
                INetworkProtocol proto = (INetworkProtocol) object;
                String scheme = video.getScheme();
                if(proto.getSchemes().contains(scheme)) {
                    if(proto.isBridgeNeeded()) {
                        video = proto.toBridgeUri(video, page.getUserData());
                    }
                }
            }
            
            result.put("Result", XmlRenderer.renderVideo(videoPage, video, objectId));
            result.put("NumberReturned", 1);
            result.put("TotalMatches", 1);
        }

        return result;
    }
}
