package de.berlios.vch.parser;

import java.util.Comparator;

public class WebPageTitleComparator implements Comparator<IWebPage> {

    @Override
    public int compare(IWebPage o1, IWebPage o2) {
        return o1.getTitle().toLowerCase().compareTo(o2.getTitle().toLowerCase());
    }

}
