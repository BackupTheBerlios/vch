package de.berlios.vch.i18n;

import java.util.List;

public interface Messages {
    public String translate(String key, Object... args);
    public List<ResourceBundleProvider> getResourceBundleProviders();
    public void addProvider(ResourceBundleProvider rbp);
    public void removeProvider(ResourceBundleProvider rbp);
}
