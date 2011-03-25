package de.berlios.vch.parser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

import de.berlios.vch.net.INetworkProtocol;

// TODO move to vch.net ?!
@Component
@Provides
public class HTTP implements INetworkProtocol {

    private List<String> schemes = Arrays.asList(new String[] { "http", "https" });

    public HTTP() {
    }

    @Override
    public String getName() {
        return "Hyper Text Transfer Protocol";
    }

    @Override
    public List<String> getSchemes() {
        return schemes;
    }

    @Override
    public boolean isBridgeNeeded() {
        return false;
    }

    @Override
    public URI toBridgeUri(URI videoUri, Map<String, ?> connectionDetails) throws URISyntaxException {
        return videoUri;
    }
}
