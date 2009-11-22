package de.berlios.vch.download;

import de.berlios.vch.parser.IVideoPage;

public interface DownloadFactory {
    public Download createDownload(IVideoPage page);
    
    /**
     * @param video
     * @return true, if this DownloadFactory is able to create a Download instance for the given video
     */
    public boolean accept(IVideoPage video);
}
