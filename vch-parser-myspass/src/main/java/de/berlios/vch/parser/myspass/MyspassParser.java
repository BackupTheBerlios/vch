package de.berlios.vch.parser.myspass;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.tags.Bullet;
import org.htmlparser.tags.BulletList;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
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
import de.berlios.vch.parser.WebPageTitleComparator;

@Component
@Provides
public class MyspassParser implements IWebParser {

    final static String CHARSET = "utf-8";

    final static String BASE_URI = "http://www.myspass.de";

    public static final String ID = MyspassParser.class.getName();

    private static final Map<String, String> HTTP_HEADER = new HashMap<String, String>();
    static {
        HTTP_HEADER.put("X-Requested-With", "XMLHttpRequest");
    }

    @Requires
    private LogService logger;

    @Override
    public IOverviewPage getRoot() throws Exception {
        IOverviewPage overview = new OverviewPage();
        overview.setUri(new URI("vchpage://localhost/" + getId()));
        overview.setTitle(getTitle());
        overview.setParser(ID);

        try {
            String content = HttpUtils.get(BASE_URI + "/myspass/ganze-folgen/", null, CHARSET);
            NodeList shows = HtmlParserUtils.getTags(content, CHARSET, "div#showsAZContainer a.showsAZUrl");
            logger.log(LogService.LOG_DEBUG, "Found shows: " + shows.size());
            for (NodeIterator iterator = shows.elements(); iterator.hasMoreNodes();) {
                LinkTag link = (LinkTag) iterator.nextNode();
                IOverviewPage programPage = new OverviewPage();
                programPage.setParser(getId());
                programPage.setTitle(Translate.decode(link.getLinkText()));
                programPage.setUri(new URI(BASE_URI + link.getLink().trim()));
                overview.getPages().add(programPage);
                logger.log(LogService.LOG_DEBUG, "Added " + link.getLinkText() + " at " + link.getLink());
            }
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't parse overview page", e);
        }

        Collections.sort(overview.getPages(), new WebPageTitleComparator());
        return overview;
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            String uri = opage.getUri().toString();
            if (uri.contains("tvshows") || uri.contains("webshows")) {
                return parseShowPage(opage);
            } else if (uri.contains("getEpisodeListFromSeason")) {
                return parseSeasonPage(opage);
            }

        } else if (page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            return parseVideoPage(vpage);
        }

