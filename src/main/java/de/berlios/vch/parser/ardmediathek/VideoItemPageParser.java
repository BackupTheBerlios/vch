package de.berlios.vch.parser.ardmediathek;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;

import de.berlios.vch.Config;
import de.berlios.vch.http.handler.StreamBridgeHandler;
import de.berlios.vch.utils.AsxParser;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;

public class VideoItemPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(VideoItemPageParser.class);
    
    /** List of supported video formats in preferred order. 
     *  So wmv is preferred to quicktime. 
     */
    private static String[] supportedFormats = {
        "microsoftmedia",
        "quicktime"/*,
        "flashmedia"*/
    };
    
    private static Map<String, String> formatToMimetype = new HashMap<String, String>();
    static {
        formatToMimetype.put("microsoftmedia", "video/wmv");
        formatToMimetype.put("flashmedia",     "video/flv");
        formatToMimetype.put("quicktime",      "video/mp4");
    }
    
    @SuppressWarnings("unchecked")
    public static SyndEntry parse(String url) throws IOException, ParserException {
        String content = HttpUtils.get(url, ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);
        // first parse the available formats for this video
        List<String> formats = parseAvailableFormats(content);
        for (int i = 0; i < supportedFormats.length; i++) {
            if(formats.contains(supportedFormats[i])) {
                // the preferred format is the first supported format
                String preferredFormat = supportedFormats[i];
                logger.trace("Best format is {}", preferredFormat);
                SyndEnclosure enclosure = parseVideo(content, preferredFormat);
                
                if(enclosure != null) { // an supported video exists
                    // create a new entry
                    SyndEntry entry = new SyndEntryImpl();
                    entry.getEnclosures().add(enclosure);
                    
                    // parse title
                    entry.setTitle(parseTitle(content));
                    
                    // set link
                    entry.setLink(url);
                    
                    // add guid to freign markup, so that RomeToModelConverter uses that guid
                    if(url != null && url.length() > 0) {
                        Element elem = new Element("guid");
                        elem.setText(url);
                        ((List<Element>)entry.getForeignMarkup()).add(elem);
                    }
                    
                    // parse description
                    String description = parseDescription(content);
                    logger.trace("Description {}", description);
                    SyndContent desc = new SyndContentImpl();
                    desc.setValue(description);
                    entry.setDescription(desc);
                    
                    // parse pubDate
                    try {
                        Date date = parseDate(content);
                        logger.trace("Parsed date {}", date);
                        entry.setPublishedDate(date);
                    } catch (ParseException e) {
                        logger.warn("Couldn't parse publish date. Using current time!", e);
                        logger.trace("Content: {}", content);
                        entry.setPublishedDate(new Date());
                    }
                    
                    if("flashmedia".equalsIgnoreCase(preferredFormat)) {
                        if(entry.getEnclosures().size() > 0 && 
                                ((SyndEnclosure)entry.getEnclosures().get(0)).getUrl().startsWith("rtmp")) 
                        {
                            SyndEnclosure enc = (SyndEnclosure)entry.getEnclosures().get(0);
                            String videoUri = enc.getUrl();
                            String path = Config.getInstance().getHandlerMapping().getPath(StreamBridgeHandler.class);
                            String base = Config.getInstance().getBaseUrl();
                            String uri = base + path + "?uri=" + URLEncoder.encode(videoUri, "UTF-8");
                            enc.setUrl(uri);
                        }
                    }
                    
                    return entry;
                }
            } else {
                logger.trace("No supported video format found on page {} (Formats: {})", url, formats.toString());
            }
        }
        
        return null;
    }
    
    private static String parseDescription(String content) throws ParserException {
        return Translate.decode(HtmlParserUtils.getText(content, ARDMediathekParser.CHARSET, "div[class~=playerinfo] p"));
    }

    private static String parseTitle(String content) throws ParserException {
        return Translate.decode(HtmlParserUtils.getText(content, ARDMediathekParser.CHARSET, "h2[class=beitragstitel]"));
    }
    
    private static Date parseDate(String content) throws ParserException, ParseException {
        NodeList list = (NodeList) HtmlParserUtils.getTags(content, ARDMediathekParser.CHARSET, "span[class=date]");
        for (NodeIterator iterator = list.elements(); iterator.hasMoreNodes();) {
            Node node = (Node) iterator.nextNode();
            String string = node.toPlainTextString().trim();
            Pattern p = Pattern.compile("\\s*Sendung vom:\\s*(\\d+\\.\\d+\\.\\d+\\s*)\\s*\\|\\s*(\\d+):(\\d+)\\s*Uhr\\s*", Pattern.DOTALL);
            Matcher m = p.matcher(string);
            if(m.matches()) {
                Calendar pubDate = Calendar.getInstance();
                pubDate.setTime(new SimpleDateFormat("dd.MM.yy").parse(m.group(1)));
                pubDate.set(Calendar.HOUR_OF_DAY, Integer.parseInt(m.group(2)));
                pubDate.set(Calendar.MINUTE, Integer.parseInt(m.group(3)));
                return new Date(pubDate.getTimeInMillis());
            }
        }
        
        return new Date();
    }
    
    private static SyndEnclosure parseVideo(String content, String preferredFormat) {
        String uri = null;
        // TODO ensure that 1,2,3 stand for low, medium and high quality
        Pattern low = Pattern.compile("player\\.avaible_url\\[\'"+preferredFormat+"\'\\]\\[\'1\'\\] = \"(.+)\";");
        Pattern medium = Pattern.compile("player\\.avaible_url\\[\'"+preferredFormat+"\'\\]\\[\'2\'\\] = \"(.+)\";");
        Pattern high = Pattern.compile("player\\.avaible_url\\[\'"+preferredFormat+"\'\\]\\[\'3\'\\] = \"(.+)\";");
        
        Matcher mLow = low.matcher(content);
        Matcher mMedium = medium.matcher(content);
        Matcher mHigh = high.matcher(content);
        
        if(mHigh.find()) {
            logger.trace("Found hq video {}", mHigh.group(1));
            uri = mHigh.group(1);
        } else if(mMedium.find()) {
            logger.trace("Found mq video {}", mMedium.group(1));
            uri = mMedium.group(1);
        } else if(mLow.find()) {
            logger.trace("Found lq video {}", mLow.group(1));
            uri = mLow.group(1);
        } else {
            logger.warn("No supported video format found");
        }
        
        if("microsoftmedia".equalsIgnoreCase(preferredFormat)) { 
            if(uri != null && !uri.startsWith("mms://")) {
                try {
                    Map<String, List<String>> headers = HttpUtils.head(uri, null, ARDMediathekParser.CHARSET);
                    String type = HttpUtils.getHeaderField(headers, "Content-Type");
                    if("video/x-ms-asf".equalsIgnoreCase(type) || "video/x-ms-asx".equalsIgnoreCase(type)) {
                        // we only have an asx file yet
                        uri = AsxParser.getUri(uri);
                    }
                } catch (Exception e) {
                    logger.trace("Couldn't detect content type. Leaving video URI as is");
                }
            }
        }
        
        SyndEnclosure enclosure = null;
        if(uri != null) {
            enclosure = new SyndEnclosureImpl();
            enclosure.setUrl(uri);
            enclosure.setType(formatToMimetype.get(preferredFormat));
            logger.trace("Mimetype is {}", formatToMimetype.get(preferredFormat));
        }
        
        return enclosure;
    }

    private static List<String> parseAvailableFormats(String content) {
        List<String> formats = new ArrayList<String>();
        Pattern p = Pattern.compile("player.avaibleplayers = new Array\\(\"(\\w+)\"(?:,\"(\\w+)\")*\\)");
        Matcher m = p.matcher(content);
        if(m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                formats.add(m.group(i));
            }
        }
        return formats;
    }
}
