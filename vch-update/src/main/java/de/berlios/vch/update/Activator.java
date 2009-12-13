package de.berlios.vch.update;

import java.io.IOException;
import java.net.URL;
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
import org.osgi.service.obr.RepositoryAdmin;

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

    @Requires
    private LogService logger;
    
    @Requires
    private HttpService httpService;
    
    @Requires
    private Messages messages;
    
    @Requires
    private TemplateLoader templateLoader;
    
    @Requires
    private RepositoryAdmin repoAdmin;
    
    private BundleContext ctx;
    
    private ResourceBundle resourceBundle;
    
    private UpdateServlet servlet;
    
    private ServiceRegistration menuReg;
    
    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }
    
    @Validate
    public void start() {
        try {
            registerServlet();
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't register config servlet", e);
        }
        
        loadOBRs();
    }
    
    private void loadOBRs() {
        // add repos from configuration
        try {
            List<String> obrs = servlet.getOBRs();
            for (String uri : obrs) {
                logger.log(LogService.LOG_INFO, "Adding bundle repository " + uri);
                repoAdmin.addRepository(new URL(uri));
            }
        } catch (Exception e) {
            logger.log(LogService.LOG_WARNING, "Couldn't add repository", e);
        }
    }

    private void registerServlet() throws ServletException, NamespaceException {
        // register the extensions servlet
        servlet = new UpdateServlet();
        servlet.setBundleContext(ctx);
        servlet.setMessages(messages);
        servlet.setTemplateLoader(templateLoader);
        servlet.setLogger(logger);
        httpService.registerServlet(UpdateServlet.PATH, servlet, null, null);
        
        // register the configuration servlet
        UpdateConfigServlet configServlet = new UpdateConfigServlet(servlet);
        configServlet.setBundleContext(ctx);
        configServlet.setMessages(messages);
        configServlet.setTemplateLoader(templateLoader);
        configServlet.setLogger(logger);
        httpService.registerServlet(UpdateConfigServlet.PATH, configServlet, null, null);
        
        // register resource context for static files
        ResourceHttpContext resourceHttpContext = new ResourceHttpContext(ctx, logger);
        httpService.registerResources(UpdateServlet.STATIC_PATH, "/htdocs", resourceHttpContext);
        
        // register web interface menu
        WebMenuEntry menu = new WebMenuEntry();
        menu.setTitle(getResourceBundle().getString("I18N_EXTENSIONS"));
        menu.setPreferredPosition(Integer.MAX_VALUE-1);
        menu.setLinkUri("#");
        WebMenuEntry content = new WebMenuEntry(getResourceBundle().getString("I18N_EXTENSIONS"));
        content.setLinkUri(UpdateServlet.PATH);
        menu.getChilds().add(content);
        WebMenuEntry config = new WebMenuEntry(getResourceBundle().getString("I18N_CONFIG"));
        config.setLinkUri(UpdateConfigServlet.PATH);
        content.getChilds().add(config);
        menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
    }

    @Invalidate
    public void stop() {
        unregisterServlet();
        if(menuReg != null) {
            menuReg.unregister();
        }
    }

    private void unregisterServlet() {
        if(httpService != null) {
            httpService.unregister(UpdateServlet.PATH);
            httpService.unregister(UpdateServlet.STATIC_PATH);
            httpService.unregister(UpdateConfigServlet.PATH);
        }
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if(resourceBundle == null) {
            try {
                logger.log(LogService.LOG_INFO, "Loading resource bundle for " + getClass().getSimpleName());
                resourceBundle = ResourceBundleLoader.load(ctx, Locale.getDefault());
            } catch (IOException e) {
                logger.log(LogService.LOG_ERROR, "Couldn't load resource bundle", e);
            }
        }
        return resourceBundle;
    }
}
