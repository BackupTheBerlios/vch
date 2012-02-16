package de.berlios.vch.parser.youtube;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

    private URI parseOnDemand(String videoLink) {
        logger.log(LogService.LOG_DEBUG, "Getting video link for " + videoLink);

        URI medialink = null;
        try {
            Map<String, String> params = new HashMap<String, String>();
            params.put("Accept-Encoding", "gzip");
            String pageContent = HttpUtils.get(videoLink, params, "UTF-8");

            StringTokenizer st = new StringTokenizer(pageContent, "\n");
            while (st.hasMoreTokens()) {
                String line = st.nextToken();
                if (line.contains("yt.playerConfig")) {
                    logger.log(LogService.LOG_DEBUG, line);
                    int openingBracket = line.indexOf('{');
                    String jsonObjectString = line.substring(openingBracket).trim();
                    JSONObject jsonObject = new JSONObject(jsonObjectString);
                    // for (Iterator iterator = jsonObject.sortedKeys(); iterator.hasNext();) {
                    // String key = (String) iterator.next();
                    // System.out.println(key + " = " + jsonObject.get(key));
                    // }
                    JSONObject args = (JSONObject) jsonObject.get("args");

                    List<Integer> formatList = getFormatList(args);
                    logger.log(LogService.LOG_DEBUG, "The following formats are available " + formatList);
                    int format = prefs.getInt("video.quality", 34);
                    if (!formatList.contains(format)) {
                        logger.log(LogService.LOG_INFO, "Video is not available in preferred format " + format + ". Using format " + formatList.get(0));
                        format = formatList.get(0);
                    }

                    Map<Integer, String> streamUris = getFormatStreamMap(formatList, args);
                    String streamUri = streamUris.get(format);
                    medialink = normalizeUri(new URI(streamUri));
                    normalizeUri(medialink);

                    // parse duration
                    try {
                        long duration = args.getLong("length_seconds");
                        setDuration(duration);
                    } catch (Exception e) {
                        logger.log(LogService.LOG_WARNING, "Couldn't parse video duration");
                    }

                    break;
                }
            }
        } catch (Exception e) {
            logger.log(LogService.LOG_ERROR, "Couldn't parse Youtube video page", e);
        }

        return medialink;
    }

    /**
     * Some URIs have duplicate parameters and youtube doesn't seem to like that. So we have to clean that up.
     * 
     * @param streamUri
     * @throws URISyntaxException
     */
    private URI normalizeUri(URI streamUri) throws URISyntaxException {
        // remove duplicate params by putting them into a map and
        // rebuild the query string from this map
        Map<String, String> params = new HashMap<String, String>();
        String query = streamUri.getQuery();
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            params.put(keyValue[0], keyValue[1]);
        }
        StringBuilder queryBuilder = new StringBuilder();
        for (Entry<String, String> entry : params.entrySet()) {
            queryBuilder.append(entry.getKey()).append('=').append(entry.getValue()).append('&');
        }
        queryBuilder.deleteCharAt(queryBuilder.length() - 1);
        query = queryBuilder.toString();

        String scheme = streamUri.getScheme();
        String auth = streamUri.getAuthority();
        String path = streamUri.getPath();
        String fragment = streamUri.getFragment();

        // rebuild the uri with the clean query string
        String uri = scheme + "://" + auth + path;
        uri += (query != null && query.length() > 0) ? ('?' + query) : "";
        uri += (fragment != null && fragment.length() > 0) ? ('#' + fragment) : "";
        return new URI(uri);
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

    private Map<Integer, String> getFormatStreamMap(List<Integer> formatList, JSONObject args) throws JSONException, UnsupportedEncodingException {
        Map<Integer, String> result = new HashMap<Integer, String>();
        String formatStreamMap = args.getString("url_encoded_fmt_stream_map");
        String[] formats = formatStreamMap.split(",");
        for (int i = 0; i < formats.length; i++) {
            String format = URLDecoder.decode(formats[i], "UTF-8");
            String streamUri = format.substring(4);
            if (streamUri.contains(";")) {
                streamUri = streamUri.substring(0, streamUri.indexOf(';'));
            }
            logger.log(LogService.LOG_DEBUG, "Found stream uri " + URLDecoder.decode(streamUri, "UTF-8"));
            result.put(formatList.get(i), streamUri);
        }
        return result;
    }
}
