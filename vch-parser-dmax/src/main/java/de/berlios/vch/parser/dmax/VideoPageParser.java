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
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.VideoPage;
import flex.messaging.io.amf.ASObject;
import flex.messaging.io.amf.client.AMFConnection;

public class VideoPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(VideoPageParser.class);
    
    public IVideoPage parse(VideoPage page) throws Exception {
        // TODO parse desc and thumb
        String pageContent = HttpUtils.get(page.getUri().toString(), null, DmaxParser.CHARSET);
        Calendar pubCal = Calendar.getInstance();
        pubCal.setTime(parsePubDate(pageContent));
        Map<String, Object> video = getVideo(pageContent);
        page.setPublishDate(pubCal);
        page.setVideoUri(new URI((String) video.get("video")));
        page.setDuration( (Long)video.get("duration") );
        return page;
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
    
//    public NodeList getChapterItemCells() throws ParserException {
//        Div div = (Div) HtmlParserUtils.getTag(pageContent, DmaxParser.CHARSET, "div#vp-perpage-related-long-form-head");
//        Div chapterContainer = (Div) div.getNextSibling().getNextSibling();
//        return HtmlParserUtils.getTags(chapterContainer.toHtml(), DmaxParser.CHARSET, "div[class~=vp-promo-item]");
//    }
//    
//    public String getFeedLink() throws Exception {
//        LinkTag a = (LinkTag) HtmlParserUtils.getTag(pageContent, DmaxParser.CHARSET, "div#vp-perpage-videoinfo div[class~=vp-perpage-clipinfo] p a");
//        if(a != null) {
//            return a.getLink();
//        } else {
//            throw new Exception("Feed link not found");
//        }
//    }
    
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
        logger.debug("Video: {}, duration: {}", videoUri, length);
        return video;
    }
}
