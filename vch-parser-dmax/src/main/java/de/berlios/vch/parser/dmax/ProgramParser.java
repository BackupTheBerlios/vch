package de.berlios.vch.parser.dmax;

import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.Div;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.dmax.pages.EpisodePage;
import de.berlios.vch.parser.dmax.pages.ProgramPage;

public class ProgramParser {
    private static transient Logger logger = LoggerFactory.getLogger(ProgramParser.class);
    
    public IWebPage parse(IWebPage page) throws Exception {
        // parse the program ID
        String cellHtml = (String) page.getUserData().get("itemCell");
        LinkTag a = (LinkTag) HtmlParserUtils.getTag(cellHtml, DmaxParser.CHARSET, "div.vp-promo-image a");
        String videoPageUri = DmaxParser.BASE_URI + a.getLink();
        String videoPageContent = HttpUtils.get(videoPageUri, null, DmaxParser.CHARSET);
        logger.debug("Parsing page {}", videoPageUri);
        NodeList breadCrumbLinks = HtmlParserUtils.getTags(videoPageContent, DmaxParser.CHARSET, "div#vp-breadcrumb span[class~=showHub] a");
        a = (LinkTag) breadCrumbLinks.elementAt(breadCrumbLinks.size()-1);
        String programId = a.extractLink();
        programId = programId.substring(0, programId.length() -1);
        programId = programId.substring(programId.lastIndexOf('/') + 1, programId.length());

        String programOverview = DmaxParser.BASE_URI + "/video/morevideo.shtml?sort=date&contentSize=100&pageType=showHub&displayBlockName=recentLong&name="+programId;
        IOverviewPage episodes = new ProgramPage();
        episodes.setTitle(page.getTitle());
        episodes.setParser(DmaxParser.ID);
        episodes.setUri(new URI(videoPageUri));
        
        String content = HttpUtils.get(programOverview, null, DmaxParser.CHARSET);
        NodeList itemCells = HtmlParserUtils.getTags(content, DmaxParser.CHARSET,
                "div#vp-perpage-promolist div[class~=vp-promo-item]");
        for (NodeIterator iterator = itemCells.elements(); iterator.hasMoreNodes();) {
            final Div itemCell = (Div) iterator.nextNode();
            String episodeCellHtml = itemCell.toHtml();

            IOverviewPage episode = new EpisodePage();
            episode.setParser(DmaxParser.ID);
            
            // parse the page title
            String title = HtmlParserUtils.getText(episodeCellHtml, DmaxParser.CHARSET, "span.vp-promo-subtitle-title");
            title = title.trim();
            title = title.replaceAll("( [Tt]eil \\d+$| \\d+$)", "");
            episode.setTitle(title);
            
            // parse the video page uri
            LinkTag videoPageLink = (LinkTag) HtmlParserUtils.getTag(episodeCellHtml, DmaxParser.CHARSET,
                    "a.vp-promo-title");
            videoPageUri = DmaxParser.BASE_URI + videoPageLink.extractLink();
            episode.setUri(new URI(videoPageUri));
            
            // parse the episode chunk count
            String episodeCountText = HtmlParserUtils.getText(episodeCellHtml,
                    DmaxParser.CHARSET, "a.vp-promo-title + span");
            Pattern p = Pattern.compile("\\(\\d+ von (\\d+)\\)");
            Matcher m = p.matcher(episodeCountText);
            if(m.matches()) {
                episode.getUserData().put("episodeChunkCount", m.group(1));
            } else {
                logger.warn("Episode chunk count unknown. Skipping episode {}", title);
                continue;
            }
            
            logger.info("Episode: {}, {}", title, videoPageUri);
            episodes.getPages().add(episode);
        }
        
        return episodes;
    }
}
