package de.berlios.vch.parser.br;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.osgi.service.log.LogService;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPageTitleComparator;

@Component
@Provides
public class BrMediathekParser implements IWebParser {

    public static final String ID = BrMediathekParser.class.getName();
    public static final String XML_URI = "http://rd.gl-systemhaus.de/br/b7/listra/archive/archive.xml.zip.adler32";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    private Map<String, IOverviewPage> sendungen = new HashMap<String, IOverviewPage>();

    @Requires
    private LogService logger;

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        // load and parse the xml, if the archive is outdated
        reloadArchive();

        // add the pages
        page.getPages().addAll(sendungen.values());
        Collections.sort(page.getPages(), new WebPageTitleComparator());

        return page;
    }

    private void reloadArchive() throws Exception {
        // parse the archive data
        parseArchiveXml();

        // filter out programs without episode
        for (Iterator<Entry<String, IOverviewPage>> iterator = sendungen.entrySet().iterator(); iterator.hasNext();) {
            Entry<String, IOverviewPage> entry = iterator.next();
            IOverviewPage opage = entry.getValue();
            if (opage.getPages().size() <= 0) {
                iterator.remove();
                logger.log(LogService.LOG_DEBUG, "Removing " + opage.getTitle() + " because it has no episodes");
            }
        }
    }

    private void parseArchiveXml() throws Exception {
        sendungen.clear();

        URL url = new URL(XML_URI);
        ZipInputStream zin = new ZipInputStream(url.openStream());
        ZipEntry entry = zin.getNextEntry();
        if (entry != null) {
            DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document archive = builder.parse(zin);

            // parse all programs
            parsePrograms(archive);

            // parse the episodes
            parseEpisodes(archive);
        } else {
            throw new IOException("Archive zip file is empty");
        }
    }

    private void parseEpisodes(Document archive) throws Exception {
        // parse "ausstrahlungen"
        NodeList list = archive.getElementsByTagName("ausstrahlung");
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            Node videos = findChildWithTagName(n, "videos");
            if (videos.hasChildNodes()) {
                IVideoPage video = new VideoPage();
                video.setParser(getId());

                // parse the title
                String title = findChildWithTagName(n, "titel").getTextContent();
                String subtitle = findChildWithTagName(n, "nebentitel").getTextContent();
                title = subtitle.isEmpty() ? title : title + " - " + subtitle;
                video.setTitle(title);

                // parse the description
                String desc = findChildWithTagName(n, "beschreibung").getTextContent();
                video.setDescription(desc);

                // parse the video
                parseVideo(n, video);

                // parse the pubDate (2011-03-29T09:15:00)
                String beginnPlan = findChildWithTagName(n, "beginnPlan").getTextContent();
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                Date pubDate = sdf.parse(beginnPlan);
                Calendar cal = Calendar.getInstance();
                cal.setTime(pubDate);
                video.setPublishDate(cal);

                // parse the endePlan to calculate the duration
                String endePlan = findChildWithTagName(n, "endePlan").getTextContent();
                if (endePlan != null && !endePlan.trim().isEmpty()) {
                    pubDate = sdf.parse(endePlan);
                    Calendar calEnde = Calendar.getInstance();
                    calEnde.setTime(pubDate);
                    long duration = (calEnde.getTimeInMillis() - cal.getTimeInMillis()) / 1000;
                    video.setDuration(duration);
                }

                // parse the program id
                String programId = findChildWithTagName(n, "sendung").getTextContent();

                // parse the episode id
                String id = getAttribute(n, "id");
                video.setUri(new URI("br://sendung/" + programId + "/episode/" + id));

                // parse the thumbnail
                Node bild = findChildWithTagName(n, "bild");
                if (bild != null) {
                    String uri = bild.getTextContent();
                    if (uri != null && uri.length() > 0) {
                        video.setThumbnail(new URI(uri));
                    }
                } else {
                    // check if the program has thumbnail and use that instead
                    if (sendungen.get(programId).getUserData().get("thumbnail") != null) {
                        video.setThumbnail(new URI((String) sendungen.get(programId).getUserData().get("thumbnail")));
                    }
                }

                // add this episode to the program
                if (video.getVideoUri() != null) {
                    sendungen.get(programId).getPages().add(video);
                }
            }
        }
    }

    private void parseVideo(Node n, IVideoPage video) throws URISyntaxException {
        NodeList videos = findChildWithTagName(n, "videos").getChildNodes();
        List<Video> allSizes = new ArrayList<BrMediathekParser.Video>();
        for (int i = 0; i < videos.getLength(); i++) {
            Node vid = videos.item(i);
            if ("video".equals(vid.getNodeName())) {
                Video v = new Video();
                v.size = getAttribute(vid, "groesse");
                v.host = getAttribute(vid, "host");
                v.app = getAttribute(vid, "application");
                String stream = getAttribute(vid, "stream").trim();
                if (!stream.startsWith("mp4:") && stream.endsWith(".mp4")) {
                    stream = "mp4:" + stream;
                }
                v.stream = stream;
                v.uri = "rtmp://" + v.host + "/" + v.app + "/" + v.stream;
                allSizes.add(v);
            }
        }

        if (!allSizes.isEmpty()) {
            Collections.sort(allSizes);
            Collections.reverse(allSizes);
            Video bestQuality = allSizes.get(0);
            video.setVideoUri(new URI(bestQuality.uri));
            video.getUserData().put("streamName", bestQuality.stream);
        }
    }

    private static class Video implements Comparable<Video> {
        String host;
        String app;
        String stream;
        String uri;
        String size;

        private static Map<String, Integer> sizeToInt = new HashMap<String, Integer>();
        static {
            sizeToInt.put("xlarge", 3);
            sizeToInt.put("large", 2);
            sizeToInt.put("small", 1);
        }

        @Override
        public int compareTo(Video o) {
            Integer thisValue = sizeToInt.get(size);
            Integer otherValue = sizeToInt.get(o.size);

            if (thisValue == null) {
                thisValue = 0;
            }
            if (otherValue == null) {
                otherValue = 0;
            }

            return thisValue.compareTo(otherValue);
        }
    }

    private void parsePrograms(Document archive) throws Exception {
        NodeList parent = archive.getElementsByTagName("sendungen");
        NodeList list = parent.item(0).getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (!"sendung".equals(n.getNodeName())) {
                continue;
            }

            IOverviewPage sendung = new OverviewPage();
            sendung.setParser(ID);

            // parse the website
            String website = getAttribute(n, "website");

            // parse the id
            String programId = getAttribute(n, "id");
            if (website != null && !website.trim().isEmpty()) {
                sendung.setUri(new URI(website));
            } else {
                sendung.setUri(new URI("br://sendung/" + programId));
            }

            // parse the title
            String name = getAttribute(n, "name");
            sendung.setTitle(name);

            // parse the thubmnail of the program, which is used, when the episodes don't have a thumbnail on their own
            String bild = getAttribute(n, "bild");
            if (bild != null && !bild.trim().isEmpty()) {
                sendung.getUserData().put("thumbnail", bild);
            }

            sendungen.put(programId, sendung);
            logger.log(LogService.LOG_DEBUG, "Adding program " + name + " ID[" + programId + "]");

            // parse podcasts child, if existant
            Node podcasts = findChildWithTagName(n, "podcasts");
            if (podcasts != null) {
                NodeList feeds = podcasts.getChildNodes();
                for (int j = 0; j < feeds.getLength(); j++) {
                    Node feed = feeds.item(j);
                    Node image = findChildWithTagName(feed, "image");
                    URI thumb = null;
                    if (image != null) {
                        thumb = new URI(image.getTextContent());
                    }

                    NodeList feedChilds = feed.getChildNodes();
                    for (int k = 0; k < feedChilds.getLength(); k++) {
                        Node child = feedChilds.item(k);
                        if ("podcast".equals(child.getNodeName())) {
                            IVideoPage video = new VideoPage();
                            video.setParser(ID);

                            // get podcast id to set a dummy uri
                            String podcastId = getAttribute(child, "id");
                            video.setUri(new URI("br://sendung/" + programId + "/episode/" + podcastId));

                            // set the thumbnail if there is one
                            if (thumb != null) {
                                video.setThumbnail(thumb);
                            } else {
                                // check if the program has thumbnail and use that instead
                                if (sendung.getUserData().get("thumbnail") != null) {
                                    video.setThumbnail(new URI((String) sendung.getUserData().get("thumbnail")));
                                }
                            }

                            // parse the title
                            String title = findChildWithTagName(child, "title").getTextContent();
                            video.setTitle(title);

                            // parse the description
                            String description = findChildWithTagName(child, "description").getTextContent();
                            video.setDescription(description);

                            // parse the publish date
                            String date = findChildWithTagName(child, "pubdate").getTextContent();
                            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                            try {
                                Calendar cal = Calendar.getInstance();
                                cal.setTime(sdf.parse(date));
                                video.setPublishDate(cal);
                            } catch (ParseException e) {
                                logger.log(LogService.LOG_WARNING, "Couldn't parse pubdate " + date + " with format " + DATE_FORMAT);
                            }

                            // parse the duration
                            String durationString = findChildWithTagName(child, "duration").getTextContent();
                            String[] parts = durationString.split(":");
                            try {
                                long duration = Long.parseLong(parts[0]) * 3600 + Long.parseLong(parts[1]) * 60 + Long.parseLong(parts[2]);
                                video.setDuration(duration);
                            } catch (RuntimeException re) {
                                logger.log(LogService.LOG_WARNING, "Couldn't parse duration " + durationString);
                            }

                            // parse the video
                            String uri = findChildWithTagName(child, "enclosure").getTextContent();
                            try {
                                video.setVideoUri(new URI(uri));
                                sendung.getPages().add(video);
                            } catch (RuntimeException re) {
                                logger.log(LogService.LOG_WARNING, "Couldn't parse video uri", re);
                            }
                        }
                    }
                }
            }
        }
    }

    private String getAttribute(Node n, String attrName) {
        Node attr = n.getAttributes().getNamedItem(attrName);
        if (attr != null) {
            return attr.getNodeValue();
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
    public String getTitle() {
        return "BR Mediathek";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        return page;
    }

    @Override
    public String getId() {
        return ID;
    }
}