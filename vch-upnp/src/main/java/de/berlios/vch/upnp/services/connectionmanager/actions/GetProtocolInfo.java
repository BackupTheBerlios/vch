package de.berlios.vch.upnp.services.connectionmanager.actions;

import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.upnp.UPnPAction;
import org.osgi.service.upnp.UPnPStateVariable;

import de.berlios.vch.upnp.services.connectionmanager.variables.SinkProtocolInfo;
import de.berlios.vch.upnp.services.connectionmanager.variables.SourceProtocolInfo;

public class GetProtocolInfo implements UPnPAction {

    // variables
    private SourceProtocolInfo sourceProtocolInfo = new SourceProtocolInfo();
    private SinkProtocolInfo sinkProtocolInfo = new SinkProtocolInfo();
    private Map<String, UPnPStateVariable> variables = new HashMap<String, UPnPStateVariable>();
    
    public GetProtocolInfo() {
        variables.put("Source", sourceProtocolInfo);
        variables.put("Sink", sinkProtocolInfo);
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
        return new String[] {"Source", "Sink"};
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
        result.put("Sink", "");
        result.put("Source", "http-get:*:application/octet-stream:*,http-get:*:application/ogg:*,http-get:*:application/vnd:*,http-get:*:application/x-gzip:*,http-get:*:audio/mpeg:*,http-get:*:image/jpeg:*,http-get:*:text/plain:*,http-get:*:video/mp4:*,http-get:*:video/x-flv:*,http-get:*:video/x-ms-wmv:*,http-get:*:video/x-msvideo:*");
        return result;
    }

}
