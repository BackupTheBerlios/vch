package de.berlios.vch.osdserver.io.command;

public class Version extends Command {

    private String version = "0.1";
    
    public Version() {}
    
    public Version(String version) {
        this.version = version;
    }
    
    @Override
    public String getCommand() {
        return "version " + version;
    }

    @Override
    public String toString() {
        return "Version " + version;
    }

}
