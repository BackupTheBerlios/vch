package de.berlios.vch.parser;

import java.util.Arrays;
import java.util.List;

import de.berlios.vch.net.INetworkProtocol;

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
