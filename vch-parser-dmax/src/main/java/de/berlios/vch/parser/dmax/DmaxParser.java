package de.berlios.vch.parser.dmax;

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.htmlparser.tags.Div;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPageTitleComparator;
import de.berlios.vch.parser.dmax.pages.EpisodePage;
import de.berlios.vch.parser.dmax.pages.ProgramListing;
import de.berlios.vch.parser.dmax.pages.RootPage;

public class DmaxParser implements IWebParser, BundleActivator {

    private static transient Logger logger = LoggerFactory.getLogger(DmaxParser.class);

    final static String CHARSET = "utf-8";

    private final int MAX_ITEMS = 100;

    final static String BASE_URI = "http://www.dmax.de";
    
    public static final String ID = DmaxParser.class.getName();

    private final String LANDING_PAGE = BASE_URI + "/video/morevideo.shtml?name=longform&sort=date&contentSize="
            + MAX_ITEMS + "&pageType=longFormHub&displayBlockName=popularLong";
    
    private ProgramParser programParser = new ProgramParser();
    private VideoPageParser videoParser = new VideoPageParser();
    
    @Override
    public IOverviewPage getRoot() throws Exception {
        IOverviewPage overview = new RootPage();
        overview.setUri(new URI("#"));
        overview.setTitle(getTitle());
        
        final Set<IWebPage> categories = new HashSet<IWebPage>();
        List<Thread> threads = new LinkedList<Thread>();
        for (int i = 1; i <= MAX_ITEMS / 20; i++) {
            final String URI = LANDING_PAGE + "&page=" + i;
            Thread t = new Thread() {
                public void run() {
                    try {
                        String content = HttpUtils.get(URI, null, CHARSET);
                        NodeList itemCells = HtmlParserUtils.getTags(content, CHARSET,
                                "div#vp-perpage-promolist div[class~=vp-promo-item]");
                        for (NodeIterator iterator = itemCells.elements(); iterator.hasMoreNodes();) {
                            final Div itemCell = (Div) iterator.nextNode();
                            String cellHtml = itemCell.toHtml();

                            // parse the page title
                            String title = Translate.decode(HtmlParserUtils.getText(cellHtml,
                                    DmaxParser.CHARSET, "a.vp-promo-title").trim());

                            OverviewPage page = new ProgramListing();
                            page.setUri(new URI(URI));
                            page.setTitle(title);
                            page.getUserData().put("itemCell", cellHtml);
                            page.setParser(ID);
                            categories.add(page);
                        }
                    } catch (Exception e) {
                        logger.error("Couldn't parse overview page", e);
                    }
                }
            };
            threads.add(t);
            t.start();
        }
        
        // wait for all threads to finish
        for (Thread thread : threads) {
            thread.join();
        }
        overview.getPages().addAll(categories);
        Collections.sort(overview.getPages(), new WebPageTitleComparator());
        return overview;
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        String type = (String) page.getUserData().get("dmax.type");
        if(ProgramListing.class.getSimpleName().equals(type)) {
            return programParser.parse(page);
        } else if(EpisodePage.class.getSimpleName().equals(type)) {
            int episodeChunkCount = Integer.parseInt((String)page.getUserData().get("episodeChunkCount"));
            logger.info("Selected episode has {} videos", episodeChunkCount);
            String videoPageUri = page.getUri().toString();
            String videoPageUriTemplate = videoPageUri.substring(0, videoPageUri.lastIndexOf('-')+1);
            IOverviewPage opage = (IOverviewPage) page;
            for (int i = 1; i <= episodeChunkCount ; i++) {
                VideoPage videoPage = new VideoPage();
                videoPage.setParser(ID);
                String title = page.getTitle();
                title = title.substring(0, title.lastIndexOf(" "));
                title += " " + i;
                videoPage.setTitle(title);
                videoPage.setUri(new URI(videoPageUriTemplate + i + '/'));
                opage.getPages().add(videoPage);
            }
            return page;
        } else if(page instanceof IVideoPage) {
            return videoParser.parse((VideoPage) page);
        }

        throw new Exception("Not yet implemented!");
    }

    @Override
    public String getTitle() {
        return "DMAX Videotheke";
    }

    @Override
    public void start(BundleContext ctx) throws Exception {
        // register parser service
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