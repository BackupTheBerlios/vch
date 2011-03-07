package de.berlios.vch.search.web;

import java.io.IOException;
import java.util.Locale;
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
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.parser.service.IParserService;
import de.berlios.vch.search.ISearchService;
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
    private TemplateLoader templateLoader;

    @Requires
    private Messages i18n;

    @Requires
    private HttpService http;

    @Requires
    private ISearchService searchService;

    private BundleContext ctx;

    private ResourceBundle resourceBundle;

    private ServiceRegistration menuReg;

    @Requires
    private IParserService parserService;

    public Activator(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Validate
    public void start() throws ServletException, NamespaceException {
        // register search servlet // TODO ipojo
        SearchServlet servlet = new SearchServlet(this);
        servlet.setBundleContext(ctx);
        servlet.setLogger(logger);
        servlet.setTemplateLoader(templateLoader);
        servlet.setMessages(i18n);

        http.registerServlet(SearchServlet.PATH, servlet, null, null);

        // register resource context for static files
        ResourceHttpContext resourceHttpContext = new ResourceHttpContext(ctx, logger);
        http.registerResources(SearchServlet.STATIC_PATH, "/htdocs", resourceHttpContext);

        // register web interface menu
        IWebMenuEntry menu = new WebMenuEntry(getResourceBundle().getString("I18N_SEARCH"));
        menu.setPreferredPosition(Integer.MIN_VALUE + 1);
        menu.setLinkUri("#");
        SortedSet<IWebMenuEntry> childs = new TreeSet<IWebMenuEntry>();
        IWebMenuEntry entry = new WebMenuEntry();
        entry.setTitle(getResourceBundle().getString("I18N_SEARCH"));
        entry.setLinkUri(SearchServlet.PATH);
        childs.add(entry);
        menu.setChilds(childs);
        menuReg = ctx.registerService(IWebMenuEntry.class.getName(), menu, null);
    }

    @Invalidate
    public void stop() {
        if (http != null) {
            http.unregister(SearchServlet.PATH);
            http.unregister(SearchServlet.STATIC_PATH);
        }

        // unregister the web menu
        if (menuReg != null) {
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

    ISearchService getSearchService() {
        return searchService;
    }

    IParserService getParserService() {
        return parserService;
    }
}
