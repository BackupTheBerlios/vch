package de.berlios.vch.parser.lindenstr;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.Tag;
import org.htmlparser.Text;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
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
public class LindenStrParser implements IWebParser {

    public static final String ID = LindenStrParser.class.getName();
    
    public static final String STREAM_BASE = "rtmp://gffstream.fcod.llnwd.net:1935/a792/e2";

    public static final String BASE_URI = "http://www.lindenstrasse.de";
    private static final String START_PAGE = BASE_URI + "/lindenstrasse/lindenstrassecms.nsf/index/910A93860D8FB367C1257690004991EC?OpenDocument&par=pg01";
    
    public static final String CHARSET = "iso-8859-1";
    
    @Requires
    private LogService logger;
    
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
        NodeList links = HtmlParserUtils.getTags(content, CHARSET, "td[width=\"340\"] a");

        SimpleDateFormat sdf = new SimpleDateFormat("'(Sendedatum: 'dd. MMMM yyyy')'");
        for (int i = 0; i < links.size(); i++) {
            try {
                LinkTag link = (LinkTag) links.elementAt(i);
                IVideoPage video = new VideoPage();
                video.setParser(getId());
                video.setTitle(Translate.decode(link.getLinkText().trim()));
                video.setUri(new URI(BASE_URI + link.extractLink()));
                
                try {
                    Text date = (Text) link.getNextSibling().getNextSibling().getNextSibling().getNextSibling();
                    String tagContent = date.toPlainTextString();
                    Date pubDate = sdf.parse(tagContent);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(pubDate);
                    video.setPublishDate(cal);
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
            String videoUri = flashvars.substring(start, stop);
            String playPath = "mp4:" + videoUri.substring(STREAM_BASE.length(), videoUri.length()-4);
            vpage.setVideoUri(new URI(videoUri));
            vpage.getUserData().put("streamName", playPath);
        }
        return page;
    }
}
