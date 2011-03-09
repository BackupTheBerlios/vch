package de.berlios.vch.parser.tivi;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.felix.ipojo.annotations.Bind;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Invalidate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.apache.felix.ipojo.annotations.Unbind;
import org.apache.felix.ipojo.annotations.Validate;
import org.htmlparser.util.ParserException;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.i18n.ResourceBundleLoader;
import de.berlios.vch.i18n.ResourceBundleProvider;
import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPageTitleComparator;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;

@Component
@Provides
public class TiviParser implements IWebParser, ResourceBundleProvider {

    public static final String ID = TiviParser.class.getName();

    public static final String BASE_URI = "http://www.tivi.de";
    private static final String START_PAGE = BASE_URI + "/tiviVideos/navigation?view=flashXml";

    public static final String CHARSET = "utf-8";

    private List<String> supportedProtocols = new ArrayList<String>();

    @Requires
    private LogService logger;

    private ResourceBundle resourceBundle;

    private BundleContext ctx;

    public TiviParser(BundleContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        String content = HttpUtils.get(START_PAGE, null, CHARSET);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(content)));
        NodeList nodes = doc.getElementsByTagName("ns2:node");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String title = node.getAttributes().getNamedItem("label").getNodeValue();
            String uri = node.getTextContent();
            IOverviewPage program = new OverviewPage();
            program.setParser(getId());
            program.setTitle(title);
            program.setUri(new URI(BASE_URI + uri));
            page.getPages().add(program);
        }

        Collections.sort(page.getPages(), new WebPageTitleComparator());
        return page;
    }

    @Override
    public String getTitle() {
        return "ZDFtivi";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if (page instanceof IOverviewPage) {
            IOverviewPage opage = (IOverviewPage) page;
            String content = HttpUtils.get(opage.getUri().toString(), null, CHARSET);
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(content)));
            NodeList nodes = doc.getElementsByTagName("ns3:video-teaser");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                NodeList childs = node.getChildNodes();
                IVideoPage video = new VideoPage();
                video.setParser(getId());
                opage.getPages().add(video);
                for (int j = 0; j < childs.getLength(); j++) {
                    Node child = childs.item(j);
                    if ("ns3:headline".equals(child.getNodeName())) {
                        video.setTitle(child.getTextContent());
                    } else if ("ns3:page".equals(child.getNodeName())) {
                        video.setUri(new URI(BASE_URI + child.getTextContent()));
                    } else if ("ns3:text".equals(child.getNodeName())) {
                        String text = child.getTextContent();
                        if (text != null && !text.isEmpty()) {
                            video.setTitle(text);
                        }
                    } else if ("ns3:image".equals(child.getNodeName())) {
                        String path = child.getTextContent();
                        if (path != null && !path.isEmpty()) {
                            video.setThumbnail(new URI(BASE_URI + path));
                        }
                    }
                }
            }
        } else if (page instanceof IVideoPage) {
            parseVideo((IVideoPage) page);
        }
        return page;
    }

    private void parseVideo(IVideoPage page) throws IOException, ParserException, URISyntaxException,
            NoSupportedVideoFoundException, ParserConfigurationException, SAXException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSS");

        IVideoPage video = page;
        String content = HttpUtils.get(video.getUri().toString(), null, CHARSET);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(content)));

        String subtitle = getTextContent(doc, "subtitle");
        if (subtitle != null) {
            video.setTitle(subtitle);
        }

        Node information = getFirstElementByTagName(doc, "information");
        if (information != null) {

            // parse description
            String description = getTextContent(information, "text");
            if (description != null && !description.isEmpty()) {
                video.setDescription(description);
            }

            // parse availability
            String available = getTextContent(information, "availableUntil");
            if (available != null) {
                try {
                    available = available.substring(0, available.length() - 6);
                    Date until = dateFormat.parse(available);
                    String desc = video.getDescription() != null ? video.getDescription() : "";
                    String availableUntil = getResourceBundle().getString("available_until");
                    availableUntil = MessageFormat.format(availableUntil, DateFormat.getDateTimeInstance()
                            .format(until));
                    video.setDescription(desc + "\n\n" + availableUntil);
                } catch (ParseException e) {
                    logger.log(LogService.LOG_WARNING, "Couldn't parse available until date", e);
                }
            }

            // parse publish date
            String pubDateString = getTextContent(information, "airTime");
            if (pubDateString != null) {
                try {
                    pubDateString = pubDateString.substring(0, pubDateString.length() - 6);
                    Date pubDate = dateFormat.parse(pubDateString);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(pubDate);
                    video.setPublishDate(cal);
                } catch (ParseException e) {
                    logger.log(LogService.LOG_WARNING, "Couldn't parse publish date", e);
                }
            }

            // parse duration
            try {
                String durationString = findChildWithTagName(information, "duration").getTextContent();
                if (durationString != null) {
                    Matcher m = Pattern.compile("P\\d+Y\\d+M\\d+DT(\\d+)H(\\d+)M(\\d+)\\.000S").matcher(durationString);
                    if (m.matches()) {
                        int seconds = 0;
                        seconds += TimeUnit.HOURS.toSeconds(Long.parseLong(m.group(1)));
                        seconds += TimeUnit.MINUTES.toSeconds(Long.parseLong(m.group(2)));
                        seconds += Long.parseLong(m.group(3));
                        video.setDuration(seconds);
                    }
                }
            } catch (Exception e) {
                logger.log(LogService.LOG_WARNING, "Couldn't parse video duration", e);
            }
        }

        // parse video URI
        Node stream = getFirstElementByTagName(doc, "stream");
        String videoUriString = getTextContent(stream, "high");
        if (videoUriString == null) {
            videoUriString = getTextContent(stream, "medium");
            if (videoUriString == null) {
                videoUriString = getTextContent(stream, "low");
            }
        }
        String meta = HttpUtils.get(videoUriString, null, CHARSET);
        Document metaDoc = builder.parse(new InputSource(new StringReader(meta)));
        String uri = getFirstElementByTagName(metaDoc, "default-stream-url").getTextContent();
        URI videoUri = new URI(uri);
        if (supportedProtocols.contains(videoUri.getScheme())) {
            video.setVideoUri(videoUri);
            int start = uri.indexOf("mp4:");
            int stop = uri.length() - 4;
            String playPath = uri.substring(start, stop);
            logger.log(LogService.LOG_DEBUG, "Video URI is " + uri);
            logger.log(LogService.LOG_DEBUG, "StreamName is " + playPath);
            video.getUserData().put("streamName", playPath);
        } else {
            throw new NoSupportedVideoFoundException(videoUriString.toString(), supportedProtocols);
        }
    }

    private Node getFirstElementByTagName(Document doc, String tagName) {
        NodeList list = doc.getElementsByTagName(tagName);
        if (list.getLength() > 0) {
            return list.item(0);
        } else {
            return null;
        }
    }

    private String getTextContent(Document doc, String tagName) {
        Node node = getFirstElementByTagName(doc, tagName);
        if (node != null) {
            return node.getTextContent();
        } else {
            return null;
        }
    }

    private String getTextContent(Node parent, String tagName) {
        Node node = findChildWithTagName(parent, tagName);
        if (node != null) {
            return node.getTextContent();
        } else {
            return null;
        }
    }

    private Node findChildWithTagName(Node parent, String tagName) {
        if (parent == null) {
            return null;
        }

        NodeList childs = parent.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node child = childs.item(i);
            if (child.getNodeName().equals(tagName)) {
                return child;
            } else if (child.hasChildNodes()) {
                Node result = findChildWithTagName(child, tagName);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
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
