package de.berlios.vch.parser.n24;

import java.net.MalformedURLException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.htmlparser.Tag;
import org.htmlparser.nodes.TextNode;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.Translate;
import org.json.JSONObject;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;

@Component
@Provides
public class N24Parser implements IWebParser {

    public static final String ID = N24Parser.class.getName();

    public static final String CHARSET = "UTF-8";
    public static final String BASE_URI = "http://www.n24.de";
    public static final String START_PAGE = BASE_URI + "/mediathek/";
    public static final String STREAM_BASE = "rtmp://pssimn24fs.fplive.net:1935/pssimn24";

    @Requires
    private LogService logger;

    private List<String> supportedProtocols = new ArrayList<String>();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public IOverviewPage getRoot() throws Exception {
        final OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        String content = HttpUtils.get(START_PAGE, null, CHARSET);
        NodeList naviLinks = HtmlParserUtils.getTags(content, CHARSET, "div#mediacenter_nav ul li a");
        for (int i = 0; i < naviLinks.size(); i++) {
            LinkTag link = (LinkTag) naviLinks.elementAt(i);
            OverviewPage category = new OverviewPage();
            category.setParser(ID);
            category.setTitle(link.getLinkText());
            String uri = link.extractLink();
            if (uri.startsWith("/")) {
                category.setUri(new URI(BASE_URI + uri));
            } else {
                category.setUri(new URI(uri));
            }
            logger.log(LogService.LOG_DEBUG, "Found category " + category.getUri().toString());
            page.getPages().add(category);
        }

        NodeList sections = HtmlParserUtils.getTags(content, CHARSET, "div#main div[class~=box][class~=std_box]");
        for (int i = 0; i < sections.size(); i++) {
            String sectionHtml = sections.elementAt(i).toHtml();
            LinkTag link = (LinkTag) HtmlParserUtils.getTag(sectionHtml, CHARSET, "div.intro h2 a");
            OverviewPage category = new OverviewPage();
            category.setParser(ID);
            category.setTitle(Translate.decode(link.getLinkText()));

            Tag hidden = HtmlParserUtils.getTag(sectionHtml, CHARSET, "input[type=hidden]");
            String json = Translate.decode(hidden.getAttribute("value"));
            JSONObject jo = new JSONObject(json);
            String path = jo.getString("source_url");
            String ressort = jo.getString("ressort");
            category.setUri(new URI(BASE_URI + path + "?dataset_name=" + ressort + "&page=1&limit=40"));

            page.getPages().add(category);
        }

        return page;
    }

    @Override
    public String getTitle() {
        return "N24 Mediathek";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            String content = HttpUtils.get(page.getUri().toString(), null, CHARSET);
            NodeList list = HtmlParserUtils.getTags(content, CHARSET, "ul.video_teaser_list li");
            for (int i = 0; i < list.size(); i++) {
                String itemHtml = list.elementAt(i).toHtml();
                LinkTag a = (LinkTag) HtmlParserUtils.getTag(itemHtml, CHARSET, "a");
                ImageTag img = (ImageTag) HtmlParserUtils.getTag(itemHtml, CHARSET, "a img");

                TextNode n = (TextNode) HtmlParserUtils.getTag(itemHtml, CHARSET, "a strong").getNextSibling();
                String title = n.getText();

                String subtitle = HtmlParserUtils.getText(itemHtml, CHARSET, "h3");
                if (subtitle != null && !subtitle.trim().isEmpty()) {
                    title = title.concat(" - ").concat(subtitle);
                }

                IVideoPage video = new VideoPage();
                video.setParser(ID);
                video.setTitle(title.toString());
                video.setThumbnail(new URI(START_PAGE + img.extractImageLocn()));
                video.setUri(new URI(BASE_URI + a.extractLink()));
                opage.getPages().add(video);
            }
        } else if (page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;
            String content = HttpUtils.get(page.getUri().toString(), null, CHARSET);

            // parse the description
            String description = HtmlParserUtils.getText(content, CHARSET, "div.player_infos div.col1 p");
            vpage.setDescription(description);

            // parse the pub date
            String dateString = HtmlParserUtils.getText(content, CHARSET, "div.player_infos span.date");
            String format = "dd.MM.yyyy HH:mm:ss";
            try {
                Date date = new SimpleDateFormat(format).parse(dateString);
                Calendar pubDate = Calendar.getInstance();
                pubDate.setTime(date);
                vpage.setPublishDate(pubDate);
            } catch (ParseException e) {
                logger.log(LogService.LOG_WARNING, "Couldn't parse date string " + dateString + " with format " + format);
            }

            // parse the video uri
            Tag input = HtmlParserUtils.getTag(content, CHARSET, "div.player input[type=hidden][class~=jsb_flash_player]");
            String playerjson = Translate.decode(input.getAttribute("value"));
            JSONObject playerOptions = new JSONObject(playerjson);
            String filename = playerOptions.getString("filename");
            URI videoUri = new URI(STREAM_BASE + "/" + filename);
            vpage.setVideoUri(videoUri);
            vpage.getUserData().put("streamName", filename);

            // check if the video format is supported
            if (!supportedProtocols.contains(vpage.getVideoUri().getScheme())) {
                throw new NoSupportedVideoFoundException(vpage.getVideoUri().toString(), supportedProtocols);
            }
        }
        return page;
    }

    // ############ ipojo stuff #########################################

    // validate and invalidate method seem to be necessary for the bind methods to work
    @Validate
    public void start() throws MalformedURLException {
    }

    @Invalidate
    public void stop() {
    }

    @Bind(id = "supportedProtocols", aggregate = true)
    public synchronized void addProtocol(INetworkProtocol protocol) {
        supportedProtocols.addAll(protocol.getSchemes());
    }

    @Unbind(id = "supportedProtocols", aggregate = true)
    public synchronized void removeProtocol(INetworkProtocol protocol) {
        supportedProtocols.removeAll(protocol.getSchemes());
    }
}
