package de.berlios.vch.parser.servustv;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
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
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;

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
        if(page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            if(page.getUri().toString().startsWith("servus://program/")) {
                String programId = page.getUri().getPath().substring(1);
                String pageUri = BASE_URI + "/cs/Satellite?pagename=ServusTV%2FAjax%2FMediathekData&nachThemen=all"
                    +"&nachThemenNodeId=null&nachThemen_changed=1&nachSendung_changed=2&ajax=true&nachSendung="
                    + programId;
                String content = HttpUtils.get(pageUri, null, CHARSET);
                NodeList episodes = HtmlParserUtils.getTags(content, CHARSET, "ul.programScrollerSmall li");
                NodeIterator iter = episodes.elements();
                while(iter.hasMoreNodes()) {
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
                   } catch(Exception e) {
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
        } else if(page instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) page;
            
            // determine the uri of the xml descriptor of the video
            String content = HttpUtils.get(video.getUri().toString(), null, CHARSET);
            LinkTag playerButton = (LinkTag) HtmlParserUtils.getTag(content, CHARSET, "div.mod2Col div.modBody a[class~=playControl]");
            URI link = new URI(BASE_URI + playerButton.extractLink());
            logger.log(LogService.LOG_INFO, link.toString());
            Map<String, List<String>> params = HttpUtils.parseQuery(link.getQuery());
            String articleId = params.get("assetId").get(0);
            String cid = params.get("cid").get(0);
            String xmlUri = BASE_URI + "/cs/Satellite?articleId=" + articleId 
                + "&c=ST_Video"    
                + "&cid=" + cid
                + "&pagename=servustv/ST_Video/VideoPlayerDataXML&programType=vod";
            
            // download the xml descriptior and parse the video uri
            String xml = HttpUtils.get(xmlUri, null, CHARSET);
            // because they are to stupid to send well-formed xml we have to parse manually >:(
            String _videoUri = getElementContent(xml, "high_video_url");
            String playPath = _videoUri.substring(STREAM_BASE.length()+1, _videoUri.length());
            URI videoUri = new URI(_videoUri);
            if(supportedProtocols.contains(videoUri.getScheme())) {
                video.setVideoUri(videoUri);
                video.getUserData().put("streamName", playPath);
            } else {
                throw new NoSupportedVideoFoundException(videoUri.toString(), supportedProtocols);
            }
            
            // parse the duration
            String durationString = null;
            try {
                durationString = getElementContent(xml, "duration");
                if(durationString != null && !durationString.isEmpty()) {
                    long duration = Long.parseLong(durationString);
                    video.setDuration(duration);
                }
            } catch (Exception e) {
                logger.log(LogService.LOG_WARNING, "Couldn't parse duration " + durationString);
            }
        }
        
        return page;
    }

    private String getElementContent(String xml, String elementName) {
        int start = xml.indexOf('<'+elementName+'>') + elementName.length() + 2;
        int stop = xml.indexOf("</"+elementName+'>');
        if(stop == start) {
            return "";
        } else {
            return xml.substring(start, stop);
        }
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
    
    @Unbind(id="supportedProtocols", aggregate = true)
    public synchronized void removeProtocol(INetworkProtocol protocol) {
        supportedProtocols.removeAll(protocol.getSchemes());
    }
}