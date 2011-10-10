package de.berlios.vch.parser.msn;

import java.net.URI;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.log.LogService;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;

@Component
@Provides
public class MSNParser implements IWebParser {
    public static final String ID = MSNParser.class.getName();
    public static final String BASE_URI = "http://video.de.msn.com";
    public static final String CHARSET = "UTF-8";

    private FilmeUndSerienParser fsParser;

    @Requires
    private LogService logger;

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        OverviewPage movies = new OverviewPage();
        movies.setParser(ID);
        movies.setTitle("Filme und Serien");
        movies.setUri(new URI(FilmeUndSerienParser.OVERVIEW_MOVIES));
        page.getPages().add(movies);

        return page;
    }

    @Override
    public String getTitle() {
        return "MSN";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page.getUri().toString().toLowerCase().contains("filme-und-serien")) {
            fsParser.parse(page);
        } else if (page instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) page;
            if (video.getVideoUri().toString().equals(FilmeUndSerienParser.OVERVIEW_MOVIES)) {
                fsParser.parse(page);
            }
        } else {
            logger.log(LogService.LOG_WARNING, "No parser found for page " + page.getUri());
        }
        return page;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Validate
    public void start() {
        fsParser = new FilmeUndSerienParser(logger);
    }
}