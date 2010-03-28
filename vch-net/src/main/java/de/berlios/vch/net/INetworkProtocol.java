package de.berlios.vch.net;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

public interface INetworkProtocol {
    public List<String> getSchemes();
    
    public String getName();
    
    public boolean isBridgeNeeded();

    /**
     * Changes the video URI to point to a VCH servlet, which acts as http -> protocol bridge
     * @param videoUri 
     * @param page
     * @throws URISyntaxException 
     */
    public URI toBridgeUri(URI videoUri, Map<String, ?> connectionDetails) throws URISyntaxException;
}
