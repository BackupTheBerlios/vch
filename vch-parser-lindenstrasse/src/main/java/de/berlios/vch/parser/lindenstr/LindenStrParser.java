package de.berlios.vch.parser.lindenstr;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.htmlparser.Tag;
import org.htmlparser.tags.Div;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
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
public class LindenStrParser implements IWebParser, ResourceBundleProvider {

    public static final String ID = LindenStrParser.class.getName();

    public static final String STREAM_BASE = "rtmp://gffstream.fcod.llnwd.net:1935/a792/e2";

    public static final String BASE_URI = "http://www.lindenstrasse.de";
    private static final String START_PAGE = BASE_URI + "/Multimedia/Videos/folgen.jsp";

    public static final String CHARSET = "iso-8859-1";

    @Requires
    private LogService logger;

    private List<String> supportedProtocols = new ArrayList<String>();

    private ResourceBundle resourceBundle;

    private BundleContext ctx;

    public LindenStrParser(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public IOverviewPage getRoot() throws Exception {
        IOverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        String content = HttpUtils.get(START_PAGE, null, CHARSET);
        NodeList divs = HtmlParserUtils.getTags(content, CHARSET, "div[class=LinkeSpalte-Bildbox-Liste-164-Pixel]");
        logger.log(LogService.LOG_DEBUG, "Found " + divs.size() + " episodes");

        NodeIterator iter = divs.elements();
        while (iter.hasMoreNodes()) {
            IOverviewPage opage = new OverviewPage();
            opage.setParser(ID);

            Div div = (Div) iter.nextNode();
            String divContent = div.toHtml();

            // parse the thumbnail
            ImageTag img = (ImageTag) HtmlParserUtils.getTag(divContent, CHARSET, "a img");

            // parse the title
            String episode = HtmlParserUtils.getText(divContent, CHARSET, "div[class=LinkeSpalte-Bildbox-Liste-164-Pixel-Bildueberschrift] a");
            LinkTag link = (LinkTag) HtmlParserUtils.getTag(divContent, CHARSET, "p[class=LinkeSpalte-Bildbox-Liste-164-Pixel-Bildueberschrift] a");
            String title = link.getLinkText();
            title = title.substring(1, title.length() - 1);
            title = episode + " - " + title;
            logger.log(LogService.LOG_DEBUG, "Title: " + title);

            // parse the publish date
            String _pubdate = HtmlParserUtils.getText(divContent, CHARSET, "p[class=LinkeSpalte-Bildbox-Liste-164-Pixel-Text2]");
            String pattern = "dd.MM.yyyy";
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            Calendar cal = null;
            try {
                Date pubDate = sdf.parse(_pubdate);
                cal = Calendar.getInstance();
                cal.setTime(pubDate);
            } catch (Exception e) {
                logger.log(LogService.LOG_WARNING, "Coulnd't parse publish date " + _pubdate + " with pattern " + pattern);
            }

            // create page structure
            opage.setTitle(title);
            opage.setUri(new URI("linden://" + _pubdate));
            page.getPages().add(opage);

            // add two vido pages. one for low and one for high quality
            IVideoPage low = new VideoPage();
            low.setParser(ID);
            low.setPublishDate(cal);
            low.setThumbnail(new URI(BASE_URI + img.getImageURL()));
            low.setTitle(title + " (" + getResourceBundle().getString("low_quality") + ")");
            low.setUri(new URI(BASE_URI + link.extractLink() + "&startMedium=1"));
            opage.getPages().add(low);

            IVideoPage high = new VideoPage();
            high.setParser(ID);
            high.setPublishDate(cal);
            high.setThumbnail(new URI(BASE_URI + img.getImageURL()));
            high.setTitle(title + " (" + getResourceBundle().getString("high_quality") + ")");
            high.setUri(new URI(BASE_URI + link.extractLink() + "&q=L"));
            opage.getPages().add(high);
        }

        return page;
    }

    @Override
    public String getTitle() {
        return "Lindenstraße";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IVideoPage) {
            IVideoPage vpage = (IVideoPage) page;

            // parse the video uri
            String content = HttpUtils.get(page.getUri().toString(), null, CHARSET);
            Tag param = HtmlParserUtils.getTag(content, CHARSET, "param[name=\"flashvars\"]");
            String flashvars = param.getAttribute("value");
            int start = flashvars.indexOf("dslSrc=") + 7;
            int stop = flashvars.indexOf('&', start);
            String _videoUri = flashvars.substring(start, stop);
            String playPath = "mp4:" + _videoUri.substring(STREAM_BASE.length(), _videoUri.length() - 4);
            URI videoUri = new URI(_videoUri);
            if (supportedProtocols.contains(videoUri.getScheme())) {
                vpage.setVideoUri(videoUri);
                vpage.getUserData().put("streamName", playPath);
            } else {
                throw new NoSupportedVideoFoundException(videoUri.toString(), supportedProtocols);
            }
        }
        return page;
    }

    @Override
    public ResourceBundle getResourceBundle() {
        if (resourceBundle == null) {
            try {
                logger.log(LogService.LOG_DEBUG, "Loading resource bundle for " + getClass().getSimpleName());
                resourceBundle = ResourceBundleLoader.load(ctx, Locale.getDefault());
            } catch (IOException e) {
                logger.log(LogService.LOG_ERROR, "Couldn't load resource bundle", e);
            }
        }
        return resourceBundle;
    }

    // ############ ipojo stuff #########################################

    // validate and invalidate method seem to be necessary for the bind methods to work
    @Validate
    public void start() {
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
