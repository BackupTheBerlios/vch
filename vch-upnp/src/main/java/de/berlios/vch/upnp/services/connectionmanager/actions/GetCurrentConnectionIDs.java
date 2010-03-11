package de.berlios.vch.upnp.services.connectionmanager.actions;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.upnp.services.connectionmanager.variables.CurrentConnectionIDs;

public class GetCurrentConnectionIDs implements UPnPAction {
    
    private static transient Logger logger = LoggerFactory.getLogger(GetCurrentConnectionIDs.class);
    
    private Map<String, UPnPStateVariable> variables = new HashMap<String, UPnPStateVariable>();
    
    public GetCurrentConnectionIDs() {
        variables.put("ConnectionIDs", new CurrentConnectionIDs());
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
        return new String[] { "ConnectionIDs" };
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
        // TODO implement
        logger.debug("invoke({})", args);
        return null;
    }

}
