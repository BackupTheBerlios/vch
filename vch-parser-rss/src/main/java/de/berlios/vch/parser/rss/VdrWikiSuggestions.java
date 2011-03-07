package de.berlios.vch.parser.rss;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.htmlparser.tags.TextareaTag;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.HtmlParserUtils;

public class VdrWikiSuggestions {
    public static List<Group> loadSuggestions() throws IOException, ParserException {
        List<Group> result = new ArrayList<Group>();
        String content = HttpUtils.get("http://vdr-wiki.de/wiki/index.php?title=Vodcatcher_Helper/Feeds&action=edit", null, "UTF-8");
        
        TextareaTag ta = (TextareaTag) HtmlParserUtils.getTag(content, "UTF-8", "textarea#wpTextbox1");
        String raw = ta.toPlainTextString().trim();
        Scanner sc = new Scanner(raw).useDelimiter("\n");
        Pattern headlinePattern = Pattern.compile("^=(.*)=$");
        Pattern itemPattern = Pattern.compile("^\\*\\s+\\[(.*)\\]$");
        Group current = null;
        while(sc.hasNext()) {
            String line = sc.next().trim();
            Matcher headlineMatcher = headlinePattern.matcher(line);
            Matcher itemMatcher = itemPattern.matcher(line);
            if(headlineMatcher.matches()) {
                String title = headlineMatcher.group(1);
                current = new Group();
                current.title = title;
                result.add(current);
            } else if(itemMatcher.matches()) {
                String item = Translate.decode(itemMatcher.group(1));
                int firstSpace = item.indexOf(' ');
                Feed feed = new Feed();
                feed.title = item.substring(firstSpace + 1);
                feed.uri = item.substring(0, firstSpace);
                current.feeds.add(feed);
            }
        }
        return result;
    }
    
    public static class Group {
        String title;
        List<Feed> feeds = new ArrayList<Feed>();
    }
    
    public static class Feed {
        String title;
        String uri;
    }
}
