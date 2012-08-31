package de.berlios.vch.search.n24;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.framework.ServiceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.search.ISearchProvider;

@Component
@Provides
public class N24SearchProvider implements ISearchProvider {

    @Requires
    private LogService logger;

    @Requires(filter = "(instance.name=vch.parser.n24)")
    private IWebParser parser;

    public static final String STREAM_BASE = "rtmp://pssimn24fs.fplive.net:1935/pssimn24";
    public static final String SEARCH_URI = "http://www.n24.de/mediathek/${query}/search/";

    private List<String> supportedProtocols = new ArrayList<String>();

    @Override
    public String getName() {
        return "N24 Mediencenter";
    }

    @Override
    public IOverviewPage search(String query) throws Exception {
        if (parser == null) {
            throw new ServiceException("N24 Mediathek Parser is not available");
        }

        // execute the search
        String uri = SEARCH_URI.replaceAll("\\$\\{query\\}", query);
        logger.log(LogService.LOG_DEBUG, "Executing search with URI " + uri);

        IOverviewPage opage = new OverviewPage();
        opage.setTitle(getName());
        opage.setParser(getId());
        opage.setUri(new URI(uri));

        parser.parse(opage);
        return opage;
    }

    @Override
    public String getId() {
        return parser.getId();
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (parser == null) {
            throw new ServiceException("N24 Mediathek Parser is not available");
        }

        if (page instanceof IVideoPage) {
            return parser.parse(page);
        }

        return page;
    }

    // ############ ipojo stuff #########################################

    // validate and invalidate method seem to be necessary for the bind methods to work
    @Validate
    public void start() throws MalformedURLException {
    }

    @Invalidate
    public void stop() {
    }

    @Bind(id = "supportedProtocols", aggregate = true)
    public synchronized void addProtocol(INetworkProtocol protocol) {
        supportedProtocols.addAll(protocol.getSchemes());
    }

    @Unbind(id = "supportedProtocols", aggregate = true)
    public synchronized void removeProtocol(INetworkProtocol protocol) {
        supportedProtocols.removeAll(protocol.getSchemes());
    }

}
