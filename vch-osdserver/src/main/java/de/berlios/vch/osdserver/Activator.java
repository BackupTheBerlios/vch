package de.berlios.vch.osdserver;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.prefs.Preferences;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.config.ConfigService;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.service.IParserService;
import de.berlios.vch.playlist.PlaylistService;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;

@Component
@Provides
public class Activator implements ResourceBundleProvider {

    @Requires
    private LogService logger;

    public static ServiceTracker parserServiceTracker;

    private BundleContext ctx;

    @Requires
    private Messages i18n;

    @Requires
    private HttpService httpService;

    @Requires
    private ConfigService cs;
    
    @Requires
    private TemplateLoader templateLoader;
    
    @Requires
    private PlaylistService playlistService;

    private ResourceBundle resourceBundle;
    
    private ServiceRegistration menuReg;

    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Validate
    public void start() {
        parserServiceTracker = new ServiceTracker(ctx, IParserService.class.getName(), null);
        parserServiceTracker.open();

        try {
            httpService.registerServlet("/osdserver", new ActivatorServlet(ctx, i18n, playlistService), null, null);
            registerConfigServlet();
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't register osdserver startup servlet", e);
        }
    }

    private void registerConfigServlet() {
        Preferences prefs = cs.getUserPreferences(ctx.getBundle().getSymbolicName());
        ConfigServlet servlet = new ConfigServlet(prefs);
        servlet.setLogger(logger);
        servlet.setBundleContext(ctx);
        servlet.setMessages(i18n);
        servlet.setTemplateLoader(templateLoader);
        try {
            // register the servlet
            httpService.registerServlet(ConfigServlet.PATH, servlet, null, null);

            // register web interface menu
            IWebMenuEntry menu = new WebMenuEntry(getResourceBundle().getString("I18N_CONFIGURATION"));
            menu.setPreferredPosition(Integer.MAX_VALUE - 1);
            menu.setLinkUri("#");
            SortedSet<IWebMenuEntry> childs = new TreeSet<IWebMenuEntry>();
            IWebMenuEntry entry = new WebMenuEntry();
            entry.setTitle("Osdserver");
            entry.setLinkUri(ConfigServlet.PATH);
            childs.add(entry);
            menu.setChilds(childs);
            menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't register osdserver config servlet", e);
        }
    }

    @Invalidate
    public void stop() {
        parserServiceTracker.close();
        parserServiceTracker = null;
        
        // unregister the config servlet
        if(httpService != null) {
            httpService.unregister("/osdserver");
            httpService.unregister(ConfigServlet.PATH);
        }
        
        // unregister the web menu
        if(menuReg != null) {
            menuReg.unregister();
        }
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
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
