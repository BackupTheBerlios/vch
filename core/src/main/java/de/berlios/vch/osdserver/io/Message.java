package de.berlios.vch.osdserver.io;

import java.io.Serializable;

/**
 * Represents a OsdMessage of the osdserver protocol. Either a command or a response.
 * 
 * @author <a href="mailto:henrik.niehaus@gmx.de">Henrik Niehaus</a>
 */
public abstract class Message implements Serializable {

    /**
     * Returns a String representation for debug purposes
     */
    public abstract String toString();
}
