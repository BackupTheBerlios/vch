package de.berlios.vch.i18n;

import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.service.log.LogService;

/* TODO problem lösen, dass manche bundles schon auf messages zugreifen, bevor
 * andere sich registriert haben. dadurch können übersetzungen fehlen.
 */
@Component
@Provides
public class MessagesImpl implements Messages {
    @Requires 
    LogService logger;
    
    private List<ResourceBundleProvider> providers = new LinkedList<ResourceBundleProvider>();
    
    public String translate(String key, Object... args) {
        for (ResourceBundleProvider provider : providers) {
            String msg = null;
            try {
                msg = provider.getResourceBundle().getString(key);
            } catch (Exception e) {}
            
            if(msg != null) {
                return MessageFormat.format(msg, args);
            } 
        }
        return "I18N_NOT_FOUND [" + key + "]";
    }
    
    public List<ResourceBundleProvider> getResourceBundleProviders() {
        return providers;
    }

    @Override
    public void addProvider(ResourceBundleProvider rbp) {
        providers.add(rbp);
        logger.log(LogService.LOG_DEBUG, "ResourceBundleProvider registered ["+rbp.getClass().getName()+"]. #RBPs: " + providers.size());
    }

    @Override
    public void removeProvider(ResourceBundleProvider rbp) {
        providers.remove(rbp);
        logger.log(LogService.LOG_DEBUG, "ResourceBundleProvider removed ["+rbp.getClass().getName()+"]. #RBPs: " + providers.size());
    }
}