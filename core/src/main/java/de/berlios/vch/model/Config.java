package de.berlios.vch.model;

public class Config {
    private String parameterKey;

    private String parameterValue;

    public String getParameterKey() {
        return parameterKey;
    }

    public void setParameterKey(String key) {
        this.parameterKey = key;
    }
    
    public String getParameterValue() {
        return parameterValue;
    }

    public void setParameterValue(String value) {
        this.parameterValue = value;
    }
}