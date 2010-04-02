package de.berlios.vch.download.sorting;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.berlios.vch.download.jaxb.DownloadDTO;
import de.berlios.vch.i18n.Messages;

public class ByTitle implements SortStrategy {

    private Messages messages;
    
    public ByTitle(Messages messages) {
        this.messages = messages;
    }
    
    @Override
    public String getName() {
        return messages.translate("I18N_SORT_BY_TITLE");
    }

    @Override
    public void sort(List<DownloadDTO> downloads) {
        Collections.sort(downloads, new Comparator<DownloadDTO>() {
            @Override
            public int compare(DownloadDTO o1, DownloadDTO o2) {
                if(o1 == null) return -1;
                if(o2 == null) return 1;
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });
    }
}
