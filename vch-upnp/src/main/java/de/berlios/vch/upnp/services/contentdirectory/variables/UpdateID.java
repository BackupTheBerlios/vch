package de.berlios.vch.upnp.services.contentdirectory.variables;

import org.osgi.service.upnp.UPnPStateVariable;

public class UpdateID implements UPnPStateVariable {

    @Override
    public String[] getAllowedValues() {
        return null;
    }

    @Override
    public Object getDefaultValue() {
        return null;
    }

    @Override
    public Class<Long> getJavaDataType() {
        return Long.class;
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
        return "A_ARG_TYPE_UpdateID";
    }

    @Override
    public Number getStep() {
        return null;
    }

    @Override
    public String getUPnPDataType() {
        return UPnPStateVariable.TYPE_UI4;
    }

    @Override
    public boolean sendsEvents() {
        return false;
    }

}
