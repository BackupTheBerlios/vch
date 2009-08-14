package de.berlios.vch.parser.youtube;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.dbutils.DbUtils;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndImage;
import com.sun.syndication.feed.synd.SyndImageImpl;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.EnclosureDAO;
import de.berlios.vch.http.handler.OndemandStreamHandler;
import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.parser.OndemandParser;

// TODO support HQ videos, if available
public class YoutubeParser extends AbstractPageParser implements OndemandParser {
	
    private static transient Logger logger = LoggerFactory.getLogger(YoutubeParser.class);
    
    private final String PAGE_ENCODING = "UTF-8";
    
    public String parseOnDemand (String videoLink) {
        logger.debug("Getting video link for " + videoLink);
    	
    	String medialink = null;
    	try {
    		
            URL url = new URL(videoLink);
            URLConnection con = url.openConnection();
            con.addRequestProperty("Accept-Encoding", "gzip");
            
            InputStream in = con.getInputStream();
            String encoding = con.getContentEncoding();
            if("gzip".equalsIgnoreCase(encoding)) {
                in = new GZIPInputStream(in);
            }
            
            BufferedReader bi = new BufferedReader(new InputStreamReader(in, PAGE_ENCODING));
            String line;
            while ((line = bi.readLine()) != null) {
            	
            	if (line.matches(".*fullscreenUrl.*")) {
            		
            		String video_id = "video_id=" + line.split("video_id=")[1].split("&")[0];
            		String t = "&t=" + line.split("&t=")[1].split("&")[0];
            		medialink = "http://www.youtube.com/get_video?" + video_id + t;

            	}
            	
            }
    	} catch (Exception e) {
    		logger.error("", e);    		
    	}

    	return medialink; 
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
    	int urlcount = Config.getInstance().getProperty("parser.youtube.urls").split(";").length;
    	for (int looper = 0 ; looper < urlcount ; looper++) {
	    	try {
	            // Hole RSS
	    		logger.debug("Parse URL: " +Config.getInstance().getProperty("parser.youtube.urls").split(";")[looper] );
	            URL feedUrl = new URL(Config.getInstance().getProperty("parser.youtube.urls").split(";")[looper]);
	            SyndFeedInput input = new SyndFeedInput();
	
	            // RSS in das SyndFeed Object Parsen
	            XmlReader xmlReader = new XmlReader(feedUrl);
	            SyndFeed feed = input.build(xmlReader);
	            feed.setEncoding(xmlReader.getEncoding());
	            String title = feed.getTitle();
	            feed.setTitle("Youtube - " + title);
	            feed.setDescription(title);
	            SyndImage image = new SyndImageImpl();
	            image.setUrl("http://www.youtube.com/img/pic_youtubelogo_123x63.gif");
	            feed.setImage(image);
	            
	            // Über den Feed loopen
	            List<SyndEntry> items = feed.getEntries();
	            Iterator<SyndEntry> i = items.iterator();
	            while (i.hasNext()) {
	                SyndEntry current = (SyndEntryImpl) i.next();
	                
	                // the atom parser transforms the <id> tag to URI
	                // we take this and add it to foreignMarkup and can use it
	                // in RomeToModelConverter to set the guid
	                Element elem = new Element("guid");
	                elem.setText(current.getUri());
	                ((List<Element>)current.getForeignMarkup()).add(elem);

	                // set ondemand flag in foreign markup
                    elem = new Element("ondemand");
                    elem.setText("true");
                    ((List<Element>)current.getForeignMarkup()).add(elem);
	                
	                List<SyndEnclosure> enclosures = new ArrayList<SyndEnclosure>();
	                SyndEnclosureImpl enc = new SyndEnclosureImpl();
	                enc.setType("video/flv");
	                String path = Config.getInstance().getHandlerMapping().getPath(OndemandStreamHandler.class);
	                StringBuilder url = new StringBuilder(Config.getInstance().getBaseUrl());
	                url.append(path);
	                url.append("?provider="); url.append(getClass().getName());
	                url.append("&url="); url.append(URLEncoder.encode(current.getLink(), "UTF-8"));
	                enc.setUrl(url.toString());
	                enclosures.add(enc);
	                current.setEnclosures(enclosures);
	            }
	            
	            saveFeed2DB(feed);
	            addRssFeed(feed.getLink());

	    	} catch (Exception e){
                logger.error("Couldn't parse Youtube webpage",e);
            }
    	}
    	
    	/* Youtube changes the enclosure link 
         * each time, so we get orphan enclosures,
         * which can be deleted */
        Connection conn = null;
        try {
            conn = ConnectionManager.getInstance().getConnection();
            new EnclosureDAO(conn).deleteOrphan();
        } catch (SQLException e) {
            logger.error("Couldn't delete orphan enclosures", e);
        } finally {
            try {
                DbUtils.close(conn);
            } catch (SQLException e) {
                logger.error("Couldn't close database connection", e);
            }
        }
        
        // add all feeds to Group
        Group youtube = new Group();
        youtube.setName(Messages.translate(getClass(), "group_name"));
        youtube.setDescription(Messages.translate(getClass(), "group_desc"));
        addFeedsToGroup(youtube);
    }
}
