package de.berlios.vch.upnp;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.upnp.UPnPDevice;

import de.berlios.vch.upnp.devices.VchDevice;

public class Activator implements BundleActivator {

    public static BundleContext context;

    @Override
    public void start(BundleContext ctx) throws Exception {
        Activator.context = ctx;
        UPnPDevice device = new VchDevice();
        context.registerService(UPnPDevice.class.getName(), device, device.getDescriptions(null));
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
    }
}
