package de.berlios.vch.playlist.web;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.playlist.PlaylistService;
import de.berlios.vch.uri.IVchUriResolveService;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;

@Component
public class Activator implements ResourceBundleProvider {

    @Requires
    private LogService logger;

    @Requires
    private TemplateLoader templateLoader;

    @Requires
    private Messages i18n;

    @Requires
    private HttpService http;
    
    @Requires
    private PlaylistService playlistService;

    private BundleContext ctx;

    private ResourceBundle resourceBundle;
    
    private ServiceRegistration menuReg;
    
    @Requires
    private IVchUriResolveService uriResolver;

    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Validate
    public void start() throws ServletException, NamespaceException {
        // register translation
        i18n.addProvider(this);
        
        // register playlist servlet
        PlaylistServlet servlet = new PlaylistServlet(this);
        servlet.setBundleContext(ctx);
        servlet.setLogger(logger);
        servlet.setTemplateLoader(templateLoader);
        servlet.setMessages(i18n);

        http.registerServlet(PlaylistServlet.PATH, servlet, null, null);
        
        // register web interface menu
        IWebMenuEntry menu = new WebMenuEntry(i18n.translate("I18N_PLAYLIST"));
        menu.setPreferredPosition(Integer.MIN_VALUE + 10);
        menu.setLinkUri("#");
        SortedSet<IWebMenuEntry> childs = new TreeSet<IWebMenuEntry>();
        IWebMenuEntry entry = new WebMenuEntry();
        entry.setTitle(i18n.translate("I18N_MANAGE"));
        entry.setLinkUri(PlaylistServlet.PATH);
        childs.add(entry);
        menu.setChilds(childs);
        menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
    }

    @Invalidate
    public void stop() {
        if (http != null) {
            http.unregister(PlaylistServlet.PATH);
        }
        
        // unregister the web menu
        if(menuReg != null) {
            menuReg.unregister();
        }
        
        i18n.removeProvider(this);
    }
    
    PlaylistService getPlaylistService() {
        return playlistService;
    }
    
    IVchUriResolveService getUriResolverService() {
        return uriResolver;
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
