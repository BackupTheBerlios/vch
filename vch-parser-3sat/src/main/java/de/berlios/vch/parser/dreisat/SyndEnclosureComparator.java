package de.berlios.vch.parser.dreisat;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.sun.syndication.feed.synd.SyndEnclosure;

/**
 * Compares two SyndEnclosures according to their type WMV -> quicktime quicktime -> real HQ -> LQ
 * 
 * @author <a href="mailto:hampelratte@users.berlios.de">hampelratte@users.berlios.de</a>
 */
public class SyndEnclosureComparator implements Comparator<SyndEnclosure> {

    private Map<String, Integer> typePriorities = new HashMap<String, Integer>();
    private Map<String, Integer> qualiPriorities = new HashMap<String, Integer>();

    public SyndEnclosureComparator() {
        //@formatter:off
        typePriorities.put("video/vnd.rn-realvideo", 0);
        typePriorities.put("video/3gpp",             1);
        typePriorities.put("video/quicktime",        2);
        typePriorities.put("video/x-ms-asf",         3);
        typePriorities.put("video/webm",             4);
        
        qualiPriorities.put("unknown",  0);
        qualiPriorities.put("56",       1);
        qualiPriorities.put("300",      2);
        qualiPriorities.put("smart",    3);
        qualiPriorities.put("veryhigh", 4);
        //@formatter:on
    }

    @Override
    public int compare(SyndEnclosure enc1, SyndEnclosure enc2) {
        String type1 = enc1.getType().toLowerCase();
        String type2 = enc2.getType().toLowerCase();
        String quali1 = getQuality(enc1);
        String quali2 = getQuality(enc2);

        if (getTypePriority(type1) > getTypePriority(type2)) {
            return 1;
        } else if (getTypePriority(type1) < getTypePriority(type2)) {
            return -1;
        } else if (qualiPriorities.get(quali1) > qualiPriorities.get(quali2)) {
            return 1;
        } else if (qualiPriorities.get(quali1) < qualiPriorities.get(quali2)) {
            return -1;
        }

        return 0;
    }

    public int getTypePriority(String type) {
        Integer prio = typePriorities.get(type);
        return prio == null ? 0 : prio;
    }

    private String getQuality(SyndEnclosure enc) {
        String uri = enc.getUrl();
        if (uri.contains("/56/")) {
            return "56";
        } else if (uri.contains("/300/")) {
            return "300";
        } else if (uri.contains("/veryhigh/")) {
            return "veryhigh";
        } else if (uri.contains("/smart/")) {
            return "smart";
        } else {
            return "unknown";
        }
    }
}