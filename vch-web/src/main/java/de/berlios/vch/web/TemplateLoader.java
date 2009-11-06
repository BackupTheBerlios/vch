package de.berlios.vch.web;

import java.util.Map;

public interface TemplateLoader {
    public String loadTemplate(String filename);
    
    public String loadTemplate(String filename, Map<String, Object> params);
}
