package de.berlios.vch.parser.rtlnow;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.dbutils.DbUtils;
import org.htmlparser.Node;
import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;

import de.berlios.vch.Config;
import de.berlios.vch.Constants;
import de.berlios.vch.db.ConnectionManager;
import de.berlios.vch.db.dao.ItemDAO;
import de.berlios.vch.model.Item;
import de.berlios.vch.utils.HttpUtils;
import de.berlios.vch.utils.RomeToModelConverter;

public class ProgramPageParser {
    private static transient Logger logger = LoggerFactory.getLogger(ProgramPageParser.class);
    
    private String url;
    private String charset;
    private String pageContent;
    private final String userAgent = Config.getInstance().getProperty("parser.user.agent");
    private final String accept = "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5";
    
    public ProgramPageParser(String url, String charset) throws IOException {
        this.url = url;
        this.charset = charset;
        
        downloadPageContent();
    }
    
    private void downloadPageContent() throws IOException {
        Map<String,String> headers = new HashMap<String, String>();
        headers.put("User-Agent", userAgent);
        pageContent = HttpUtils.get(url, headers, charset);
    }
    
    @SuppressWarnings("unchecked")
    public List<SyndEntry> getEntries() throws IOException, ParserException {
        logger.debug("Parsing program page " + url);
        
        // parse the programPage
        List<SyndEntry> entries = getEntries(pageContent);
        
        // check, if we have to parse all pages
        // only parse other pages if all entries on the first page are new
        boolean parseAllPages = true;
        for (Iterator iterator = entries.iterator(); iterator.hasNext();) {
            SyndEntry syndEntry = (SyndEntry) iterator.next();
            String guid = RomeToModelConverter.getForeignMarkupValue("guid", syndEntry);
            Item item = new Item();
            item.setGuid(guid);
            if(guid != null) {
                Connection conn = null;
                try {
                    conn = ConnectionManager.getInstance().getConnection();
                    if(new ItemDAO(conn).exists(item)) {
                        logger.debug("Entry from first page found in DB -> don't parse other pages");
                        parseAllPages = false;
                        break;
                    }
                } catch (SQLException e) {
                    logger.warn("Couldn't check existence of item",e);
                } finally {
                    try {
                        DbUtils.close(conn);
                    } catch (SQLException e) {
                        logger.error("Couldn't close database connection", e);
                    }
                }
            }
        }
        
        // evidence of a pagination -> do ajax call and parse the response
        if(parseAllPages && pageContent.contains("pagesel")) {
            List<String> ajaxParams = getAjaxParams();
            for (Iterator<String> iterator = ajaxParams.iterator(); iterator.hasNext();) {
                String params = iterator.next();
                Map<String,String> headers = new HashMap<String, String>();
                headers.put("User-Agent", userAgent);
                headers.put("Accept", accept);

                String response = HttpUtils.post(url, headers, params.getBytes("UTF-8"), "UTF-8");
                List<SyndEntry> ajaxEntries = getEntries(response);
                entries.addAll(ajaxEntries);
            }
        }
        
        return entries;
    }

