package de.berlios.vch.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import de.berlios.vch.Config;

public class PropertiesLoader {
    
    public static Properties loadFromJar(String path) throws IOException {
        InputStream in = Config.class.getResourceAsStream(path);
        Properties props = new Properties();
        props.load(in);
        return props;
    }
}
