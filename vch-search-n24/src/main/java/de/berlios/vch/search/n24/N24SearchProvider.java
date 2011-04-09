package de.berlios.vch.search.n24;

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
import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.cache.Cache;
import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;
import de.berlios.vch.parser.n24.tvnext.ArrayOfTvNextClip;
import de.berlios.vch.parser.n24.tvnext.TvNextClip;
import de.berlios.vch.parser.n24.tvnext.TvNextCore;
import de.berlios.vch.parser.n24.tvnext.TvNextCorePortType;
import de.berlios.vch.search.ISearchProvider;

@Component
@Provides
public class N24SearchProvider implements ISearchProvider {

    @Requires
    private LogService logger;

    private URL wsdlUrl;
    private QName serviceName;
    private TvNextCore service;

    public static final String STREAM_BASE = "rtmp://pssimn24fs.fplive.net:1935/pssimn24";

    private List<String> supportedProtocols = new ArrayList<String>();

    private Cache<String, IVideoPage> resultCache = new Cache<String, IVideoPage>("N24 Search Result Cache", 20, 5, TimeUnit.MINUTES);

    @Override
    public String getName() {
        return "N24 Mediencenter";
    }

    @Override
    public IOverviewPage search(String query) throws Exception {
        // execute the search
        final TvNextCorePortType port = service.getPort(TvNextCorePortType.class);
        ArrayOfTvNextClip result = port.search(query, 0, 20);
        List<TvNextClip> clips = result.getTvNextClip();

        IOverviewPage opage = new OverviewPage();
        opage.setTitle(getName());
        opage.setParser(getId());

        for (TvNextClip clip : clips) {
            if (clip.getHeader() == null) {
                logger.log(LogService.LOG_WARNING, "Clip header is null");
                continue;
            }

            VideoPage video = new VideoPage();
            video.setParser(getId());

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
                // set a dummy uri, so that this page is identifyable
                String uri = "n24://" + clip.getSource().getValue();
                video.setUri(new URI(uri));
                resultCache.put(uri, video);
                opage.getPages().add(video);
            } catch (URISyntaxException e) {
                logger.log(LogService.LOG_ERROR, e.getMessage(), e);
            }
        }

        return opage;
    }

    @Override
    public String getId() {
        return N24SearchProvider.class.getName();
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IVideoPage) {
            IVideoPage vpage = resultCache.get(page.getUri().toString());
            if (vpage == null) {
                throw new RuntimeException("Page [" + page.getUri() + "] not found in cache");
            }
            if (!supportedProtocols.contains(vpage.getVideoUri().getScheme())) {
                throw new NoSupportedVideoFoundException(vpage.getVideoUri().toString(), supportedProtocols);
            }
            page = vpage;
        }
        return page;
    }

    // ############ ipojo stuff #########################################

    // validate and invalidate method seem to be necessary for the bind methods to work
    @Validate
    public void start() throws MalformedURLException {
        wsdlUrl = new URL("http://mediencenter.n24.de/index.php/service/wsdl");
        serviceName = new QName("http://schemas.exozet.com/tvnext/services/core/", "TvNextCore");
        service = new TvNextCore(wsdlUrl, serviceName);
    }

    @Invalidate
    public void stop() {
        wsdlUrl = null;
        serviceName = null;
        service = null;
    }

    @Bind(id = "supportedProtocols", aggregate = true)
    public synchronized void addProtocol(INetworkProtocol protocol) {
        supportedProtocols.addAll(protocol.getSchemes());
    }

    @Unbind(id = "supportedProtocols", aggregate = true)
    public synchronized void removeProtocol(INetworkProtocol protocol) {
        supportedProtocols.removeAll(protocol.getSchemes());
    }

}
