package de.berlios.vch.download.rtmp;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;

import de.berlios.vch.net.INetworkProtocol;

@Component
@Provides
public class RTMP implements INetworkProtocol {
    
    private List<String> schemes = Arrays.asList(new String[] { "rtmp", "rtmpt" });

    @Requires
    private HttpService httpService;
    
    @Requires 
    private LogService logger;
    
    private BundleContext ctx;
    
    public RTMP(BundleContext ctx) {
        this.ctx = ctx;
    }

    public String getName() {
        return "Real Time Messaging Protocol";
    }

    public List<String> getSchemes() {
        return schemes;
    }

    public boolean isBridgeNeeded() {
        return true;
    }

    @Override
    public URI toBridgeUri(URI videoUri, Map<String, ?> connectionDetails) throws URISyntaxException {
        // host
        String host = videoUri.getHost();
  
        // app
        String streamName = (String) connectionDetails.get("streamName");
        String app = videoUri.getPath() + (videoUri.getQuery() != null ? "?" + videoUri.getQuery() : "")
                + (videoUri.getFragment() != null ? "#" + videoUri.getFragment() : "");
        app = app.substring(1); // cut off the leading /
        int pos = app.indexOf(streamName);
        if(streamName.startsWith("mp4:")) {
            pos = app.indexOf(streamName.substring(4));
        }
        app = app.substring(0, pos);


        String servletHost = "localhost";
        InetAddress localMachine;
        try {
            localMachine = InetAddress.getLocalHost();
            servletHost = localMachine.getHostName();
        } catch (UnknownHostException e) {
            logger.log(LogService.LOG_ERROR, "Couldn't determine host name of this host. Falling back to \"localhost\"");
        }    
        String port = ctx.getProperty("org.osgi.service.http.port");
        return new URI("http://" + servletHost + ":" + port + StreamBridge.PATH + "?host=" + host + "&app=" + app
                + "&stream=" + streamName);
    }

    @Validate
    public void start() {
        // register stream bridge servlet
        StreamBridge streamBridge = new StreamBridge();
        streamBridge.setBundleContext(ctx);
        streamBridge.setLogger(logger);
        try {
            httpService.registerServlet(StreamBridge.PATH, streamBridge, null, null);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't register stream bridge servlet", e);
        }
    }
}
