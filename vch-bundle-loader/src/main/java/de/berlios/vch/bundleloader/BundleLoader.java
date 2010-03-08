package de.berlios.vch.bundleloader;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BundleLoader implements BundleActivator, BundleListener {
    private static transient Logger logger = LoggerFactory.getLogger(BundleLoader.class);
    
    private static final String DIR = "plugins";
    
    private BundleContext ctx;
    
    @Override
    public void start(BundleContext ctx) throws Exception {
        this.ctx = ctx;
        load();
        ctx.addBundleListener(this);
    }

    public void load() {
        List<Bundle> installedBundles = new ArrayList<Bundle>();
        File pluginDir = new File(DIR);
        if (!pluginDir.exists()) {
            logger.warn("Directory {} does not exist", DIR);
            return;
        }

        // discover new bundle jars
        File[] plugins = pluginDir.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
        
        // install the bundles
        for (File file : plugins) {
            Bundle bundle = null;
            try {
                bundle = ctx.installBundle("file:" + file.getAbsolutePath());
                installedBundles.add(bundle);
                file.delete();
            } catch (BundleException e) {
                logger.warn("Couldn't install plugin " + file.getAbsolutePath(), e);
            }
        }
        
        // start the bundles
        for (Bundle bundle : installedBundles) {
            try {
                if (resolve(bundle)) {
                    bundle.start();
                }
            } catch (BundleException e) {
                logger.warn("Couldn't start bundle " + bundle.getSymbolicName(), e);
            }
        }
    }

    public void bundleChanged(BundleEvent e) {
        if (e.getType() == BundleEvent.INSTALLED) {
            load();
        }
    }

    private boolean resolve(Bundle bundle) {
        ServiceReference ref = ctx.getServiceReference(PackageAdmin.class.getName());
        if (ref == null) {
            logger.warn("PackageAdmin service is unavailable.");
            return false;
        }

        PackageAdmin pa = (PackageAdmin) ctx.getService(ref);
        if (pa == null) {
            logger.warn("PackageAdmin service is unavailable.");
            return false;
        }

        return pa.resolveBundles(new Bundle[] { bundle });
    }
    
    @Override
    public void stop(BundleContext ctx) throws Exception {}
}
