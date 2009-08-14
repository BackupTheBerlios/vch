package de.berlios.vch.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Messages {
    private static transient Logger logger = LoggerFactory.getLogger(Messages.class);
    
    private ResourceBundle bundle;

    private static Messages instance;

    private Messages() {
        try {
            loadBundle(Locale.getDefault());
        } catch (IOException e) {
            logger.error("Couldn't load resource bundle for locale " + Locale.getDefault() + ". Loading defaults...", e);
            try {
                loadBundle(Locale.ENGLISH);
            } catch (IOException e1) {
                logger.error("Couldn't load default resource bundle", e);
            }
        }
    }

    public static ResourceBundle getBundle() {
        if (instance == null) {
            instance = new Messages();
        }
        return instance.getResourceBundle();
    }
    
    @SuppressWarnings("unchecked")
    public static String translate(Class clazz, String key) {
        key = clazz.getName() + "." + key;
        return getBundle().getString(key);
    }
    
    private void loadBundle(Locale locale) throws IOException {
        logger.debug("Loading messages for locale " + locale.toString());
        InputStream in = Messages.class.getResourceAsStream("/lang/lang_" + locale.toString() + ".properties");
        bundle = new PropertyResourceBundle(in);
    }
    
    private ResourceBundle getResourceBundle() {
        return bundle;
    }

    /**
     * Translates a given key and replaces placeholders with the given parameters.
     * @param clazz
     * @param key
     * @param params
     *      An Object[] of the params, which will be used to replace the placeholders.
     *      The message has to be compatible to {@link MessageFormat}
     * @return
     */
    @SuppressWarnings("unchecked")
    public static String translate(Class clazz, String key, Object[] params) {
        String mesg = translate(clazz, key);
        MessageFormat format = new MessageFormat("");
        format.applyPattern(mesg);
        return format.format(params);
    }
}