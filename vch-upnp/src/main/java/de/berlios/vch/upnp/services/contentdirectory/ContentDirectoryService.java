package de.berlios.vch.upnp.services.contentdirectory;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

import de.berlios.vch.upnp.services.contentdirectory.actions.Browse;
import de.berlios.vch.upnp.services.contentdirectory.actions.GetSearchCapabilities;
import de.berlios.vch.upnp.services.contentdirectory.actions.GetSortCapabilities;
import de.berlios.vch.upnp.services.contentdirectory.actions.GetSystemUpdateID;
import de.berlios.vch.upnp.services.contentdirectory.variables.BrowseFlag;
import de.berlios.vch.upnp.services.contentdirectory.variables.ContainerUpdateIDs;
import de.berlios.vch.upnp.services.contentdirectory.variables.Count;
import de.berlios.vch.upnp.services.contentdirectory.variables.Filter;
import de.berlios.vch.upnp.services.contentdirectory.variables.Index;
import de.berlios.vch.upnp.services.contentdirectory.variables.ObjectID;
import de.berlios.vch.upnp.services.contentdirectory.variables.Result;
import de.berlios.vch.upnp.services.contentdirectory.variables.SearchCapabilities;
import de.berlios.vch.upnp.services.contentdirectory.variables.SortCapabilities;
import de.berlios.vch.upnp.services.contentdirectory.variables.SortCriteria;
import de.berlios.vch.upnp.services.contentdirectory.variables.SystemUpdateID;
import de.berlios.vch.upnp.services.contentdirectory.variables.UpdateID;

public class ContentDirectoryService implements UPnPService {

    private static final String ID = "urn:schemas-upnp-org:serviceId:ContentDirectory";
    private static final String TYPE = "urn:schemas-upnp-org:service:ContentDirectory:1";
    
    // actions
    private Browse browse = new Browse();
    private GetSearchCapabilities getSearchCapabilities = new GetSearchCapabilities();
    private GetSortCapabilities getSortCapabilities = new GetSortCapabilities();
    private GetSystemUpdateID getSystemUpdateID = new GetSystemUpdateID();
    private Map<String, UPnPAction> actions = new HashMap<String, UPnPAction>();
    
    // variables
    private BrowseFlag browseFlag = new BrowseFlag();
    private ContainerUpdateIDs containerUpdateIDs = new ContainerUpdateIDs();
    private Count count = new Count();
    private Filter filter = new Filter();
    private Index index = new Index();
    private ObjectID objectID = new ObjectID();
    private Result result = new Result();
    private SearchCapabilities searchCapabilities = new SearchCapabilities();
    private SortCapabilities sortCapabilities = new SortCapabilities();
    private SortCriteria sortCriteria = new SortCriteria();
    private SystemUpdateID systemUpdateID = new SystemUpdateID();
    private UpdateID updateID = new UpdateID();
    private Map<String, UPnPStateVariable> variables = new HashMap<String, UPnPStateVariable>();
    
    public ContentDirectoryService() {
        actions.put(getSystemUpdateID.getName(), getSystemUpdateID);
        actions.put(browse.getName(), browse);
        actions.put(getSortCapabilities.getName(), getSortCapabilities);
        actions.put(getSearchCapabilities.getName(), getSearchCapabilities);
        
        variables.put(index.getName(), index);
        variables.put(count.getName(), count);
        variables.put(updateID.getName(), updateID);
        variables.put(containerUpdateIDs.getName(), containerUpdateIDs);
        variables.put(browseFlag.getName(), browseFlag);
        variables.put(sortCapabilities.getName(), sortCapabilities);
        variables.put(systemUpdateID.getName(), systemUpdateID);
        variables.put(searchCapabilities.getName(), searchCapabilities);
        variables.put(sortCriteria.getName(), sortCriteria);
        variables.put(result.getName(), result);
        variables.put(filter.getName(), filter);
        variables.put(objectID.getName(), objectID);
    }
    
    @Override
    public UPnPAction getAction(String name) {
        return actions.get(name);
    }

    @Override
    public UPnPAction[] getActions() {
        return actions.values().toArray(new UPnPAction[actions.size()]);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public UPnPStateVariable getStateVariable(String name) {
        return variables.get(name);
    }

    @Override
    public UPnPStateVariable[] getStateVariables() {
        return variables.values().toArray(new UPnPStateVariable[variables.size()]);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public String getVersion() {
        return "1";
    }
}
