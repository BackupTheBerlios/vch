package de.berlios.vch.parser.arte;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.DbUtils;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

import de.berlios.vch.Config;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ChannelDAO;
import de.berlios.vch.http.handler.OndemandStreamHandler;
import de.berlios.vch.model.Channel;
import de.berlios.vch.rome.videocast.VideocastImpl;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;
import de.berlios.vch.utils.ModelToRomeConverter;

public class ParserThread implements Runnable {

    private static transient Logger logger = LoggerFactory.getLogger(ParserThread.class);
    
    private Queue<SyndEntry> queue;
    
    public ParserThread(Queue<SyndEntry> queue) {
        this.queue = queue;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        logger.debug("ParserThread started");
        while(true) {
            // if the thread has been interrupted, end thread
            if(Thread.currentThread().isInterrupted()) {
                return;
            }
            
            SyndEntry entry = null;
            try {
                entry = queue.poll();
                
                // if queue is empty, all feeds have been parsed -> end thread
                if(entry == null) {
                    logger.debug("No more links available. Stopping parser thread");
                    return;
                }
                
                // download the page content
                String content = HttpUtils.get(entry.getLink(), ArteParser.HTTP_HEADERS, ArteParser.CHARSET);
                logger.debug("Getting media link in media page:" + entry.getLink());
                logger.trace(content);
                
                // parse the feed title
                String feedName = entry.getTitle();
                
                // parse the entry title
                entry.setTitle(parseTitle(entry, content));
                
                // parse description
                SyndContent description = new SyndContentImpl();
                description.setValue(parseDescription(content));
                entry.setDescription(description);
                
                // add guid to freign markup, so that RomeToModelConverter uses that guid
                Element elem = new Element("guid");
                elem.setText(entry.getLink());
                ((List<Element>)entry.getForeignMarkup()).add(elem);
                
                // set ondemand flag in foreign markup
                elem = new Element("ondemand");
                elem.setText("true");
                ((List<Element>)entry.getForeignMarkup()).add(elem);

                // parse the enclosure link
                String enclosureUri = parseEnclosureUri(content);
                if (enclosureUri != null && enclosureUri.length() > 0) {
                    VideocastImpl myvidcast = new VideocastImpl();
                    myvidcast.setImage("");
                    entry.getModules().add(myvidcast);
                    SyndEnclosureImpl enc = new SyndEnclosureImpl();
                    enc.setType("video/wmv");
                    List<SyndEnclosure> enclist = entry.getEnclosures();
                    enc.setUrl(enclosureUri);
                    enclist.add(enc);
                    entry.setEnclosures(enclist);
                } 
                
                
                SyndFeed feed = ArteParser.channels.get(feedName);
                if(feed == null) {
                    // look up this feed in the DB. in this case we search by name and not by link
                    // because the link for each arte channel may change
                    Connection conn = ConnectionManager.getInstance().getConnection();
                    Channel channel = new ChannelDAO(conn).findByName(feedName);
                    DbUtils.close(conn);
                    if(channel != null) {
                        feed = ModelToRomeConverter.convert(channel);
                    }
                    if(feed == null) {
                        feed = new SyndFeedImpl();
                        feed.setTitle(feedName);
                        feed.setLink(entry.getLink());
                        feed.setFeedType("rss_2.0");
                        feed.setDescription("");
                        feed.setPublishedDate(new Date());
                    }
                    ArteParser.channels.put(feedName, feed);
                }
                
                feed.getEntries().add(entry);
            } catch (Exception e) {
                logger.error("Couldn't parse program page " + entry.getLink(), e);
            }
        }
    }
    
    private String parseTitle(SyndEntry entry, String content) throws ParserException, IOException {
        Pattern p = Pattern.compile("var\\s*playerUrl\\s*=\\s*\'(.*)\';");
        Matcher m = p.matcher(content);
        if(m.find()) {
            URL page = new URL(entry.getLink());
            URL detailsPage = new URL(page.getProtocol(), page.getHost(), page.getPort(), m.group(1));
            String details = HttpUtils.get(detailsPage.toString(), null, ArteParser.CHARSET);
            return HtmlParserUtils.getText(details, ArteParser.CHARSET, "span[id=abc]");
        } else {
            return "N/A";
        }
    }

    private String parseDescription(String content) throws ParserException {
        String desc = HtmlParserUtils.getText(content, ArteParser.CHARSET, "p.headline");
        if(desc.trim().isEmpty()) {
            desc = HtmlParserUtils.getText(content, ArteParser.CHARSET, "p.text");
        }
    	return Translate.decode(desc.trim());
    }
    
    private String parseEnclosureUri(String content) throws UnsupportedEncodingException {
        List<String[]> videos = new ArrayList<String[]>();
        Pattern pFormat = Pattern.compile("availableFormats\\[\\d*\\]\\[\"format\"\\] = \"(\\w*)\";");
        Pattern pQuality = Pattern.compile("availableFormats\\[\\d*\\]\\[\"quality\"\\] = \"(\\w*)\";"); 
        Pattern pUrl = Pattern.compile("availableFormats\\[\\d*\\]\\[\"url\"\\] = \"(.*)\";");
        Matcher mFormat = pFormat.matcher(content);
        Matcher mQuality = pQuality.matcher(content);
        Matcher mUrl = pUrl.matcher(content);
        while(mFormat.find()) {
            int index = mFormat.start();
            String format = mFormat.group(1);
            if(mQuality.find(index+1)) {
                index = mQuality.start();
                String quality = mQuality.group(1);
                if(mUrl.find(index+1)) {
                    String uri = mUrl.group(1);
                    String[] video = new String[] {format, quality, uri};
                    videos.add(video);
                }
            }
        }
        
        if(videos.size() == 0) {
            return null;
        }
        
        sort(videos);
        String[] video = videos.get(videos.size()-1);
        String path = Config.getInstance().getHandlerMapping().getPath(OndemandStreamHandler.class);
        StringBuilder url = new StringBuilder(Config.getInstance().getBaseUrl());
        url.append(path);
        url.append("?provider="); url.append(ArteParser.class.getName());
        url.append("&url="); url.append(URLEncoder.encode(video[2], "UTF-8"));
        return url.toString();
    }
    
    /**
     * Sorts videos represented by an String[] {format, quality, uri}
     * WMV > FLV
     * HQ > MQ
     * @param videos
     */
    private void sort(List<String[]> videos) {
        Collections.sort(videos, new Comparator<String[]>() {
            @Override
            public int compare(String[] v1, String[] v2) {
                if(v1[0].equals("WMV") && !v2[0].equals("WMV")) {
                    return 1;
                } else if(!v1[0].equals("WMV") && v2[0].equals("WMV")) {
                    return -1;
                } else if(v1[1].equals("HQ") && !v2[0].equals("HQ")) {
                    return 1;
                } else if(!v1[1].equals("HQ") && v2[0].equals("HQ")) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
    }
}
