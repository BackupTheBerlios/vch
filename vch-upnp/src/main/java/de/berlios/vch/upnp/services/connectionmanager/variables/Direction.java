package de.berlios.vch.upnp.services.connectionmanager.variables;

import org.osgi.service.upnp.UPnPStateVariable;

public class Direction implements UPnPStateVariable {

    @Override
    public String[] getAllowedValues() {
        return new String[] {"Input", "Output"};
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
        return "A_ARG_TYPE_Direction";
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
