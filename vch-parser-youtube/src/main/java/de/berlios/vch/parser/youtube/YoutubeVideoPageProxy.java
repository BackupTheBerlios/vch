package de.berlios.vch.parser.youtube;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.VideoPage;


public class YoutubeVideoPageProxy extends VideoPage {
    
    private LogService logger;
    
    private Preferences prefs;
    
    public YoutubeVideoPageProxy(LogService logger, Preferences prefs) {
        this.logger = logger;
        this.prefs = prefs;
    }
    
    @Override
    public URI getVideoUri() {
        String link = getUri().toString();
        return parseOnDemand(link);
    }
    
    private URI parseOnDemand (String videoLink) {
        logger.log(LogService.LOG_DEBUG, "Getting video link for " + videoLink);
        
        URI medialink = null;
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("Accept-Encoding", "gzip");
            String pageContent = HttpUtils.get(videoLink, params, "UTF-8");
            
            StringTokenizer st = new StringTokenizer(pageContent, "\n");
            while(st.hasMoreTokens()) {
                String line = st.nextToken();
                if (line.contains("var swfConfig")) {
//                    logger.log(LogService.LOG_DEBUG, line);
                    int openingBracket = line.indexOf('{');
                    String jsonObjectString = line.substring(openingBracket).trim();
                    JSONObject jsonObject = new JSONObject(jsonObjectString);
//                    for (Iterator iterator = jsonObject.sortedKeys(); iterator.hasNext();) {
//                        String key = (String) iterator.next();
//                        System.out.println(key + " = " + jsonObject.get(key));
//                    }
                    JSONObject args = (JSONObject) jsonObject.get("args");
                    String video_id = args.getString("video_id");
                    String t = args.getString("t");
                    List<Integer> formatList = getFormatList(args);
                    logger.log(LogService.LOG_DEBUG, "The following formats are available " + formatList);
                    int format = prefs.getInt("video.quality", 34);
                    if(!formatList.contains(format)) {
                        logger.log(LogService.LOG_INFO, "Video is not available in preferred format " + format
                                + ". Using format " + formatList.get(0));
                        format = formatList.get(0);
                    }
                    medialink = new URI("http://www.youtube.com/get_video" + "?asv=3&el=detailpage&video_id=" + video_id + "&t=" + t + "&fmt=" + format);
                    
                    // parse duration
                    try {
                        long duration = args.getLong("length_seconds");
                        setDuration(duration);
                    } catch (Exception e) {
                        logger.log(LogService.LOG_WARNING, "Couldn't parse video duration");
                    }
                }
            }
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't parse Youtube video page", e);            
        }

        return medialink; 
    }

    private List<Integer> getFormatList(JSONObject args) throws JSONException {
        List<Integer> result = new ArrayList<Integer>();
        String formatList = args.getString("fmt_list");
        String[] formats = formatList.split(",");
        for (String format : formats) {
            String[] tokens = format.split("/");
            String formatId = tokens[0];
            result.add(Integer.parseInt(formatId));
        }
        return result;
    }
    
    
}
