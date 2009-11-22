package de.berlios.vch.download;

import java.io.File;

import de.berlios.vch.parser.IVideoPage;

public interface Download extends Runnable {
    public enum Status {
        STOPPED,
        WAITING,
        STARTING,
        DOWNLOADING,
        FINISHED,
        CANCELED,
        FAILED
    }

    /**
     * 
     * @return the path to file, where the download will be saved. May be null.
     *         In this case the data will be discarded.
     */
    public String getLocalFile();
    
    /**
     * Returns the progress of the download as a percentage value (0-100)
     * @return the progress percentage or -1 if the progress is unknown
     */
    public int getProgress();
    
    /**
     * Cancels the download. 
     * Downloaded data will be deleted
     */
    public void cancel();
    
    /**
     * Get the download speed
     * @return the throughput in kbyte/s
     */
    public float getSpeed();
    
    /**
     * Returns if this download can be paused and continued at a later time
     * @return if this download can be paused and continued at a later time
     */
    public boolean isPauseSupported();
    
    /**
     * Stops/pauses the download. 
     * The download can be restarted at a later time
     */
    public void stop();
    
    public void setStatus(Status status);
    
    public Status getStatus();
    
    public IVideoPage getVideoPage();
    
    public boolean isStartable();
    
    public boolean isRunning();
    
    public String getId();
    
    public void setId(String id);
    
    public void addDownloadStateListener(DownloadStateListener listener);
    
    public void setDestinationDir(File destination);
}
