package de.berlios.vch.parser.sf;

import java.net.URI;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.Tag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.jdom.Element;
import org.osgi.service.log.LogService;

import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndFeed;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.rss.RssParser;

@Component
@Provides
public class SfVideoportalParser implements IWebParser {
    public static final String CHARSET = "UTF-8";

    public static final String ID = SfVideoportalParser.class.getName();

    private final String BASE_URI = "http://www.srf.ch";
    private final String START_PAGE = BASE_URI + "/podcasts";

    @Requires
    private LogService logger;

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        String content = HttpUtils.get(START_PAGE, null, CHARSET);

        NodeList sections = HtmlParserUtils.getTags(content, CHARSET, "div[class~=sendungen] div[class~=l-container]");
        for (int i = 0; i < sections.size(); i++) {
            String html = sections.elementAt(i).toHtml();
            String title = HtmlParserUtils.getText(html, CHARSET, "h2.border");

            OverviewPage sectionPage = new OverviewPage();
            sectionPage.setParser(ID);
            sectionPage.setTitle(title);
            sectionPage.setUri(new URI("sf://section/" + title));

            NodeList programs = HtmlParserUtils.getTags(html, CHARSET, "div[class~=l-container] ul[class~=filter] > li");
            for (int j = 0; j < programs.size(); j++) {
                html = programs.elementAt(j).toHtml();

                LinkTag programPageLink = (LinkTag) HtmlParserUtils.getTag(html, CHARSET, "div.module-content h3.tv a");
                if (programPageLink == null) {
                    // this probably is a radio podcast
                    continue;
                }

                String name = Translate.decode(programPageLink.getLinkText()).trim();
                Tag podcast = HtmlParserUtils.getTag(html, CHARSET, "div.podcast input");
                if (podcast == null) {
                    continue;
                }
                String uri = podcast.getAttribute("value");

                OverviewPage programPage = new OverviewPage();
                programPage.setParser(ID);
                programPage.setTitle(name);
                programPage.setUri(new URI(uri));
                sectionPage.getPages().add(programPage);

                logger.log(LogService.LOG_DEBUG, "Program URI is " + programPage.getUri());
            }

            if (!sectionPage.getPages().isEmpty()) {
                page.getPages().add(sectionPage);
            }
        }
        return page;
    }

    @Override
    public String getTitle() {
        return "SF Videoportal";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IOverviewPage) {
            if ("sf".equals(page.getUri().getScheme())) {
                return page;
            } else {
                logger.log(LogService.LOG_DEBUG, "Parsing program page at " + page.getUri());
                IOverviewPage programPage = (IOverviewPage) page;

                String rssContent = HttpUtils.get(page.getUri().toString(), null, CHARSET);
                SyndFeed feed = RssParser.parse(rssContent);
                for (Iterator<?> iterator = feed.getEntries().iterator(); iterator.hasNext();) {
                    SyndEntry entry = (SyndEntry) iterator.next();
                    VideoPage video = new VideoPage();
                    video.setParser(getId());
                    video.setTitle(entry.getTitle());
                    video.setDescription(entry.getDescription().getValue());
                    Calendar pubCal = Calendar.getInstance();
                    pubCal.setTime(entry.getPublishedDate());
                    video.setPublishDate(pubCal);
                    video.setVideoUri(new URI(((SyndEnclosure) entry.getEnclosures().get(0)).getUrl()));
                    if (entry.getLink() != null) {
                        video.setUri(new URI(entry.getLink()));
                    } else {
                        video.setUri(video.getVideoUri());
                    }

                    // look, if we have a duration in the foreign markup
                    @SuppressWarnings("unchecked")
                    List<Element> fm = (List<Element>) entry.getForeignMarkup();
                    for (Element element : fm) {
                        if ("duration".equals(element.getName())) {
                            try {
                                video.setDuration(Long.parseLong(element.getText()));
                            } catch (Exception e) {
                            }
                        }
                    }

                    programPage.getPages().add(video);
                }
            }
        } else if (page instanceof IVideoPage) {
            logger.log(LogService.LOG_DEBUG, "Parsing video page at " + page.getUri());
            // parse video page
            // parseVideoPage((IVideoPage) page);
            return page;
        }

        return page;
    }

    @Override
    public String getId() {
        return ID;
    }
}