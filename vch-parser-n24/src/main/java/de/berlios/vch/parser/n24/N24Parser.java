package de.berlios.vch.parser.n24;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.log.LogService;

import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;
import de.berlios.vch.parser.n24.tvnext.ArrayOfN24Ressort;
import de.berlios.vch.parser.n24.tvnext.N24Ressort;
import de.berlios.vch.parser.n24.tvnext.TvNextClip;
import de.berlios.vch.parser.n24.tvnext.TvNextCore;
import de.berlios.vch.parser.n24.tvnext.TvNextCorePortType;

@Component
@Provides
public class N24Parser implements IWebParser {

    public static final String ID = N24Parser.class.getName();
    
    public static final String STREAM_BASE = "rtmp://pssimn24fs.fplive.net:1935/pssimn24";

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
        try {
            // TODO move initialization to start method 
            URL wsdlUrl = new URL("http://mediencenter.n24.de/index.php/service/wsdl");
            QName serviceName = new QName("http://schemas.exozet.com/tvnext/services/core/","TvNextCore");
            TvNextCore service = new TvNextCore(wsdlUrl, serviceName);
            
            TvNextCorePortType port = (TvNextCorePortType) service.getPort(TvNextCorePortType.class);
            ArrayOfN24Ressort array = port.getRessorts(10);
            List<N24Ressort> ressorts = array.getN24Ressort();
            for (N24Ressort ressort : ressorts) {
                //TODO ressort magazine nochmal aufspalten in einzelne feeds
                
                OverviewPage ressortPage = new OverviewPage();
                ressortPage.setParser(ID);
                ressortPage.setTitle(ressort.getTitle().getValue());
                ressortPage.setUri(new URI("http://www.n24.de/" + ressort.getTitle().getValue()));
                //feed.setDescription(ressort.getDescription() != null ? ressort.getDescription().getValue() : "");
                
                List<TvNextClip> clips = port.getClipsByRessortId(ressort.getId().getValue(), 0, 100).getTvNextClip();
                for (TvNextClip clip : clips) {
                    if(clip.getHeader() == null) {
                        logger.log(LogService.LOG_WARNING, "Clip header is null");
                        continue;
                    }
                    
                    VideoPage video = new VideoPage();
                    video.setParser(ID);
                    
                    // set the title
                    video.setTitle(clip.getHeader().getValue());
                    
                    // set the description
                    video.setDescription(clip.getTitle().getValue());

                    // try to parse the publish date
                    try {
                        Date date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(clip.getCreatedTimestamp().getValue());
                        Calendar publishDate = Calendar.getInstance();
                        publishDate.setTime(date);
                        video.setPublishDate(publishDate);
                    } catch (ParseException e) {
                        logger.log(LogService.LOG_WARNING, "Couldn't parse video pubDate", e);
                    }

                    // set the duration
                    video.setDuration(clip.getFlvDuration().getValue());
                    
                    // set the video uri
                    try {
                        URI videoUri = new URI(STREAM_BASE + clip.getStreamPath().getValue().replaceAll(" ", "+"));
                        video.setVideoUri(videoUri);
                        video.getUserData().put("streamName", clip.getStreamPath().getValue().replaceAll(" ", "+"));
                        
                        ressortPage.getPages().add(video);
                    } catch (URISyntaxException e) {
                        logger.log(LogService.LOG_ERROR, e.getMessage(), e);
                    }
                    
                    // set a dummy uri, so that this page is identifyable
                    video.setUri(new URI("n24://" + clip.getSource().getValue()));
                }
                
                page.getPages().add(ressortPage);
                
            }
        } catch (MalformedURLException e) {
            logger.log(LogService.LOG_ERROR, "WSDL URL invalid", e);
        }
        return page;
    }

    @Override
    public String getTitle() {
        return "N24 Mediencenter";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            if(!supportedProtocols.contains(vpage.getVideoUri().getScheme())) {
                throw new NoSupportedVideoFoundException(vpage.getVideoUri().toString(), supportedProtocols);
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
