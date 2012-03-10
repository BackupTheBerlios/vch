package de.berlios.vch.search.dreisat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.TableRow;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.osgi.service.log.LogService;
import org.xml.sax.SAXException;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.search.ISearchProvider;

@Component
@Provides
public class DreisatSearchProvider implements ISearchProvider {

    public static final String BASE_URI = "http://www.3sat.de";

    private static final String SEARCH_PAGE = BASE_URI + "/mediathek/mediathek.php?mode=search&query=";

    public static final String CHARSET = "iso-8859-15";

    public static Map<String, String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.2.13) Gecko/20110108 Gentoo Firefox/3.6.13");
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }

    @Requires
    private LogService logger;

    @Override
    public String getName() {
        return "3sat";
    }

    @Override
    public IOverviewPage search(String query) throws Exception {
        // execute the search
        String uri = SEARCH_PAGE + URLEncoder.encode(query, "UTF-8");
        String content = HttpUtils.get(uri, null, "UTF-8");

        // parse the result and create an overview page
        IOverviewPage opage = new OverviewPage();
        opage.setParser(getId());
        opage.setUri(new URI(uri));
        parseResult(opage, content);
        return opage;
    }

    private void parseResult(IOverviewPage opage, String content) throws Exception {
        NodeList rows = HtmlParserUtils.getTags(content, CHARSET, "table.media_result tr");
        NodeIterator iter = rows.elements();
        while (iter.hasMoreNodes()) {
            TableRow row = (TableRow) iter.nextNode();

            // skip the table header
            if ("media_result_kopf".equalsIgnoreCase(row.getAttribute("class"))) {
                continue;
            }

            String rowContent = row.toHtml();

            // check if the result points to a video
            if ("video".equalsIgnoreCase(HtmlParserUtils.getText(rowContent, CHARSET, "span.thumb").trim())) {
                // create a new subpage of type IVideoPage
                IVideoPage video = new VideoPage();
                video.setParser(getId());
                opage.getPages().add(video);

                // parse the page uri
                LinkTag a = (LinkTag) HtmlParserUtils.getTag(rowContent, CHARSET, "a.media_result_thumb_txt");
                String uri = BASE_URI + a.getLink();
                video.setUri(new URI(uri));

                // parse the thumb
                ImageTag img = (ImageTag) HtmlParserUtils.getTag(rowContent, CHARSET, "span.popup img");
                video.setThumbnail(new URI(BASE_URI + img.getImageURL()));

                // parse the title
                String title = Translate.decode(img.getAttribute("alt"));
                if (title.startsWith("video: ")) {
                    title = title.substring(7);
                }
                video.setTitle(title);

                // parse the pubdate
                String normal = HtmlParserUtils.getText(rowContent, CHARSET, "span.normal");
                Matcher m = Pattern.compile("\\(.*(\\d\\d)\\.(\\d\\d).(\\d\\d).*").matcher(normal);
                if (m.find()) {
                    int day = Integer.parseInt(m.group(1));
                    int month = Integer.parseInt(m.group(2));
                    int year = Integer.parseInt(m.group(3));
                    Calendar cal = Calendar.getInstance();
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.YEAR, year);
                    video.setPublishDate(cal);
                }

                // parse the duration
                video.setDuration(parseDuration(normal));
            }
        }
    }

    @Override
    public String getId() {
        return DreisatSearchProvider.class.getName();
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) page;
            parseVideo(video);
        }
        return page;
    }

    private void parseVideo(IVideoPage video) throws IOException, ParserException, URISyntaxException, SAXException, ParserConfigurationException {
        // parse the thumbnail
        String content = HttpUtils.get(video.getUri().toString(), HTTP_HEADERS, CHARSET);
        ImageTag thumb = (ImageTag) HtmlParserUtils.getTag(content, CHARSET, "div.media img.still");
        video.setThumbnail(new URI(BASE_URI + thumb.getImageURL()));

        content = HttpUtils.get(video.getUri().toString() + "&mode=play", HTTP_HEADERS, CHARSET);

        // parse the title
        String program = HtmlParserUtils.getText(content, CHARSET, "div.mediadetail span.left a");
        String episode = HtmlParserUtils.getText(content, CHARSET, "div.mediadetail h2");
        video.setTitle(program + " - " + episode);

        // parse the description
        video.setDescription(HtmlParserUtils.getText(content, CHARSET, "span.text"));

        // parse the video uri
        int start = content.indexOf("playerBottomFlashvars.mediaURL = \"") + 34;
        int stop = content.indexOf('"', start);
        String smil = content.substring(start, stop);
        logger.log(LogService.LOG_DEBUG, "SMIL URI is " + smil);
        String smilContent = HttpUtils.get(smil, HTTP_HEADERS, CHARSET);
        SmilParser.parseVideoUri(video, smilContent);

        // parse the pubdate
        String dateString = HtmlParserUtils.getText(content, CHARSET, "div.mediadetail span.right");
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(sdf.parse(dateString));
            video.setPublishDate(cal);
        } catch (ParseException e) {
            logger.log(LogService.LOG_WARNING, "Couldn't parse date [" + dateString + "]", e);
        }

        // parse the duration
        video.setDuration(parseDuration(HtmlParserUtils.getText(content, CHARSET, "span.norm")));
    }

    private long parseDuration(String text) {
        Matcher m = Pattern.compile("(\\d\\d*)\\:(\\d\\d)min").matcher(text);
        if (m.find()) {
            int minutes = Integer.parseInt(m.group(1));
            int seconds = Integer.parseInt(m.group(2));
            return (minutes * 60 + seconds);
        }

        return -1;
    }
}
