package de.berlios.vch.parser.rtlnow;

import java.io.IOException;
import java.net.MalformedURLException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.DbUtils;
import org.htmlparser.Node;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.SimpleNodeIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.EnclosureDAO;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.parser.OndemandParser;
import de.berlios.vch.utils.AsxParser;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;
import de.berlios.vch.utils.concurrent.ParserThreadFactory;

public class RTLnowParser extends AbstractPageParser implements OndemandParser {

    private static transient Logger logger = LoggerFactory.getLogger(RTLnowParser.class);
    
    public static final String BASE_URL = "http://rtl-now.rtl.de";
    
    public static final String CHARSET = "iso-8859-1";

    @Override
    public void run() {
        // parse main pages for programs
        List<LinkTag> programmPages;
        try {
             programmPages = parseMainPage();
        } catch (Exception e) {
            logger.error("Couldn't parse main page " + BASE_URL, e);
            return;
        }

        // the links in this queue will be processed by several threads
        Queue<LinkTag> queue = new ConcurrentLinkedQueue<LinkTag>(programmPages);
        
        // create a thread pool and process the link queue
        int maxThreads = Config.getInstance().getIntValue("parser.rtlnow.maxthreads");
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads, new ParserThreadFactory(Messages.translate(getClass(), "group_name")));
        for (int i = 0; i < maxThreads && !queue.isEmpty(); i++) {
            executorService.execute(new ParserThread(this, queue));
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
        
        // delete orphan enclosures
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
                logger.error("Couldn't close DB connection", e);
            }
        }
        
        // add all feeds to Group
        Group rtlnow = new Group();
        rtlnow.setName(Messages.translate(getClass(), "group_name"));
        rtlnow.setDescription(Messages.translate(getClass(), "group_desc"));
        addFeedsToGroup(rtlnow);
    }

    private List<LinkTag> parseMainPage() throws ParserException, MalformedURLException, IOException {
        logger.debug("Parsing main page " + BASE_URL);
        
        String content = HttpUtils.get(BASE_URL, null, CHARSET);
        NodeList links = HtmlParserUtils.getTags(content, CHARSET, "div[class=freenavi] a");
        logger.info("Found {} free programs", links.size());
        
        List<LinkTag> pages = new ArrayList<LinkTag>();
        NodeIterator iter = links.elements();
        while(iter.hasMoreNodes()) {
            LinkTag link = (LinkTag) iter.nextNode();
            LinkTag titledLink = getTitledLink(link);
	            if(titledLink != null) {
	            String uri = titledLink.getLink();
	            uri = uri.startsWith("/") ? uri : '/' + uri;
	            titledLink.setLink(BASE_URL + uri);
	            pages.add(titledLink);
            } else {
            	logger.error("Couldn't find title for program {}", link.getLink());
            }
        }
        
        return pages;
    }
    
    private LinkTag getTitledLink(LinkTag imageLink) {
    	Node parent = imageLink.getParent().getParent();
    	NodeList childs = parent.getChildren();
    	for (SimpleNodeIterator iterator = childs.elements(); iterator.hasMoreNodes();) {
			Node child = (Node) iterator.nextNode();
			if(child instanceof Div) {
				Div div = (Div) child;
				if("seriennavi_link".equals(div.getAttribute("class"))) {
					return (LinkTag) div.getChild(0);
				}
			}
		}
    	return null;
    }

    @Override
    public String parseOnDemand(String webpage) {
        String enclosureURI = null;
        
        try {
            String asxFile = parseContainerPage(webpage);

            if (asxFile.startsWith("mms://")) { // not an asx file
                enclosureURI = asxFile;
            } else {
                enclosureURI = parseAsxFile(asxFile);
            }
        } catch (IOException e) {
            logger.error("Couldn't parse container page", e);
        }
        
        return enclosureURI;
    }
    
    private String parseAsxFile(String asxFile) {
        logger.debug("Parsing ASX file " + asxFile);
        if(asxFile != null) {
            // the first entry is in most cases advertising, so
            // in the first try, we take entry 2 (index 1)
            // if this fails, there probably is no ad, so we take
            // the first entry
            String uri = AsxParser.getUri(asxFile, 1);
            if(uri.length() > 0) {
                return uri;
            } else {
                return AsxParser.getUri(asxFile, 0);
            }
        }
        
        return null;
    }

    private String parseContainerPage(String containerPageUrl) throws IOException {
        logger.debug("Parsing container page " + containerPageUrl);
        Map<String,String> headers = new HashMap<String, String>();
        headers.put("User-Agent", Config.getInstance().getProperty("parser.user.agent"));
        String containerPage = HttpUtils.get(containerPageUrl, headers, RTLnowParser.CHARSET);
        Pattern p = Pattern.compile("var movie=\"(.*)\";");
        Matcher m = p.matcher(containerPage);
        if(m.find()) {
            return m.group(1);
        }
        
        return null;
    }
}
