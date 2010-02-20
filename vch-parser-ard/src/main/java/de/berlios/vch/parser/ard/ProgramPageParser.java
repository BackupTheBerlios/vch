package de.berlios.vch.parser.ard;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.Node;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.VideoPage;

public class ProgramPageParser {
    
    public List<IVideoPage> parse(String pageUri, int pageNo) throws IOException, ParserException, URISyntaxException {
        pageUri = pageUri.replaceAll("/index.html", "/goto="+Integer.toString(pageNo)+"/index.html"); 
        List<IVideoPage> videoPages = new ArrayList<IVideoPage>();
        
        String content = HttpUtils.get(pageUri, ARDMediathekParser.HTTP_HEADERS, ARDMediathekParser.CHARSET);
        NodeList items = HtmlParserUtils.getTags(content, ARDMediathekParser.CHARSET, "ol[class~=mt-list_view] li");
        NodeIterator iter = items.elements();
        while(iter.hasMoreNodes()) {
            Node currentNode = iter.nextNode(); 
            String itemHtml = currentNode.toHtml();
            LinkTag a = (LinkTag) HtmlParserUtils.getTag(itemHtml, ARDMediathekParser.CHARSET, "h3 a");
            if(a == null) {
                // probably a podcast item continue with the next one
                continue;
            }

            String itemPageUri = ARDMediathekParser.BASE_URL + a.getLink();
            VideoPage video = new VideoPage(); //VideoItemPageParser.parse(itemPageUri);
            video.setUri(new URI(itemPageUri));
            if(video != null) {
                video.setParser(ARDMediathekParser.ID);
                videoPages.add(video);
                
                // parse title
                video.setTitle(HtmlParserUtils.getText(currentNode.toHtml(), ARDMediathekParser.CHARSET, "div h3.mt-title").trim());
                //video.setTitle(HtmlParserUtils.getText(itemHtml, ARDMediathekParser.CHARSET, "h3"));
                
                // parse enclosure duration
                String clipLaenge = HtmlParserUtils.getText(itemHtml, ARDMediathekParser.CHARSET, "span.cliplaenge");
                if (clipLaenge.length() > 0) {
                    Pattern p = Pattern.compile("(\\d+):(\\d+) min");
                    Matcher m = p.matcher(clipLaenge);
                    if (m.matches()) {
                        int min = Integer.parseInt(m.group(1));
                        int sec = Integer.parseInt(m.group(2));
                        video.setDuration(min * 60 + sec);
                    }
                }
                
//                // parse image
//                ImageTag img = (ImageTag) HtmlParserUtils.getTag(itemHtml, ARDMediathekParser.CHARSET, "div.show_passepartout img");
//                if(img != null) {
//                    Videocast myvidcast = new VideocastImpl();
//                    myvidcast.setImage(ARDMediathekParser.BASE_URL + img.getImageURL());
//                    entry.getModules().add(myvidcast);
//                }
                
                // parse infotext
                video.setDescription(HtmlParserUtils.getText(itemHtml, ARDMediathekParser.CHARSET, "span.infotext"));
            }
        }
        return videoPages;
    }
    
    

}
