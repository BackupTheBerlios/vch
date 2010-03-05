package de.berlios.vch.parser;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

import de.berlios.vch.net.INetworkProtocol;

@Component
@Provides
public class MMS implements INetworkProtocol {

    private List<String> schemes = Arrays.asList(new String[] {"mms"});
    
    public MMS() {}
    
    public String getName() {
        return "Microsoft Media Server Protocol";
    }

    public List<String> getSchemes() {
        return schemes;
    }

    public boolean isBridgeNeeded() {
        return false;
    }
}
