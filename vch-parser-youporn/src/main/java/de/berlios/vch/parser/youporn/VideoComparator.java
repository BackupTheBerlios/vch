package de.berlios.vch.parser.youporn;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Compares two videos according to their type, size and quality
 * 
 * @author <a href="mailto:hampelratte@users.berlios.de">hampelratte@users.berlios.de</a>
 */
public class VideoComparator implements Comparator<Video> {

    private Map<String, Integer> typePriorities = new HashMap<String, Integer>();

    public VideoComparator() {
        // mp4 has higher priority than mpg
        typePriorities.put("mp4", 2);
        typePriorities.put("mpg", 1);
        typePriorities.put("unknown", 0);
    }

    @Override
    public int compare(Video vt1, Video vt2) {
        if (typePriorities.get(vt1.getType()) > typePriorities.get(vt2.getType())) {
            return -1;
        } else if (typePriorities.get(vt1.getType()) < typePriorities.get(vt2.getType())) {
            return 1;
        } else if (vt1.getHeight() > vt2.getHeight()) {
            return -1;
        } else if (vt1.getHeight() < vt2.getHeight()) {
            return 1;
        } else if (vt1.getBitrate() > vt2.getBitrate()) {
            return -1;
        } else if (vt1.getBitrate() < vt2.getBitrate()) {
            return 1;
        }

        return 0;
    }
}
