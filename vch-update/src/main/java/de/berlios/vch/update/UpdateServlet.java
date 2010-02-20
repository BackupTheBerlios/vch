package de.berlios.vch.update;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Vector;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;
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
import de.berlios.vch.web.NotifyMessage;
import de.berlios.vch.web.NotifyMessage.TYPE;
import de.berlios.vch.web.servlets.BundleContextServlet;

public class UpdateServlet extends BundleContextServlet {
    
    public static final String PATH = "/extensions";
    
    public static final String STATIC_PATH = PATH + "/static";
    
    private Preferences prefs;
    
    private List<Resource> availableBundles = new Vector<Resource>();
    
    private List<BundleRepresentation> installedBundles = new Vector<BundleRepresentation>();
    
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // if the user has submitted any form, execute the actions
            if(req.getParameter("submit_install") != null) {
                install(req, resp);
            } else if(req.getParameter("submit_uninstall") != null) {
                uninstall(req);
            } else if(req.getParameter("submit_stop") != null) {
                stopBundles(req, resp);
            } else if(req.getParameter("submit_start") != null) {
                startBundles(req, resp);
            } else if(req.getParameter("submit_update") != null) {
                updateBundles(req, resp);
            }
            
            // render page parts 
            if(req.getParameter("tab") != null) {
                try {
                    String tab = req.getParameter("tab");
                    if("installed".equalsIgnoreCase(tab)) {
                        updateInstalledList();
                        renderInstalled(req, resp);
                    } else if("available".equalsIgnoreCase(tab)) {
                        updateInstalledList();
                        updateAvailableList();
                        renderAvailable(req, resp);
                    }
                } catch (ServiceUnavailableException e) {
                    error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getLocalizedMessage(), true);
                }
            } else if(req.getParameter("updates") != null) {
                try {
                    List<Resource> available = downloadAvailableList();
                    resp.setContentType("application/json; charset=utf-8");
                    resp.getWriter().write(toJSON(available));
                } catch (ServiceUnavailableException e) {
                    error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getLocalizedMessage(), true);
                }
            } else {
                renderMainPage(req, resp);
            }
        } catch (ServiceUnavailableException e) {
            error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, e.getLocalizedMessage());
        } catch (Exception e) {
            error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        } 
    }
    
    private void updateBundles(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String[] bundleIds = req.getParameterValues("installed");
        if(bundleIds == null) {
            addNotify(req, new NotifyMessage(TYPE.ERROR, i18n.translate("info.no_extension_selected")));
            return;
        }
        
        ServiceReference sr = getBundleContext().getServiceReference(RepositoryAdmin.class.getName());
        if (sr == null) {
            error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, i18n.translate("error.obr_not_available"));
        }
        RepositoryAdmin adm = (RepositoryAdmin) getBundleContext().getService(sr);
        if (adm == null) {
            error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, i18n.translate("error.obr_not_available"));
        }
        Resolver resolver = adm.resolver();
        
        for (String bundleId : bundleIds) {
            int _bundleId = Integer.parseInt(bundleId);
            Bundle bundle = getBundleContext().getBundle(_bundleId);
            String symbolicName = bundle.getSymbolicName();
            // String version = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
            String filter = "(symbolicname=" + symbolicName + ")";
            logger.log(LogService.LOG_INFO, "Resolving " + filter);
            Collection<Resource> resources = filterByVersion(adm.discoverResources(filter));
            logger.log(LogService.LOG_INFO, "Found " + resources.size() + " resources");
            if (resources.size() > 0) {
                try {
                    bundle.uninstall();
                    for (Resource resource : resources) {
                        logger.log(LogService.LOG_INFO, "Adding " + resource.getSymbolicName() + " to update list");
                        resolver.add(resource);
                    }
                } catch (BundleException e1) {
                    String msg = i18n.translate("error.uninstall_extension");
                    logger.log(LogService.LOG_ERROR, msg, e1);
                    addNotify(req, new NotifyMessage(TYPE.ERROR, msg, e1));
                }
            }
        }

        if (resolver.resolve()) {
            resolver.deploy(true); // deploy and start (true means "start")
            addNotify(req, new NotifyMessage(TYPE.INFO, i18n.translate("info.please_restart")));
        } else {
            String msg = i18n.translate("error.load_list");
            logger.log(LogService.LOG_ERROR, msg);
            addNotify(req, new NotifyMessage(TYPE.ERROR, msg));
        }
        updateInstalledList();
        updateAvailableList();
    }

    private void stopBundles(HttpServletRequest req, HttpServletResponse resp) {
        String[] bundleIds = req.getParameterValues("installed");
        if(bundleIds == null) {
            addNotify(req, new NotifyMessage(TYPE.ERROR, i18n.translate("info.no_extension_selected")));
            return;
        }
        for (String bundleId : bundleIds) {
            int _bundleId = Integer.parseInt(bundleId);
            try {
                Bundle bundle = getBundleContext().getBundle(_bundleId);
                if(bundle != null) {
                    bundle.stop();
                } else {
                    addNotify(req, new NotifyMessage(TYPE.WARNING, i18n.translate("warning.bundle_does_not_exist", _bundleId)));
                }
            } catch (BundleException e) {
                String msg = i18n.translate("error.stop_bundle", _bundleId);
                logger.log(LogService.LOG_ERROR, msg, e);
                addNotify(req, new NotifyMessage(TYPE.ERROR, msg, e));
            }
        }
    }
    
    private void startBundles(HttpServletRequest req, HttpServletResponse resp) {
        String[] bundleIds = req.getParameterValues("installed");
        if(bundleIds == null) {
            addNotify(req, new NotifyMessage(TYPE.ERROR, i18n.translate("info.no_extension_selected")));
            return;
        }
        for (String bundleId : bundleIds) {
            int _bundleId = Integer.parseInt(bundleId);
            try {
                Bundle bundle = getBundleContext().getBundle(_bundleId);
                if(bundle != null) {
                    bundle.start();
                } else {
                    addNotify(req, new NotifyMessage(TYPE.WARNING, i18n.translate("warning.bundle_does_not_exist", _bundleId)));
                }
            } catch (BundleException e) {
                String msg = i18n.translate("error.start_bundle", _bundleId);
                logger.log(LogService.LOG_ERROR, msg, e);
                addNotify(req, new NotifyMessage(TYPE.ERROR, msg, e));
            }
        }
    }

    private void renderAvailable(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> tplParams = new HashMap<String, Object>();
        List<ResourceRepresentation> list = new ArrayList<ResourceRepresentation>(availableBundles.size());
        for (Resource resource : availableBundles) {
            list.add(new ResourceRepresentation(resource));
        }
        tplParams.put("ACTION", PATH);
        tplParams.put("AVAILABLE", list);
        String template = templateLoader.loadTemplate("extensions_available.ftl", tplParams);
        resp.getWriter().println(template);
    }

    private void renderInstalled(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> tplParams = new HashMap<String, Object>();
        tplParams.put("ACTION", PATH);
        tplParams.put("INSTALLED", installedBundles);
        String template = templateLoader.loadTemplate("extensions_installed.ftl", tplParams);
        resp.getWriter().println(template);
    }

    private void renderMainPage(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Map<String, Object> tplParams = new HashMap<String, Object>();
        String path = req.getRequestURI();
        tplParams.put("ACTION", path);
        tplParams.put("STATIC_PATH", STATIC_PATH);
        tplParams.put("TITLE", i18n.translate("I18N_EXTENSIONS"));
        
        // add additional js and css
        List<String> js = new ArrayList<String>();
        js.add("http://jquery-ui.googlecode.com/svn/tags/latest/ui/jquery.ui.core.js");
        js.add("http://jquery-ui.googlecode.com/svn/tags/latest/ui/jquery.ui.widget.js");
        js.add("http://jquery-ui.googlecode.com/svn/tags/latest/ui/jquery.ui.tabs.js");
        tplParams.put("JS_INCLUDES", js);
        List<String> css = new ArrayList<String>();
        css.add("http://jquery-ui.googlecode.com/svn/tags/latest/themes/base/ui.all.css");
        css.add(STATIC_PATH + "/extensions.css");
        tplParams.put("CSS_INCLUDES", css);
        
        // add errors and messages
        tplParams.put("NOTIFY_MESSAGES", getNotifyMessages(req));
        
        tplParams.put("AVAILABLE", availableBundles);
        tplParams.put("INSTALLED", installedBundles);
        String template = templateLoader.loadTemplate("extensions.ftl", tplParams);
        resp.getWriter().println(template);
    }

    private void uninstall(HttpServletRequest req) {
        String[] bundleIds = req.getParameterValues("installed");
        if(bundleIds == null) {
            addNotify(req, new NotifyMessage(TYPE.ERROR, i18n.translate("info.no_extension_selected")));
            return;
        }
        for (String bundleId : bundleIds) {
            long id = Long.parseLong(bundleId);
            Bundle bundle = getBundleContext().getBundle(id);
            if(bundle != null) {
                try {
                    bundle.uninstall();
                } catch (BundleException e) {
                    String msg = i18n.translate("error.uninstall_extension");
                    logger.log(LogService.LOG_ERROR, msg, e);
                    addNotify(req, new NotifyMessage(TYPE.ERROR, msg, e));
                }
            }
        }
    }

    private void install(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        // lookup RepositoryAdmin
        ServiceReference sr = getBundleContext().getServiceReference(RepositoryAdmin.class.getName());
        if (sr == null) {
            error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, i18n.translate("error.obr_not_available"));
        }
        RepositoryAdmin adm = (RepositoryAdmin) getBundleContext().getService(sr);
        if (adm == null) {
            error(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, i18n.translate("error.obr_not_available"));
        }
        Resolver resolver = adm.resolver();
        
        // resolve given symbolicNames to Resource objects and pass them to the Resolver
        List<Resource> available = downloadAvailableList();
        String[] symbolicNames = req.getParameterValues("available");
        if(symbolicNames == null) {
            addNotify(req, new NotifyMessage(TYPE.ERROR, i18n.translate("info.no_extension_selected")));
            return;
        }
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
            addNotify(req, new NotifyMessage(TYPE.INFO, i18n.translate("info.please_restart")));
        } else {
            for (Requirement requirement : resolver.getUnsatisfiedRequirements()) {
                String msg = "Unsatisfied requirement: " + requirement.getName() + " " + requirement.toString();
                logger.log(LogService.LOG_INFO, msg);
                addNotify(req, new NotifyMessage(TYPE.WARNING, msg));
            }
        }
    }

    private List<Resource> downloadAvailableList() throws MalformedURLException, Exception {
        ServiceReference sr = getBundleContext().getServiceReference(RepositoryAdmin.class.getName());
        if (sr == null) {
            throw new ServiceUnavailableException(i18n.translate("error.obr_not_available"));
        }
        RepositoryAdmin adm = (RepositoryAdmin) getBundleContext().getService(sr);
        if (adm == null) {
            throw new ServiceUnavailableException(i18n.translate("error.obr_not_available"));
        }
        
        // add repos from configuration
        for (String uri : getOBRs()) {
            try {
                logger.log(LogService.LOG_INFO, "Adding bundle repository " + uri);
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
        logger.log(LogService.LOG_INFO, "Loading extensions list from obrs:" + sb.toString());
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
                br.setAuthor((String) bundle.getHeaders().get(Constants.BUNDLE_VENDOR));
                br.setDescription((String) bundle.getHeaders().get(Constants.BUNDLE_DESCRIPTION));
                String version = (String) bundle.getHeaders().get(Constants.BUNDLE_VERSION);
                br.setVersion(version);
                br.setState(bundle.getState());
                installedBundles.add(br);
            }
        }
        Collections.sort(installedBundles, new BundleNameComparator());
    }

    private synchronized void updateAvailableList() throws MalformedURLException, Exception {
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
    
    private String toJSON(Resource res) {
        Map<String, Object> object = new HashMap<String, Object>();
        object.put("symbolicName", res.getSymbolicName());
        object.put("version", res.getVersion().toString());
        return new JSONObject(object).toString();
    }

    private String toJSON(List<Resource> resources) {
        if (!resources.isEmpty()) {
            String json = "[";
            for (Iterator<Resource> iterator = resources.iterator(); iterator.hasNext();) {
                Resource resource = iterator.next();
                json += toJSON(resource);
                if (iterator.hasNext()) {
                    json += ", ";
                }
            }
            return json += "]";
        } else {
            return "[]";
        }
    }
    
    public List<String> getOBRs() throws MalformedURLException, Exception {
        // lookup preferences service
        ServiceReference sr = bundleContext.getServiceReference(ConfigService.class.getName());
        if(sr != null) {
            ConfigService cs = (ConfigService) bundleContext.getService(sr);
            prefs = cs.getUserPreferences("");
        } else {
            throw new ServiceUnavailableException(i18n.translate("I18N_CONFIG_SERVICE_NOT_AVAILABLE"));
        }
        
        List<String> obrUris = new ArrayList<String>();
        try {
            Preferences persitentRepos = prefs.node("obrs");
            String[] obrs = persitentRepos.childrenNames();
            for (String id : obrs) {
                Preferences obr = persitentRepos.node(id);
                String uri = obr.get("uri", "");
                obrUris.add(uri);
            }
        } catch (BackingStoreException e) {
            logger.log(LogService.LOG_ERROR, "Couldn't load preferences", e);
        }
        Collections.sort(obrUris);
        if(obrUris.isEmpty()) {
            String uri = "http://vch.berlios.de/repo/releases/repository.xml"; 
            addOBR(uri);
            obrUris.add(uri);
        }
        return obrUris;
    }
    
    public void addOBR(String uri) throws MalformedURLException, Exception {
        ServiceReference sr = getBundleContext().getServiceReference(RepositoryAdmin.class.getName());
        if(sr == null) {
            throw new ServiceUnavailableException(i18n.translate("error.obr_not_available"));
        }
        RepositoryAdmin adm = (RepositoryAdmin) getBundleContext().getService(sr);
        if(adm == null) {
            throw new ServiceUnavailableException(i18n.translate("error.obr_not_available"));
        }
        adm.addRepository(new URL(uri));
        
        Preferences obrs = prefs.node("obrs");
        String id = UUID.randomUUID().toString();
        Preferences obr = obrs.node(id);        
        obr.put("uri", uri);
    }
    
    public void removeOBR(String uri) throws MalformedURLException, ServiceUnavailableException {
        ServiceReference sr = getBundleContext().getServiceReference(RepositoryAdmin.class.getName());
        if(sr == null) {
            throw new ServiceUnavailableException(i18n.translate("error.obr_not_available"));
        }
        RepositoryAdmin adm = (RepositoryAdmin) getBundleContext().getService(sr);
        
        
        Preferences obrs = prefs.node("obrs");
        try {
            for (String key : obrs.childrenNames()) {
                Preferences obr = obrs.node(key);
                if(uri.equals(obr.get("uri", ""))) {
                    obr.removeNode();
                    adm.removeRepository(new URL(uri));
                }
            }
        } catch (BackingStoreException e) {
            logger.log(LogService.LOG_ERROR, "Couldn't remove obr", e);
        }
    }
}
