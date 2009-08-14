package de.berlios.vch.osdserver.io.command;

public class Quit extends Command {

    @Override
    public String getCommand() {
        return "quit";
    }

    @Override
    public String toString() {
        return "Quit";
    }

}
