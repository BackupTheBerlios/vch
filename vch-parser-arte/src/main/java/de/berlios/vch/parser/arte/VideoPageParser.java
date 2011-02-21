package de.berlios.vch.parser.arte;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.htmlparser.Tag;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleContext;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;

public class VideoPageParser {

    private Map<String, Integer> formatPrio = new HashMap<String, Integer>();

    private BundleContext ctx;

    private LogService logger;

    private static final String APP_NAME = "/a3903/o35/";

    public VideoPageParser(BundleContext ctx, LogService logger) {
        this.ctx = ctx;
        this.logger = logger;

        formatPrio.put("hd", 2);
        formatPrio.put("sd", 1);
        formatPrio.put("EQ", 0);
    }

    public void parse(IVideoPage video) throws URISyntaxException, IOException, ParserException, SAXException,
    ParserConfigurationException, NoSupportedVideoFoundException, DOMException {
        logger.log(LogService.LOG_DEBUG, "Getting media link in media page:" + video.getUri());

        // parse the video link
        parseVideoUri(video);
    }

    private void parseVideoUri(IVideoPage video) throws IOException, ParserException, URISyntaxException, SAXException,
    ParserConfigurationException, NoSupportedVideoFoundException, DOMException {
        // create list of supported network protocols
        List<String> supportedProtocols = new ArrayList<String>();
        ServiceTracker st = new ServiceTracker(ctx, INetworkProtocol.class.getName(), null);
        st.open();
        Object[] protocols = st.getServices();
        for (Object object : protocols) {
            INetworkProtocol protocol = (INetworkProtocol) object;
            supportedProtocols.addAll(protocol.getSchemes());
        }
        st.close();

        // parse the swf location for swf verification
        String content = HttpUtils.get(video.getUri().toString(), ArteParser.HTTP_HEADERS, ArteParser.CHARSET);
        Tag embed = HtmlParserUtils.getTag(content, ArteParser.CHARSET, "embed");
        String swfUri = Translate.decode(embed.getAttribute("src"));
        video.getUserData().put("swfUri", new URI(swfUri));
        logger.log(LogService.LOG_INFO, "SWF URI: " + swfUri);

        // parse the html page to get the video ref file
        Tag param = HtmlParserUtils.getTag(content, ArteParser.CHARSET, "param[name=movie]");
        String movie = Translate.decode(param.getAttribute("value"));
        URI movieUri = new URI(movie);
        Map<String, List<String>> params = HttpUtils.parseQuery(movieUri.getQuery());
        String refFileUri = params.get("videorefFileUrl").get(0);
        logger.log(LogService.LOG_DEBUG, "Video ref file is at " + refFileUri);

        // parse the description
        String desc = HtmlParserUtils.getText(content, ArteParser.CHARSET, "div.recentTracksCont p");
        video.setDescription(desc);

        // parse the video ref file
        content = HttpUtils.get(refFileUri, ArteParser.HTTP_HEADERS, ArteParser.CHARSET);
        Tag v = HtmlParserUtils.getTag(content, ArteParser.CHARSET, "video[lang=de]");
        String refFileDe = v.getAttribute("ref");
        logger.log(LogService.LOG_DEBUG, "DE ref file " + refFileDe);

        // get the de ref file
        content = HttpUtils.get(refFileDe, ArteParser.HTTP_HEADERS, ArteParser.CHARSET);
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        .parse(new InputSource(new StringReader(content)));

        // parse the title
        Node name = doc.getElementsByTagName("name").item(0);
        video.setTitle(name.getTextContent());

        // parse the thumb
        Node thumb = doc.getElementsByTagName("firstThumbnailUrl").item(0);
        video.setThumbnail(new URI(thumb.getTextContent()));

        // parse the pubdate (Tue, 11 Jan 2011 11:07:44 +0100)
        try {
            Node pubDate = doc.getElementsByTagName("dateVideo").item(0);
            // parse with locale english, so that the parsing of the names works
            Date date = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(pubDate.getTextContent());
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            video.setPublishDate(cal);
        } catch(Exception e) {
            logger.log(LogService.LOG_WARNING, "Couldn't parse publish date", e);
        }

        // parse the video URIs
        Node urls = doc.getElementsByTagName("urls").item(0);
        NodeList childs = urls.getChildNodes();
        URI bestVideo = null;
        int bestQuali = Integer.MIN_VALUE;
        for (int i = 0; i < childs.getLength(); i++) {
            Node url = childs.item(i);
            if ("url".equals(url.getNodeName())) {
                String quality = url.getAttributes().getNamedItem("quality").getNodeValue();
                URI uri = new URI(url.getTextContent());
                if (supportedProtocols.contains(uri.getScheme())) {
                    int qualiPrio = formatPrio.get(quality);
                    if (qualiPrio > bestQuali) {
                        bestVideo = uri;
                        bestQuali = qualiPrio;
                    }
                }
            }
        }


        if (bestVideo != null) {
            logger.log(LogService.LOG_INFO, "Best format found is " + bestVideo.toString());
            video.setVideoUri(bestVideo);
            String streamName = bestVideo.getPath();
            streamName += bestVideo.getQuery() != null ? "?" + bestVideo.getQuery() : "";
            streamName += bestVideo.getFragment() != null ? "#" + bestVideo.getFragment() : "";
            streamName = streamName.substring(APP_NAME.length());
            logger.log(LogService.LOG_INFO, "Stream name is " + streamName);
            video.getUserData().put("streamName", streamName);
        } else {
            throw new NoSupportedVideoFoundException(video.getUri().toString(), supportedProtocols);
        }
    }
}
