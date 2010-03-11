package de.berlios.vch.upnp.services.connectionmanager;

import java.util.HashMap;
import java.util.Map;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPService;
import org.osgi.service.upnp.UPnPStateVariable;

import de.berlios.vch.upnp.services.connectionmanager.actions.GetCurrentConnectionIDs;
import de.berlios.vch.upnp.services.connectionmanager.actions.GetCurrentConnectionInfo;
import de.berlios.vch.upnp.services.connectionmanager.actions.GetProtocolInfo;
import de.berlios.vch.upnp.services.connectionmanager.variables.AVTransportID;
import de.berlios.vch.upnp.services.connectionmanager.variables.ConnectionID;
import de.berlios.vch.upnp.services.connectionmanager.variables.ConnectionManager;
import de.berlios.vch.upnp.services.connectionmanager.variables.ConnectionStatus;
import de.berlios.vch.upnp.services.connectionmanager.variables.CurrentConnectionIDs;
import de.berlios.vch.upnp.services.connectionmanager.variables.Direction;
import de.berlios.vch.upnp.services.connectionmanager.variables.ProtocolInfo;
import de.berlios.vch.upnp.services.connectionmanager.variables.RcsID;
import de.berlios.vch.upnp.services.connectionmanager.variables.SinkProtocolInfo;
import de.berlios.vch.upnp.services.connectionmanager.variables.SourceProtocolInfo;

public class ConnectionManagerService implements UPnPService {

    private static final String ID = "urn:schemas-upnp-org:serviceId:ConnectionManager";
    private static final String TYPE = "urn:schemas-upnp-org:service:ConnectionManager:1";

    // actions
    private GetProtocolInfo getProtocolInfo = new GetProtocolInfo();
    private GetCurrentConnectionInfo getCurrentConnectionInfo = new GetCurrentConnectionInfo();
    private GetCurrentConnectionIDs getCurrentConnectionIDs = new GetCurrentConnectionIDs();
    private Map<String, UPnPAction> actions = new HashMap<String, UPnPAction>();

    // variables
    private ConnectionManager connectionManager = new ConnectionManager();
    private SourceProtocolInfo sourceProtocolInfo = new SourceProtocolInfo();
    private ConnectionID connectionID = new ConnectionID();
    private RcsID rcsID = new RcsID();
    private ConnectionStatus connectionStatus = new ConnectionStatus();
    private ProtocolInfo protocolInfo = new ProtocolInfo();
    private SinkProtocolInfo sinkProtocolInfo = new SinkProtocolInfo();
    private Direction direction = new Direction();
    private AVTransportID avTransportID = new AVTransportID();
    private CurrentConnectionIDs currentConnectionIDs = new CurrentConnectionIDs();
    private Map<String, UPnPStateVariable> variables = new HashMap<String, UPnPStateVariable>();

    public ConnectionManagerService() {
        // add actions to helper map
        actions.put(getProtocolInfo.getName(), getProtocolInfo);
        actions.put(getCurrentConnectionInfo.getName(), getCurrentConnectionInfo);
        actions.put(getCurrentConnectionIDs.getName(), getCurrentConnectionIDs);

        // add variables to helper map
        variables.put(connectionManager.getName(), connectionManager);
        variables.put(sourceProtocolInfo.getName(), sourceProtocolInfo);
        variables.put(connectionID.getName(), connectionID);
        variables.put(rcsID.getName(), rcsID);
        variables.put(connectionStatus.getName(), connectionStatus);
        variables.put(protocolInfo.getName(), protocolInfo);
        variables.put(sinkProtocolInfo.getName(), sinkProtocolInfo);
        variables.put(direction.getName(), direction);
        variables.put(avTransportID.getName(), avTransportID);
        variables.put(currentConnectionIDs.getName(), currentConnectionIDs);
    }

    @Override
    public UPnPAction getAction(String name) {
        return actions.get(name);
    }

    @Override
    public UPnPAction[] getActions() {
        return new UPnPAction[] { getProtocolInfo, getCurrentConnectionInfo, getCurrentConnectionIDs };
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
        return new UPnPStateVariable[] { connectionManager, sourceProtocolInfo, connectionID, rcsID, connectionStatus,
                protocolInfo, sinkProtocolInfo, direction, avTransportID, currentConnectionIDs };
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
