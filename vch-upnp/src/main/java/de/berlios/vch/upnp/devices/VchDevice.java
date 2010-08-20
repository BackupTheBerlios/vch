package de.berlios.vch.upnp.devices;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.osgi.framework.Constants;
import org.osgi.service.upnp.UPnPDevice;
import org.osgi.service.upnp.UPnPIcon;
import org.osgi.service.upnp.UPnPService;

import de.berlios.vch.upnp.Activator;
import de.berlios.vch.upnp.services.connectionmanager.ConnectionManagerService;
import de.berlios.vch.upnp.services.contentdirectory.ContentDirectoryService;

public class VchDevice implements UPnPDevice {

    private final static String DEVICE_ID = "uuid:VCH-Media-Server";
    private Properties description;
    
    private ConnectionManagerService cms = new ConnectionManagerService();
    private ContentDirectoryService cds = new ContentDirectoryService();
    
    private Map<String, UPnPService> services = new HashMap<String, UPnPService>();

    public VchDevice() {
        setupDeviceProperties();
        services.put(cms.getId(), cms);
        services.put(cds.getId(), cds);
    }

    private void setupDeviceProperties() {
        description = new Properties();
        description.put(UPnPDevice.UPNP_EXPORT, "");
        description.put(org.osgi.service.device.Constants.DEVICE_CATEGORY, new String[] { UPnPDevice.DEVICE_CATEGORY });
        description.put(UPnPDevice.FRIENDLY_NAME, "VCH Media Server");
        description.put(UPnPDevice.MANUFACTURER, "VCH Project");
        description.put(UPnPDevice.MANUFACTURER_URL, "http://developer.berlios.de/projects/vch");
        description.put(UPnPDevice.MODEL_DESCRIPTION, Activator.context.getBundle().getHeaders().get(
                Constants.BUNDLE_DESCRIPTION));
        description.put(UPnPDevice.MODEL_NAME, Activator.context.getBundle().getHeaders().get(Constants.BUNDLE_NAME));
        description.put(UPnPDevice.MODEL_NUMBER, Activator.context.getBundle().getHeaders().get(
                Constants.BUNDLE_VERSION));
        description.put(UPnPDevice.TYPE, "urn:schemas-upnp-org:device:MediaServer:1");
        description.put(UPnPDevice.UDN, DEVICE_ID);
        
        description.put(UPnPService.TYPE, new String[] { 
                "urn:schemas-upnp-org:service:ConnectionManager:1",
                "urn:schemas-upnp-org:service:ContentDirectory:1" });
        description.put(UPnPService.ID, new String[] { 
                "urn:schemas-upnp-org:serviceId:ConnectionManager",
                "urn:schemas-upnp-org:serviceId:ContentDirectory" });
        
        String port = Activator.context.getProperty("org.osgi.service.http.port");
        InetAddress inet;
        try {
            inet = InetAddress.getLocalHost();
            String hostname = inet.getHostName();
            // TODO this is the wrong URI
            description.put(UPnPDevice.PRESENTATION_URL,"http://"+hostname + ":"+port+"/upnp/description/");
        } catch (UnknownHostException e) {
            System.out.println("Warning: enable to cacth localhost name");
        }

    }

    @Override
    public Dictionary getDescriptions(String locale) {
        return description;
    }

    @Override
    public UPnPIcon[] getIcons(String locale) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public UPnPService getService(String serviceId) {
        return services.get(serviceId);
    }

    @Override
    public UPnPService[] getServices() {
        return new UPnPService[] {cms, cds};
    }

}