    @SuppressWarnings("unchecked")
    private List<SyndEntry> getEntries(String pageContent) throws ParserException, MalformedURLException {
        Parser parser = Parser.createParser(pageContent, charset);
        NodeList divs = parser.extractAllNodesThatMatch(new NodeFilter() {
            @Override
            public boolean accept(Node node) {
                if(node instanceof LinkTag) {
                    LinkTag link = (LinkTag) node;
                    Div parent = (Div) link.getParent();
                    String href = link.getLink();
                    if(href != null && href.indexOf(".php?container_id") > 0 
                            && "buy".equals(parent.getAttribute("class"))
                            && href.indexOf("paytype") <= 0)
                    {
                        return true;
                    }
                }
                return false;
            }
            
        });
        
        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        NodeIterator iter = divs.elements();
        while(iter.hasMoreNodes()) {
            SyndEntry entry = new SyndEntryImpl();
            
            // set the page link
            LinkTag link = (LinkTag) iter.nextNode();
            URL context = new URL(RTLnowParser.BASE_URL);
            URL containerUrl = new URL(context, link.getLink());
            entry.setLink(containerUrl.toString());
            
            // set the guid
            Element elem = new Element("guid");
            elem.setText(link.getLink());
            ((List<Element>)entry.getForeignMarkup()).add(elem);
            
            
            // parse other attributes
            Div div = (Div) link.getParent().getParent();
            NodeList childs = div.getChildren();
            NodeIterator iterator = childs.elements();
            while(iterator.hasMoreNodes()) {
                Node node = iterator.nextNode();
                if(node instanceof Div) {
                    Div divNode = (Div) node;
                    String styleClass = divNode.getAttribute("class");
                    if(styleClass == null) {
                        continue;
                    } else if(styleClass.contains("time")) {
                        String timeString = divNode.childAt(0).getText();
                        try {
                            Date pubDate = new SimpleDateFormat(Constants.RTLNOW_DATE_FORMAT).parse(timeString);
                            entry.setPublishedDate(pubDate);
                        } catch (Exception e) {
                            logger.warn("Couldn't parse pubDate " + timeString + " on page " + url + " Using the current time", e);
                            entry.setPublishedDate(new Date());
                        }
                    } else if(styleClass.contains("title")) {
                        // set the title
                        LinkTag titleLink = (LinkTag) divNode.getChild(0);
                        String title = Translate.decode(titleLink.getLinkText());
                        entry.setTitle(title);
                    }
                }
            }
            entries.add(entry);
        }
        
        return entries;
    }
    
    private List<String> getAjaxParams() throws ParserException {
        Parser parser = Parser.createParser(pageContent, charset);
        NodeList links = parser.extractAllNodesThatMatch(new NodeFilter() {
            @Override
            public boolean accept(Node node) {
                if(node instanceof LinkTag) {
                    LinkTag link = (LinkTag) node;
                    if(link.getLink() != null && link.getLink().startsWith("xajax_show_top_and_movies")) {
                        return true;
                    }
                }
                return false;
            }
            
        });
        
        List<String> ajaxParams = new ArrayList<String>();
        NodeIterator iter = links.elements();
        while(iter.hasMoreNodes()) {
            LinkTag link = (LinkTag) iter.nextNode();
            String href = link.getLink();
            Pattern p = Pattern.compile("xajax_show_top_and_movies\\((\\d*),\'(\\d*.?\\d*)\',\'(reiter1)\',\'(\\d*)\',\'(\\d*)\',\'(\\d*)\'\\);void\\(0\\)");
            Matcher m = p.matcher(href);
            if(m.matches()) {
                StringBuilder sb = new StringBuilder();
                sb.append("xajax=show_top_and_movies&xajaxr=");
                sb.append(System.currentTimeMillis());
                for (int i = 1; i <= m.groupCount() ; i++) {
                    sb.append("&xajaxargs[]="); sb.append(m.group(i));
                }
                ajaxParams.add(sb.toString());
            }
        }
        return ajaxParams;
    }

    public String parseImage() throws ParserException, MalformedURLException {
        // TODO parse images for the newer pages like ahornallee
        logger.debug("Parsing channel image for " + url);
        Parser parser = Parser.createParser(pageContent, charset);
        NodeList divs = parser.extractAllNodesThatMatch(new NodeFilter() {
            @Override
            public boolean accept(Node node) {
                if(node instanceof Div) {
                    Div div = (Div) node;
                    if("centerimg".equalsIgnoreCase(div.getAttribute("class"))) {
                        return true;
                    }
                }
                return false;
            }
            
        });
        
        NodeIterator iter = divs.elements();
        while(iter.hasMoreNodes()) {
            Div div = (Div) iter.nextNode();
            ImageTag img = (ImageTag) div.getChild(0);
            URL context = new URL(RTLnowParser.BASE_URL);
            URL url = new URL(context, img.getImageURL());
            return url.toString();
        }
        
        return "";
    }
}