package de.berlios.vch.osdserver.io.svdrp;

import org.hampelratte.svdrp.Command;

public class CheckXineliboutputSvdrpInterface extends Command {

    @Override
    public String getCommand() {
        return "plug xineliboutput help pmda";
    }

    @Override
    public String toString() {
        return "Xineliboutput plugin PMDA test";
    }

}
