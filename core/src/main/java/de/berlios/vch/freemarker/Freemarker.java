package de.berlios.vch.freemarker;

import java.util.Locale;

import de.berlios.vch.Config;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;

public class Freemarker {
    private static Freemarker instance;

    private Configuration config;
    
    private Freemarker() {
        config = new Configuration();
        config.setTemplateLoader(new ClassTemplateLoader(Config.class,"/templates/"));
        config.setObjectWrapper(new DefaultObjectWrapper());
        config.setEncoding(Locale.getDefault(), Config.getInstance().getProperty("html.encoding"));
        config.setURLEscapingCharset(Config.getInstance().getProperty("html.encoding"));
    }

    public static Freemarker getInstance() {
        if (instance == null) {
            instance = new Freemarker();
        }
        return instance;
    }
    
    public Configuration getConfiguration() {
        return config;
    }
}