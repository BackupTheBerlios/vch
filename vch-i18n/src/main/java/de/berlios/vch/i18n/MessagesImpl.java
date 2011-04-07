package de.berlios.vch.i18n;

import java.text.MessageFormat;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.service.log.LogService;

@Component
@Provides
public class MessagesImpl implements Messages {
    @Requires 
    LogService log;
    
    @Requires(optional=true)
    private ResourceBundleProvider[] providers;
    
    public String translate(String key, Object... args) {
        if(providers != null) {
            for (ResourceBundleProvider provider : providers) {
                String msg = null;
                try {
                    msg = provider.getResourceBundle().getString(key);
                } catch (Exception e) {}
                
                if(msg != null) {
                    return MessageFormat.format(msg, args);
                }
            }
        }
        return "I18N_NOT_FOUND [" + key + "]";
    }
    
    public ResourceBundleProvider[] getResourceBundleProviders() {
        return providers;
    }
}