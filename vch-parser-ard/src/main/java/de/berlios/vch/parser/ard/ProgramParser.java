package de.berlios.vch.parser.ard;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.OverviewPage;


public class ProgramParser {
    private static transient Logger logger = LoggerFactory.getLogger(ProgramParser.class);
    
    private int pageCount = 1;

    private ProgramPageParser programPageParser;
    
    public ProgramParser() {
        programPageParser = new ProgramPageParser();
    }
    
    public OverviewPage parse(String pageUri) throws Exception {
        OverviewPage overview = new OverviewPage();
        overview.setParser(ARDMediathekParser.ID);
        
        String content = HttpUtils.get(pageUri, ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);

        // check if there is a podcast, which can be parsed
        //parsePodcast(overview, content);

        overview.setTitle(HtmlParserUtils.getText(Translate.decode(content), ARDMediathekParser.CHARSET, "div.mt-infobox h3"));
        overview.setUri(new URI(pageUri));

        NodeList links = HtmlParserUtils.getTags(content, ARDMediathekParser.CHARSET, "a[class~=mt-box_preload]");
        LinkTag link = (LinkTag) links.elementAt(links.size()-1);
        pageUri = ARDMediathekParser.BASE_URL + link.extractLink().replaceAll("view=switch", "view=list");
        
        String videoList = HttpUtils.get(pageUri, ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);
        
        pageCount = Math.min(Math.max(determinePageCount(videoList), 1), 5); // TODO config param or make parser paginated
        logger.debug("Program {} has {} pages", pageUri, pageCount);
        for (int i = 1; i <= pageCount; i++) {
            logger.debug("Parsing page {} / {}", i, pageCount);
            // http://www.ardmediathek.de/ard/servlet/ajax-cache/3516992/view=switch/documentId=3407000/index.html
            // http://www.ardmediathek.de/ard/servlet/ajax-cache/3516962/view=list/documentId=317338/goto=3/index.html
            List<IVideoPage> videos = programPageParser.parse(pageUri, i);
            logger.trace("Found {} video items on program page {} - {}", new Object[] { videos.size(), overview.getTitle(), i });
            overview.getPages().addAll(videos);
        }
        
        return overview;
    }

    // TODO do something with the podcast
//    private void parsePodcast(OverviewPage page, String content) throws ParserException, IOException {
//        NodeList tags = HtmlParserUtils.getTags(content, ARDMediathekParser.CHARSET, "div[class~=buttons] a");
//        NodeIterator iter = tags.elements();
//        while(iter.hasMoreNodes()) {
//            Node node = iter.nextNode();
//            if(node instanceof LinkTag) {
//                LinkTag link = (LinkTag) node;
//                if(link.getAttribute("title") != null && "Diesen Podcast jetzt abonnieren".equalsIgnoreCase(link.getAttribute("title"))) {
//                    String aboPage = HttpUtils.get(ARDMediathekParser.BASE_URL + link.getLink(), ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);
//                    Tag podcastField = HtmlParserUtils.getTag(aboPage, ARDMediathekParser.CHARSET, "div[class=podcatcher] input");
//                    if(podcastField != null) {
//                        InputTag input = (InputTag) podcastField;
//                        String podcastUri = input.getAttribute("value");
//                        logger.debug("Found podcast {}", podcastUri);
//                        try {
//                            SyndFeed feed = RssParser.parse(podcastUri);
//                            if(!feed.getTitle().toLowerCase().contains("podcast")) {
//                                feed.setTitle(feed.getTitle() + " - Podcast");
//                            }
//                        } catch (Exception e) {
//                            logger.error("Couldn't parse podcast " + podcastUri, e);
//                        }
//                    }
//                }
//            }
//        }
//    }

    private int determinePageCount(String pageContent) throws IOException, ParserException {
        NodeList pages = HtmlParserUtils.getTags(pageContent, ARDMediathekParser.CHARSET, "select.ajax-paging-select option");
        return pages.size();
    }
}
