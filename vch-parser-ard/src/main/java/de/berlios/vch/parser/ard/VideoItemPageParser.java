package de.berlios.vch.parser.ard;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.net.INetworkProtocol;
import de.berlios.vch.parser.AsxParser;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.exceptions.NoSupportedVideoFoundException;

public class VideoItemPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(VideoItemPageParser.class);

    public static VideoPage parse(VideoPage page, BundleContext ctx) throws IOException, ParserException, URISyntaxException, NoSupportedVideoFoundException {
        String content = HttpUtils.get(page.getUri().toString(), ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);

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

        // first parse the available formats for this video
        List<VideoType> videos = parseAvailableVideos(content);

        // sort by best format and quality
        Collections.sort(videos, new VideoTypeComparator());

        // find the first supported protocol
        VideoType bestVideo = null;
        for (VideoType video : videos) {
            URI uri = new URI(video.getUri());
            if (supportedProtocols.contains(uri.getScheme())) {
                bestVideo = video;
                break;
            }
        }

        if (bestVideo != null) {
            // set the video uri
            if (bestVideo.getUri().startsWith("http")) {
                Map<String, List<String>> headers = HttpUtils.head(bestVideo.getUri(), ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);
                String contentType = HttpUtils.getHeaderField(headers, "Content-Type");
                if ("video/x-ms-asf".equals(contentType)) {
                    bestVideo.uriPart2 = AsxParser.getUri(bestVideo.getUri());
                }
            }
            page.setVideoUri(new URI(bestVideo.getUri()));
            page.getUserData().put("streamName", bestVideo.uriPart2);

            logger.info("Best video found is: " + page.getVideoUri().toString());

            // parse title
            page.setTitle(parseTitle(content));

            // parse description
            String description = parseDescription(content);
            logger.trace("Description {}", description);
            page.setDescription(description);
            page.getUserData().remove("desc");

            // parse pubDate
            try {
                Calendar date = parseDate(content);
                logger.trace("Parsed date {}", date);
                page.setPublishDate(date);
            } catch (ParseException e) {
                logger.warn("Couldn't parse publish date. Using current time!", e);
                logger.trace("Content: {}", content);
                page.setPublishDate(Calendar.getInstance());
            }

            return page;
        } else {
            throw new NoSupportedVideoFoundException(page.getUri().toString(), supportedProtocols);
        }
    }

    private static String parseDescription(String content) throws ParserException {
        return Translate.decode(HtmlParserUtils.getText(content, ARDMediathekParser.CHARSET,
                "div.mt-player_content div[class~=js-collapseable] div[class~=js-collapseable_content] div p"));
    }

    private static String parseTitle(String content) throws ParserException {
        return Translate.decode(HtmlParserUtils.getText(content, ARDMediathekParser.CHARSET, "div.mt-player_content h2"));
    }

    private static Calendar parseDate(String content) throws ParserException, ParseException {
        NodeList list = HtmlParserUtils.getTags(content, ARDMediathekParser.CHARSET, "p.clipinfo");
        for (NodeIterator iterator = list.elements(); iterator.hasMoreNodes();) {
            Node node = iterator.nextNode();
            String string = node.toPlainTextString().trim();
            Pattern p = Pattern.compile("\\s*Online seit:\\s*(\\d+\\.\\d+\\.\\d+\\s*)\\s*", Pattern.DOTALL);
            Matcher m = p.matcher(string);
            if (m.find()) {
                Calendar pubDate = Calendar.getInstance();
                pubDate.setTime(new SimpleDateFormat("dd.MM.yy").parse(m.group(1)));
                return pubDate;
            }
        }

        return Calendar.getInstance();
    }

    private static List<VideoType> parseAvailableVideos(String content) throws URISyntaxException {
        List<VideoType> videos = new ArrayList<VideoType>();
        Pattern p = Pattern.compile("addMediaStream\\((\\d+), (\\d+), \"(.*)\", \"(.*)\", \"(.*)\"\\);");
        Matcher m = p.matcher(content);
        while (m.find()) {
            String uriPart1 = m.group(3);
            String uriPart2 = m.group(4);

            // this is a normal uri
            if (uriPart1.trim().isEmpty()) {
                uriPart1 = uriPart2;
                uriPart2 = "";
            }

            int format = Integer.parseInt(m.group(1));
            int quality = Integer.parseInt(m.group(2));
            VideoType vt = new VideoType(uriPart1, uriPart2, format, quality);
            videos.add(vt);
        }
        return videos;
    }

    /**
     * Container class for the different video types and qualities. The URI is split into uriPart1 and uriPart2. This is needed for rtmp streams.
     */
    public static class VideoType {
        private String uriPart1;
        private String uriPart2;
        private int format;
        private int quality;

        public VideoType(String uriPart1, String uriPart2, int format, int quality) {
            super();
            this.uriPart1 = uriPart1;
            this.uriPart2 = uriPart2;
            this.format = format;
            this.quality = quality;
        }

        public String getUri() {
            String uri = "";
            if (uriPart1 != null && uriPart1.length() > 0) {
                uri += uriPart1;
                if (!(uriPart1.endsWith("/") || uriPart2.startsWith("/"))) {
                    uriPart1 += "/";
                }
            }
            uri += uriPart2;
            return uri;
        }

        public String getUriPart1() {
            return uriPart1;
        }

        public void setUriPart1(String uriPart1) {
            this.uriPart1 = uriPart1;
        }

        public String getUriPart2() {
            return uriPart2;
        }

        public void setUriPart2(String uriPart2) {
            this.uriPart2 = uriPart2;
        }

        public int getFormat() {
            return format;
        }

        public void setFormat(int format) {
            this.format = format;
        }

        public int getQuality() {
            return quality;
        }

        public void setQuality(int quality) {
            this.quality = quality;
        }
    }
}
