package de.berlios.vch.android.web;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

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

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
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
    
    private List<ServiceRegistration> serviceRegs = new LinkedList<ServiceRegistration>();
   
    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Validate
    public void start() {
        try {
            // register downloads and configuration servlet
            registerServlets();
            
            // register web interface menu
            WebMenuEntry menu = new WebMenuEntry();
            menu.setTitle("Android");
            menu.setPreferredPosition(Integer.MAX_VALUE);
            menu.setLinkUri(Servlet.PATH);
            ServiceRegistration sr = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
            serviceRegs.add(sr);
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't start android web servlet", e);
        }
    }

    @Invalidate
    public void stop() {
        // unregister the servlet
        unregisterServlets();

        // unregister web menu etc
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

    private void unregisterServlets() {
        if(httpService != null) {
            httpService.unregister(Servlet.PATH);
            httpService.unregister(Servlet.STATIC_PATH);
        }
    }

    private void registerServlets() throws ServletException, NamespaceException {
        Servlet servlet = new Servlet();
        servlet.setBundleContext(ctx);
        servlet.setMessages(messages);
        servlet.setTemplateLoader(templateLoader);
        servlet.setLogger(logger);
        
        // register servlet
        httpService.registerServlet(Servlet.PATH, servlet, null, null);
        
        // register resource context for static files
        ResourceHttpContext resourceHttpContext = new ResourceHttpContext(ctx, logger);
        httpService.registerResources(Servlet.STATIC_PATH, "/htdocs", resourceHttpContext);
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
