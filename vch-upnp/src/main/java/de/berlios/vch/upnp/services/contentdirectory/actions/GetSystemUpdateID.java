package de.berlios.vch.upnp.services.contentdirectory.actions;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;

import de.berlios.vch.upnp.services.contentdirectory.variables.SystemUpdateID;

public class GetSystemUpdateID implements UPnPAction {
    
    private Map<String, UPnPStateVariable> variables = new HashMap<String, UPnPStateVariable>();
    
    public GetSystemUpdateID() {
        variables.put("Id", new SystemUpdateID());
    }
    
    @Override
    public String[] getInputArgumentNames() {
        return null;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String[] getOutputArgumentNames() {
        return new String[] {"Id"};
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
        Hashtable<String, String> result = new Hashtable<String, String>();
        result.put("Id", "1");
        return result;
    }

}
