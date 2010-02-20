package de.berlios.vch.parser.ard;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.AsxParser;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;

public class VideoItemPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(VideoItemPageParser.class);
    
    /** List of video file patterns in preferred order. */
    private static String[] supportedFormats = {
        "http.*hq\\.flv",
        "http.*hi\\.flv",
        "mms.*(512|hi)\\.wmv",
        "http.*\\.flv",
        "mms.*(256|lo)\\.wmv",
        "http.*ms256.*\\.asx",
        "(mms|http).*m\\.wmv.*",
        "http.*ms128.*\\.asx",
        "(mms|http).*s\\.wmv.*",
        "http.*t\\.flv"
    };
    
    
    public static VideoPage parse(VideoPage page) throws IOException, ParserException, URISyntaxException, NoSupportedVideoFoundException {
        String content = HttpUtils.get(page.getUri().toString(), ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);
        // first parse the available formats for this video
        List<String> videos = parseAvailableVideos(content);
        for (int i = 0; i < supportedFormats.length; i++) {
            for (String video : videos) {
                if(Pattern.matches(supportedFormats[i], video)) {
                    // set the video uri
                    if(video.startsWith("http")) {
                        Map<String, List<String>> headers = HttpUtils.head(video, ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);
                        String contentType = HttpUtils.getHeaderField(headers, "Content-Type");
                        if("video/x-ms-asf".equals(contentType)) {
                            video = AsxParser.getUri(video);
                        }
                    }
                    page.setVideoUri(new URI(video));
                    
                    // parse title
                    page.setTitle(parseTitle(content));
                    
                    // parse description
                    String description = parseDescription(content);
                    logger.trace("Description {}", description);
                    page.setDescription(description);
                    page.getUserData().remove("desc");
                    
                    // parse pubDate
                    try {
                        Calendar date = parseDate(content);
                        logger.trace("Parsed date {}", date);
                        page.setPublishDate(date);
                    } catch (ParseException e) {
                        logger.warn("Couldn't parse publish date. Using current time!", e);
                        logger.trace("Content: {}", content);
                        page.setPublishDate(Calendar.getInstance());
                    }
                    
                    // TODO enable streambridge
//                    if("flashmedia".equalsIgnoreCase(preferredFormat)) {
//                        if(entry.getEnclosures().size() > 0 && 
//                                ((SyndEnclosure)entry.getEnclosures().get(0)).getUrl().startsWith("rtmp")) 
//                        {
//                            SyndEnclosure enc = (SyndEnclosure)entry.getEnclosures().get(0);
//                            String videoUri = enc.getUrl();
//                            String path = Config.getInstance().getHandlerMapping().getPath(StreamBridgeHandler.class);
//                            String base = Config.getInstance().getBaseUrl();
//                            String uri = base + path + "?uri=" + URLEncoder.encode(videoUri, "UTF-8");
//                            enc.setUrl(uri);
//                        }
//                    }
                    
                    return page;
                }

            }
        }
        
        throw new NoSupportedVideoFoundException(page.getUri().toString(), videos);
    }
    
    private static String parseDescription(String content) throws ParserException {
        return Translate.decode(HtmlParserUtils.getText(content, ARDMediathekParser.CHARSET, "div.mt-player_content div[class~=js-collapseable] div[class~=js-collapseable_content] div p"));
    }

    private static String parseTitle(String content) throws ParserException {
        return Translate.decode(HtmlParserUtils.getText(content, ARDMediathekParser.CHARSET, "div.mt-player_content h2"));
    }
    
    private static Calendar parseDate(String content) throws ParserException, ParseException {
        NodeList list = (NodeList) HtmlParserUtils.getTags(content, ARDMediathekParser.CHARSET, "p.clipinfo");
        for (NodeIterator iterator = list.elements(); iterator.hasMoreNodes();) {
            Node node = (Node) iterator.nextNode();
            String string = node.toPlainTextString().trim();
            Pattern p = Pattern.compile("\\s*Online seit:\\s*(\\d+\\.\\d+\\.\\d+\\s*)\\s*", Pattern.DOTALL);
            Matcher m = p.matcher(string);
            if(m.find()) {
                Calendar pubDate = Calendar.getInstance();
                pubDate.setTime(new SimpleDateFormat("dd.MM.yy").parse(m.group(1)));
                return pubDate;
            }
        }
        
        return Calendar.getInstance();
    }

    private static List<String> parseAvailableVideos(String content) {
        List<String> videos = new ArrayList<String>();
        Pattern p = Pattern.compile("addMediaStream\\(\\d+, \\d+, \"\", \"(.*)\"\\);");
        Matcher m = p.matcher(content);
        while(m.find()) {
            videos.add(m.group(1));
        }
        return videos;
    }
}
