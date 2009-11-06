package de.berlios.vch.parser.ard;

import java.io.IOException;
import java.net.URI;
import java.util.List;

import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
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

        overview.setTitle(HtmlParserUtils.getText(content, ARDMediathekParser.CHARSET, "h4[class=sendung]"));
        overview.setUri(new URI(pageUri));
//        overview.setDescription(Translate.decode(HtmlParserUtils.getText(content, ARDMediathekParser.CHARSET,
//                "div[class~=daten] span[class=sendetitel]")));

        // try to parse a feed image
//        ImageTag img = (ImageTag) HtmlParserUtils.getTag(content, ARDMediathekParser.CHARSET,
//                "div[class~=sendungAM] div.passepartout img");
//        if (img != null) {
//            SyndImage image = new SyndImageImpl();
//            image.setUrl(ARDMediathekParser.BASE_URL + img.getImageURL());
//            feed.setImage(image);
//        }

        pageCount = Math.min(determinePageCount(content), 5); // TODO config param or make parser paginated
        logger.debug("Program {} has {} pages", pageUri, pageCount);
        for (int i = 1; i <= pageCount; i++) {
            logger.debug("Parsing page {} / {}", i, pageCount);
            List<IVideoPage> videos = programPageParser.parse(pageUri, i);
            logger.trace("Found {} video items on program page {} - {}", new Object[] { videos.size(),
                    overview.getTitle(), i });
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
        NodeList pages = HtmlParserUtils.getTags(pageContent, ARDMediathekParser.CHARSET, "div[class~=navi_folgeseiten] li strong");
        return pages.size();
    }
}
