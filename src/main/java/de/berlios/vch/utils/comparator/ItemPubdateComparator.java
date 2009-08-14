package de.berlios.vch.utils.comparator;

import java.util.Comparator;

import de.berlios.vch.model.Item;

public class ItemPubdateComparator implements Comparator<Item> {

    @Override
    public int compare(Item o1, Item o2) {
        if(o1.getPubDate() == null) {
            return -1;
        } else if(o1.getPubDate() == null) {
            return 1;
        } else {
            return o1.getPubDate().compareTo(o2.getPubDate());
        }
    }

}
