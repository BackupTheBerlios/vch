package de.berlios.vch.download.sorting;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

import de.berlios.vch.download.jaxb.DownloadDTO;

public class ByTitle implements SortStrategy {

    private ResourceBundle rb;

    public ByTitle(ResourceBundle rb) {
        this.rb = rb;
    }

    @Override
    public String getName() {
        return rb.getString("I18N_SORT_BY_TITLE");
    }

    @Override
    public void sort(List<DownloadDTO> downloads) {
        Collections.sort(downloads, new Comparator<DownloadDTO>() {
            @Override
            public int compare(DownloadDTO o1, DownloadDTO o2) {
                if (o1 == null) {
                    return -1;
                }
                if (o2 == null) {
                    return 1;
                }
                return o1.getTitle().compareTo(o2.getTitle());
            }
        });
    }
}
