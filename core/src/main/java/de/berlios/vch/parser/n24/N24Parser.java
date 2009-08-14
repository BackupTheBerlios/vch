package de.berlios.vch.parser.n24;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.xml.namespace.QName;

import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEnclosure;
import com.sun.syndication.feed.synd.SyndEnclosureImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;

import de.berlios.vch.parser.AbstractPageParser;
import de.berlios.vch.parser.n24.tvnext.ArrayOfN24Ressort;
import de.berlios.vch.parser.n24.tvnext.N24Ressort;
import de.berlios.vch.parser.n24.tvnext.TvNextClip;
import de.berlios.vch.parser.n24.tvnext.TvNextCore;
import de.berlios.vch.parser.n24.tvnext.TvNextCorePortType;

public class N24Parser extends AbstractPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(N24Parser.class);
    
    public static final String STREAM_BASE = "rtmp://pssimn24fs.fplive.net:1935/pssimn24";
    
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        try {
            URL wsdlUrl = new URL("http://mediencenter.n24.de/index.php/service/wsdl");
            QName serviceName = new QName("http://schemas.exozet.com/tvnext/services/core/","TvNextCore");
            TvNextCore service = new TvNextCore(wsdlUrl, serviceName);
            
            TvNextCorePortType port = (TvNextCorePortType) service.getPort(TvNextCorePortType.class);
            ArrayOfN24Ressort array = port.getRessorts(10);
            List<N24Ressort> ressorts = array.getN24Ressort();
            for (N24Ressort ressort : ressorts) {
                //TODO ressort magazine nochmal aufspalten in einzelne feeds
                SyndFeed feed = new SyndFeedImpl();
                feed.setTitle(ressort.getTitle().getValue());
                feed.setLink("http://www.n24.de/" + ressort.getTitle().getValue());
                feed.setDescription(ressort.getDescription() != null ? ressort.getDescription().getValue() : "");
                try {
                    feed.setPublishedDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(ressort.getCreatedTimestamp().getValue()));
                } catch (ParseException e) {
                    logger.warn("Couldn't parse channel pubDate", e);
                }

                List<TvNextClip> clips = port.getClipsByRessortId(ressort.getId().getValue(), 0, 100).getTvNextClip();
                for (TvNextClip clip : clips) {
                    SyndEntry entry = new SyndEntryImpl();
                    entry.setTitle(clip.getHeader().getValue());
                    
                    SyndContent description = new SyndContentImpl();
                    description.setValue(clip.getTitle().getValue());
                    entry.setDescription(description);
                    
                    try {
                        entry.setPublishedDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(clip.getCreatedTimestamp().getValue()));
                    } catch (ParseException e) {
                        logger.warn("Couldn't parse video pubDate", e);
                    }
                    
                    // set duration in foreign markup
                    Element elem = new Element("duration");
                    elem.setText(clip.getFlvDuration().getValue().toString());
                    ((List<Element>)entry.getForeignMarkup()).add(elem);
                    
                    SyndEnclosure enc = new SyndEnclosureImpl();
                    enc.setType("video/flv");
                    enc.setUrl(STREAM_BASE + clip.getStreamPath().getValue());
                    entry.getEnclosures().add(enc);

                    feed.getEntries().add(entry);
                }
                
                // feed speichern
                super.saveFeed2DB(feed);
                // feed url speichern
                super.addRssFeed(feed.getLink());
            }
        } catch (MalformedURLException e) {
            logger.error("WSDL URL invalid", e);
        }
    }
}
