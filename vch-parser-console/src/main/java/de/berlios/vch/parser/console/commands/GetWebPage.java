package de.berlios.vch.parser.console.commands;

import java.io.PrintStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.apache.felix.shell.Command;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.WebPageTitleComparator;

// TODO don't keep references to the parsers, so that parser updates immediately take effect
public class GetWebPage implements Command {

    private BundleContext ctx = null;

    private ServiceTracker st;

    private Stack<IWebPage> menuStack = new Stack<IWebPage>();

    public GetWebPage(BundleContext context) {
        ctx = context;
        st = new ServiceTracker(ctx, IWebParser.class.getName(), null);
        st.open();
    }

    @Override
    public void execute(String cmd, PrintStream out, PrintStream err) {
        try {
            String[] args = cmd.split(" ");
            if (args.length == 1) {
                if (menuStack.isEmpty()) {
                    IOverviewPage page = getParsers();
                    menuStack.push(page);
                }
                showMenu(out, err, menuStack.peek());
            } else {
                if ("..".equals(args[1])) {
                    if (menuStack.isEmpty())
                        return;

                    if (menuStack.size() > 1) {
                        menuStack.pop();
                    }
                    showMenu(out, err, menuStack.peek());
                } else {
                    if (menuStack.size() <= 0) {
                        err.println("Page not found");
                        return;
                    }

                    IWebPage currentPage = menuStack.peek();
                    if (currentPage instanceof IOverviewPage) {
                        int id = Integer.parseInt(args[1]);
                        IOverviewPage opage = (IOverviewPage) currentPage;
                        for (IWebPage page : opage.getPages()) {
                            int pageId = (Integer) page.getUserData().get("menu_id");
                            if (pageId == id) {
                                IWebParser parser = getParser(page.getParser());
                                if(parser != null) {
                                    out.println("Loading page. Please wait...");
                                    if (page.getUri() != null && "vchpage://root".equals(page.getUri().toString())) {
                                        menuStack.push(parser.getRoot());
                                        showMenu(out, err, menuStack.peek());
                                    } else {
                                        menuStack.push(parser.parse(page));
                                        showMenu(out, err, menuStack.peek());
                                    }
                                    return;
                                } else {
                                    err.println("This parser is not available anymore");
                                }
                            }
                        }

                        err.println("Page with ID " + id + " not found");
                    } else {
                        IWebParser parser = getParser(currentPage.getParser());
                        if(parser != null) {
                            IWebPage nextLevel = parser.parse(currentPage);
                            showMenu(out, err, nextLevel);
                        } else {
                            err.println("This parser is not available anymore");
                        }
                    }
                }
            }
        } catch (Exception e) {
            err.println("Couldn't execute command: " + cmd);
            e.printStackTrace(err);
        }
    }

    private void showMenu(PrintStream out, PrintStream err, IWebPage page) throws Exception {
        int count = 0;
        if (page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            for (IWebPage wp : opage.getPages()) {
                wp.getUserData().put("menu_id", count);
                out.println("[" + count + "] " + wp.getTitle());
                count++;
            }
        } else if (page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            out.println(page.getTitle());
            out.println("Length: " + TimeUnit.SECONDS.toMinutes(vpage.getDuration()) + " mins");
            out.println("Online since: " + SimpleDateFormat.getDateTimeInstance().format(vpage.getPublishDate().getTime()));
            out.println("URI: " + vpage.getVideoUri());
            if(vpage.getDescription() != null && !vpage.getDescription().isEmpty()) {
                out.println("Description: " + vpage.getDescription());
            }
        } else {
            if(page != null) {
                page.getUserData().put("menu_id", count);
                out.println("[" + count + "] " + page.getTitle());
            } else {
                out.println("No data available");
            }
        }
    }

    @Override
    public String getName() {
        return "vchget";
    }

    @Override
    public String getShortDescription() {
        return "parses a web page and lists the results";
    }

    @Override
    public String getUsage() {
        return getName() + " [<id> | ..]";
    }

    private IOverviewPage getParsers() throws Exception {
        Object[] parsers = st.getServices();
        IOverviewPage overview = new OverviewPage();
        if (parsers != null && parsers.length > 0) {
            for (Object o : parsers) {
                IWebParser parser = (IWebParser) o;
                IOverviewPage parserPage = new OverviewPage();
                parserPage.setTitle(parser.getTitle());
                parserPage.setUri(new URI("vchpage://root"));
                parserPage.setParser(parser.getId());
                overview.getPages().add(parserPage);
            }
        }
        
        Collections.sort(overview.getPages(), new WebPageTitleComparator());
        return overview;
    }
    
    public IWebParser getParser(String id) {
        Object[] parsers = st.getServices();
        for (Object o : parsers) {
            IWebParser parser = (IWebParser) o;
            if(id.equals(parser.getId())) {
                return parser;
            }
        }
        
        return null;
    }
}
