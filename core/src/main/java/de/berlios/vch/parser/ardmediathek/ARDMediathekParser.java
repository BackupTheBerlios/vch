package de.berlios.vch.parser.ardmediathek;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.ParserException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;
import de.berlios.vch.utils.concurrent.ParserThreadFactory;

public class ARDMediathekParser extends AbstractPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(ARDMediathekParser.class);
    
    public static final String BASE_URL = "http://www.ardmediathek.de";
    
    private static final String LANDING_PAGE = BASE_URL + "/ard/servlet/";
    
    public static final String CHARSET = "UTF-8";
    
    public static Map<String,String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", Config.getInstance().getProperty("parser.user.agent"));
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }
    
    
    @Override
    public void run() {
        List<String> programPages = null;
        try {
            programPages = getProgramPages();
        } catch (Exception e) {
            logger.error("Couldn't parse main page for ARD Mediathek", e);
            return;
        }
        
        logger.info("Found {} programs", programPages.size());
        
        // the links in this queue will be processed by several threads
        Queue<String> queue = new ConcurrentLinkedQueue<String>(programPages);
        
        // create a thread pool and process the link queue
        int maxThreads = Config.getInstance().getIntValue("parser.ardmediathek.maxthreads");
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads, new ParserThreadFactory(Messages.translate(getClass(), "group_name")));
        for (int i = 0; i < maxThreads; i++) {
            executorService.execute(new ProgramParser(this, queue));
        }
        
        try {
            // shutdown executor service:
            // all active task will be finished, then the executor service
            // will be shut down, so that no thread keeps alive and the
            // RSSFeedCatcher will terminate as expected
            executorService.shutdown();
            
            // wait 5 minutes for the threads to finish
            // and then shutdown all threads immediately
            executorService.awaitTermination(1l, TimeUnit.HOURS);
            executorService.shutdownNow();
        } catch (InterruptedException e) {
            logger.error("ExecutorService interrupted", e);
        }
        
        // add all feeds to Group
        Group ard = new Group();
        ard.setName(Messages.translate(getClass(), "group_name"));
        ard.setDescription(Messages.translate(getClass(), "group_desc"));
        addFeedsToGroup(ard);
    }

    private List<String> getProgramPages() {
        List<String> pageUris = new ArrayList<String>();
        try {
            String landingPage = HttpUtils.get(LANDING_PAGE, HTTP_HEADERS, CHARSET);
            Tag selectProg = HtmlParserUtils.getTag(landingPage, CHARSET, "form[id=tv_form] select");
            if(selectProg != null) {
                NodeIterator iter = selectProg.getChildren().elements();
                while(iter.hasMoreNodes()) {
                    Node child = iter.nextNode();
                    if(child instanceof OptionTag) {
                        OptionTag option = (OptionTag) child;
                        String pageId = option.getValue();
                        if(pageId != null) {
                            pageUris.add("http://www.ardmediathek.de/ard/servlet/content/1214?moduleId=" + pageId);
                        }
                    }
                }
            } else {
                throw new RuntimeException("No programs found. Maybe the page layout has changed");
            }
        } catch (ParserException e) {
            logger.error("Couldn't parse landing page", e);
        } catch (IOException e) {
            logger.error("Couldn't load the landing page", e);
        }
        return pageUris;
    }

}