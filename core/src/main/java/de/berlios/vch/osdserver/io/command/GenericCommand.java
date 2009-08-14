package de.berlios.vch.osdserver.io.command;

public class GenericCommand extends Command {

    private String cmd;
    
    public GenericCommand(String cmd) {
        super();
        this.cmd = cmd;
    }

    @Override
    public String getCommand() {
        return cmd;
    }

    @Override
    public String toString() {
        return cmd;
    }

}
