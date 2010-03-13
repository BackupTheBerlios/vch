package de.berlios.vch.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceBundleLoader {

    private static transient Logger logger = LoggerFactory.getLogger(ResourceBundleLoader.class);

    public static ResourceBundle load(BundleContext ctx, Locale locale) throws IOException {
        logger.debug("Loading messages for locale {} for bundle {}", locale.toString(), ctx.getBundle()
                .getSymbolicName());
        URL propsUrl = ctx.getBundle().getResource("/lang/lang_" + locale.toString() + ".properties");
        if(propsUrl == null) {
            // use fallback locale
            logger.debug("Messages not found. Falling back to locale {} for bundle {}", Locale.UK, ctx.getBundle()
                    .getSymbolicName());
            propsUrl = ctx.getBundle().getResource("/lang/lang_" + Locale.UK + ".properties");
        }
        InputStream in = propsUrl.openStream();
        return new PropertyResourceBundle(in);
    }
}
