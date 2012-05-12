package de.berlios.vch.parser.dreisat;

import java.net.URI;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.htmlparser.Node;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableColumn;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.jdom.Element;
import org.osgi.service.log.LogService;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.AsxParser;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPageTitleComparator;

@Component
@Provides
public class DreisatParser implements IWebParser {

    public static final String BASE_URL = "http://www.3sat.de";

    private static final String LANDING_PAGE = BASE_URL + "/page/?source=/specials/133576/index.html";

    public static final String CHARSET = "iso-8859-15";

    public static final String ID = DreisatParser.class.getName();

    public static Map<String, String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", "Mozilla/5.0 (X11; Linux i686; rv:10.0.3) Gecko/20100101 Firefox/10.0.3");
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }

    public DreisatFeedParser feedParser;

    @Requires
    private LogService logger;

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        // add all rss feeds to
        String landingPage = HttpUtils.get(LANDING_PAGE, HTTP_HEADERS, CHARSET);
        NodeList tableRows = HtmlParserUtils.getTags(landingPage, CHARSET, "table.article_table_main_fix tr");
        NodeIterator iter = tableRows.elements();
        while (iter.hasMoreNodes()) {
            Node child = iter.nextNode();
            if (child instanceof TableRow) {
                TableRow tr = (TableRow) child;
                if (tr.getChildCount() < 10) {
                    continue;
                }

                TableColumn td = (TableColumn) tr.childAt(1);
                String title = Translate.decode(td.toPlainTextString());

                td = (TableColumn) tr.childAt(7);
                logger.log(LogService.LOG_DEBUG, td.toString());
                if (td.getChild(0) instanceof LinkTag) {
                    LinkTag rss = (LinkTag) td.getChild(0);
                    String path = rss.extractLink();
                    if (path.startsWith("/mediaplayer/rss/")) {
                        OverviewPage feedPage = new OverviewPage();
                        feedPage.setParser(ID);
                        feedPage.setTitle(title);
                        feedPage.setUri(new URI(BASE_URL + path));
                        page.getPages().add(feedPage);
                    }
                } else {
                    logger.log(LogService.LOG_DEBUG, title + " has no RSS feed");
                }
            }
        }

        // add the general 3sat mediathek feed
        OverviewPage feedPage = new OverviewPage();
        feedPage.setParser(ID);
        feedPage.setTitle("3sat-Mediathek allgemein");
        feedPage.setUri(new URI(BASE_URL + "/mediathek/rss/mediathek.xml"));
        page.getPages().add(feedPage);
        Collections.sort(page.getPages(), new WebPageTitleComparator());
        return page;
    }

    @Override
    public String getTitle() {
        return "3sat Mediathek";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof VideoPage) {
            VideoPage vpage = (VideoPage) page;
            if (vpage.getVideoUri().toString().endsWith(".asx")) {
                String uri = AsxParser.getUri(vpage.getVideoUri().toString());
                vpage.setVideoUri(new URI(uri));
                page.getUserData().remove("video");
            }

            String content = HttpUtils.get(vpage.getUri().toString(), null, CHARSET);
            ImageTag img = (ImageTag) HtmlParserUtils.getTag(content, CHARSET, "div.seite div.media img.still");
            if (img != null) {
                vpage.setThumbnail(new URI(BASE_URL + img.getImageURL()));
            }

            return page;
        } else {
            SyndFeed feed = feedParser.parse(page);
            OverviewPage feedPage = new OverviewPage();
            feedPage.setParser(ID);
            feedPage.setTitle(page.getTitle());
            feedPage.setUri(page.getUri());

            for (Iterator<?> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
                SyndEntry entry = (SyndEntry) iterator.next();

                if (entry.getEnclosures().size() == 0) {
                    continue;
                }

                VideoPage video = new VideoPage();
                video.setParser(ID);
                video.setTitle(entry.getTitle());
                if (entry.getDescription() != null) {
                    video.setDescription(entry.getDescription().getValue());
                }
                video.setUri(new URI(entry.getLink()));
                Calendar pubCal = Calendar.getInstance();
                pubCal.setTime(entry.getPublishedDate());
                video.setPublishDate(pubCal);
                video.setVideoUri(new URI(((SyndEnclosure) entry.getEnclosures().get(0)).getUrl()));

                // look, if we have a duration in the foreign markup
                @SuppressWarnings("unchecked")
                List<Element> fm = (List<Element>) entry.getForeignMarkup();
                for (Element element : fm) {
                    if ("duration".equals(element.getName())) {
                        try {
                            video.setDuration(Long.parseLong(element.getText()));
                        } catch (Exception e) {
                        }
                    }
                }

                feedPage.getPages().add(video);
            }
            return feedPage;
        }
    }

    @Validate
    public void start() {
        feedParser = new DreisatFeedParser(logger);
    }

    @Invalidate
    public void stop() {
    }

    @Override
    public String getId() {
        return ID;
    }
}