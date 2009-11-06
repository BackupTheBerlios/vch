package de.berlios.vch.parser.zdf;

import java.util.Comparator;

import com.sun.syndication.feed.synd.SyndEnclosure;

public class SyndEnclosureComparator implements Comparator<SyndEnclosure> {

    @Override
    public int compare(SyndEnclosure o1, SyndEnclosure o2) {
        if(o1.getUrl().endsWith(".asx") && o2.getUrl().endsWith("mov")) {
            return 1;
        } else if(o1.getUrl().endsWith("mov") && o1.getUrl().endsWith("asx")) {
            return -1;
        }
        
        return 0;
    }
}
