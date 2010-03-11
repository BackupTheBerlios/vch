package de.berlios.vch.parser.zdf;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.Div;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.AsxParser;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

public class ZDFMediathekParser implements IWebParser, BundleActivator {
    public static final String ID = ZDFMediathekParser.class.getName();
    private static final String BASE_URI = "http://www.zdf.de";
    private static final String OVERVIEW_RUBRIKEN = BASE_URI + "/ZDFmediathek/hauptnavigation/rubriken?flash=off";
    private static final String OVERVIEW_ABZ = "dummy://localhost/"+ID+"/a-z";
    
    public static final String CHARSET = "UTF-8";
    
    private Map<String, String> aBisZ = new HashMap<String, String>();
    
    public ZDFMediathekParser() {
        aBisZ.put("0-9", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz8?flash=off&teaserListIndex=1000");
        aBisZ.put("ABC", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz0?flash=off&teaserListIndex=1000");
        aBisZ.put("DEF", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz1?flash=off&teaserListIndex=1000");
        aBisZ.put("GHI", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz2?flash=off&teaserListIndex=1000");
        aBisZ.put("JKL", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz3?flash=off&teaserListIndex=1000");
        aBisZ.put("MNO", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz4?flash=off&teaserListIndex=1000");
        aBisZ.put("PQRS", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz5?flash=off&teaserListIndex=1000");
        aBisZ.put("TUV", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz6?flash=off&teaserListIndex=1000");
        aBisZ.put("WXYZ", "http://www.zdf.de/ZDFmediathek/hauptnavigation/sendung-a-bis-z/saz7?flash=off&teaserListIndex=1000");
    }
    
    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));
        
        OverviewPage rubriken = new OverviewPage();
        rubriken.setParser(ID);
        rubriken.setTitle("Rubriken");
        rubriken.setUri(new URI(OVERVIEW_RUBRIKEN));
        page.getPages().add(rubriken);
        
        OverviewPage abz = new OverviewPage();
        abz.setTitle("Sendungen A-Z");
        abz.setParser(ID);
        abz.setUri(new URI(OVERVIEW_ABZ));
        page.getPages().add(abz);
        
        return page;
    }

    @Override
    public String getTitle() {
        return "ZDFmediathek";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof IVideoPage) {
            IVideoPage video = (IVideoPage) page;
            parseVideoPage(video);
            // parse asx file, if necessary
            if(video.getVideoUri().toString().toLowerCase().endsWith(".asx")) {
                String newUri = AsxParser.getUri(video.getVideoUri().toString());
                video.setVideoUri(new URI(newUri));
            }
            return page;
        } else {
            if(page.getUri().toString().equals(OVERVIEW_ABZ)) {
                for (Entry<String,String> abzPage : aBisZ.entrySet()) {
                    String title = abzPage.getKey();
                    String uri = abzPage.getValue();
                    OverviewPage tmp = new OverviewPage();
                    tmp.setParser(ID);
                    tmp.setTitle(title);
                    tmp.setUri(new URI(uri));
                    ((IOverviewPage)page).getPages().add(tmp);
                }
            } else {
                parseOverviewPage((IOverviewPage) page);
            }
            return page;
        }
    }
    
    private void parseVideoPage(IVideoPage video) throws IOException, ParserException, URISyntaxException, ParseException {
        String content = HttpUtils.get(video.getUri().toString(), null, CHARSET);
        
        // parse the description
        video.setDescription(Translate.decode(HtmlParserUtils.getText(content, CHARSET, "div.beitrag p.kurztext")));
        
        // parse the thumbnail image
        ImageTag img = (ImageTag) HtmlParserUtils.getTag(content, CHARSET, "div.beitrag img");
        video.setThumbnail(new URI (BASE_URI + img.getImageURL()));
        
        // parse the video uri
        NodeList videoLinks = HtmlParserUtils.getTags(content, CHARSET, "ul.dslChoice li a");
        LinkTag dsl2000 = (LinkTag) videoLinks.elementAt(1);
        video.setVideoUri(new URI(dsl2000.extractLink()));
        
        // parse the pubDate
        String datum = HtmlParserUtils.getText(content, CHARSET, "p.datum");
        datum = datum.substring(datum.lastIndexOf(',')+1).trim();
        Date pubDate = new SimpleDateFormat("dd.MM.yyyy").parse(datum);
        Calendar cal = Calendar.getInstance();
        cal.setTime(pubDate);
        video.setPublishDate(cal);
        
        // parse the duration
        Matcher m = Pattern.compile("VIDEO,\\s+(\\d+):(\\d+)").matcher(content);
        if(m.find()) {
            int minutes = Integer.parseInt(m.group(1));
            int seconds = Integer.parseInt(m.group(2));
            video.setDuration(60 * minutes + seconds);
        }
    }

    public void parseOverviewPage(IOverviewPage page) throws Exception {
        // clear the parsed pages, so that we don't have duplicate results, if a user
        // opens this page multiple times
        page.getPages().clear();
        
        // set the number of displayed items on the page to 1000
        String uri = HttpUtils.addParameter(page.getUri().toString(), "teaserListIndex", "1000");
        
        String content = HttpUtils.get(uri, null, CHARSET);
        NodeList titles = HtmlParserUtils.getTags(content, CHARSET, "div.text");
        NodeIterator iter = titles.elements();
        while(iter.hasMoreNodes()) {
            Div div = (Div) iter.nextNode();
            NodeList links = (NodeList) HtmlParserUtils.getTags(div.toHtml(), CHARSET, "a");
            LinkTag a = (LinkTag) links.elementAt(1);
            
            // parse page uri
            String pageUri = BASE_URI + Translate.decode(a.extractLink());
            
            // parse page title
            String title = Translate.decode(a.getLinkText()).trim();
            
            // detect page type
            LinkTag typeLink = (LinkTag) links.elementAt(2);
            boolean isVideo = Translate.decode(typeLink.getLinkText()).trim().toLowerCase().contains("video");
            
            // create an OverviewPage or a VideoPage
            IWebPage subPage = null;
            URI subPageUri = new URI(pageUri.replaceAll(" ", "+"));
            if(isOverviewPage(subPageUri)) {
                subPage = new OverviewPage();
            } else {
                VideoPage tmp = new VideoPage();
                // parse the pubDate
                String datum = ((LinkTag)links.elementAt(0)).getLinkText();
                datum = datum.substring(datum.lastIndexOf(',')+1).trim();
                Date pubDate = new SimpleDateFormat("dd.MM.yyyy").parse(datum);
                Calendar cal = Calendar.getInstance();
                cal.setTime(pubDate);
                tmp.setPublishDate(cal);
                subPage = tmp;
            }
            subPage.setParser(ID);
            subPage.setTitle(title);
            subPage.setUri(subPageUri);
            
            // if the subpage is an item page and if it is video, add it to the overview page
            // otherwise it is an overview page and we add it, too
            if (!isOverviewPage(subPageUri)) {
                if(isVideo) {
                    page.getPages().add(subPage);
                }
            } else {
                page.getPages().add(subPage);
            }
        }
    }
    
    private boolean isOverviewPage(URI uri) {
        return uri != null && uri.getPath() != null && uri.getPath().matches(".*/\\d+");
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
}