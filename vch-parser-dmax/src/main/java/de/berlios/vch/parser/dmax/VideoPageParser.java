package de.berlios.vch.parser.dmax;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.htmlparser.Tag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IVideoPage;
import flex.messaging.io.amf.ASObject;
import flex.messaging.io.amf.client.AMFConnection;

public class VideoPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(VideoPageParser.class);
    
    public IVideoPage parse(IVideoPage page) throws Exception {
        String pageContent = HttpUtils.get(page.getUri().toString(), null, DmaxParser.CHARSET);

        // parse the video uri
        Map<String, Object> video = getVideo(pageContent);
        page.setVideoUri(new URI((String) video.get("video")));
        
        // parse the duration
        page.setDuration( (Long)video.get("duration") );
        
        // use the publish date from amf or fallback to parse the html page
        Calendar pubCal = Calendar.getInstance();
        if(video.get("pubDate") != null) {
            pubCal.setTime((Date) video.get("pubDate"));
            page.setPublishDate(pubCal);
        } else {
            try {
                pubCal.setTime(parsePubDate(pageContent));
                page.setPublishDate(pubCal);
            } catch (Exception e) {
                logger.warn("Couldn't parse publish date: {}", e.getLocalizedMessage());
                page.setPublishDate(Calendar.getInstance());
            }
        }
        
        // use the description from amf or fallback to parse the html page
        if(video.get("description") != null) {
            page.setDescription((String) video.get("description"));
        } else {
            page.setDescription(parseDescription(pageContent));
        }
        
        // parse the thumb
        if(video.get("thumb") != null) {
            page.setThumbnail(new URI((String) video.get("thumb")));
        }
        
        return page;
    }
    
    private String parseDescription(String pageContent) throws ParserException {
        ParagraphTag p = (ParagraphTag) HtmlParserUtils.getTag(pageContent, DmaxParser.CHARSET, "div.vp-perpage-clipinfo p");
        return p != null ? p.getStringText() : "";
    }

    public Date parsePubDate(String pageContent) throws ParserException, ParseException {
        Tag dateTag = HtmlParserUtils.getTag(pageContent, DmaxParser.CHARSET, "div.vp-perpage-clipinfo ul li");
        String dateString = dateTag.toHtml();
        dateString = dateString.split(":")[1].trim();
        
        // temporarily switch the locale to en
        Locale current = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        
        // parse the date
        SimpleDateFormat sdf = new SimpleDateFormat("'</strong> 'dd-MMM-yyyy'</li>'");
        Date pubDate = sdf.parse(dateString);
        
        // reset the locale
        Locale.setDefault(current);
        
        return pubDate;
    }
    
    @SuppressWarnings("unchecked")
    public Map<String, Object> getVideo(String pageContent) throws Exception {
        // extract the experienceId
        NodeList params = HtmlParserUtils.getTags(pageContent, DmaxParser.CHARSET, "param");
        NodeIterator iter = params.elements();
        String mediaId = "0";
        String playerId = "0";
        while(iter.hasMoreNodes()) {
            Tag param = (Tag) iter.nextNode();
            if("@videoPlayer".equals(param.getAttribute("name"))) {
                mediaId = param.getAttribute("value"); 
            } else if("playerID".equals(param.getAttribute("name"))) {
                playerId = param.getAttribute("value");
            }
        }
        
        AMFConnection conn = new AMFConnection();
        conn.connect("http://c.brightcove.com/services/messagebroker/amf");
        
        ASObject obj = new ASObject("com.brightcove.experience.ContentOverride");
        obj.put("contentIds", null);
        obj.put("target", "videoPlayer");
        obj.put("contentRefIds", null);
        obj.put("featuredId", Double.NaN);
        obj.put("contentRefId", null);
        obj.put("featuredRefId", null);
        obj.put("contentType", "0");
        obj.put("contentId", Double.parseDouble(mediaId));
        Object response = conn.call("com.brightcove.experience.ExperienceRuntimeFacade.getProgrammingWithOverrides", Double.parseDouble(playerId), new Object[] {obj});
        logger.trace("AMF response: {}", response);
        
        HashMap videoPlayer = (HashMap) ((HashMap) response).get("videoPlayer");
        HashMap mediaDTO = (HashMap) videoPlayer.get("mediaDTO");
        Map<String, Object> video = new HashMap<String, Object>();
        String videoUri = (String) mediaDTO.get("FLVFullLengthURL");
        double length = (Double) mediaDTO.get("length");
        video.put("video", videoUri);
        video.put("duration", TimeUnit.MILLISECONDS.toSeconds((long) length));
        video.put("description", mediaDTO.get("shortDescription"));
        video.put("thumb", mediaDTO.get("thumbnailURL"));
        video.put("pubDate", mediaDTO.get("publishedDate"));
        logger.debug("Video: {}, duration: {}", videoUri, length);
        return video;
    }
}
