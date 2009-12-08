package de.berlios.vch.update;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class UpdateServlet extends BundleContextServlet {
    
    public static final String PATH = "/extensions";
    
    private Preferences prefs;
    
    private List<Resource> availableBundles = new Vector<Resource>();
    
    private List<BundleRepresentation> installedBundles = new Vector<BundleRepresentation>();
    
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // lookup preferences service
        ServiceReference sr = bundleContext.getServiceReference(ConfigService.class.getName());
        if(sr != null) {
            ConfigService cs = (ConfigService) bundleContext.getService(sr);
            prefs = cs.getUserPreferences("");
        } else {
            error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Config service not available"); // TODO show error on webpage
            return;
        }
        
        if(req.getParameter("submit_install") != null) {
            install(req.getParameterValues("available"));
        } else if(req.getParameter("submit_uninstall") != null) {
            uninstall(req.getParameterValues("installed"));
        }
        
        updateInstalledList();
        updateAvailableList();
        
        Map<String, Object> tplParams = new HashMap<String, Object>();
        String path = req.getRequestURI();
        tplParams.put("ACTION", path);
        tplParams.put("TITLE", i18n.translate("I18N_EXTENSIONS"));
        
        // add errors and messages
        tplParams.put("ERRORS", req.getAttribute("errors"));
        tplParams.put("MESSAGES", req.getAttribute("messages"));
        
        tplParams.put("AVAILABLE", availableBundles);
        tplParams.put("INSTALLED", installedBundles);
        String template = templateLoader.loadTemplate("extensions.ftl", tplParams);
        resp.getWriter().println(template);
    }
    
    private void uninstall(String[] bundleIds) {
        for (String bundleId : bundleIds) {
            long id = Long.parseLong(bundleId);
            Bundle bundle = getBundleContext().getBundle(id);
            if(bundle != null) {
                try {
                    bundle.uninstall();
                } catch (BundleException e) {
                    logger.log(LogService.LOG_ERROR, "Couldn't uninstall bundle", e); // TODO show error on webpage
                }
            }
        }
    }

    private void install(String[] symbolicNames) {
        // lookup RepositoryAdmin
        ServiceReference sr = getBundleContext().getServiceReference(RepositoryAdmin.class.getName());
        if (sr == null) {
            logger.log(LogService.LOG_ERROR, "OBR service not available"); // TODO show error on webpage
            return;
        }
        RepositoryAdmin adm = (RepositoryAdmin) getBundleContext().getService(sr);
        if (adm == null) {
            logger.log(LogService.LOG_ERROR, "OBR service not available"); // TODO show error on webpage
            return;
        }
        Resolver resolver = adm.resolver();
        
        // resolve given symbolicNames to Resource objects and pass them to the Resolver
        List<Resource> available = downloadAvailableList();
        for (String symbolicName : symbolicNames) {
            for (Resource resource : available) {
                if(resource.getSymbolicName().equals(symbolicName)) {
                    resolver.add(resource);
                }
            }
        }
        
        // try to resolve the bundles and then install them
        if (resolver.resolve()) {
            resolver.deploy(true);
        } else {
            // TODO show error on webpage
            for (Requirement req : resolver.getUnsatisfiedRequirements()) {
                logger.log(LogService.LOG_INFO, "Unsatisfied requirement: " + req.getName() + " " + req.toString());
            }
        }
    }

    private List<Resource> downloadAvailableList() {
        ServiceReference sr = getBundleContext().getServiceReference(RepositoryAdmin.class.getName());
        RepositoryAdmin adm = (RepositoryAdmin) getBundleContext().getService(sr);
        
        // add repos from configuration
        for (String uri : getRepoUris()) {
            try {
                adm.addRepository(new URL(uri));
            } catch (Exception e) {
                logger.log(LogService.LOG_WARNING, "Couldn't add repository", e);
            }
        }
        
        Repository[] repos = adm.listRepositories();
        StringBuilder sb = new StringBuilder();
        for (Repository repo : repos) {
            sb.append('\n').append(repo.getName()).append(' ').append(repo.getURL().toString());
        }
        logger.log(LogService.LOG_INFO, "Loading extensions list");
        String filter = "(symbolicname=*)"; // get all bundles
        logger.log(LogService.LOG_INFO, "Resolving " + filter);
        Resource[] res = adm.discoverResources(filter);
        Collection<Resource> resources = filterByVersion(res);
        for (Iterator<Resource> iterator = resources.iterator(); iterator.hasNext();) {
            Resource resource = iterator.next();
            Capability[] caps = resource.getCapabilities();
            Capability capability = getCapability(caps, "vch");
            if (capability != null) {
                if (!"true".equals(capability.getProperties().get("bundle"))) {
                    iterator.remove();
                }
            } else {
                iterator.remove();
            }
        }
        logger.log(LogService.LOG_INFO, "Found " + resources.size() + " resources");
        List<Resource> allResources = new ArrayList<Resource>();
        allResources.addAll(resources);
        Collections.sort(allResources, new ResourceNameComparator());
        return allResources;
    }
    
    private class ResourceNameComparator implements Comparator<Resource> {
        public int compare(Resource r1, Resource r2) {
            return r1.getPresentationName().compareTo(r2.getPresentationName());
        }
    }
    
    /**
     * Filters all old version, so that only the newest versions of a plugin are in the returned collection
     * 
     * @param res
     * @return
     */
    private Collection<Resource> filterByVersion(Resource[] res) {
        Map<String, Resource> filterMap = new HashMap<String, Resource>();
        for (int i = 0; i < res.length; i++) {
            if (filterMap.containsKey(res[i].getSymbolicName())) {
                // we filter the list of available bundles. only the newest
                // bundles are included
                Resource r1 = filterMap.get(res[i].getSymbolicName());
                Resource r2 = res[i];
                if (r2.getVersion().compareTo(r1.getVersion()) == 1) {
                    logger.log(LogService.LOG_DEBUG, "Bundle " + r1.getSymbolicName() + " with version "
                            + r1.getVersion() + " will be dropped");
                    logger.log(LogService.LOG_INFO, "Adding " + res[i].getPresentationName() + " with version "
                            + res[i].getVersion());
                    filterMap.put(res[i].getSymbolicName(), r2);
                }
            } else {
                logger.log(LogService.LOG_INFO, "Adding " + res[i].getPresentationName() + " with version "
                        + res[i].getVersion());
                filterMap.put(res[i].getSymbolicName(), res[i]);
            }
        }
        return filterMap.values();
    }
    
    private Capability getCapability(Capability[] caps, String name) {
        for (Capability capability : caps) {
            if(name.equals(capability.getName())) {
                return capability;
            }
        }
        return null;
    }
    
    private List<String> getRepoUris() {
        List<String> list = new ArrayList<String>();
        String repos = prefs.get("plugin_repos", "VCH Bundle Repository,http://vch.berlios.de/repo/releases/repository.xml");
        String[] keyValues = repos.split(";");
        for (String string : keyValues) {
            String[] keyValue = string.split(",");
            list.add(keyValue[1]);
        }
        return list;
    }
    
    private class BundleNameComparator implements Comparator<BundleRepresentation> {
        public int compare(BundleRepresentation b1, BundleRepresentation b2) {
            return b1.getName().toLowerCase().compareTo(b2.getName().toLowerCase());
        }
    }

    private String getBundleName(Bundle bundle) {
        // Get the bundle name or location.
        String name = (String) bundle.getHeaders().get(Constants.BUNDLE_NAME);
        // If there is no name, then default to symbolic name.
        name = (name == null) ? bundle.getSymbolicName() : name;
        // If there is no symbolic name, resort to location.
        name = (name == null) ? bundle.getLocation() : name;
        return name;
    }
    
    private synchronized void updateInstalledList() {
        installedBundles.clear();
        Bundle[] bundles = getBundleContext().getBundles();
        for (Bundle bundle : bundles) {
            if ("true".equalsIgnoreCase((String) bundle.getHeaders().get("VCH-Bundle"))) {
                BundleRepresentation br = new BundleRepresentation();
                br.setBundleId(bundle.getBundleId());
                br.setName(getBundleName(bundle));
                br.setSymbolicName(bundle.getSymbolicName());
                String version = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
                br.setVersion(version);
                installedBundles.add(br);
            }
        }
        Collections.sort(installedBundles, new BundleNameComparator());
    }

    private synchronized void updateAvailableList() {
        availableBundles.clear();
        availableBundles.addAll(downloadAvailableList());
        for (Iterator<Resource> iterator = availableBundles.iterator(); iterator.hasNext();) {
            Resource res = iterator.next();
            if (isInstalled(res)) {
                iterator.remove();
            }
        }
    }
    
    private boolean isInstalled(Resource resource) {
        for (Iterator<BundleRepresentation> iterator = installedBundles.iterator(); iterator.hasNext();) {
            BundleRepresentation current = iterator.next();
            if (current.getSymbolicName().equals(resource.getSymbolicName())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        get(req, resp);
    }
}
