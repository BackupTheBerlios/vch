package de.berlios.vch.parser.orf;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.htmlparser.Tag;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.tags.OptionTag;
import org.htmlparser.tags.ParagraphTag;
import org.htmlparser.tags.SelectTag;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.htmlparser.util.Translate;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.AsxParser;
import de.berlios.vch.parser.HtmlParserUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;

@Component
@Provides
public class OrfParser implements IWebParser {

    public static final String ID = OrfParser.class.getName();
    
    protected static final String BASE_URI = "http://tvthek.orf.at";
    protected static final String CHARSET = "UTF-8";
    
    @Requires
    private LogService logger;

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));
        
        String content = HttpUtils.get(BASE_URI, null, CHARSET);
        SelectTag select = (SelectTag) HtmlParserUtils.getTag(content, CHARSET, "form#programs select");
        for (OptionTag option : select.getOptionTags()) {
            String uri = option.getValue();
            if(uri.startsWith("http://")) {
                String name = Translate.decode(option.toPlainTextString().trim());
                IOverviewPage opage = new OverviewPage();
                opage.setParser(ID);
                opage.setTitle(name);
                opage.setUri(new URI(uri));
                page.getPages().add(opage);
            }
        }
        
        return page;
    }

    @Override
    public String getTitle() {
        return "ORF TVthek";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        if(page instanceof IOverviewPage) {
            if("dummy".equals(page.getUri().getScheme())) {
                return page;
            }
            
            return parseOverviewPage(page);
        } else if(page instanceof IVideoPage) {
            return parseVideoPage(page);
        } else {
            return page;
        }
    }
    
    private IWebPage parseOverviewPage(IWebPage page) throws Exception {
        IOverviewPage opage = (IOverviewPage) page;
        String content = HttpUtils.get(page.getUri().toString(), null, CHARSET);
        
        // parse the current video
        String title = HtmlParserUtils.getText(content, CHARSET, "h3.title span");
        IVideoPage video = new VideoPage();
        video.setParser(getId());
        video.setTitle(title);
        video.setUri(page.getUri());
        opage.getPages().add(video);
        
        // parse more episodes
        NodeList dayList = HtmlParserUtils.getTags(content, CHARSET, "div#more-episodes div.scrollbox > ul > li");
        if(dayList.size() > 0) {
            for (NodeIterator iterator = dayList.elements(); iterator.hasMoreNodes();) {
                // add the day
                Tag li = (Tag) iterator.nextNode();
                title = Translate.decode(li.getFirstChild().toPlainTextString().trim());
                IOverviewPage day = new OverviewPage();
                day.setParser(getId());
                day.setTitle(title);
                day.setUri(new URI("dummy://" + UUID.randomUUID()));
                opage.getPages().add(day);
                
                // add episodes for the day
                NodeList episodeList = HtmlParserUtils.getTags(li.toHtml(), CHARSET, "ul li");
                for (NodeIterator epIter = episodeList.elements(); epIter.hasMoreNodes();) {
                    Tag epli = (Tag) epIter.nextNode();
                    title = Translate.decode(epli.toPlainTextString().trim());
                    video = new VideoPage();
                    video.setParser(getId());
                    video.setTitle(title);
                    LinkTag link = (LinkTag) HtmlParserUtils.getTag(epli.toHtml(), CHARSET, "a");
                    video.setUri(new URI(BASE_URI + link.extractLink()));
                    day.getPages().add(video);
                }
            }
        }
        
        return page;
    }

    private IVideoPage parseVideoPage(IWebPage page) throws ParserException, IOException, URISyntaxException {
        IVideoPage vpage = (IVideoPage) page;
        String content = HttpUtils.get(page.getUri().toString(), null, CHARSET);
        
        // parse the video uri
        Tag param = HtmlParserUtils.getTag(content, CHARSET, "param[name=URL]");
        String videoUri = BASE_URI + param.getAttribute("value");
        if(videoUri.toLowerCase().endsWith(".asx")) {
            videoUri = AsxParser.getUri(videoUri);
        }
        vpage.setVideoUri(new URI(videoUri));
        
        // parse the title
        String title = HtmlParserUtils.getText(content, CHARSET, "h3.title span");
        vpage.setTitle(title);
        
        // parse description if available
        ParagraphTag desc = (ParagraphTag) HtmlParserUtils.getTag(content, CHARSET, "div#info-tab div.content p");
        if(desc != null) {
            vpage.setDescription(Translate.decode(desc.toPlainTextString().trim()));
        }
        
        // parse the pubdate
        String datePattern = "dd.MM.yyyy";
        Matcher m = Pattern.compile("(\\d{2}\\.\\d{2}\\.\\d{4})").matcher(title);
        if(m.find()) {
            String date = m.group(1);
            logger.log(LogService.LOG_DEBUG, "PubDate is " + date);
            
            try {
                Date pubDate = new SimpleDateFormat(datePattern).parse(date);
                Calendar cal = Calendar.getInstance();
                cal.setTime(pubDate);
                vpage.setPublishDate(cal);
            } catch (ParseException e) {
                logger.log(LogService.LOG_WARNING, "Couldn't parse pubDate " + date + " with pattern " + datePattern);
            }
        }
        
        return vpage;
    }

    @Override
    public String getId() {
        return ID;
    }
}