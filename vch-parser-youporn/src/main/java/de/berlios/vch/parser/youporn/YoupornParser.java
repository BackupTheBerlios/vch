package de.berlios.vch.parser.youporn;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.htmlparser.Node;
import org.htmlparser.tags.ImageTag;
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
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;

public class YoupornParser implements IWebParser, BundleActivator {
    private static transient Logger logger = LoggerFactory.getLogger(YoupornParser.class);

    private final String CHARSET = "UTF-8";

    private final String BASEURL = "http://www.youporn.com";

    public static final String ID = YoupornParser.class.getName();

    private final static String COOKIE = "age_verified=1";

    static Map<String, String> headers = new HashMap<String, String>();
    static {
        headers.put("Cookie", COOKIE);
        headers.put("User-Agent", "Mozilla/5.0 (X11; Linux i686; rv:10.0.3) Gecko/20100101 Firefox/10.0.3");
        headers.put("Accept", "*/*");
    }

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        String content = HttpUtils.get(BASEURL, headers, CHARSET);

        OverviewPage overview = new OverviewPage();
        overview.setParser(ID);
        overview.setTitle("Youporn");
        overview.setUri(new URI("http://www.youporn.com"));

        NodeList cells = HtmlParserUtils.getTags(content, CHARSET, "div.videoList ul li.videoBox");
        for (NodeIterator iterator = cells.elements(); iterator.hasMoreNodes();) {
            Node cell = iterator.nextNode();
            String cellContent = cell.toHtml();
            LinkTag link = (LinkTag) HtmlParserUtils.getTag(cellContent, CHARSET, "a");
            String title = Translate.decode(HtmlParserUtils.getText(cellContent, CHARSET, "h1 a").trim());

            // try to parse the video duration
            String _duration = HtmlParserUtils.getText(cellContent, CHARSET, "*[class~=duration]").trim();
            long duration = 0;
            try {
                String[] d = _duration.split(":");
                int minutes = Integer.parseInt(d[0]);
                int seconds = Integer.parseInt(d[1]);
                duration = minutes * 60 + seconds;
            } catch (Exception e) {
                logger.warn("Couldn't parse duration", e);
            }

            // try to get a thumbnail
            URI thumbnail = null;
            try {
                ImageTag thumb = (ImageTag) HtmlParserUtils.getTag(cellContent, CHARSET, "a img.flipbook");
                thumbnail = new URI(thumb.extractImageLocn());
            } catch (Exception e) {
                logger.warn("Couldn't find the thumbnail", e);
            }

            VideoPage vpage = new VideoPage();
            vpage.setParser(ID);
            vpage.setTitle(title);
            vpage.setUri(new URI(BASEURL + link.extractLink()));
            vpage.setDuration(duration);
            vpage.setThumbnail(thumbnail);
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
        if (page instanceof VideoPage) {
            VideoPage video = (VideoPage) page;
            parseVideoDetails(video);
            return page;
        } else {
            return page;
        }
    }

    private void parseVideoDetails(VideoPage video) throws IOException, ParserException, URISyntaxException, NoSupportedVideoFoundException {

        String content = HttpUtils.get(video.getUri().toString(), headers, CHARSET);

        // continue with the next page, if the download didn't work
        if (content.length() == 0) {
            logger.warn("Couldn't download page {}", video.getUri());
            return;
        }

        // parse video uris
        NodeList downloads = HtmlParserUtils.getTags(content, CHARSET, "div#downloadPopup ul.downloadList li a");
        URI bestQualityVideo = getBestVideoLink(video.getUri(), downloads);
        video.setVideoUri(bestQualityVideo);

        // parse description
        String description = HtmlParserUtils.getTag(content, CHARSET, "div#content ul.spaced").toPlainTextString().trim();
        description = Translate.decode(description);
        description = description.replaceAll("[ \\t\\x0B\\f\\r]{2,}", " ");
        description = description.replaceAll("\\n\\s+\\n", "\\\n");
        description = description.replaceAll("^\\s+", "");
        video.setDescription(description.trim());

        // parse pubDate
        Locale currentLocale = Locale.getDefault();
        try {
            String d = description.substring(description.indexOf("Date:") + 5).trim();
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
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

    private URI getBestVideoLink(URI sourcePage, NodeList downloads) throws URISyntaxException, NoSupportedVideoFoundException {
        List<Video> videos = new ArrayList<Video>();

        for (int i = 0; i < downloads.size(); i++) {
            LinkTag link = (LinkTag) downloads.elementAt(i);

            URI videoUri = new URI(Translate.decode(link.extractLink()));
            logger.trace("Found video: {}", videoUri);

            Video video = new Video();
            video.setUri(videoUri);
            File serverPath = new File(videoUri.getPath());
            if (serverPath.getName().contains(".mp4")) {
                video.setType("mp4");
            } else if (serverPath.getName().contains(".mpg")) {
                video.setType("mpg");
            } else {
                video.setType("unknown");
            }

            String quality = serverPath.getParentFile().getName();
            try {
                quality = quality.substring(0, quality.lastIndexOf('_'));
                int height = Integer.parseInt(quality.substring(0, quality.indexOf('_') - 1));
                int bitrate = Integer.parseInt(quality.substring(quality.indexOf('_') + 1, quality.length() - 1));
                video.setHeight(height);
                video.setBitrate(bitrate);
            } catch (Exception e) {
                logger.error("Couldn't determine video height and bitrate", e);
            }

            videos.add(video);
        }

        Collections.sort(videos, new VideoComparator());
        if (videos.size() > 0) {
            Video vid = videos.get(0);
            logger.debug("Best quality video is {} with a height of {} px and a bitrate of {} kbit/s",
                    new Object[] { vid.getType(), vid.getHeight(), vid.getBitrate() });
            return vid.getUri();
        } else {
            List<String> formats = new ArrayList<String>(videos.size());
            for (Video video : videos) {
                formats.add(video.getType());
            }
            throw new NoSupportedVideoFoundException(sourcePage.toString(), formats);
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