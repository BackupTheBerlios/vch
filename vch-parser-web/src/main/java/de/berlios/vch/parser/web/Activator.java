package de.berlios.vch.parser.web;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.IParserService;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.web.ResourceHttpContext;
import de.berlios.vch.web.TemplateLoader;
import de.berlios.vch.web.menu.IWebMenuEntry;
import de.berlios.vch.web.menu.WebMenuEntry;

@Component
@Provides
public class Activator implements ResourceBundleProvider {
    
    private Map<String, ServiceRegistration> registrations = new HashMap<String, ServiceRegistration>();
    
    private ServiceTracker parserTracker;
    
    private BundleContext ctx;
    
    @Requires
    private LogService logger;
    
    @Requires
    private Messages messages;
    
    @Requires
    private TemplateLoader templateLoader;
    
    @Requires
    private HttpService http;
    
    @Requires
    private IParserService parserService;
    
    private ResourceBundle resourceBundle;
    
    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Validate
    public void start() throws Exception {
        openParserTracker(ctx);
        registerServlet();
    }

    private void registerServlet() throws ServletException, NamespaceException {
        BrowseServlet servlet = new BrowseServlet(parserService);
        servlet.setBundleContext(ctx);
        servlet.setMessages(messages);
        servlet.setTemplateLoader(templateLoader);
        servlet.setLogger(logger);
        http.registerServlet(BrowseServlet.PATH, servlet, null, null);
        
        // register resource context for static files
        ResourceHttpContext resourceHttpContext = new ResourceHttpContext(ctx, logger);
        http.registerResources(BrowseServlet.STATIC_PATH, "/htdocs", resourceHttpContext);
    }
    
    private void unregisterServlet() {
        if(http != null) {
            http.unregister(BrowseServlet.PATH);
        }
    }

    private void openParserTracker(final BundleContext ctx) {
        parserTracker = new ServiceTracker(ctx, IWebParser.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                IWebParser parser = (IWebParser) ctx.getService(reference);
                IWebMenuEntry parserEntry = new WebMenuEntry(messages.translate("I18N_BROWSE"));
                parserEntry.setLinkUri("#");
                SortedSet<IWebMenuEntry> childs = new TreeSet<IWebMenuEntry>();
                IWebMenuEntry entry = new WebMenuEntry();
                entry.setTitle(parser.getTitle());
                entry.setLinkUri("/parser?id=" + parser.getClass().getName());
                childs.add(entry);
                parserEntry.setChilds(childs);

                Dictionary<String, String> options = new Hashtable<String, String>();
                options.put("vch.parser", parser.getClass().getName());
                ServiceRegistration reg = ctx.registerService(IWebMenuEntry.class.getName(), parserEntry, options);
                registrations.put(parser.getClass().getName(), reg);
                
                return super.addingService(reference);
            }
            
            @Override
            public void removedService(ServiceReference reference, Object service) {
                ServiceRegistration reg = registrations.get(service.getClass().getName());
                registrations.remove(service.getClass().getName());
                if (reg != null) {
                    IWebMenuEntry menu = (IWebMenuEntry) ctx.getService(reg.getReference());
                    logger.log(LogService.LOG_INFO, "Unregistering web menu entry \n " + createMenuTree(menu, ""));
                    reg.unregister();
                }

                super.removedService(reference, service);
            }
        };
        parserTracker.open();
    }

    private StringBuilder createMenuTree(IWebMenuEntry menu, String indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(menu.getTitle()).append('\n');
        if(menu.getChilds() != null) {
            for (IWebMenuEntry entry : menu.getChilds()) {
                sb.append(createMenuTree(entry, indent + "  "));
            }
        }
        return sb;
    }
    
    @Invalidate
    public void stop() throws Exception {
        if(parserTracker != null) {
            parserTracker.close();
        }
        
        for (ServiceRegistration reg : registrations.values()) {
            reg.unregister();
        }
        
        unregisterServlet();
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
