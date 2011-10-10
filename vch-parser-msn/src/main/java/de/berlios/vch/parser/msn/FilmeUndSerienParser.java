package de.berlios.vch.parser.msn;

import static de.berlios.vch.parser.msn.MSNParser.CHARSET;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.htmlparser.Tag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;

public class FilmeUndSerienParser {

    public static final String OVERVIEW_MOVIES = "http://videokatalog.msn.de/Unterhaltung/Filme-und-Serien/";

    private LogService logger;

    private int[] videoFilePrio = new int[] { 1002, 102, 103, 104, 1003, 101 };

    public FilmeUndSerienParser(LogService logger) {
        this.logger = logger;
    }

    public void parse(IWebPage page) throws Exception {
        if (page instanceof IVideoPage) {
            parseVideoPage((IVideoPage) page);
        } else {
            if (OVERVIEW_MOVIES.equals(page.getUri().toString())) {
                parseCategories(page);
            } else if (page.getUri().toString().startsWith(OVERVIEW_MOVIES)) {
                parseOverview(page);
            }
        }
    }

    private void parseVideoPage(IVideoPage page) throws IOException, ParserException, JSONException, URISyntaxException, NoSupportedVideoFoundException {
        String content = HttpUtils.get(page.getUri().toString(), null, CHARSET);

        // parse the description
        ParagraphTag p = (ParagraphTag) HtmlParserUtils.getTag(content, CHARSET, "div.videoDetail div > p");
        Div div = (Div) p.getParent();
        String desc = Translate.decode(div.toPlainTextString());
        page.setDescription(desc);
        logger.log(LogService.LOG_DEBUG, desc);

        // get the video player params
        String jsonStart = "embedMsnPlayer('rich_embed_container_veeseo', ";
        String jsonStop = "});";
        int start = content.indexOf(jsonStart);
        int stop = content.indexOf(jsonStop);
        if (start >= 0 && stop > start) {
            start = start + jsonStart.length();
            stop++;
            String json = content.substring(start, stop);
            JSONObject jo = new JSONObject(json);
            JSONObject videoData = jo.getJSONObject("videoData");

            // get the available video files
            // format codes:
            // 1002 = H264 640x360
            // 1003 = VP6F 424x240
            // 1004 = Silverlight Smooth Streaming ?!?
            // 101 = H264 320x180
            // 102 = H264 640x360
            // 103 = H264 720x576
            Map<Integer, String> videoFiles = new HashMap<Integer, String>();
            JSONArray files = videoData.getJSONArray("videoFiles");
            for (int i = 0; i < files.length(); i++) {
                JSONObject file = files.getJSONObject(i);
                int format = file.getInt("formatCode");
                String url = file.getString("url");
                logger.log(LogService.LOG_DEBUG, "Video format " + format + " - " + url);
                videoFiles.put(format, url);
            }
            page.setVideoUri(new URI(determineBestQuality(videoFiles)));

            // get the duration
            int duration = videoData.getInt("durationSecs");
            page.setDuration(duration);

            // get the pubDate 2010-11-30T11:06:00
            try {
                String startDate = videoData.getString("startDate");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
                Date date = sdf.parse(startDate);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                page.setPublishDate(cal);
            } catch (Exception e) {
                logger.log(LogService.LOG_WARNING, "Could not parse publish date", e);
            }

        } else {
            throw new NoSupportedVideoFoundException(page.getUri().toString(), null);
        }
    }

    private void parseOverview(IWebPage page) throws Exception {
        IOverviewPage opage = (IOverviewPage) page;
        String content = HttpUtils.get(page.getUri().toString(), null, CHARSET);
        Tag iframe = HtmlParserUtils.getTag(content, CHARSET, "div.categoryContainer iframe[width=\"938\"]");
        String src = iframe.getAttribute("src");
        content = HttpUtils.get(src, null, CHARSET);
        NodeList slideItems = HtmlParserUtils.getTags(content, CHARSET, "div[class~=\"slide_item\"]");
        NodeIterator iter = slideItems.elements();
        while (iter.hasMoreNodes()) {
            Div slideItem = (Div) iter.nextNode();
            String slideHtml = slideItem.toHtml();
            IVideoPage video = new VideoPage();
            video.setParser(MSNParser.ID);

            ImageTag img = (ImageTag) HtmlParserUtils.getTag(slideHtml, CHARSET, "a img");
            video.setThumbnail(new URI("http://videokatalog.msn.de" + img.getImageURL()));
            LinkTag a = (LinkTag) HtmlParserUtils.getTag(slideHtml, CHARSET, "a[title]");
            String title = Translate.decode(a.getAttribute("title"));
            video.setTitle(title);
            String uri = a.extractLink();
            video.setUri(new URI(uri));
            video.setVideoUri(new URI(OVERVIEW_MOVIES));
            opage.getPages().add(video);
        }
    }

    private void parseCategories(IWebPage page) throws Exception {
        IOverviewPage opage = (IOverviewPage) page;
        String content = HttpUtils.get(OVERVIEW_MOVIES, null, CHARSET);
        NodeList links = HtmlParserUtils.getTags(content, CHARSET, "li[class=\"category\"] a");
        logger.log(LogService.LOG_INFO, "Found " + links.size() + " categories");
        for (int i = 0; i < links.size(); i++) {
            LinkTag a = (LinkTag) links.elementAt(i);
            IOverviewPage category = new OverviewPage();
            category.setParser(MSNParser.ID);
            category.setTitle(Translate.decode(a.getLinkText()));
            category.setUri(new URI(a.extractLink()));
            opage.getPages().add(category);
        }
    }

    private String determineBestQuality(Map<Integer, String> videoFiles) {
        for (int format : videoFilePrio) {
            String uri = videoFiles.get(format);
            if (uri != null) {
                logger.log(LogService.LOG_DEBUG, "Best video quality format is " + format);
                return uri;
            }
        }
        return null;
    }
}
