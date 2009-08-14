package de.berlios.vch.utils.comparator;

import java.util.Comparator;

import de.berlios.vch.model.Channel;

public class ChannelTitleComparator implements Comparator<Channel> {

    @Override
    public int compare(Channel c1, Channel c2) {
        if(c1 == null || c2 == null) {
            return 0;
        }
        
        return c1.getTitle().toLowerCase().compareTo(c2.getTitle().toLowerCase());
    }

}
