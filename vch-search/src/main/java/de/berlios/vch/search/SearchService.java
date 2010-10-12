package de.berlios.vch.search;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.log.LogService;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.OverviewPage;

@Component
@Provides
public class SearchService implements ISearchService {

    @Requires
    private LogService logger;
    
    private Set<ISearchProvider> searchProviders = new HashSet<ISearchProvider>();
    
    @Override
    public IOverviewPage search(final String query) {
        if(query.length() < 3) {
            throw new IllegalArgumentException("Query is too short. Enter at least 3 characters.");
        }

        logger.log(LogService.LOG_DEBUG, "Searching for \""+query+"\"");
        final IOverviewPage result = new OverviewPage();
        result.setParser("search");
        result.setTitle("Search results"); // TODO i18n
        
        // create a task for each search provider and execute it with the thread pool
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (final ISearchProvider searchProvider : searchProviders) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        IOverviewPage r = searchProvider.search(query);
                        r.setTitle(searchProvider.getName());
                        result.getPages().add(r);
                    } catch (Exception e) {
                        logger.log(LogService.LOG_ERROR, "Error occured while searching with " + 
                                searchProvider.getClass().getName() + ". No results will be available from this provider.", e);
                    }        
                }
            });
        }
        
        // wait for all search tasks to finish, but wait at most x seconds 
        executor.shutdown();
        try {
            executor.awaitTermination(60, TimeUnit.SECONDS);
            executor.shutdownNow();
        } catch (InterruptedException e) {
            logger.log(LogService.LOG_WARNING, "Couldn't stop search thread pool", e);
        }
        
        return result;
    }
    
    @Override
    public IWebPage parse(IWebPage page) throws Exception {
    	// TODO Auto-generated method stub
    	return null;
    }
    
// ############ ipojo stuff #########################################    
    
    // validate and invalidate methods seem to be necessary for the bind methods to work
    @Validate
    public void start() {}
    
    @Invalidate
    public void stop() {}

    @Bind(id = "searchProviders", aggregate = true)
    public synchronized void addProvider(ISearchProvider provider) {
        logger.log(LogService.LOG_INFO, "Adding search provider " + provider.getClass().getName());
        searchProviders.add(provider);
        logger.log(LogService.LOG_INFO, searchProviders.size() + " search providers available");
    }
    
    @Unbind(id="searchProviders", aggregate = true)
    public synchronized void removeProvider(ISearchProvider provider) {
        logger.log(LogService.LOG_INFO, "Removing search provider " + provider.getClass().getName());
        searchProviders.remove(provider);
        logger.log(LogService.LOG_INFO, searchProviders.size() + " search providers available");
    }

}
