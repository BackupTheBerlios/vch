package de.berlios.vch.parser;

import java.util.Arrays;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

import de.berlios.vch.net.INetworkProtocol;

@Component
@Provides
public class HTTP implements INetworkProtocol {

    private List<String> schemes = Arrays.asList(new String[] {"http", "https"});
    
    public HTTP() {}
    
    public String getName() {
        return "Hyper Text Transfer Protocol";
    }

    public List<String> getSchemes() {
        return schemes;
    }

    public boolean isBridgeNeeded() {
        return false;
    }
}
