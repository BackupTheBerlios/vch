package de.berlios.vch.parser.youporn;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

public class YoupornParser implements IWebParser, BundleActivator {
    private static transient Logger logger = LoggerFactory.getLogger(YoupornParser.class);
    
    private final String CHARSET = "UTF-8";
    
    private final String BASEURL = "http://www.youporn.com";
    
    public static final String ID = YoupornParser.class.getName();
    
    private final static String COOKIE = "__utma=60671397.1341618993.1198254651.1198254651.1198254651.1; __utmb=60671397; __utmc=60671397; __utmz=60671397.1198254651.1.1.utmccn=(direct)|utmcsr=(direct)|utmcmd=(none); age_check=1";
    
    static Map<String,String> headers = new HashMap<String, String>();
    static {
        headers.put("Cookie", COOKIE);
    }
    
    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        
        String content = HttpUtils.get(BASEURL, headers, CHARSET);

        OverviewPage overview = new OverviewPage();
        overview.setParser(ID);
        overview.setTitle("Youporn");
        overview.setUri(new URI("http://www.youporn.com"));
        
        NodeList cells = HtmlParserUtils.getTags(content, CHARSET, "div#video-listing ul li");
        for (NodeIterator iterator = cells.elements(); iterator.hasMoreNodes();) {
            Node cell = iterator.nextNode();
            LinkTag link = (LinkTag) HtmlParserUtils.getTag(cell.toHtml(), CHARSET, "a");
            String title = Translate.decode(HtmlParserUtils.getText(cell.toHtml(), CHARSET, "h1 a").trim());
            String _duration = HtmlParserUtils.getText(cell.toHtml(), CHARSET, "div.duration_views h2").trim();
            long duration = 0;
            try {
                String[] d = _duration.split(":");
                int minutes = Integer.parseInt(d[0]);
                int seconds = Integer.parseInt(d[1]);
                duration = minutes * 60 + seconds;
            } catch(Exception e) {
                logger.warn("Couldn't parse duration", e);
            }
            
            VideoPage vpage = new VideoPage();
            vpage.setParser(ID);
            vpage.setTitle(title);
            vpage.setUri(new URI(BASEURL + link.extractLink()));
            vpage.setDuration(duration);
            
            overview.getPages().add(vpage);
        }
        return overview;
    }

    @Override
    public String getTitle() {
        return "Youporn";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof VideoPage) {
            VideoPage video = (VideoPage) page;
            parseVideoDetails(video);
            return page;
        } else {
            return page;
        }
    }

    private void parseVideoDetails(VideoPage video) throws IOException, ParserException, URISyntaxException {

        String content = HttpUtils.get(video.getUri().toString(), headers, CHARSET);

        // continue with the next page, if the download didn't work
        if (content.length() == 0) {
            logger.warn("Couldn't download page {}", video.getUri());
            return;
        }

        // parse enclosure
        LinkTag download = (LinkTag) HtmlParserUtils.getTag(content, CHARSET, "div#download a");

        video.setVideoUri(new URI(download.getLink()));

        // parse description
        String description = ((Tag) HtmlParserUtils.getTag(content, CHARSET, "div#details")).toPlainTextString().trim();
        description = Translate.decode(description);
        description = description.replaceAll("[ \\t\\x0B\\f\\r]{2,}", " ");
        description = description.replaceAll("\\n\\s+\\n", "\\\n");
        description = description.replaceAll("^\\s+", "");
        video.setDescription(description.trim());
        
        // parse pubDate
        Locale currentLocale = Locale.getDefault();
        try {
            String d = description.substring(description.indexOf("Date:") + 5).trim();
            SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH);
            Date date = sdf.parse(d);
            Calendar pubDate = Calendar.getInstance();
            pubDate.setTime(date);
            video.setPublishDate(pubDate);
        } catch (Exception e) {
            logger.warn("Couldn't parse pubDate", e);
            video.setPublishDate(Calendar.getInstance());
        } finally {
            Locale.setDefault(currentLocale);
        }
    }

    @Override
    public void start(BundleContext ctx) throws Exception {
        ctx.registerService(IWebParser.class.getName(), this, null);
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
    }
    
    @Override
    public String getId() {
        return ID;
    }
}