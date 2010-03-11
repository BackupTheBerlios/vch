package de.berlios.vch.upnp.services.connectionmanager.variables;

import org.osgi.service.upnp.UPnPStateVariable;

public class RcsID implements UPnPStateVariable {

    @Override
    public String[] getAllowedValues() {
        return null;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public Class<java.lang.Integer> getJavaDataType() {
        return Integer.class;
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
        return "A_ARG_TYPE_RcsID";
    }

    @Override
    public Number getStep() {
        return null;
    }

    @Override
    public String getUPnPDataType() {
        return UPnPStateVariable.TYPE_I4;
    }

    @Override
    public boolean sendsEvents() {
        return false;
    }

}
