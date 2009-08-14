package de.berlios.vch.parser.ardmediathek;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.ImageTag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.jdom.Element;

import com.sun.syndication.feed.synd.SyndEntry;

import de.berlios.vch.rome.videocast.Videocast;
import de.berlios.vch.rome.videocast.VideocastImpl;
import de.berlios.vch.utils.HtmlParserUtils;
import de.berlios.vch.utils.HttpUtils;

public class ProgramPageParser {

    @SuppressWarnings("unchecked")
    public static List<SyndEntry> parse(String pageUri, int pageNo) throws IOException, ParserException {
        pageUri = HttpUtils.addParameter(pageUri, "goto", Integer.toString(pageNo));
        List<SyndEntry> entries = new ArrayList<SyndEntry>();
        
        String content = HttpUtils.get(pageUri, ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);
        NodeList items = HtmlParserUtils.getTags(content, ARDMediathekParser.CHARSET, "li[class~=zelle]");
        NodeIterator iter = items.elements();
        while(iter.hasMoreNodes()) {
            String itemHtml = iter.nextNode().toHtml();
            LinkTag a = (LinkTag) HtmlParserUtils.getTag(itemHtml, ARDMediathekParser.CHARSET, "div[class~=bewertung_icons] a[class=video]");
            if(a == null) {
                // probably a podcast item continue with the next one
                continue;
            }

            String itemPageUri = ARDMediathekParser.BASE_URL + a.getLink();
            SyndEntry entry = VideoItemPageParser.parse(itemPageUri);
            if(entry != null) {
                entries.add(entry);
                
                // parse enclosure duration
                if (entry.getEnclosures().size() > 0) {
                    String clipLaenge = HtmlParserUtils.getText(itemHtml, ARDMediathekParser.CHARSET, "span.cliplaenge");
                    if (clipLaenge.length() > 0) {
                        Pattern p = Pattern.compile("(\\d+):(\\d+) min");
                        Matcher m = p.matcher(clipLaenge);
                        if (m.matches()) {
                            int min = Integer.parseInt(m.group(1));
                            int sec = Integer.parseInt(m.group(2));
                            
                            // set duration in foreign markup
                            Element elem = new Element("duration");
                            elem.setText(Long.toString(min * 60 + sec));
                            ((List<Element>)entry.getForeignMarkup()).add(elem);
                        }
                    }
                }
                
                // parse image
                ImageTag img = (ImageTag) HtmlParserUtils.getTag(itemHtml, ARDMediathekParser.CHARSET, "div.show_passepartout img");
                if(img != null) {
                    Videocast myvidcast = new VideocastImpl();
                    myvidcast.setImage(ARDMediathekParser.BASE_URL + img.getImageURL());
                    entry.getModules().add(myvidcast);
                }
                
                // parse infotext, if player page didn't contain a description
                if(entry.getDescription().getValue().length() == 0) {
                    entry.getDescription().setValue(HtmlParserUtils.getText(itemHtml, ARDMediathekParser.CHARSET, "span.infotext"));
                }
            }
        }
        return entries;
    }
    
    

}
