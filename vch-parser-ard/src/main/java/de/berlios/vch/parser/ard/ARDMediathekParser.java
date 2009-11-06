package de.berlios.vch.parser.ard;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.htmlparser.Node;
import org.htmlparser.Tag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.util.NodeIterator;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

public class ARDMediathekParser implements IWebParser, BundleActivator {

    public static final String BASE_URL = "http://www.ardmediathek.de";
    
    private static final String LANDING_PAGE = BASE_URL + "/ard/servlet/";
    
    public static final String CHARSET = "UTF-8";
    
    public static final String ID = ARDMediathekParser.class.getName(); 
    
    private ProgramParser programParser = new ProgramParser();
    
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
        
        String landingPage = HttpUtils.get(LANDING_PAGE, HTTP_HEADERS, CHARSET);
        Tag selectProg = HtmlParserUtils.getTag(landingPage, CHARSET, "form[id=tv_form] select");
        if(selectProg != null) {
            NodeIterator iter = selectProg.getChildren().elements();
            while(iter.hasMoreNodes()) {
                Node child = iter.nextNode();
                if(child instanceof OptionTag) {
                    OptionTag option = (OptionTag) child;
                    String pageId = option.getValue();
                    if(pageId != null && !pageId.trim().isEmpty()) {
                        OverviewPage overview = new OverviewPage();
                        overview.setParser(ID);
                        overview.setTitle(option.getOptionText());
                        overview.setUri(new URI("http://www.ardmediathek.de/ard/servlet/content/1214?moduleId=" + pageId));
                        page.getPages().add(overview);
                    }
                }
            }
        } else {
            throw new RuntimeException("No programs found. Maybe the page layout has changed");
        }
        
        return page;
    }
    
    

    @Override
    public String getTitle() {
        return "ARD Mediathek";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof IOverviewPage) {
            page = programParser.parse(page.getUri().toString());
        } else if(page instanceof VideoPage) {
            page = VideoItemPageParser.parse((VideoPage) page);
        }
        return page;
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