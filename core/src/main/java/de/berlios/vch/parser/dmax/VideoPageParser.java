package de.berlios.vch.parser.dmax;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.htmlparser.Tag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;

import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;
import flex.messaging.io.amf.ASObject;
import flex.messaging.io.amf.client.AMFConnection;

public class VideoPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(VideoPageParser.class);
    
    private String uri;
    private String pageContent;
    
    public VideoPageParser(String uri) {
        this.uri = uri;
    }
    
    public void loadPage() throws IOException {
        pageContent = HttpUtils.get(uri, null, DmaxParser.CHARSET);
    }
    
    public Date parsePubDate() throws ParserException, ParseException {
        String dateString = HtmlParserUtils.getText(pageContent, DmaxParser.CHARSET, "div.vp-perpage-clipinfo ul li");
        dateString = dateString.split(":")[1].trim();
        
        // temporarily switch the locale to en
        Locale current = Locale.getDefault();
        Locale.setDefault(Locale.ENGLISH);
        
        // parse the date
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMMM-yyyy");
        Date pubDate = sdf.parse(dateString);
        
        // reset the locale
        Locale.setDefault(current);
        
        return pubDate;
    }
    
    public NodeList getChapterItemCells() throws ParserException {
        Div div = (Div) HtmlParserUtils.getTag(pageContent, DmaxParser.CHARSET, "div#vp-perpage-related-long-form-head");
        Div chapterContainer = (Div) div.getNextSibling().getNextSibling();
        return HtmlParserUtils.getTags(chapterContainer.toHtml(), DmaxParser.CHARSET, "div[class~=vp-promo-item]");
    }
    
    public String getFeedLink() throws Exception {
        LinkTag a = (LinkTag) HtmlParserUtils.getTag(pageContent, DmaxParser.CHARSET, "div#vp-perpage-videoinfo div[class~=vp-perpage-clipinfo] p a");
        if(a != null) {
            return a.getLink();
        } else {
            throw new Exception("Feed link not found");
        }
    }
    
    @SuppressWarnings("unchecked")
    public SyndEnclosure getEnclosure() throws Exception {
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
        String video = (String) mediaDTO.get("FLVFullLengthURL");
        double length = (Double) mediaDTO.get("length");
        logger.debug("Video: {}, duration: {}", video, length);
        
        SyndEnclosure encl = new SyndEnclosureImpl();
        encl.setUrl(video);
        encl.setType("video/flv");
        
        return encl;
    }
}
