package de.berlios.vch.http;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.freemarker.Freemarker;
import de.berlios.vch.http.handler.ConfigGroupsHandler;
import de.berlios.vch.http.handler.ConfigHandler;
import de.berlios.vch.http.handler.CustomChannelHandler;
import de.berlios.vch.http.handler.DownloadHandler;
import de.berlios.vch.http.handler.FileHandler;
import de.berlios.vch.i18n.Messages;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

public class TemplateLoader {
    
    private static transient Logger logger = LoggerFactory.getLogger(TemplateLoader.class);
    
    public static String loadTemplate(String filename) {
        return loadTemplate(filename, new HashMap<String, Object>());
    }
    
    public static String loadTemplate(String filename, Map<String, Object> params) {
        addDefaultParameters(params);
        convertI18NKeys(params);
        
        String page = null;
        Configuration config = Freemarker.getInstance().getConfiguration();
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

    private static void convertI18NKeys(Map<String, Object> params) {
        // replace all placeholders by the given parameters
        for (Iterator<String> iterator = params.keySet().iterator(); iterator.hasNext();) {
            // if param indicates a localized string, get the value from the resource bundle
            String param = iterator.next();
            if(param.startsWith("I18N_")) {
                String value = (String)params.get(param);
                value = Messages.getBundle().getString(value);
                params.put(param, value);
            }
        }
    }

    private static String navigation;
    private static String loadNavigation() {
        if(navigation == null) {
            Map<String, Object> templateParams = new HashMap<String, Object>();
            templateParams.put("I18N_CHANNELS", "navigation.channels");
            templateParams.put("I18N_CONFIG", "navigation.config");
            templateParams.put("I18N_GROUPS", "navigation.groups");
            templateParams.put("I18N_HELP", "navigation.help");
            templateParams.put("I18N_DOWNLOADS", "navigation.downloads");
            
            HandlerMapping hm = Config.getInstance().getHandlerMapping();
            String customPath = hm.getPath(CustomChannelHandler.class);
            String configPath = hm.getPath(ConfigHandler.class);
            String groupConfigPath = hm.getPath(ConfigGroupsHandler.class);
            String downloadsPath = hm.getPath(DownloadHandler.class);
            templateParams.put("CUSTOM_PATH", customPath);
            templateParams.put("CONFIG_PATH", configPath);
            templateParams.put("GROUP_CONFIG_PATH", groupConfigPath);
            templateParams.put("DOWNLOADS_PATH", downloadsPath);
            String helpRoot = Config.getInstance().getHandlerMapping().getPath(FileHandler.class) + "/help";
            templateParams.put("HELP_ROOT", helpRoot);
            templateParams.put("HELP_PATH", (Locale.getDefault() + "/index.html"));
            
            convertI18NKeys(templateParams);
            
            Configuration config = Freemarker.getInstance().getConfiguration();
            try {
                Template tpl = config.getTemplate("navigation.ftl");
                StringWriter out = new StringWriter();
                tpl.process(templateParams, out);
                out.flush();
                navigation = out.toString();
            } catch (IOException e) {
                logger.error("Couldn't load navigation template", e);
            } catch (TemplateException e) {
                logger.error("Error while processing the navigation template", e);
            }
        }
        return navigation;
    }
    
    private static void addDefaultParameters(Map<String, Object> params) {
        // docroot
        String docRoot = Config.getInstance().getHandlerMapping().getPath(FileHandler.class);
        params.put("DOCROOT", docRoot);
        
        // helproot
        params.put("HELP_ROOT", docRoot + "/help");
        
        // page encoding
        params.put("ENCODING", Config.getInstance().getProperty("html.encoding"));
        
        String version = Config.getInstance().getManifestProperty("VCH-Version");
        String revision = Config.getInstance().getManifestProperty("VCH-Revision");
        params.put("VERSION", version != null ? version : "");
        params.put("REVISION", revision != null ? revision : "");
        
        // navigation 
        params.put("NAVIGATION", loadNavigation());
    }
}
