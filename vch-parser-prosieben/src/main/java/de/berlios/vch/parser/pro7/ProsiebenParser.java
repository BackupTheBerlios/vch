package de.berlios.vch.parser.pro7;

import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.IOverviewPage;
import de.berlios.vch.parser.IVideoPage;
import de.berlios.vch.parser.IWebPage;
import de.berlios.vch.parser.IWebParser;
import de.berlios.vch.parser.OverviewPage;
import de.berlios.vch.parser.VideoPage;
import de.berlios.vch.parser.WebPageTitleComparator;

@Component
@Provides
public class ProsiebenParser implements IWebParser {
    public static final String CHARSET = "UTF-8";

    public static final String ID = ProsiebenParser.class.getName();

    private static final String BASE_URI = "http://pro7_api.cellmp.de";
    private static final String CATEGORY_URI = BASE_URI + "/2.0/getCategories.php?device=android";
    private static final String PROGRAM_URI = BASE_URI + "/2.0/getClips.php?device=android&category=";

    // /2.0/getClips.php?device=android&category=25413

    public static Map<String, String> HTTP_HEADERS = new HashMap<String, String>();
    static {
        HTTP_HEADERS.put("User-Agent", "Apache-HttpClient/UNAVAILABLE (java 1.4)");
    }

    @Requires
    private LogService logger;

    @Override
    public IOverviewPage getRoot() throws Exception {
        OverviewPage page = new OverviewPage();
        page.setParser(ID);
        page.setTitle(getTitle());
        page.setUri(new URI("vchpage://localhost/" + getId()));

        String json = HttpUtils.get(CATEGORY_URI, HTTP_HEADERS, CHARSET);
        JSONArray programs = new JSONArray(json);
        for (int i = 0; i < programs.length(); i++) {
            JSONObject program = programs.getJSONObject(i);
            IOverviewPage category = new OverviewPage();
            category.setParser(getId());
            category.setTitle(program.getString("categoryTitle"));
            category.setUri(new URI("pro7://category/" + program.getString("id")));
            page.getPages().add(category);
        }

        Collections.sort(page.getPages(), new WebPageTitleComparator());
        return page;
    }

    @Override
    public String getTitle() {
        return "Pro7";
    }

    @Override
    public IWebPage parse(IWebPage page) throws Exception {
        logger.log(LogService.LOG_INFO, "Parsing page " + page.getUri());
        URI pageUri = page.getUri();
        if (pageUri.toString().contains("episode")) {

        } else if ("category".equalsIgnoreCase(pageUri.getHost())) {
            IOverviewPage opage = (IOverviewPage) page;
            String id = pageUri.getPath().substring(1);
            String requestUri = PROGRAM_URI + id;
            String content = HttpUtils.get(requestUri, HTTP_HEADERS, CHARSET);
            JSONArray episodes = new JSONArray(content);
            for (int i = 0; i < episodes.length(); i++) {
                JSONObject episode = episodes.getJSONObject(i);
                IVideoPage video = new VideoPage();
                video.setParser(getId());
                // parse the title
                video.setTitle(episode.getString("title"));

                // create the page uri
                String categoryId = episode.getString("cat_id");
                String episodeId = episode.getString("id");
                video.setUri(new URI("pro7://category/" + categoryId + "/episode/" + episodeId));

                // parse the description
                video.setDescription(episode.getString("description"));

                // parse the thumbnail
                video.setThumbnail(new URI(episode.getString("generated_image_url")));

                // parse the video uri
                video.setVideoUri(new URI(episode.getString("generated_video_url_high")));

                // parse the duration
                try {
                    String[] duration = episode.getString("duration").split(":");
                    int minutes = Integer.parseInt(duration[0]);
                    int seconds = Integer.parseInt(duration[1]);
                    video.setDuration(minutes * 60 + seconds);
                } catch (Exception e) {
                    logger.log(LogService.LOG_WARNING, "Couldn't parse video duration", e);
                }

                // parse the publish date
                String broadcast = episode.getString("broadcast");
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                try {
                    Date pubDate = sdf.parse(broadcast);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(pubDate);
                    video.setPublishDate(cal);
                } catch (Exception e) {
                    logger.log(LogService.LOG_WARNING, "Couldn't parse publish date", e);
                }

                opage.getPages().add(video);
            }
            logger.log(LogService.LOG_DEBUG, content);
        }

        return page;
    }

    @Override
    public String getId() {
        return ID;
    }
}