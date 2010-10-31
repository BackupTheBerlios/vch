package de.berlios.vch.parser.lindenstr;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.tags.LinkTag;
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
public class LindenStrParser implements IWebParser {

    public static final String ID = LindenStrParser.class.getName();
    
    public static final String STREAM_BASE = "rtmp://gffstream.fcod.llnwd.net:1935/a792/e2";

    public static final String BASE_URI = "http://www.lindenstrasse.de";
    private static final String START_PAGE = BASE_URI + "/lindenstrasse/lindenstrassecms.nsf/index/910A93860D8FB367C1257690004991EC?OpenDocument&par=pg01";
    
    public static final String CHARSET = "iso-8859-1";
    
    @Requires
    private LogService logger;
    
    private List<String> supportedProtocols = new ArrayList<String>();
    
    @Override
    public String getId() {
        return ID;
    }

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));
        
        String content = HttpUtils.get(START_PAGE, null, CHARSET);
        NodeList links = HtmlParserUtils.getTags(content, CHARSET, "td[width=\"332\"] a");

        SimpleDateFormat sdf = new SimpleDateFormat("'(Sendedatum: 'dd. MMMM yyyy')'");
        for (int i = 0; i < links.size(); i++) {
            try {
                LinkTag link = (LinkTag) links.elementAt(i);
                IVideoPage video = new VideoPage();
                video.setParser(getId());
                video.setTitle(Translate.decode(link.getLinkText().trim()));
                video.setUri(new URI(BASE_URI + link.extractLink()));
                
                try {
                    Node sibling = link;
                    while( (sibling = sibling.getNextSibling()) != null) {
                        String tagContent = sibling.toPlainTextString();
                        if(tagContent.startsWith("(Sendedatum")) {
                            Date pubDate = sdf.parse(tagContent);
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(pubDate);
                            video.setPublishDate(cal);
                            break;
                        } else if(sibling instanceof LinkTag) {
                            // we arrived at the next link, so we didn't find a date for this link
                            break;
                        }
                    }
                } catch(Exception e) {
                    logger.log(LogService.LOG_WARNING, "Couldn't parse publish date", e);
                }
                
                page.getPages().add(video);
            } catch(Exception e) {
                logger.log(LogService.LOG_WARNING, "Couldn't add all videos", e);
            }
        }
        
        return page;
    }

    @Override
    public String getTitle() {
        return "Lindenstraße";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            
            // parse the video uri
            String content = HttpUtils.get(page.getUri().toString(), null, CHARSET);
            Tag param = HtmlParserUtils.getTag(content, CHARSET, "param[name=\"flashvars\"]");
            String flashvars = param.getAttribute("value");
            int start = flashvars.indexOf("dslSrc=") + 7;
            int stop = flashvars.indexOf('&', start);
            String _videoUri = flashvars.substring(start, stop);
            String playPath = "mp4:" + _videoUri.substring(STREAM_BASE.length(), _videoUri.length()-4);
            URI videoUri = new URI(_videoUri);
            if(supportedProtocols.contains(videoUri.getScheme())) {
                vpage.setVideoUri(videoUri);
                vpage.getUserData().put("streamName", playPath);
            } else {
                throw new NoSupportedVideoFoundException(videoUri.toString(), supportedProtocols);
            }
        }
        return page;
    }
    
// ############ ipojo stuff #########################################    
    
    // validate and invalidate method seem to be necessary for the bind methods to work
    @Validate
    public void start() {}
    
    @Invalidate
    public void stop() {}

    @Bind(id = "supportedProtocols", aggregate = true)
    public synchronized void addProtocol(INetworkProtocol protocol) {
        supportedProtocols.addAll(protocol.getSchemes());
    }
    
    @Unbind(id="supportedProtocols", aggregate = true)
    public synchronized void removeProtocol(INetworkProtocol protocol) {
        supportedProtocols.removeAll(protocol.getSchemes());
    }
}