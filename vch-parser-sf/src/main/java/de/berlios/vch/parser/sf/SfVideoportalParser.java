package de.berlios.vch.parser.sf;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.Node;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
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
public class SfVideoportalParser implements IWebParser {
    public static final String CHARSET = "UTF-8";

    public static final String ID = SfVideoportalParser.class.getName();

    private final String BASE_URI = "http://www.videoportal.sf.tv";
    private final String START_PAGE = BASE_URI + "/sendungen";

    @Requires
    private LogService logger;

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        String content = HttpUtils.get(START_PAGE, null, CHARSET);
        NodeList sections = HtmlParserUtils.getTags(content, CHARSET, "div.az_unit");
        for (int i = 0; i < sections.size(); i++) {
            Node section = sections.elementAt(i);
            String title = HtmlParserUtils.getText(section.toHtml(), CHARSET, "div.grey_box h2");
            OverviewPage sectionPage = new OverviewPage();
            sectionPage.setParser(ID);
            sectionPage.setTitle(title);
            sectionPage.setUri(new URI("sf://section/" + title));
            page.getPages().add(sectionPage);

            NodeList programs = HtmlParserUtils.getTags(section.toHtml(), CHARSET, "div.az_row");
            for (int j = 0; j < programs.size(); j++) {
                Node program = programs.elementAt(j);
                String html = program.toHtml();
                String name = HtmlParserUtils.getText(html, CHARSET, "a.sendung_name");
                String uri = BASE_URI + ((LinkTag) HtmlParserUtils.getTag(html, CHARSET, "a.sendung_name")).extractLink();

                OverviewPage programPage = new OverviewPage();
                programPage.setParser(ID);
                programPage.setTitle(name);
                programPage.setUri(new URI(uri));
                sectionPage.getPages().add(programPage);
            }
        }

        return page;
    }

    @Override
    public String getTitle() {
        return "SF Videoportal";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IOverviewPage) {
            if ("sf".equals(page.getUri().getScheme())) {
                return page;
            } else {
                // parse program page
                parseProgramPage((IOverviewPage) page);
            }
        } else if (page instanceof IVideoPage) {
            // parse video page
            parseVideoPage((IVideoPage) page);
        }

        return page;
    }

    private void parseVideoPage(IVideoPage page) throws IOException, ParserException, URISyntaxException, JSONException {
        String query = page.getUri().getQuery();
        int idStart = query.indexOf("id=") + 3;
        int idStop = query.indexOf(';', idStart);
        String id = query.substring(idStart, idStop);

        String json = HttpUtils.get(BASE_URI + "/cvis/segment/" + id + "/.json", null, CHARSET);
        json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1);
        JSONObject jo = new JSONObject(json);

        // parse the video uri
        parseVideoUri(page, jo);

        // parse the title
        page.setTitle(jo.getString("description_title"));

        // parse the description
        page.setDescription(jo.getString("description_lead"));

        // parse the publish date
        page.setPublishDate(Calendar.getInstance());
        String date = jo.getString("time_published");
        try {
            // 2012-03-08 21:02:38
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            page.getPublishDate().setTime(sdf.parse(date));
        } catch (Exception e) {
            logger.log(LogService.LOG_WARNING, "Couldn't parse publish date " + date, e);
        }

    }

    private void parseVideoUri(IVideoPage page, JSONObject jo) throws JSONException, URISyntaxException {
        // parse the available video formats
        List<VideoType> videos = new ArrayList<VideoType>();
        JSONArray streams = jo.getJSONArray("streaming_urls");
        for (int i = 0; i < streams.length(); i++) {
            JSONObject stream = streams.getJSONObject(i);
            String codecV = stream.getString("codec_video");
            String codecA = stream.getString("codec_audio");
            int width = stream.getInt("frame_width");
            int height = stream.getInt("frame_height");
            int bitrate = stream.getInt("bitrate");
            String uri = stream.getString("url");
            logger.log(LogService.LOG_DEBUG, "Found video: " + codecV + " " + codecA + " " + width + "x" + height + " @ " + bitrate + " kbit/s");

            VideoType video = new VideoType(uri, width, bitrate);
            videos.add(video);
        }

        int status = jo.getInt("http_code");
        if (status != 200) {
            throw new RuntimeException("Unexpected error: HTTP status is " + status);
        }

        if (videos.size() == 0) {
            throw new RuntimeException("No videos found!");
        }

        // sort the videos by their quality and select the best one
        Collections.sort(videos);
        Collections.reverse(videos);
        VideoType bestQuality = videos.get(0);
        logger.log(LogService.LOG_INFO, "Best video quality is " + bestQuality.getWidth() + " @ " + bestQuality.getBitrate() + " kbit/s");
        page.setVideoUri(new URI(bestQuality.getUri()));

        // extract the streamName for RTMP dowload
        String uri = bestQuality.getUri();
        String streamName = uri.substring(uri.indexOf("mp4:"));
        logger.log(LogService.LOG_DEBUG, "RTMP stream name is " + streamName);
        page.getUserData().put("streamName", streamName);

        // add SWF uri for swf verification
        page.getUserData().put("swfUri", BASE_URI + "/flash/videoplayer.swf");
    }

    private void parseProgramPage(IOverviewPage page) throws Exception {
        String content = HttpUtils.get(page.getUri().toString(), null, CHARSET);

        // first parse "aktuelle sendung"
        LinkTag a = (LinkTag) HtmlParserUtils.getTag(content, CHARSET, "div.act_sendung_info div.left h2 a");
        if (a != null) {
            String title = a.getLinkText();
            String uri = BASE_URI + a.extractLink();
            VideoPage video = new VideoPage();
            video.setParser(ID);
            video.setUri(new URI(uri));
            video.setTitle(title);
            page.getPages().add(video);
        }

        // parse "zurückliegende sendungen"
        NodeList prevPrograms = HtmlParserUtils.getTags(content, CHARSET, "div.prev_sendungen a.sendung_title");
        for (int i = 0; i < prevPrograms.size(); i++) {
            LinkTag program = (LinkTag) prevPrograms.elementAt(i);
            String title = program.getLinkText();
            String uri = BASE_URI + program.extractLink();
            VideoPage video = new VideoPage();
            video.setParser(ID);
            video.setUri(new URI(uri));
            video.setTitle(title);
            page.getPages().add(video);
        }
    }

    @Override
    public String getId() {
        return ID;
    }
}