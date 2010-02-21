package de.berlios.vch.net;

import java.util.List;

public interface INetworkProtocol {
    public List<String> getSchemes();
    
    public String getName();
    
    public boolean isBridgeNeeded();
}
