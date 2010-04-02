package de.berlios.vch.download.sorting;

import java.util.List;

import de.berlios.vch.download.jaxb.DownloadDTO;

public interface SortStrategy {
    public String getName();
    
    public void sort(List<DownloadDTO> downloads);
}