        throw new Exception("Not yet implemented!");
    }

    private IVideoPage parseVideoPage(IVideoPage vpage) throws Exception {
        String uri = vpage.getUri().toString();
        Matcher m = Pattern.compile("--/(\\d+)/").matcher(uri);
        if (m.find()) {
            String id = m.group(1);
            uri = BASE_URI + "/myspass/includes/apps/video/getvideometadataxml.php?id=" + id;
            String content = HttpUtils.get(uri, null, CHARSET);
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));
            // String program = doc.getElementsByTagName("format").item(0).getTextContent();
            String episode = doc.getElementsByTagName("title").item(0).getTextContent();
            String description = doc.getElementsByTagName("description").item(0).getTextContent();
            String duration = doc.getElementsByTagName("duration").item(0).getTextContent();
            String[] parts = duration.split(":");
            int minutes = Integer.parseInt(parts[0]);
            int seconds = Integer.parseInt(parts[1]);
            int durationSeconds = minutes * 60 + seconds;
            String preview = doc.getElementsByTagName("imagePreview").item(0).getTextContent();
            String videoUri = doc.getElementsByTagName("url_flv").item(0).getTextContent();

            vpage.setTitle(episode);
            vpage.setDescription(description);
            vpage.setVideoUri(new URI(videoUri));
            vpage.setThumbnail(new URI(preview));
            vpage.setDuration(durationSeconds);
            return vpage;
        } else {
            throw new Exception("No ID found in URI");
        }
    }

    private IWebPage parseSeasonPage(IOverviewPage opage) throws Exception {
        String uri = opage.getUri().toString();
        String content = HttpUtils.get(uri, null, CHARSET);

        // if the uri does not contain a pageNumber, we have to check for pagination, otherwise this has been done before
        if (!uri.contains("pageNumber")) {
            // check if we have pagination
            BulletList ul = (BulletList) HtmlParserUtils.getTag(content, CHARSET, "div.parentUbox ul.pageination");
            if (ul != null) {
                Bullet middle = (Bullet) HtmlParserUtils.getTag(content, CHARSET, "li.pageinationMiddle");
                String pagination = Translate.decode(middle.toPlainTextString());
                String pattern = "(\\d+)";
                Matcher m = Pattern.compile(pattern).matcher(pagination);
                if (m.find()) {
                    int pageCount = Integer.parseInt(m.group(1));
                    for (int i = 0; i < pageCount; i++) {
                        uri = opage.getUri().toString() + "&pageNumber=" + i;
                        IOverviewPage page = new OverviewPage();
                        page.setParser(getId());
                        page.setTitle("Seite " + (i + 1));
                        page.setUri(new URI(uri));
                        opage.getPages().add(page);
                    }
                } else {
                    logger.log(LogService.LOG_WARNING, "Pattern " + pattern + " didn't match pagination HTML [" + pagination + "]");
                }
                return opage;
            }
        }

        // parse the episode page for episodes
        NodeList episodes = HtmlParserUtils.getTags(content, CHARSET, "tr.episodeListInformation td.title div a");
        for (NodeIterator iterator = episodes.elements(); iterator.hasMoreNodes();) {
            LinkTag a = (LinkTag) iterator.nextNode();
            String path = a.getLink();
            String episodeUri = BASE_URI + path;
            String title = Translate.decode(a.getLinkText());
            IVideoPage vpage = new VideoPage();
            vpage.setParser(getId());
            vpage.setTitle(title);
            vpage.setUri(new URI(episodeUri));
            opage.getPages().add(vpage);
        }
        return opage;
    }

    private IOverviewPage parseShowPage(IOverviewPage opage) throws Exception {
        String uri = opage.getUri().toString();
        String content = HttpUtils.get(uri, null, CHARSET);

        LinkTag a = (LinkTag) HtmlParserUtils.getTag(content, CHARSET, "th.season_episode a");
        String ajaxGeraffel = null;
        if (a != null) {
            ajaxGeraffel = a.getAttribute("onclick");
            uri = parseSeasonPageUri(ajaxGeraffel);
            content = HttpUtils.get(uri, HTTP_HEADER, CHARSET);
        } else {
            // simple site, no further parsing necessary
        }
        NodeList seasons = HtmlParserUtils.getTags(content, CHARSET, "ul.episodeListSeasonList li a");
        for (NodeIterator iterator = seasons.elements(); iterator.hasMoreNodes();) {
            LinkTag link = (LinkTag) iterator.nextNode();

            IOverviewPage seasonPage = new OverviewPage();
            seasonPage.setParser(getId());
            seasonPage.setTitle(Translate.decode(link.getLinkText()));
            ajaxGeraffel = link.getAttribute("onclick");
            seasonPage.setUri(new URI(parseSeasonPageUri(ajaxGeraffel)));
            opage.getPages().add(seasonPage);
            logger.log(LogService.LOG_DEBUG, "Added " + link.getLinkText() + " at " + seasonPage.getUri());
        }

        return opage;
    }

    private String parseSeasonPageUri(String onclick) throws UnsupportedEncodingException {
        onclick = onclick.substring(5, onclick.length() - 1);
        String[] parts = onclick.split(",");
        String file = parts[1].trim();
        file = file.substring(1, file.length() - 1);
        String query = parts[2].trim();
        if (!query.endsWith("'")) {
            query = (parts[2] + ',' + parts[3]).trim();
        }
        query = Translate.decode(query);
        query = query.substring(1, query.length() - 1).replace(' ', '+');
        return BASE_URI + "/myspass/includes/php/" + file + "?action=" + query;
    }

    @Override
    public String getTitle() {
        return "MySpass";
    }

    @Override
    public String getId() {
        return ID;
    }
}
