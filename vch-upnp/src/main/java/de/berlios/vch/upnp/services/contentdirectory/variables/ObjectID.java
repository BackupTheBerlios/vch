package de.berlios.vch.upnp.services.contentdirectory.variables;

import org.osgi.service.upnp.UPnPStateVariable;

public class ObjectID implements UPnPStateVariable {

    @Override
    public String[] getAllowedValues() {
        return null;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public Class<String> getJavaDataType() {
        return String.class;
    }

    @Override
    public Number getMaximum() {
        return null;
    }

    @Override
    public Number getMinimum() {
        return null;
    }

    @Override
    public String getName() {
        return "A_ARG_TYPE_ObjectID";
    }

    @Override
    public Number getStep() {
        return null;
    }

    @Override
    public String getUPnPDataType() {
        return UPnPStateVariable.TYPE_STRING;
    }

    @Override
    public boolean sendsEvents() {
        return false;
    }

}
