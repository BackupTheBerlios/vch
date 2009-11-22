package de.berlios.vch.config;

import java.util.prefs.Preferences;

public interface ConfigService {
    public Preferences getUserPreferences(String node);
}
