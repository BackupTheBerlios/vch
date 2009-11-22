package de.berlios.vch.download;

import java.util.EventListener;

public interface DownloadStateListener extends EventListener {
    
    public void downloadStateChanged(AbstractDownload download);
}
