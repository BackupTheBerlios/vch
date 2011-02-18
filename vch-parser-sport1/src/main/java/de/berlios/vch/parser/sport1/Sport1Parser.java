package de.berlios.vch.parser.sport1;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.Node;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.osgi.service.log.LogService;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

@Component
@Provides
public class Sport1Parser implements IWebParser {
    public static final String CHARSET = "UTF-8";
    public static final String ID = Sport1Parser.class.getName();
    public static final String BASE_URI = "http://www.sport1.de";
    public static final String START_PAGE = BASE_URI + "/de/video";
    public static final String SERVICE = "http://medianac.nacamar.de/index.php/partnerservices2/executeplaylist?partner_id=117&subp_id=11700&format=2&playlist_id=";
    public static final String VIDEO_PAGE_URI = BASE_URI + "/de/video/#/0,";
    
    @Requires
    private LogService logger;
    
    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));
        
        String content = HttpUtils.get(START_PAGE, null, CHARSET);
        NodeList menuItems = HtmlParserUtils.getTags(content, CHARSET, "div.menu_bottom div.subnavi > a");
        for (int i = 0; i < menuItems.size(); i++) {
            LinkTag item = (LinkTag) menuItems.elementAt(i);
            String title = Translate.decode(item.getLinkText());
            String uri = BASE_URI + item.getLink();
            IOverviewPage category = new OverviewPage();
            category.setParser(getId());
            category.setTitle(title);
            category.setUri(new URI(uri));
            
            Div parent = (Div) item.getParent();
            if(hasSubCategories(parent)) {
                logger.log(LogService.LOG_INFO, title + " has subcategories");
                category.setUri(new URI("dummy://" + i));
                NodeList subCategories = HtmlParserUtils.getTags(parent.toHtml(), CHARSET, "div.subsubnavi a");
                for (int j = 0; j < subCategories.size(); j++) {
                    LinkTag link = (LinkTag) subCategories.elementAt(j);
                    title = Translate.decode(link.getLinkText());
                    uri = BASE_URI + link.getLink();
                    IOverviewPage subCategory = new OverviewPage();
                    subCategory.setParser(getId());
                    subCategory.setTitle(title);
                    subCategory.setUri(new URI(uri));
                    category.getPages().add(subCategory);
                }
            } 
            
            page.getPages().add(category);
        }
        
        return page;
    }
    
    private boolean hasSubCategories(Div parent) {
        if(parent != null) {
            for (int i = 0; i < parent.getChildCount(); i++) {
                Node child = parent.getChild(i);
                if(child instanceof Div) {
                    Div childDiv = (Div) child;
                    if("subsubnavi".equalsIgnoreCase(childDiv.getAttribute("class"))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String getTitle() {
        return "Sport1";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            if(!"dummy".equals(opage.getUri().getScheme())) {
                parseOverviewPage(opage);
            }
        }
        return page;
    }

    private void parseOverviewPage(IOverviewPage opage) throws Exception {
        if("service".equals(opage.getUri().getScheme())) {
            parseServiceXml(opage, SERVICE + opage.getUri().getQuery());
        } else {
            String content = HttpUtils.get(opage.getUri().toString(), null, CHARSET);
            String playlistArray = findParameter(content, "playlistId");
            if(playlistArray != null) {
                List<String> playlists = parseStringArray(playlistArray);
                logger.log(LogService.LOG_INFO, "Parsed playlists " + playlists);
                if(playlists.size() > 1) {
                    List<String> playlistNames = parseStringArray(findParameter(content, "playlistName"));
                    for (int i = 0; i < playlists.size(); i++) {
                        IOverviewPage subpage = new OverviewPage();
                        subpage.setParser(getId());
                        subpage.setTitle(playlistNames.get(i));
                        subpage.setUri(new URI("service://playlist?" + playlists.get(i)));
                        opage.getPages().add(subpage);
                    }
                } else {
                    parseServiceXml(opage, SERVICE + playlists.get(0));
                }
            } else {
                throw new RuntimeException("Page does not contain playlists");
            }
        }
    }
    
    private void parseServiceXml(IOverviewPage opage, String uri) throws Exception {
        String content = HttpUtils.get(uri, null, CHARSET); // though the XML claims to be latin1, it's UTF-8
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(content)));
        org.w3c.dom.NodeList entries = doc.getElementsByTagName("entries");
        if(entries != null) {
            org.w3c.dom.Node entriesNode = (org.w3c.dom.Node) entries.item(0);
            org.w3c.dom.NodeList childs = entriesNode.getChildNodes();
            for (int i = 0; i < childs.getLength(); i++) {
                org.w3c.dom.Node child = childs.item(i);
                IVideoPage video = new VideoPage();
                video.setParser(getId());
                video.setTitle(getChildContent(child, "name"));
                video.setDescription(getChildContent(child, "description"));
                video.setDuration((long) Float.parseFloat((getChildContent(child, "duration"))));
                video.setThumbnail(new URI(getChildContent(child, "thumbnailUrl")));
                video.setVideoUri(new URI(getChildContent(child, "downloadUrl")));
                video.setUri(new URI(VIDEO_PAGE_URI + getChildContent(child, "id")));
                
                // parse pusblish date
                long pubDate = Long.parseLong(getChildContent(child, "createdAtAsInt")) * 1000;
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(pubDate);
                video.setPublishDate(cal);
                
                opage.getPages().add(video);
            }
        }
    }
    
    private String getChildContent(org.w3c.dom.Node parent, String nodeName) {
        org.w3c.dom.Node node = getChildNode(parent, nodeName);
        if(node != null) {
            return node.getTextContent();
        } else {
            return "";
        }
    }
    
    private org.w3c.dom.Node getChildNode(org.w3c.dom.Node parent, String nodeName) {
        if(parent != null) {
            org.w3c.dom.NodeList childs = parent.getChildNodes();
            for (int i = 0; i < childs.getLength(); i++) {
                org.w3c.dom.Node child = childs.item(i);
                if(nodeName.equals(child.getNodeName())) {
                    return child;
                }
            }
        }
        return null;
    }

    private String findParameter(String content, String param) {
        String searchString = param + ":";
        int start = content.indexOf(searchString);
        if(start > 0) {
            int stop = content.indexOf("\n", start);
            String line = content.substring(start + searchString.length(), stop).trim();
            line = line.substring(0, line.length()-1);
            logger.log(LogService.LOG_INFO, param + ": " + line);
            return line;
        }
        return null;
    }
    
    private List<String> parseStringArray(String stringArray) {
        List<String> strings = new ArrayList<String>();
        int stop = -1;
        int start = -1;
        while( (start = stringArray.indexOf('\'', stop+1)) >= 0 ) {
            stop = stringArray.indexOf('\'', start + 1);
            if(stop > 0) {
                String playlistId = stringArray.substring(start+1, stop);
                strings.add(playlistId);
            }
        }
        return strings;
        
    }

    @Override
    public String getId() {
        return ID;
    }
}