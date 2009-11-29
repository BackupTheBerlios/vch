package de.berlios.vch.update;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.ServletException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.BundleContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
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
    
    private BundleContext ctx;
    
    private ResourceBundle resourceBundle;
    
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
    }
    
    private void registerServlet() throws ServletException, NamespaceException {
        UpdateServlet servlet = new UpdateServlet();
        servlet.setBundleContext(ctx);
        servlet.setMessages(messages);
        servlet.setTemplateLoader(templateLoader);
        httpService.registerServlet(UpdateServlet.PATH, servlet, null, null);
        
        // register web interface menu
        WebMenuEntry config = new WebMenuEntry();
        config.setTitle(getResourceBundle().getString("I18N_EXTENSIONS"));
        config.setPreferredPosition(Integer.MAX_VALUE-1);
        config.setLinkUri("#");
        WebMenuEntry content = new WebMenuEntry(getResourceBundle().getString("I18N_EXTENSIONS"));
        content.setLinkUri(UpdateServlet.PATH);
        config.getChilds().add(content);
        ctx.registerService(IWebMenuEntry.class.getName(), config, null);
    }

    @Invalidate
    public void stop() {
        unregisterServlet();
    }

    private void unregisterServlet() {
        if(httpService != null) {
            httpService.unregister(UpdateServlet.PATH);
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
