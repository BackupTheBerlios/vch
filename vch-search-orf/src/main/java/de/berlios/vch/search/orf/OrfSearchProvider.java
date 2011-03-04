package de.berlios.vch.search.orf;

import java.net.URI;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.osgi.framework.ServiceException;

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
public class OrfSearchProvider implements ISearchProvider {

    protected static final String BASE_URI = "http://tvthek.orf.at";
    protected static final String SEARCH_PAGE = BASE_URI + "/search?q=";
    protected static final String CHARSET = "UTF-8";

    @Requires(filter = "(instance.name=VCH Orf Parser)")
    private IWebParser parser;

    @Override
    public String getName() {
        return "ORF TVthek";
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
        NodeList items = HtmlParserUtils.getTags(content, CHARSET, "ul.search li");
        NodeIterator iter = items.elements();
        while (iter.hasMoreNodes()) {
            Bullet li = (Bullet) iter.nextNode();
            String liContent = li.toHtml();

            // create a new subpage of type IVideoPage
            IVideoPage video = new VideoPage();
            video.setParser(getId());
            opage.getPages().add(video);

            // parse the page uri
            LinkTag a = (LinkTag) HtmlParserUtils.getTag(liContent, CHARSET, "h4 a");
            String uri = BASE_URI + a.getLink();
            video.setUri(new URI(uri));

            // parse the title
            String title = Translate.decode(a.getAttribute("title"));
            video.setTitle(title);

            // parse the description
            String desc = HtmlParserUtils.getText(liContent, CHARSET, "p a");
            video.setDescription(desc);

            // parse the thumb
            ImageTag img = (ImageTag) HtmlParserUtils.getTag(liContent, CHARSET, "a.image img");
            video.setThumbnail(new URI(BASE_URI + img.getImageURL()));

            // // parse the pubdate
            String p = HtmlParserUtils.getText(liContent, CHARSET, "p");
            Matcher m = Pattern.compile("(\\d{2})\\.(\\d{2}).(\\d{4})").matcher(p);
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
            m = Pattern.compile("\\((?:(\\d{2})\\:)?(\\d{2})\\:(\\d{2})\\)").matcher(p);
            if (m.find()) {
                int hours = 0;
                if (m.group(1) != null) {
                    hours = Integer.parseInt(m.group(1));
                }
                int minutes = Integer.parseInt(m.group(2));
                int seconds = Integer.parseInt(m.group(3));
                video.setDuration(hours * 3600 + minutes * 60 + seconds);
            }
        }
    }

    @Override
    public String getId() {
        return OrfSearchProvider.class.getName();
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (parser == null) {
            throw new ServiceException("ORF TVthek Parser is not available");
        }

        if (page instanceof IVideoPage) {
            return parser.parse(page);
        }

        return page;
    }

    // private void parseVideo(IVideoPage video) throws IOException, ParserException, URISyntaxException {
    // // parse the thumbnail
    // String content = HttpUtils.get(video.getUri().toString(), HTTP_HEADERS, CHARSET);
    // ImageTag thumb = (ImageTag) HtmlParserUtils.getTag(content, CHARSET, "div.media img.still");
    // video.setThumbnail(new URI(BASE_URI + thumb.getImageURL()));
    //
    // content = HttpUtils.get(video.getUri().toString() + "&mode=play", HTTP_HEADERS, CHARSET);
    //
    // // parse the title
    // String program = HtmlParserUtils.getText(content, CHARSET, "div.mediadetail span.left a");
    // String episode = HtmlParserUtils.getText(content, CHARSET, "div.mediadetail h2");
    // video.setTitle(program + " - " + episode);
    //
    // // parse the description
    // video.setDescription(HtmlParserUtils.getText(content, CHARSET, "span.text"));
    //
    // // parse the video uri
    // Tag param = HtmlParserUtils.getTag(content, CHARSET, "param[name=\"src\"]");
    // String src = param.getAttribute("value");
    // if (src.toLowerCase().endsWith(".asx")) {
    // src = AsxParser.getUri(src);
    // }
    // video.setVideoUri(new URI(src));
    //
    // // parse the pubdate
    // String dateString = HtmlParserUtils.getText(content, CHARSET, "div.mediadetail span.right");
    // SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
    // try {
    // Calendar cal = Calendar.getInstance();
    // cal.setTime(sdf.parse(dateString));
    // video.setPublishDate(cal);
    // } catch (ParseException e) {
    // logger.log(LogService.LOG_WARNING, "Couldn't parse date [" + dateString + "]", e);
    // }
    //
    // // parse the duration
    // video.setDuration(parseDuration(HtmlParserUtils.getText(content, CHARSET, "span.norm")));
    // }
    //
    // private long parseDuration(String text) {
    // Matcher m = Pattern.compile("(\\d\\d*)\\:(\\d\\d)min").matcher(text);
    // if (m.find()) {
    // int minutes = Integer.parseInt(m.group(1));
    // int seconds = Integer.parseInt(m.group(2));
    // return (minutes * 60 + seconds);
    // }
    //
    // return -1;
    // }
}
