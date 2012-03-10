package de.berlios.vch.parser.ard;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import de.berlios.vch.parser.ard.VideoItemPageParser.VideoType;

/**
 * Compares two videos according to their type and quality
 * 
 * @author <a href="mailto:hampelratte@users.berlios.de">hampelratte@users.berlios.de</a>
 */
public class VideoTypeComparator implements Comparator<VideoType> {

    private Map<Integer, Integer> typePriorities = new HashMap<Integer, Integer>();

    public VideoTypeComparator() {
        // flash has higher priority than wmv
        typePriorities.put(0, 2); // flash
        typePriorities.put(1, 1); // http
        typePriorities.put(2, 0); // wmv
    }

    @Override
    public int compare(VideoType vt1, VideoType vt2) {
        if (typePriorities.get(vt1.getFormat()) > typePriorities.get(vt2.getFormat())) {
            return -1;
        } else if (typePriorities.get(vt1.getFormat()) < typePriorities.get(vt2.getFormat())) {
            return 1;
        } else if (vt1.getQuality() > vt2.getQuality()) {
            return -1;
        } else if (vt1.getQuality() < vt2.getQuality()) {
            return 1;
        }

        return 0;
    }
}
