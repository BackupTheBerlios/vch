package de.berlios.vch.parser.console.commands;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Stack;
import java.util.concurrent.TimeUnit;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.shell.Command;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceException;
import org.osgi.util.tracker.ServiceTracker;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IParserService;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;

@Provides
@Component
public class GetWebPage implements Command {

    private BundleContext ctx = null;

    private ServiceTracker st;

    static Stack<IWebPage> menuStack = new Stack<IWebPage>();

    public GetWebPage(BundleContext context) {
        ctx = context;
        st = new ServiceTracker(ctx, IParserService.class.getName(), null);
        st.open();
    }

    @Override
    public void execute(String cmd, PrintStream out, PrintStream err) {
        try {
            IParserService parserService = (IParserService) st.getService();
            if(parserService == null) {
                throw new ServiceException("ParserService not available");
            }
            
            String[] args = cmd.split(" ");
            if (args.length == 1) {
                if (menuStack.isEmpty()) {
                    IOverviewPage page = parserService.getParserOverview();
                    menuStack.push(page);
                } else if(menuStack.size() == 1) {
                    menuStack.pop();
                    IOverviewPage page = parserService.getParserOverview();
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
                                IWebParser parser = parserService.getParser(page.getParser());
                                if(parser != null) {
                                    out.println("Loading page. Please wait...");
                                    menuStack.push(parserService.parse(page.getVchUri()));
                                    showMenu(out, err, menuStack.peek());
                                    return;
                                } else {
                                    err.println("This parser is not available anymore");
                                }
                            }
                        }

                        err.println("Page with ID " + id + " not found");
                    } else {
                        IWebParser parser = parserService.getParser(currentPage.getParser());
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
            err.println("Couldn't execute command: " + cmd + ": " + e.getLocalizedMessage());
            e.printStackTrace(err);
        }
    }

    public static void showMenu(PrintStream out, PrintStream err, IWebPage page) throws Exception {
        int count = 0;
        if (page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            out.println("Title: " + opage.getTitle());
            out.println("URI:   " + opage.getUri());
            out.println("------------------------");
            for (IWebPage wp : opage.getPages()) {
                wp.getUserData().put("menu_id", count);
                out.println("[" + count + "] " + wp.getTitle());
                count++;
            }
        } else if (page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            out.println(page.getTitle());
            if(vpage.getDuration() < 60) {
                out.println("Length: " + vpage.getDuration() + " seconds");
            } else {
                out.println("Length: " + TimeUnit.SECONDS.toMinutes(vpage.getDuration()) + " minutes");
            }
            String pubDate = "N/A";
            if(vpage.getPublishDate() != null) {
                pubDate = SimpleDateFormat.getDateTimeInstance().format(vpage.getPublishDate().getTime());
            }
            out.println("Online since: " + pubDate);
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
}
