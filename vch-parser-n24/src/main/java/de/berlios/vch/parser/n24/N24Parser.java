package de.berlios.vch.parser.n24;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPageTitleComparator;
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

    private URL wsdlUrl;
    private QName serviceName;
    private TvNextCore service;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public IOverviewPage getRoot() throws Exception {
        final OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        final TvNextCorePortType port = service.getPort(TvNextCorePortType.class);
        ArrayOfN24Ressort array = port.getRessorts(10);
        List<N24Ressort> ressorts = array.getN24Ressort();
        ExecutorService executor = Executors.newFixedThreadPool(10);
        for (final N24Ressort ressort : ressorts) {
            final URI ressortURI = new URI("http://www.n24.de/" + URLEncoder.encode(ressort.getTitle().getValue(), "UTF-8"));

            executor.submit(new Runnable() {
                @Override
                public void run() {
                    OverviewPage ressortPage = new OverviewPage();
                    ressortPage.setParser(ID);
                    ressortPage.setTitle(ressort.getTitle().getValue());
                    ressortPage.setUri(ressortURI);
                    page.getPages().add(ressortPage);

                    // feed.setDescription(ressort.getDescription() != null ? ressort.getDescription().getValue() : "");

                    List<TvNextClip> clips = port.getClipsByRessortId(ressort.getId().getValue(), 0, 100).getTvNextClip();
                    for (TvNextClip clip : clips) {
                        if (clip.getHeader() == null) {
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
                            // set a dummy uri, so that this page is identifyable
                            video.setUri(new URI("n24://" + clip.getSource().getValue()));

                            ressortPage.getPages().add(video);
                        } catch (URISyntaxException e) {
                            logger.log(LogService.LOG_ERROR, e.getMessage(), e);
                        }
                    }

                    // special case Magazine: we aggregate the videos by title and create sub pages
                    if ("Magazine".equals(ressortPage.getTitle())) {
                        Map<String, List<IWebPage>> magazine = new HashMap<String, List<IWebPage>>();
                        for (IWebPage page : ressortPage.getPages()) {
                            List<IWebPage> list = magazine.get(page.getTitle());
                            if (list == null) {
                                list = new ArrayList<IWebPage>();
                                magazine.put(page.getTitle(), list);
                            }
                            list.add(page);
                        }

                        // clear all videos from the ressort page. afterwards we can add subPages, which will contain the videos
                        ressortPage.getPages().clear();

                        List<String> subPageTitles = new ArrayList<String>(magazine.keySet());
                        Collections.sort(subPageTitles);
                        for (String title : subPageTitles) {
                            try {
                                IOverviewPage subPage = new OverviewPage();
                                subPage.setParser(getId());
                                subPage.setTitle(title);
                                subPage.setUri(new URI("dummy://" + UUID.randomUUID()));
                                List<IWebPage> videos = magazine.get(title);
                                for (IWebPage videoPage : videos) {
                                    if (videoPage instanceof IVideoPage) {
                                        IVideoPage vpage = (IVideoPage) videoPage;
                                        if (vpage.getDescription() != null) {
                                            String[] lines = vpage.getDescription().split("\n");
                                            vpage.setTitle(lines[0]);
                                        }
                                    }
                                    subPage.getPages().add(videoPage);
                                }
                                ressortPage.getPages().add(subPage);
                            } catch (Exception e) {
                                logger.log(LogService.LOG_ERROR, "Couldn't create subpage " + title, e);
                            }
                        }
                    }
                }
            });
        }

        // wait for all threads to finish
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);

        // sort ressorts by title
        Collections.sort(page.getPages(), new WebPageTitleComparator());
        return page;
    }

    @Override
    public String getTitle() {
        return "N24 Mediencenter";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            if (!supportedProtocols.contains(vpage.getVideoUri().getScheme())) {
                throw new NoSupportedVideoFoundException(vpage.getVideoUri().toString(), supportedProtocols);
            }
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
