package de.berlios.vch.parser.ard;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.htmlparser.util.Translate;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

@Component
@Provides
public class ARDMediathekParser implements IWebParser {

    public static final String BASE_URL = "http://www.ardmediathek.de";
    
    private static final String LANDING_PAGE = BASE_URL + "/ard/servlet/ajax-cache/3551682/view=module/index.html";
    
    public static final String CHARSET = "UTF-8";
    
    public static final String ID = ARDMediathekParser.class.getName(); 
    
    private ProgramParser programParser = new ProgramParser();
    
    private BundleContext ctx;
    
    public static Map<String,String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.1.2) Gecko/20090821 Gentoo Firefox/3.5.7");
        HTTP_HEADERS.put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
    }
    
    public ARDMediathekParser(BundleContext ctx) {
    	this.ctx = ctx;
	}
    
    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));
        
        String landingPage = HttpUtils.get(LANDING_PAGE, HTTP_HEADERS, CHARSET);
        int start = landingPage.indexOf("var sendungVerpasstListe = [");
        if(start >= 0) {
            int stop = landingPage.indexOf("];", start);
            if(stop >= 0) {
                String jsonArray = landingPage.substring(start+27, stop+2);
                JSONArray array = new JSONArray(jsonArray);
                for (int i = 0; i < array.length(); i++) {
                    JSONObject obj = (JSONObject) array.get(i);
                    OverviewPage overview = new OverviewPage();
                    overview.setParser(ID);
                    overview.setTitle(Translate.decode(obj.getString("titel")));
                    overview.setUri(new URI("http://www.ardmediathek.de" + obj.getString("link")));
                    page.getPages().add(overview);
                }
            } else {
                throw new RuntimeException("No programs found. Maybe the page layout has changed");
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
        } else if(page instanceof IVideoPage) {
            page = VideoItemPageParser.parse((VideoPage) page, ctx);
        }
        return page;
    }
    
    @Override
    public String getId() {
        return ID;
    }
}