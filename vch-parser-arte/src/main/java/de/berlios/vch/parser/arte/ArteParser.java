package de.berlios.vch.parser.arte;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Validate;
import org.htmlparser.Node;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

@Component
@Provides
public class ArteParser implements IWebParser {

    public static final String CHARSET = "UTF-8";

    public static final String ARTE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public static final String BASE_URI = "http://videos.arte.tv";

    public static final String START_PAGE = BASE_URI + "/de/videos/sendungen";

    public static final String ID = ArteParser.class.getName();

    @Requires
    private LogService logger;

    private BundleContext ctx;

    private VideoPageParser videoPageParser;

    public static Map<String, String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent",
                "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.2) Gecko/20090821 Gentoo Firefox/3.5.2");
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }

    public ArteParser(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));
        parsePrograms(page);
        return page;
    }

    private void parsePrograms(OverviewPage root) throws IOException, ParserException, URISyntaxException {
        String content = HttpUtils.get(START_PAGE, HTTP_HEADERS, CHARSET);
        NodeList programs = HtmlParserUtils.getTags(content, CHARSET, "div.newVideos h1 > a");
        for (SimpleNodeIterator iterator = programs.elements(); iterator.hasMoreNodes();) {
            LinkTag link = (LinkTag) iterator.nextNode();
            OverviewPage opage = new OverviewPage();
            opage.setParser(getId());
            opage.setTitle(Translate.decode(link.getLinkText()));
            opage.setUri(new URI(BASE_URI + link.getLink()));
            root.getPages().add(opage);

            // skip the second matching link for each program
            iterator.nextNode();
        }
    }

    @Override
    public String getTitle() {
        return "Arte+7";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) page;
            videoPageParser.parse(video);
            return video;
        } else if (page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            parseBroadcasts(opage);
            return page;
        } else {
            return page;
        }
    }

    private void parseBroadcasts(IOverviewPage opage) throws Exception {
        String uri = opage.getUri().toString() + "#/de/list///1/150/";
        String content = HttpUtils.get(uri, HTTP_HEADERS, CHARSET);
        content = content.replaceAll("<noscript>", "");
        content = content.replaceAll("</noscript>", "");
        NodeList videoDivs = HtmlParserUtils.getTags(content, CHARSET, "div.video");
        for (SimpleNodeIterator iterator = videoDivs.elements(); iterator.hasMoreNodes();) {
            String nodeContent = iterator.nextNode().toHtml();
            IVideoPage video = new VideoPage();
            video.setParser(getId());

            // parse title and page uri
            LinkTag link = (LinkTag) HtmlParserUtils.getTag(nodeContent, CHARSET, "h2 a");
            video.setTitle(Translate.decode(link.getLinkText()));
            video.setUri(new URI(BASE_URI + link.getLink()));

            // parse description
            video.setDescription(HtmlParserUtils.getText(nodeContent, CHARSET, "p.teaserText"));

            // parse thumbnail
            ImageTag thumb = (ImageTag) HtmlParserUtils.getTag(nodeContent, CHARSET, "img.thumbnail");
            video.setThumbnail(new URI(BASE_URI + thumb.getImageURL()));

            // parse date (Di, 13. Apr 2010, 00:00)
            try {
                String format = "EE, dd. MMM yyyy, HH:mm";
                Node ps = HtmlParserUtils.getTag(nodeContent, CHARSET, "p.views").getPreviousSibling()
                        .getPreviousSibling();
                String dateString = ps.getFirstChild().getText();
                SimpleDateFormat sdf = new SimpleDateFormat(format);
                Calendar pubDate = Calendar.getInstance();
                pubDate.setTime(sdf.parse(dateString));
                video.setPublishDate(pubDate);
            } catch (Exception e) {
                logger.log(LogService.LOG_WARNING, "Couldn't parse publish date", e);
            }

            opage.getPages().add(video);
        }
    }

    @Validate
    public void start() {
        videoPageParser = new VideoPageParser(ctx, logger);
    }

    @Invalidate
    public void stop() {
    }

    @Override
    public String getId() {
        return ID;
    }
}