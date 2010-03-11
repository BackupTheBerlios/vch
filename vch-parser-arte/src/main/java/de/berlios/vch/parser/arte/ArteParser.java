package de.berlios.vch.parser.arte;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPageTitleComparator;

public class ArteParser implements IWebParser, BundleActivator {
    private static transient Logger logger = LoggerFactory.getLogger(ArteParser.class);
    
    public static final String CHARSET = "UTF-8";
    
    public static final String ARTE_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    
    public static final String CAROUSEL_URL = "http://plus7.arte.tv/de/streaming-home/1698112,templateId=renderCarouselXml,CmPage=1697480,CmPart=com.arte-tv.streaming.xml&preloading=false&introLang=de";
    
    public static final String ID = ArteParser.class.getName();
    
    public static Map<String,String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.2) Gecko/20090821 Gentoo Firefox/3.5.2");
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }
    
    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));
        
        List<IWebPage> pages = createPageList();
        Collections.sort(pages, new WebPageTitleComparator());
        page.getPages().addAll(pages);
        
        return page;
    }
    
    private List<IWebPage> createPageList() throws URISyntaxException {
        List<IWebPage> pageList = new ArrayList<IWebPage>();
        List<IVideoPage> videos = getVideos(CAROUSEL_URL);
        Map<String, List<IVideoPage>> categories = new HashMap<String, List<IVideoPage>>();
        for (IVideoPage video : videos) {
            List<IVideoPage> category = categories.get(video.getTitle());
            if(category == null) {
                category = new ArrayList<IVideoPage>();
                categories.put(video.getTitle(), category);
            }
            category.add(video);
        }
        List<String> keys = new ArrayList<String>(categories.keySet());
        Collections.sort(keys);
        
        for (String title : keys) {
            List<IVideoPage> category = categories.get(title);
            if(category.size() == 1) {
                pageList.add(category.get(0));
            } else {
                OverviewPage overview = new OverviewPage();
                overview.setParser(ID);
                overview.setTitle(title);
                overview.setUri(new URI("dummy://" + UUID.randomUUID()));
                overview.getPages().addAll(category);
                pageList.add(overview);
            }
        }
        return pageList;
    }

    @Override
    public String getTitle() {
        return "Arte+7";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) page;
            if(video.getVideoUri() == null) {
                VideoPageParser.parse(video);
            }
            return video;
        } else if(page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            ExecutorService pool = Executors.newFixedThreadPool(10);
            for (final IWebPage webPage : opage.getPages()) {
                if(webPage instanceof IVideoPage) {
                    pool.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                VideoPageParser.parse((IVideoPage) webPage);
                            } catch (Exception e) {
                                logger.error("Couldn't parse page " + webPage.getUri(), e);
                            }                            
                        }
                    });
                }
            }
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.MINUTES);
            pool.shutdownNow();
            return page;
        } else {
            return page;
        }
    }

    @Override
    public void start(BundleContext ctx) throws Exception {
        ctx.registerService(IWebParser.class.getName(), this, null);
    }

    @Override
    public void stop(BundleContext ctx) throws Exception {
    }

    @Override
    public String getId() {
        return ID;
    }
    
    private List<IVideoPage> getVideos(String carouselUrl) {
        List<IVideoPage> list = new ArrayList<IVideoPage>();
        try {                                      
            String content = HttpUtils.get(carouselUrl, null, CHARSET);
            org.jdom.Document doc = new SAXBuilder().build(new StringReader(content));
            Element videos = doc.getRootElement();
            List<?> elemente = videos.getChildren();
            for (Iterator<?> iterator = elemente.iterator(); iterator.hasNext();) {
                VideoPage videoPage = new VideoPage();
                videoPage.setParser(ID);
                Element video = (Element) iterator.next();
                
                // parse link
                String mediaPageUrl = video.getChildText("targetURL");
                videoPage.setUri(new URI(mediaPageUrl));
                
                // parse title
                String title = video.getChildText("bigTitle");
                videoPage.setTitle(title);
                
                // parse pubDate
                String dateString = video.getChildText("startDate");
                Date pubDate = new SimpleDateFormat(ARTE_DATE_FORMAT).parse(dateString);
                Calendar cal = Calendar.getInstance();
                cal.setTime(pubDate);
                videoPage.setPublishDate(cal);
                
                // parse previewPicture
                String previewPicture = video.getChildText("previewPictureURL");
                videoPage.setThumbnail(new URI(previewPicture));
                
                list.add(videoPage);
            }
        } catch (Exception e) {
            logger.error("Couldn't parse carousel " + carouselUrl, e);
            if(logger.isTraceEnabled()) {
                try {
                    String content = HttpUtils.get(carouselUrl, null, ArteParser.CHARSET);
                    logger.trace("Carousel content: {}", content);
                } catch (IOException e1) {}
            }
        }
        return list;
    }
}