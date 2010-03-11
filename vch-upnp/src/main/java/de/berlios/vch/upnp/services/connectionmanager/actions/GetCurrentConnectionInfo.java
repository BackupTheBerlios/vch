package de.berlios.vch.upnp.services.connectionmanager.actions;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.upnp.services.connectionmanager.variables.AVTransportID;
import de.berlios.vch.upnp.services.connectionmanager.variables.ConnectionID;
import de.berlios.vch.upnp.services.connectionmanager.variables.ConnectionManager;
import de.berlios.vch.upnp.services.connectionmanager.variables.ConnectionStatus;
import de.berlios.vch.upnp.services.connectionmanager.variables.Direction;
import de.berlios.vch.upnp.services.connectionmanager.variables.ProtocolInfo;
import de.berlios.vch.upnp.services.connectionmanager.variables.RcsID;

public class GetCurrentConnectionInfo implements UPnPAction {
    
    private static transient Logger logger = LoggerFactory.getLogger(GetCurrentConnectionInfo.class);

    private Map<String, UPnPStateVariable> variables = new HashMap<String, UPnPStateVariable>();
    
    public GetCurrentConnectionInfo() {
        variables.put("ConnectionID", new ConnectionID());
        variables.put("RcsID", new RcsID());
        variables.put("AVTransportID", new AVTransportID());
        variables.put("ProtocolInfo", new ProtocolInfo());
        variables.put("PeerConnectionManager", new ConnectionManager());
        variables.put("PeerConnectionID", new ConnectionID());
        variables.put("Direction", new Direction());
        variables.put("Status", new ConnectionStatus());
    }
    
    @Override
    public String[] getInputArgumentNames() {
        return new String[] {"ConnectionID"};
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public String[] getOutputArgumentNames() {
        return new String[] { "RcsID", "AVTransportID", "ProtocolInfo", "PeerConnectionManager", "PeerConnectionID",
                "Direction", "Status" };
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
