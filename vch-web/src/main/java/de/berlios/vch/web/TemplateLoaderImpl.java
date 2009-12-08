package de.berlios.vch.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.web.menu.MenuBuilder;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

@Component
@Provides
public class TemplateLoaderImpl implements TemplateLoader {
    
    private static transient Logger logger = LoggerFactory.getLogger(TemplateLoaderImpl.class);
    
    private MenuBuilder menuBuilder;
    
    @Requires
    private Messages i18n;
    
    private Configuration config;
    
    public TemplateLoaderImpl(BundleContext ctx) {
        menuBuilder = new MenuBuilder(ctx);
        config = new Configuration();
        //config.setTemplateLoader(new ClassTemplateLoader(Activator.class, "/templates/"));
        config.setTemplateLoader(new BundleTemplateLoader(ctx, "/templates/"));
        config.setObjectWrapper(new DefaultObjectWrapper());
        config.setEncoding(Locale.getDefault(), "UTF-8"); // TODO config param?
        config.setURLEscapingCharset("UTF-8"); // TODO config param?
    }

    public String loadTemplate(String filename) {
        return loadTemplate(filename, new HashMap<String, Object>());
    }
    
    public String loadTemplate(String filename, Map<String, Object> params) {
        addDefaultParameters(params);
        addI18nParams(params);
        addIncludes(params);
        
        String page = null;
        try {
            Template tpl = config.getTemplate(filename);
            StringWriter out = new StringWriter();
            tpl.process(params, out);
            out.flush();
            page = out.toString();
        } catch (IOException e) {
            logger.error("Couldn't load freemarker template", e);
        } catch (TemplateException e) {
            logger.error("Error while processing the template", e);
        }
        
        return page;
    }
    
    

    public void processTemplate(String filename, Map<String, Object> params, Writer writer) {
        addDefaultParameters(params);
        addI18nParams(params);
        addIncludes(params);

        try {
            Template tpl = config.getTemplate(filename);
            tpl.process(params, writer);
        } catch (IOException e) {
            logger.error("Couldn't load freemarker template", e);
        } catch (TemplateException e) {
            logger.error("Error while processing the template", e);
        }
    }

    private void addI18nParams(Map<String, Object> params) {
        params.put("LOCALE", Locale.getDefault().toString());
        for (ResourceBundleProvider provider : i18n.getResourceBundleProviders()) {
            Enumeration<String> keys = provider.getResourceBundle().getKeys();
            while(keys.hasMoreElements()) {
                String key = keys.nextElement();
                params.put(key, provider.getResourceBundle().getObject(key));
            }
        }
    }

    private void addDefaultParameters(Map<String, Object> params) {
        // page encoding
        params.put("ENCODING", "UTF-8"/*Config.getInstance().getProperty("html.encoding")*/); // TODO config param?
//        
//          String version = Config.getInstance().getManifestProperty("VCH-Version");
//          String revision = Config.getInstance().getManifestProperty("VCH-Revision");
//          params.put("VERSION", version != null ? version : "");
//          params.put("REVISION", revision != null ? revision : "");
        
        params.put("VERSION", "");
        params.put("REVISION", "");
        
        // navigation 
        params.put("NAVIGATION", menuBuilder.getMenu());
    }
    
    private void addIncludes(Map<String, Object> params) {
        // add javascripts
        StringBuilder includes = new StringBuilder();
        @SuppressWarnings("unchecked")
        List<String> js = (List<String>) params.get("JS_INCLUDES");
        if(js != null) {
            for (String uri : js) {
                includes.append("<script type=\"text/javascript\" src=\"");
                includes.append(uri);
                includes.append("\"></script>\n");
            }
        }
        params.put("JAVASCRIPT", includes.toString());
        
        // add stylesheets
        includes = new StringBuilder();
        @SuppressWarnings("unchecked")
        List<String> css = (List<String>) params.get("CSS_INCLUDES");
        if(css != null) {
            for (String uri : css) {
                includes.append("<link type=\"text/css\" rel=\"stylesheet\" href=\"");
                includes.append(uri);
                includes.append("\" />\n");
            }
        }
        params.put("CSS", includes.toString());
    }
    
    class BundleTemplateLoader implements freemarker.cache.TemplateLoader {
        private BundleContext ctx;
        
        private String path;
        
        public BundleTemplateLoader(BundleContext ctx, String path) {
            this.ctx = ctx;
            this.path = path;
        }
        
        @Override
        public void closeTemplateSource(Object templateSource) throws IOException {}

        @Override
        public Object findTemplateSource(String name) throws IOException {
            for (Bundle bundle : ctx.getBundles()) {
                URL src = bundle.getResource(path + name);
                if(src != null) {
                    return src;
                }
            }
            return null; 
        }

        @Override
        public long getLastModified(Object templateSource) {
            return ctx.getBundle().getLastModified();
        }

        @Override
        public Reader getReader(Object templateSource, String encoding) throws IOException {
            URL src = (URL) templateSource;
            return new InputStreamReader(src.openStream());
        }
    }
}
