package de.berlios.vch.download;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import javax.servlet.ServletException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.download.osd.DownloadAction;
import de.berlios.vch.download.osd.OpenDownloadsAction;
import de.berlios.vch.download.webinterface.DownloadHttpContext;
import de.berlios.vch.download.webinterface.DownloadsServlet;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.osdserver.osd.menu.actions.ItemDetailsAction;
import de.berlios.vch.osdserver.osd.menu.actions.OverviewAction;
import de.berlios.vch.web.ResourceHttpContext;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;

@Component
@Provides
public class Activator implements ResourceBundleProvider {

    private BundleContext ctx;
    
    private ResourceBundle resourceBundle;
    
    @Requires
    private LogService logger;
    
    @Requires
    private Messages messages;
    
    @Requires
    private TemplateLoader templateLoader;
    
    @Requires
    private HttpService httpService;
   
    private DownloadManager dm;
    
    @Requires
    private ConfigService cs;
    
    private Preferences prefs;
    
    private List<ServiceRegistration> serviceRegs = new LinkedList<ServiceRegistration>();
    
    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Validate
    public void start() {
        prefs = cs.getUserPreferences(ctx.getBundle().getSymbolicName());
        setDefaults(prefs);
        try {
            // initialize doenload manager
            dm = new DownloadManagerImpl(ctx, logger);
            dm.init(prefs);
            
            // start activator servlet
            registerServlet();
            
            // register web interface menu
            WebMenuEntry downloads = new WebMenuEntry();
            downloads.setTitle(getResourceBundle().getString("I18N_DOWNLOADS"));
            downloads.setPreferredPosition(Integer.MAX_VALUE-2);
            downloads.setLinkUri("#");
            WebMenuEntry manage = new WebMenuEntry(getResourceBundle().getString("I18N_MANAGE"));
            manage.setLinkUri(DownloadsServlet.PATH);
            downloads.getChilds().add(manage);
            ServiceRegistration sr = ctx.registerService(IWebMenuEntry.class.getName(), downloads, null);
            serviceRegs.add(sr);
            
            // register osd actions
            DownloadAction action = new DownloadAction(messages, dm, logger);
            sr = ctx.registerService(ItemDetailsAction.class.getName(), action, null);
            serviceRegs.add(sr);
            OpenDownloadsAction oda = new OpenDownloadsAction(messages, dm, logger);
            sr = ctx.registerService(OverviewAction.class.getName(), oda, null);
            serviceRegs.add(sr);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't start download manager", e);
        }
    }
    
    private void setDefaults(Preferences prefs) {
        setIfEmpty(prefs, "data.dir", "data");
    }

    private void setIfEmpty(Preferences prefs, String key, String value) {
        prefs.put(key, prefs.get(key, value));
    }

    @Invalidate
    public void stop() {
        // unregister the servlet
        unregisterServlet();

        // stop the download manager
        dm.stop();
        
        // unregister osd actions and web menu etc
        for (Iterator<ServiceRegistration> iterator = serviceRegs.iterator(); iterator.hasNext();) {
            ServiceRegistration sr = iterator.next();
            unregisterService(sr);
            iterator.remove();
        }
    }
    
    private void unregisterService(ServiceRegistration sr) {
        if (sr != null) {
            sr.unregister();
        }
    }

    private void unregisterServlet() {
        if(httpService != null) {
            httpService.unregister(DownloadsServlet.PATH);
            httpService.unregister(DownloadsServlet.FILE_PATH);
            httpService.unregister(DownloadsServlet.STATIC_PATH);
        }
    }

    private void registerServlet() throws ServletException, NamespaceException {
        DownloadsServlet servlet = new DownloadsServlet(dm);
        servlet.setBundleContext(ctx);
        servlet.setMessages(messages);
        servlet.setTemplateLoader(templateLoader);
        
        // register downloads servlet
        httpService.registerServlet(DownloadsServlet.PATH, servlet, null, null);
        
        // register resource context to make downloads available
        DownloadHttpContext downloadHttpContext = new DownloadHttpContext(prefs, logger);
        httpService.registerResources(DownloadsServlet.FILE_PATH, "", downloadHttpContext);
        
        // register resource context for static files
        ResourceHttpContext resourceHttpContext = new ResourceHttpContext(ctx, logger);
        httpService.registerResources(DownloadsServlet.STATIC_PATH, "/htdocs", resourceHttpContext);
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if(resourceBundle == null) {
            try {
                logger.log(LogService.LOG_DEBUG, "Loading resource bundle for " + getClass().getSimpleName());
                resourceBundle = ResourceBundleLoader.load(ctx, Locale.getDefault());
            } catch (IOException e) {
                logger.log(LogService.LOG_ERROR, "Couldn't load resource bundle", e);
            }
        }
        return resourceBundle;
    }
}
