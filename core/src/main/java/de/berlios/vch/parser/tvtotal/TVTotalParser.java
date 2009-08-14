package de.berlios.vch.parser.tvtotal;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import de.berlios.vch.i18n.Messages;
import de.berlios.vch.model.Group;
import de.berlios.vch.parser.AbstractPageParser;

public class TVTotalParser extends AbstractPageParser {

    private static transient Logger logger = LoggerFactory.getLogger(TVTotalParser.class);
    
    public static final String CHARSET = "iso-8859-1";
    
    public static final String MAIN_URI = "http://tvtotal.prosieben.de/tvtotal/videos";
    public static final String META_DATA_URI = "http://tvtotal.prosieben.de/tvtotal/includes/php/videoplayer_metadata.php?id=";
    
    private Pattern longDate = Pattern.compile("\\d\\d\\.\\d\\d\\.\\d\\d\\d\\d");
    private Pattern shortDate = Pattern.compile("\\d\\d\\.\\d\\d\\.\\d\\d");
    
    @Override
    public void run() {
        try {
            String[] days = new String[4];
            days[0] = "mo";
            days[1] = "di";
            days[2] = "mi";
            days[3] = "do";

            SyndFeed feed = new SyndFeedImpl();
            feed.setEncoding(CHARSET);
            feed.setFeedType("rss_2.0");
            feed.setTitle(Messages.translate(getClass(), "title"));
            feed.setLink("http://tvtotal.prosieben.de/");
            feed.setDescription(Messages.translate(getClass(), "description"));
//            SyndImageImpl image = new SyndImageImpl();
//            image.setUrl("http://tvtotal.prosieben.de/tvtotal/media/images/base/logo.gif");
//            feed.setImage(image);
            

            
            List<SyndEntry> entries = new ArrayList<SyndEntry>();

            for (int i = 0; i < 4; i++) {
                // Öffne link
                URL url = new URL("http://tvtotal.prosieben.de/show/letzte_sendung/" + days[i] + "/");
                URLConnection con = url.openConnection();
                BufferedReader bi = new BufferedReader(new InputStreamReader(con.getInputStream(), CHARSET));
                String line;
                String last = "";

                Date date = new Date();

                while ((line = bi.readLine()) != null) {
                    if (line.matches(".*videoPopUpHigher.*")) {
                        if (!last.equals(line.split("\'")[1])) {
                            last = line.split("\'")[1];
                            SyndEntry entry = getEntry(line.split("\'")[1]);
                            entry.setPublishedDate(date);
                            entries.add(entry);
                        }
                    } else if (line.matches(".*TV total -.*")) {
                        // format: 10.12.2007 or 10.12.07 
                        String datestring = line.split("- ")[1].split("<")[0];
                        Matcher mLong = longDate.matcher(datestring);
                        Matcher mShort = shortDate.matcher(datestring);
                        DateFormat df = null;
                        if(mLong.matches()) {
                            df = new SimpleDateFormat("dd.MM.yyyy");
                        } else if(mShort.matches()) {
                            df = new SimpleDateFormat("dd.MM.yy");
                        } else {
                            throw new ParseException("Date has an unknown format", 0);
                        }
                        
                        date = df.parse(datestring);
                        feed.setPublishedDate(date);
                    }
                }
            }
            feed.setEntries(entries);
            //saveFeed(feed);
            saveFeed2DB(feed);
            addRssFeed(feed.getLink());

            // add all feeds to Group
            Group tvtotal = new Group();
            tvtotal.setName(Messages.translate(getClass(), "group_name"));
            tvtotal.setDescription(Messages.translate(getClass(), "group_desc"));
            addFeedsToGroup(tvtotal);
        } catch (Exception e) {
            logger.error("Couldn't parse TVTotal webpage", e);
        }
    }
    
    private SyndEntry getEntry(String link) {
        SyndEntry entry = new SyndEntryImpl();
        entry.setLink("http://tvtotal.prosieben.de" + link);

        try {

            // Öffne link
            URL url = new URL(entry.getLink());
            logger.trace("Trying to parse page {}", url);
            URLConnection con = url.openConnection();

            BufferedReader bi = new BufferedReader(new InputStreamReader(con.getInputStream(), CHARSET));
            String line;

            while ((line = bi.readLine()) != null) {
                if (line.matches(".*TV total - Videoplayer - .*")) {
                    entry.setTitle(line.split("Videoplayer - ")[1].split("<")[0]);

                } else if (line.matches(".*dataClipDescription.*")) {
                    SyndContent description = new SyndContentImpl();
                    description.setType("text/plain");
                    line = line.split("'")[3];
                    line = URLDecoder.decode(line, "UTF-8");
                    description.setValue(line);
                    entry.setDescription(description);
                } else if (line.matches(".*videoUrl.*")) {
                    SyndEnclosureImpl enc = new SyndEnclosureImpl();
                    line = line.split("'")[3];
                    line = URLDecoder.decode(line, "UTF-8");
                    enc.setUrl(line);
                    enc.setType("video/wmv");
                    @SuppressWarnings("unchecked")
                    List<SyndEnclosure> enclist = entry.getEnclosures();
                    enclist.add(enc);
                    entry.setEnclosures(enclist);
                }
            }
        } catch (Exception e) {
            logger.error("An error occured while parsing {}", entry.getLink(), e);
        }

        return entry;
    }
}
