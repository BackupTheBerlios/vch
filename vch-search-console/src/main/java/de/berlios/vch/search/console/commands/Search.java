package de.berlios.vch.search.console.commands;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.concurrent.TimeUnit;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.shell.Command;
import org.osgi.service.log.LogService;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.search.ISearchService;

@Provides
@Component
public class Search implements Command {

    @Requires
    private ISearchService searchService;
    
    @Requires
    private LogService logger;

    @Override
    public void execute(String cmd, PrintStream out, PrintStream err) {
        try {
        	logger.log(LogService.LOG_INFO, "Args " + cmd);
        	int pos = cmd.indexOf(' ');
        	if(pos < 0) {
        		out.println("You have to define a search query");
        	} else {
        		String query = cmd.substring(pos+1); 
    			IOverviewPage opage = searchService.search(query);
    			showMenu(out, err, opage);
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
        return "vchsearch";
    }

    @Override
    public String getShortDescription() {
        return "Searches on different web pages and lists the results";
    }

    @Override
    public String getUsage() {
        return getName() + " <query>";
    }
}
