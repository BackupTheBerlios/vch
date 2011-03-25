package de.berlios.vch.search.ard;

import java.net.URI;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.osgi.framework.ServiceException;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.search.ISearchProvider;

@Component
@Provides
public class ArdSearchProvider implements ISearchProvider {

    public static final String BASE_URL = "http://www.ardmediathek.de";

    private static final String SEARCH_PAGE = BASE_URL + "/ard/servlet/content/3517006?detail=40&inhalt=tv&wort=all&s=";

    public static final String CHARSET = "UTF-8";

    public static Map<String, String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.13) Gecko/20110108 Gentoo Firefox/3.6.13");
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }

    @Requires(filter = "(instance.name=vch.parser.ard)")
    private IWebParser parser;

    @Requires
    private LogService logger;

    @Override
    public String getName() {
        return parser.getTitle();
    }

    @Override
    public IOverviewPage search(String query) throws Exception {
        if (parser == null) {
            throw new ServiceException("ARD Mediathek Parser is not available");
        }

        // execute the search
        String uri = SEARCH_PAGE + URLEncoder.encode(query, "UTF-8");
        String content = HttpUtils.get(uri, HTTP_HEADERS, CHARSET);

        // parse the result
        IOverviewPage opage = parseSearchResult(content);
        opage.setParser(getId());
        opage.setUri(new URI(uri));
        return opage;
    }

    private IOverviewPage parseSearchResult(String content) throws Exception {
        IOverviewPage page = new OverviewPage();
        NodeList tags = HtmlParserUtils.getTags(content, CHARSET, "div#mt-box-suche-clips-inner div[class~=\"mt-media-item\"]");
        NodeIterator iter = tags.elements();
        while (iter.hasMoreNodes()) {
            // create a new VideoPage
            IVideoPage video = new VideoPage();
            video.setParser(getId());

            // extract the html for each item
            Div item = (Div) iter.nextNode();
            String itemContent = item.toHtml();

            // parse the title
            LinkTag title = (LinkTag) HtmlParserUtils.getTag(itemContent, CHARSET, "h3.mt-title a");
            video.setTitle(Translate.decode(title.getLinkText()));

            // parse the video page uri
            video.setUri(new URI(BASE_URL + title.getLink()));

            // parse publish date
            String text = HtmlParserUtils.getText(itemContent, CHARSET, "span.mt-airtime");
            String date = text.substring(0, text.indexOf(' '));
            Date pubDate = new SimpleDateFormat("dd.MM.yy").parse(date);
            Calendar cal = Calendar.getInstance();
            cal.setTime(pubDate);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            video.setPublishDate(cal);

            // parse the duration
            Matcher m = Pattern.compile(".*\\s*(\\d\\d):(\\d\\d)\\s*min").matcher(text);
            if (m.matches()) {
                int minutes = Integer.parseInt(m.group(1));
                int seconds = Integer.parseInt(m.group(2));
                video.setDuration(minutes * 60 + seconds);
            } else {
                logger.log(LogService.LOG_DEBUG, "No duration information found");
            }

            page.getPages().add(video);
        }
        return page;
    }

    @Override
    public String getId() {
        return parser.getId();
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (parser == null) {
            throw new ServiceException("ARD Mediathek Parser is not available");
        }

        if (page instanceof IVideoPage) {
            return parser.parse(page);
        }

        return page;
    }
}
