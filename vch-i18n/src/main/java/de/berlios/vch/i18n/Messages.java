package de.berlios.vch.i18n;


public interface Messages {
    public String translate(String key, Object... args);
    public ResourceBundleProvider[] getResourceBundleProviders();
}
