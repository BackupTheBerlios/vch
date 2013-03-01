package de.berlios.vch.parser.zdf;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.AsxParser;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

@Component
@Provides
public class ZDFMediathekParser implements IWebParser {
    public static final String ID = ZDFMediathekParser.class.getName();
    private static final String BASE_URI = "http://www.zdf.de";
    private static final String OVERVIEW_RUBRIKEN = BASE_URI + "/ZDFmediathek/hauptnavigation/rubriken?flash=off";
    private static final String OVERVIEW_ABZ = "dummy://localhost/" + ID + "/a-z";

    public static final String CHARSET = "UTF-8";

    private Map<String, String> aBisZ = new TreeMap<String, String>();

    @Requires
    private LogService logger;

    public ZDFMediathekParser() {
        aBisZ.put("0-9", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz8?flash=off&teaserListIndex=1000");
        aBisZ.put("ABC", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz0?flash=off&teaserListIndex=1000");
        aBisZ.put("DEF", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz1?flash=off&teaserListIndex=1000");
        aBisZ.put("GHI", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz2?flash=off&teaserListIndex=1000");
        aBisZ.put("JKL", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz3?flash=off&teaserListIndex=1000");
        aBisZ.put("MNO", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz4?flash=off&teaserListIndex=1000");
        aBisZ.put("PQRS", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz5?flash=off&teaserListIndex=1000");
        aBisZ.put("TUV", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz6?flash=off&teaserListIndex=1000");
        aBisZ.put("WXYZ", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz7?flash=off&teaserListIndex=1000");
    }

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        OverviewPage rubriken = new OverviewPage();
        rubriken.setParser(ID);
        rubriken.setTitle("Rubriken");
        rubriken.setUri(new URI(OVERVIEW_RUBRIKEN));
        page.getPages().add(rubriken);

        OverviewPage abz = new OverviewPage();
        abz.setParser(ID);
        abz.setTitle("Sendungen A-Z");
        abz.setUri(new URI(OVERVIEW_ABZ));
        page.getPages().add(abz);

        return page;
    }

    @Override
    public String getTitle() {
        return "ZDFmediathek";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) page;
            parseVideoPage(video);
            // parse asx file, if necessary
            if (video.getVideoUri().toString().toLowerCase().endsWith(".asx")) {
                String newUri = AsxParser.getUri(video.getVideoUri().toString());
                video.setVideoUri(new URI(newUri));
            }
            return page;
        } else {
            if (page.getUri().toString().equals(OVERVIEW_ABZ)) {
                IOverviewPage opage = (IOverviewPage) page;
                opage.getPages().clear();
                for (Entry<String, String> abzPage : aBisZ.entrySet()) {
                    String title = abzPage.getKey();
                    String uri = abzPage.getValue();
                    OverviewPage tmp = new OverviewPage();
                    tmp.setParser(ID);
                    tmp.setTitle(title);
                    tmp.setUri(new URI(uri));
                    opage.getPages().add(tmp);
                }
            } else {
                parseOverviewPage((IOverviewPage) page);
            }
            return page;
        }
    }

    private void parseVideoPage(IVideoPage video) throws IOException, ParserException, URISyntaxException, ParseException {
        String content = HttpUtils.get(video.getUri().toString(), null, CHARSET);

        // parse the title
        video.setTitle(Translate.decode(HtmlParserUtils.getText(content, CHARSET, "h1.beitragHeadline")));

        // parse the description
        video.setDescription(Translate.decode(HtmlParserUtils.getText(content, CHARSET, "div.beitrag p.kurztext")));

        // parse the thumbnail image
        Matcher m = Pattern.compile("#playerContainer\\s*\\{\\s*background-image: url\\(([^\\)]*)\\);", MULTILINE | DOTALL).matcher(content);
        if (m.find()) {
            video.setThumbnail(new URI(BASE_URI + m.group(1)));
        }

        // parse the video uri
        NodeList videoLinks = HtmlParserUtils.getTags(content, CHARSET, "ul.dslChoice li a");
        LinkTag wmvBestQuality = (LinkTag) videoLinks.elementAt(1);
        String videoUri = wmvBestQuality.extractLink();
        if (!videoUri.toLowerCase().endsWith("asx")) {
            wmvBestQuality = (LinkTag) videoLinks.elementAt(0);
            videoUri = wmvBestQuality.extractLink();
        }
        video.setVideoUri(new URI(videoUri));

        // parse the pubDate
        String datum = HtmlParserUtils.getText(content, CHARSET, "p.datum");
        datum = datum.substring(datum.lastIndexOf(',') + 1).trim();
        Date pubDate = new SimpleDateFormat("dd.MM.yyyy").parse(datum);
        Calendar cal = Calendar.getInstance();
        cal.setTime(pubDate);
        video.setPublishDate(cal);

        // parse the duration
        m = Pattern.compile("VIDEO,\\s+(\\d+):(\\d+)").matcher(content);
        if (m.find()) {
            int minutes = Integer.parseInt(m.group(1));
            int seconds = Integer.parseInt(m.group(2));
            video.setDuration(60 * minutes + seconds);
        }
    }

    public void parseOverviewPage(IOverviewPage page) throws Exception {
        // clear the parsed pages, so that we don't have duplicate results, if a user
        // opens this page multiple times
        page.getPages().clear();

        // set the number of displayed items on the page to 1000
        String uri = HttpUtils.addParameter(page.getUri().toString(), "teaserListIndex", "1000");

        String content = HttpUtils.get(uri, null, CHARSET);
        NodeList titles = HtmlParserUtils.getTags(content, CHARSET, "div.text");
        NodeIterator iter = titles.elements();
        while (iter.hasMoreNodes()) {
            Div div = (Div) iter.nextNode();
            NodeList links = HtmlParserUtils.getTags(div.toHtml(), CHARSET, "a");
            LinkTag a = (LinkTag) links.elementAt(1);

            // parse page uri
            String pageUri = BASE_URI + Translate.decode(a.extractLink());

            // parse page title
            String title = Translate.decode(a.getLinkText()).trim();

            // detect page type
            LinkTag typeLink = (LinkTag) links.elementAt(2);
            boolean isVideo = Translate.decode(typeLink.getParent().toPlainTextString()).trim().toLowerCase().contains("video");

            // create an OverviewPage or a VideoPage
            IWebPage subPage = null;
            URI subPageUri = new URI(pageUri.replaceAll(" ", "+"));
            if (isOverviewPage(subPageUri)) {
                subPage = new OverviewPage();
            } else {
                VideoPage tmp = new VideoPage();
                // parse the pubDate
                String datum = ((LinkTag) links.elementAt(0)).getLinkText();
                try {
                    datum = datum.substring(datum.lastIndexOf(',') + 1).trim();
                    Date pubDate = new SimpleDateFormat("dd.MM.yyyy").parse(datum);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(pubDate);
                    tmp.setPublishDate(cal);
                } catch (Throwable t) {
                    logger.log(LogService.LOG_WARNING, "Couldn't parse date " + datum + " for page " + title);
                    tmp.setPublishDate(Calendar.getInstance());
                }
                subPage = tmp;
            }
            subPage.setParser(ID);
            subPage.setTitle(title);
            subPage.setUri(subPageUri);

            // if the subpage is an item page and if it is video, add it to the overview page
            // otherwise it is an overview page and we add it, too
            if (!isOverviewPage(subPageUri)) {
                if (isVideo) {
                    page.getPages().add(subPage);
                }
            } else {
                page.getPages().add(subPage);
            }
        }
    }

    private boolean isOverviewPage(URI uri) {
        return uri != null && uri.getPath() != null && uri.getPath().matches(".*/\\d+");
    }

    @Override
    public String getId() {
        return ID;
    }
}