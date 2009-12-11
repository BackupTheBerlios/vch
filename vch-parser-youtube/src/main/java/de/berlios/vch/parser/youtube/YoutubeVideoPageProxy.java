package de.berlios.vch.parser.youtube;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.json.JSONObject;
import org.osgi.service.log.LogService;

import de.berlios.vch.http.client.HttpUtils;
import de.berlios.vch.parser.VideoPage;


public class YoutubeVideoPageProxy extends VideoPage {
    
    private LogService logger;
    
    public YoutubeVideoPageProxy(LogService logger) {
        this.logger = logger;
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
            String pageConent = HttpUtils.get(videoLink, params, "UTF-8");
            
            StringTokenizer st = new StringTokenizer(pageConent, "\n");
            while(st.hasMoreTokens()) {
                String line = st.nextToken();
                if (line.contains("SWF_ARGS")) {
                    int firstColon = line.indexOf(':');
                    String jsonObjectString = line.substring(firstColon+1).trim();
                    JSONObject jsonObject = new JSONObject(jsonObjectString);
//                    for (Iterator iterator = jsonObject.sortedKeys(); iterator.hasNext();) {
//                        String key = (String) iterator.next();
//                        System.out.println(key + " = " + jsonObject.get(key));
//                    }
                    String video_id = jsonObject.getString("video_id");
                    String t = jsonObject.getString("t");
                    medialink = new URI("http://www.youtube.com/get_video?video_id=" + video_id + "&t=" + t);
                    
                    // parse duration
                    try {
                        long duration = jsonObject.getLong("length_seconds");
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
}
