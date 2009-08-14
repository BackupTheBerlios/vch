package de.berlios.vch.parser.arte;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.dbutils.DbUtils;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.Config;
import de.berlios.vch.Constants;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.EnclosureDAO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.parser.OndemandParser;
import de.berlios.vch.rome.videocast.Videocast;
import de.berlios.vch.rome.videocast.VideocastImpl;
import de.berlios.vch.utils.AsxParser;
import de.berlios.vch.utils.HttpUtils;
import de.berlios.vch.utils.concurrent.ParserThreadFactory;

public class ArteParser extends AbstractPageParser implements OndemandParser {
	
	private static transient Logger logger = LoggerFactory.getLogger(ArteParser.class);
	
	public static final String CHARSET = "UTF-8";
	
	public static Map<String,String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", Config.getInstance().getProperty("parser.user.agent"));
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }
	
	public static Map<String, SyndFeed> channels;
	
	public void run (){ 
		logger.info("ArteParser started");
		channels = Collections.synchronizedMap(new HashMap<String, SyndFeed>());
		
		String startPageUrl = "http://plus7.arte.tv/de/streaming-home/1698112,templateId=renderCarouselXml,CmPage=1697480,CmPart=com.arte-tv.streaming.xml&preloading=false&introLang=de";
		List<SyndEntry> programPages = null;
		try {
            programPages = getSyndEntries(startPageUrl);
        } catch (Exception e) {
            logger.error("Couldn't parse main page " + startPageUrl, e);
            return;
        }
        
        // the links in this queue will be processed by several threads
        Queue<SyndEntry> queue = new ConcurrentLinkedQueue<SyndEntry>(programPages);
        
        // create a thread pool and process the link queue
        int maxThreads = Config.getInstance().getIntValue("parser.arte.maxthreads");
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads, new ParserThreadFactory(Messages.translate(getClass(), "group_name")));
        for (int i = 0; i < maxThreads; i++) {
            executorService.execute(new ParserThread(queue));
        }
        
        
        try {
            // shutdown executor service:
            // all active task will be finished, then the executor service
            // will be shut down, so that no thread keeps alive and the
            // RSSFeedCatcher will terminate as expected
            executorService.shutdown();
            
            // wait 5 minutes for the threads to finish
            // and then shutdown all threads immediately
            executorService.awaitTermination(5l, TimeUnit.MINUTES);
            executorService.shutdownNow();
        } catch (InterruptedException e) {
            logger.error("ExecutorService interrupted", e);
        }
        
        // save the feeds
        for (SyndFeed feed : channels.values()) {
            saveFeed2DB(feed);
            addRssFeed(feed.getLink());
        }
        
        Connection conn = null;
		try {
		    logger.info("Deleting orphan enclosures");
		    conn = ConnectionManager.getInstance().getConnection();
            new EnclosureDAO(conn).deleteOrphan();
        } catch (SQLException e) {
            logger.warn("Couldn't delete orphan enclosures", e);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
        
        // add all feeds to Group
        Group arte = new Group();
        arte.setName(Messages.translate(getClass(), "group_name"));
        arte.setDescription(Messages.translate(getClass(), "group_desc"));
        addFeedsToGroup(arte);
	}

    @SuppressWarnings("unchecked")
    private List<SyndEntry> getSyndEntries(String carouselUrl) {
        List<SyndEntry> list = new ArrayList<SyndEntry>();
        try {                                      
            URL url = new URL(carouselUrl);
            URLConnection con = url.openConnection();

            org.jdom.Document doc = new SAXBuilder().build(con.getInputStream());
            Element videos = doc.getRootElement();
            List<Element> elemente = videos.getChildren();
            for (Iterator iterator = elemente.iterator(); iterator.hasNext();) {
                Element video = (Element) iterator.next();
                SyndEntry entry = new SyndEntryImpl();
                
                // parse link
                String mediaPageUrl = video.getChildText("targetURL");
                entry.setLink(mediaPageUrl);
                
                // parse title
                String title = video.getChildText("bigTitle");
                entry.setTitle(title);
                
                // parse image
                String imageLink = video.getChildText("previewPictureURL");
                Videocast myvidcast = new VideocastImpl();
                myvidcast.setImage(imageLink);
                entry.getModules().add(myvidcast);
                
                // parse pubDate
                String dateString = video.getChildText("startDate");
                Date pubDate = new SimpleDateFormat(Constants.ARTE_DATE_FORMAT).parse(dateString);
                entry.setPublishedDate(pubDate);
                
                // set the guid
                Element elem = new Element("guid");
                elem.setText(entry.getLink());
                ((List<Element>)entry.getForeignMarkup()).add(elem);
                
                list.add(entry);
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

    @Override
    public String parseOnDemand(String webpage) {
        return AsxParser.getUri(webpage);
    }
}