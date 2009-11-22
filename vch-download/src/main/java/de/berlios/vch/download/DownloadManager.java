package de.berlios.vch.download;

import java.util.List;
import java.util.prefs.Preferences;

import de.berlios.vch.download.jaxb.DownloadDTO;
import de.berlios.vch.parser.IVideoPage;

public interface DownloadManager {

    public List<Download> getActiveDownloads();
    
    public List<DownloadDTO> getFinishedDownloads();
    
    public void stopDownload(String id);

    public void startDownload(String id);

    public void cancelDownload(String id);

    public void startDownloads();

    public void stopDownloads();

    public void downloadItem(IVideoPage page) throws Exception;

    public void deleteDownload(String id);
    
    public void init(Preferences prefs);
    
    public void stop();
}
