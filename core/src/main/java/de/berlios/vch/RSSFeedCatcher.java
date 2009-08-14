package de.berlios.vch;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.db.DBSetup;
import de.berlios.vch.parser.PageParser;
import de.berlios.vch.parser.ardmediathek.ARDMediathekParser;
import de.berlios.vch.parser.arte.ArteParser;
import de.berlios.vch.parser.brainblog.BrainblogParser;
import de.berlios.vch.parser.dmax.DmaxParser;
import de.berlios.vch.parser.dreisat.DreisatMediathekParser;
import de.berlios.vch.parser.rtlnow.RTLnowParser;
import de.berlios.vch.parser.tvtotal.TVTotalParser;
import de.berlios.vch.parser.userfeeds.UserFeedsParser;
import de.berlios.vch.parser.youporn.YoupornParser;
import de.berlios.vch.parser.youtube.YoutubeParser;
import de.berlios.vch.parser.zdfmediathek.ZDFMediathekParser;

public class RSSFeedCatcher {

    private static transient Logger logger = LoggerFactory.getLogger(RSSFeedCatcher.class);
    
    private boolean locked = false;
    
    private static RSSFeedCatcher instance;
    
    public static synchronized RSSFeedCatcher getInstance() {
        if(instance == null) {
            instance = new RSSFeedCatcher();
        }
        return instance;
    }
    
    public synchronized boolean lock() {
        if(locked) {
            return false;
        } else {
            locked = true;
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private static Class[] parserClasses = {
        ARDMediathekParser.class,
    	ArteParser.class,
        BrainblogParser.class,
        DreisatMediathekParser.class,
        RTLnowParser.class,
        TVTotalParser.class,
        YoupornParser.class,
        YoutubeParser.class,
        ZDFMediathekParser.class,
        DmaxParser.class,
        UserFeedsParser.class,
        //N24Parser.class
    };
    
    public static void main(String[] args) throws MalformedURLException {
        Config.getInstance();
        
        DBSetup setup = new DBSetup();
        setup.runSetupIfNecessary();
        
        List<String> feeds = RSSFeedCatcher.getInstance().startParsers();
        printFeedUrls(feeds);
    }
    
    @SuppressWarnings("unchecked")
    public List<String> startParsers() {
        if(!lock()) {
            return null;
        }
        
        // start the parsers, each in an own thread
        List<PageParser> parsers = new ArrayList<PageParser>();
        List<Thread> threads = new ArrayList<Thread>();
        
        for (int i = 0; i < parserClasses.length; i++) {
            Class<PageParser> clazz = parserClasses[i];
            if (!Config.getInstance().getBoolValue(clazz.getName() + ".enabled")) {
                // parser has been disabled explicitly
                continue;
            }
            
            try {
                logger.info("Loading PageParser " + clazz.getName() + "\nTo disable this parser add \""+clazz.getName()+".enabled=false\" to vodcatcherhelper.properties");
                PageParser parser = (PageParser)clazz.newInstance();
                threads.add(startParser(parser));
                parsers.add(parser);
            } catch (Exception e) {
                logger.error("Couldn't load parser " + clazz.getName(), e);
            } 
        }
        
        // wait for all parsers to finish
        try {
            for (Iterator<Thread> iterator = threads.iterator(); iterator.hasNext();) {
                Thread t = iterator.next();
                t.join(TimeUnit.MINUTES.toMillis(30));
            }
        } catch (InterruptedException e) {
            logger.error("Couldn't wait for the parsers", e);
        }
        
        // collect all feeds
        List<String> feeds = new ArrayList<String>();
        for (Iterator<PageParser> iterator = parsers.iterator(); iterator.hasNext();) {
            PageParser pageParser = iterator.next();
            feeds.addAll(pageParser.getRssFeeds());
        }
        Collections.sort(feeds);
        
        locked = false;
        return feeds;
    }
    
    private Thread startParser(PageParser parser) {
        Thread t = new Thread(parser);
        t.setName(parser.getClass().getSimpleName() + " thread");
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        return t;
    }
    
    private static void printFeedUrls(List<String> feeds) {
        Collections.sort(feeds);
        String encoding = Config.getInstance().getProperty("default.encoding");
        String baseUrl = Config.getInstance().getBaseUrl()
            + Config.getInstance().getHandlerMapping().getFeedPath()
            + "?link=";
        for (String link : feeds) {
            String encodedLink = link;
            try {
                encodedLink = URLEncoder.encode(link, encoding);
            } catch (UnsupportedEncodingException e) {
                logger.warn("Couldn't encode link", e);
            }
            System.out.println(baseUrl + encodedLink);
        }
    }
}
