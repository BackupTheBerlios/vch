package de.berlios.vch.parser.arte;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.AsxParser;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.VideoPage;

public class VideoPageParser {
    
    private static transient Logger logger = LoggerFactory.getLogger(VideoPageParser.class);
    
    public static void parse(IVideoPage v) throws URISyntaxException, IOException, ParserException  {
        String content = HttpUtils.get(v.getUri().toString(), ArteParser.HTTP_HEADERS, ArteParser.CHARSET);
        logger.debug("Getting media link in media page:" + v.getUri());
        
        VideoPage video = (VideoPage) v;
        
        // parse the entry title
        String title = parseTitle(video, content);
        if(title != null) video.setTitle(title);
        
        // parse description
        video.setDescription(parseDescription(content));
        
        // parse the video link
        String videoUri = parseVideoUri(content);
        if (videoUri != null && videoUri.length() > 0) {
            video.setVideoUri(new URI(videoUri));
        } 
    }
    
    private static String parseTitle(IVideoPage video, String content) throws ParserException, IOException  {
        Pattern p = Pattern.compile("var\\s*playerUrl\\s*=\\s*\'(.*)\';");
        Matcher m = p.matcher(content);
        if(m.find()) {
            URL page = new URL(video.getUri().toString());
            URL detailsPage = new URL(page.getProtocol(), page.getHost(), page.getPort(), m.group(1));
            String details = HttpUtils.get(detailsPage.toString(), null, ArteParser.CHARSET);
            return HtmlParserUtils.getText(details, ArteParser.CHARSET, "span[id=abc]");
        } else {
            return null;
        }
    }

    private static String parseDescription(String content) throws ParserException  {
        String headline = HtmlParserUtils.getText(content, ArteParser.CHARSET, "p.headline");
        String text = HtmlParserUtils.getText(content, ArteParser.CHARSET, "p.text");
        
        headline = headline != null ? headline.trim() : "";
        text = text != null ? text.trim() : "";
        
        return Translate.decode(headline + (headline.length() > 0 ? "\n\n" : "") + text);
    }
    
    private static String parseVideoUri(String content) throws UnsupportedEncodingException {
        List<String[]> videos = new ArrayList<String[]>();
        Pattern pFormat = Pattern.compile("availableFormats\\[\\d*\\]\\[\"format\"\\] = \"(\\w*)\";");
        Pattern pQuality = Pattern.compile("availableFormats\\[\\d*\\]\\[\"quality\"\\] = \"(\\w*)\";"); 
        Pattern pUrl = Pattern.compile("availableFormats\\[\\d*\\]\\[\"url\"\\] = \"(.*)\";");
        Matcher mFormat = pFormat.matcher(content);
        Matcher mQuality = pQuality.matcher(content);
        Matcher mUrl = pUrl.matcher(content);
        while(mFormat.find()) {
            int index = mFormat.start();
            String format = mFormat.group(1);
            if(mQuality.find(index+1)) {
                index = mQuality.start();
                String quality = mQuality.group(1);
                if(mUrl.find(index+1)) {
                    String uri = mUrl.group(1);
                    String[] video = new String[] {format, quality, uri};
                    videos.add(video);
                }
            }
        }
        
        if(videos.size() == 0) {
            return null;
        }
        
        sort(videos);
        String[] video = videos.get(videos.size()-1);
        
        String videoUri = video[2];
        if(videoUri.endsWith("asx") || videoUri.startsWith("http://")) {
            videoUri = AsxParser.getUri(videoUri);
        }
        return videoUri;
    }
    
    /**
     * Sorts videos represented by an String[] {format, quality, uri}
     * WMV > FLV
     * HQ > MQ
     * @param videos
     */
    private static void sort(List<String[]> videos) {
        Collections.sort(videos, new Comparator<String[]>() {
            @Override
            public int compare(String[] v1, String[] v2) {
                if(v1[0].equals("WMV") && !v2[0].equals("WMV")) {
                    return 1;
                } else if(!v1[0].equals("WMV") && v2[0].equals("WMV")) {
                    return -1;
                } else if(v1[1].equals("HQ") && !v2[0].equals("HQ")) {
                    return 1;
                } else if(!v1[1].equals("HQ") && v2[0].equals("HQ")) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }
}
