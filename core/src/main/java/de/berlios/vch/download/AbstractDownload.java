package de.berlios.vch.download;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;
import de.berlios.vch.model.Item;
import de.berlios.vch.utils.StringUtils;


public abstract class AbstractDownload implements Runnable {

    private static transient Logger logger = LoggerFactory.getLogger(AbstractDownload.class);
    
    private String id;

    private URI source;
    
    private File destinationDir;
    
    private long loadedBytes;
    
    private Item item;
    
    private Throwable exception;
    
    private List<DownloadStateListener> listeners = new ArrayList<DownloadStateListener>();
    
    private OutputStream outputStream;
    
    protected String host;
    protected int port;
    protected String path;
    protected String file;
    
    public enum Status {
        STOPPED,
        WAITING,
        STARTING,
        DOWNLOADING,
        FINISHED,
        CANCELED,
        FAILED
    }
    
    private Status status = Status.WAITING;
    
    public AbstractDownload(URI uri) {
        setSource(uri);
        extractConnectInfo();
    }
    
    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URI getSource() {
        return source;
    }

    public void setSource(URI source) {
        this.source = source;
    }

    public File getDestinationDir() {
        return destinationDir;
    }

    public void setDestinationDir(File destination) {
        this.destinationDir = destination;
    }

    /**
     * Returns the progress of the download as a percentage value (0-100)
     * @return the progress percentage or -1 if the progress is unknown
     */
    public abstract int getProgress();
    
    /**
     * Stops/pauses the download. 
     * The download can be restarted with {@link #start()}
     */
    public abstract void stop();
    
    /**
     * Cancels the download. 
     * Downloaded data will be deleted
     */
    public abstract void cancel();

    /**
     * Returns the number of bytes, which have been downloaded
     * @return the number of bytes, which have been downloaded
     */
    public long getLoadedBytes() {
        return loadedBytes;
    }

    /**
     * Set the number of bytes, which have been downloaded
     * @param loadedBytes
     */
    public void setLoadedBytes(long loadedBytes) {
        this.loadedBytes = loadedBytes;
    }
    
    public void increaseLoadedBytes(long loadedBytes) {
        this.loadedBytes += loadedBytes;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
        fireStateChanged();
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Item getItem() {
        return item;
    }

    /**
     * Returns if this download can be paused and continued at a later time
     * @return if this download can be paused and continued at a later time
     */
    public abstract boolean isPauseSupported();
    
    /**
     * Get the download speed
     * @return the throughput in kbyte/s
     */
    public abstract float getSpeed();

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
    }
    
    public String getExceptionString() {
        return StringUtils.stackTraceToString(getException());
    }
    
    protected void error(String msg, Exception e) {
        logger.error(msg, e);
        setException(e);
        setStatus(Status.FAILED);
    }
    
    private void extractConnectInfo() {
        URI uri = getSource();
        
        // host
        host = uri.getHost();
        
        // port
        port = uri.getPort();
        
        // directory path
        path = uri.getPath();
        if(path.startsWith("/")) {
            path = path.substring(1);
        }
        
        if(path.lastIndexOf('/') > 0) {
            path = path.substring(0, path.lastIndexOf('/'));
        }
        
        // file
        String p = uri.getPath();
        if(p.lastIndexOf('/') > 0) {
            file = p.substring(p.lastIndexOf('/'));
        } else {
            file = p;
        }
        if(uri.getQuery() != null) {
            file += "?" + uri.getQuery();
        }
    }
    
    public boolean isRunning() {
        return getStatus() == Status.STARTING || getStatus() == Status.DOWNLOADING;
    }
    
    public boolean isStartable() {
        return !isRunning() && status != Status.WAITING;
    }
    
    /**
     * 
     * @return the path to file, where the download will be saved. May be null.
     *         In this case the data will be discarded.
     */
    public abstract String getLocalFile();
    
    public void addDownloadStateListener(DownloadStateListener listener) {
        listeners.add(listener);
    }
    
    public void removeDownloadStateListener(DownloadStateListener listener) {
        listeners.remove(listener);
    }
    
    protected void fireStateChanged() {
        for (DownloadStateListener listener : listeners) {
            listener.downloadStateChanged(this);
        }
    }
    
    /**
     * Writes a .nfo file for a given video file and description. The nfo file will have
     * the same name as the video file plus the file extension .nfo
     * @param videoFile The video file to write the nfo file for
     * @param desc The text to write into the nfo file
     * @throws IOException
     */
    protected void createInfoFile(File videoFile, Item item) throws IOException {
        if(item == null || item.getTitle() == null || item.getTitle().length() == 0) {
            logger.info("Item has no title. No .nfo file will be written");
        }
        
        if(videoFile != null) {
            File nfoFile = new File(videoFile.getParentFile(), videoFile.getName() + ".nfo");
            if(!nfoFile.exists()) {
                PrintWriter pw = null;
                try {
                    pw = new PrintWriter(nfoFile, Config.getInstance().getProperty("default.encoding"));
                    pw.write(item.getTitle() + "\n");
                    if(item.getPubDate() != null) pw.write(DateFormat.getDateTimeInstance().format(item.getPubDate()) + "\n");
                    if(item.getDescription() != null) {
                        pw.write("\n" + item.getDescription());
                    }
                } finally {
                    if(pw != null) {
                        pw.close();
                    }
                }
            }
        } else {
            logger.info("Video file is null. No .nfo file will be written");
        }
        
    }
    
    /**
     * Deletes a .nfo file for a given video file, if the nfo file exists
     * @param videoFile
     */
    protected void deleteInfoFile(File videoFile) {
        if(videoFile != null) {
            File nfoFile = new File(videoFile.getParentFile(), videoFile.getName() + ".nfo");
            if(nfoFile != null && nfoFile.exists()) {
                boolean deleted = nfoFile.delete();
                if(!deleted) {
                    logger.warn("Couldn't delete file " + nfoFile.getAbsolutePath());
                }
            }
        }
    }
}
