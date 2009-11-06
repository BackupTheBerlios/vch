package de.berlios.vch.parser.console;

import org.apache.felix.shell.Command;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.berlios.vch.parser.console.commands.GetWebPage;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext ctx) throws Exception {
        ctx.registerService(Command.class.getName(), new GetWebPage(ctx), null);
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {}

}
