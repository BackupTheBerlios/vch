package de.berlios.vch.parser.servustv;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.htmlparser.Tag;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import flex.messaging.io.amf.client.AMFConnection;

@Component
@Provides
public class ServusTvParser implements IWebParser {
    public static final String CHARSET = "UTF-8";
    public static final String ID = ServusTvParser.class.getName();
    public static final String BASE_URI = "http://www.servustv.com";
    public static final String START_PAGE = BASE_URI + "/cs/Satellite/VOD-Mediathek/001259088496198";
    public static final String STREAM_BASE = "rtmp://cp81614.edgefcs.net/ondemand";

    @Requires
    private LogService logger;

    private List<String> supportedProtocols = new ArrayList<String>();

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        String content = HttpUtils.get(START_PAGE, null, CHARSET);
        NodeList programs = HtmlParserUtils.getTags(content, CHARSET, "select#nachSendung option");
        for (int i = 1; i < programs.size(); i++) {
            OptionTag option = (OptionTag) programs.elementAt(i);
            IOverviewPage opage = new OverviewPage();
            opage.setParser(ID);
            opage.setTitle(Translate.decode(option.getStringText()));
            String programId = option.getValue();
            opage.setUri(new URI("servus://program/" + programId));
            page.getPages().add(opage);

        }

        return page;
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            if (page.getUri().toString().startsWith("servus://program/")) {
                String programId = page.getUri().getPath().substring(1);
                String pageUri = BASE_URI + "/cs/Satellite?pagename=ServusTV%2FAjax%2FMediathekData&nachThemen=all"
                        + "&nachThemenNodeId=null&nachThemen_changed=1&nachSendung_changed=2&ajax=true&nachSendung=" + programId;
                String content = HttpUtils.get(pageUri, null, CHARSET);
                NodeList episodes = HtmlParserUtils.getTags(content, CHARSET, "ul.programScrollerSmall li");
                NodeIterator iter = episodes.elements();
                while (iter.hasMoreNodes()) {
                    Bullet li = (Bullet) iter.nextNode();
                    String liContent = li.toHtml();

                    IVideoPage video = new VideoPage();
                    video.setParser(ID);

                    // parse the title
                    String title = HtmlParserUtils.getText(liContent, CHARSET, "div[class~=programFormatText]");
                    title = title.replace('\n', ' ').replaceAll("\\s{2,}", " ").trim();
                    video.setTitle(title);

                    // parse the containing page uri
                    LinkTag a = (LinkTag) HtmlParserUtils.getTag(liContent, CHARSET, "div.programRightArea a");
                    String uri = BASE_URI + a.extractLink();
                    video.setUri(new URI(uri));

                    // parse the publish date
                    String pattern = "'Sendungvom'dd.MM.yy'|'HH:mm'Uhr'";
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
                        Date pubDate = sdf.parse(title.replaceAll("\\s", ""));
                        Calendar cal = Calendar.getInstance();
                        cal.setTime(pubDate);
                        video.setPublishDate(cal);
                    } catch (Exception e) {
                        logger.log(LogService.LOG_WARNING, "Couldn't parse publich date " + title + " with pattern " + pattern);
                    }

                    // parse the description
                    String desc = HtmlParserUtils.getText(liContent, CHARSET, "div.programDescription");
                    video.setDescription(desc);

                    // parse the thumbnail
                    ImageTag img = (ImageTag) HtmlParserUtils.getTag(liContent, CHARSET, "div a img");
                    String thumbUri = BASE_URI + img.getImageURL();
                    video.setThumbnail(new URI(thumbUri));

                    opage.getPages().add(video);
                }
            }
        } else if (page instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) page;

            // download the video page
            String content = HttpUtils.get(video.getUri().toString(), null, CHARSET);

            // parse the video details
            parseVideo(content, video);
        }

        return page;
    }

    public void parseVideo(String pageContent, IVideoPage video) throws Exception {
        // extract the playerKey
        Tag param = HtmlParserUtils.getTag(pageContent, CHARSET, "param[name=\"playerKey\"]");
        String playerKey = param.getAttribute("value");
        logger.log(LogService.LOG_DEBUG, "PlayerKey is " + playerKey);

        // extract the videoId
        Tag input = HtmlParserUtils.getTag(pageContent, CHARSET, "div.teaserMain a input[name=\"videoList.featured\"]");
        String videoId = input.getAttribute("value");
        logger.log(LogService.LOG_DEBUG, "Video ID is " + videoId);

        String brokerUri = "http://c.brightcove.com/services/messagebroker/amf?playerKey=" + playerKey;
        logger.log(LogService.LOG_DEBUG, "Establishing AMF connection to " + brokerUri);
        AMFConnection conn = new AMFConnection();
        conn.connect(brokerUri);
        conn.addHttpRequestHeader("Referer", "http://admin.brightcove.com/viewer/us1.23.03.02/federatedVideo/BrightcovePlayer.swf");
        conn.addHttpRequestHeader("Content-Type", "application/x-amf");

        Object response = conn.call("com.brightcove.player.runtime.PlayerMediaFacade.findMediaByReferenceId", new Object[] {
                "8e99dff8de8d8e378ac3f68ed404dd4869a4c007", 1254928709001L, videoId, 900189268001L });
        logger.log(LogService.LOG_DEBUG, "AMF response: " + response);

        HashMap<?, ?> videoDTO = (HashMap<?, ?>) response;
        String videoUri = ((String) videoDTO.get("FLVFullLengthURL")).replaceAll("_low\\.", "_high.");
        // parse the video uri
        String streamName = videoUri.substring(videoUri.indexOf('&') + 1);
        video.getUserData().put("streamName", streamName);
        video.setVideoUri(new URI(videoUri.replaceAll("&mp4:", "mp4:")));

        // set the duration
        double length = (Double) videoDTO.get("length");
        video.setDuration(TimeUnit.MILLISECONDS.toSeconds((long) length));

        // set the description
        video.setDescription((String) videoDTO.get("longDescription"));

        // set the thumbnail
        video.setThumbnail(new URI((String) videoDTO.get("thumbnailURL")));

        // set the pubdate
        Date pubDate = (Date) videoDTO.get("publishedDate");
        Calendar cal = Calendar.getInstance();
        cal.setTime(pubDate);
        video.setPublishDate(cal);
    }

    @Override
    public String getTitle() {
        return "ServusTV";
    }

    @Override
    public String getId() {
        return ID;
    }

    // ############ ipojo stuff #########################################

    // validate and invalidate method seem to be necessary for the bind methods to work
    @Validate
    public void start() {
    }

    @Invalidate
    public void stop() {
    }

    @Bind(id = "supportedProtocols", aggregate = true)
    public synchronized void addProtocol(INetworkProtocol protocol) {
        supportedProtocols.addAll(protocol.getSchemes());
    }

    @Unbind(id = "supportedProtocols", aggregate = true)
    public synchronized void removeProtocol(INetworkProtocol protocol) {
        supportedProtocols.removeAll(protocol.getSchemes());
    }
}