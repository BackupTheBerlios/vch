package de.berlios.vch.search.arte;

import java.net.URI;
import java.net.URLEncoder;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.framework.ServiceException;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.search.ISearchProvider;

@Component
@Provides
public class ArteSearchProvider implements ISearchProvider {

    public static final String BASE_URI = "http://videos.arte.tv";

    private static final String SEARCH_PAGE = BASE_URI + "/de/do_search/videos/suche?q=";

    @Requires(filter = "(instance.name=VCH Arte+7 Parser)")
    private IWebParser parser;

    @Override
    public String getName() {
        return parser.getTitle();
    }

    @Override
    public IOverviewPage search(String query) throws Exception {
        if (parser == null) {
            throw new ServiceException("Arte+7 Parser is not available");
        }

        // execute the search
        String uri = SEARCH_PAGE + URLEncoder.encode(query, "UTF-8");
        IOverviewPage opage = new OverviewPage();
        opage.setParser(parser.getId());
        opage.setUri(new URI(uri));
        IOverviewPage result = (IOverviewPage) parser.parse(opage);
        return result;
    }

    @Override
    public String getId() {
        return parser.getId();
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (parser == null) {
            throw new ServiceException("Arte+7 Parser is not available");
        }

        if (page instanceof IVideoPage) {
            return parser.parse(page);
        }

        return page;
    }
}
