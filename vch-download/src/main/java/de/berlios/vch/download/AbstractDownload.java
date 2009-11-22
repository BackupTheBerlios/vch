package de.berlios.vch.download;

import java.io.File;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.osdserver.ID;
import de.berlios.vch.parser.IVideoPage;


public abstract class AbstractDownload implements Download {

    private static transient Logger logger = LoggerFactory.getLogger(AbstractDownload.class);
    
    private String id = ID.randomId();

    private URI source;
    
    private File destinationDir;
    
    private long loadedBytes;
    
    private IVideoPage videoPage;
    
    private Throwable exception;
    
    private List<DownloadStateListener> listeners = new ArrayList<DownloadStateListener>();
    
    private OutputStream outputStream;
    
    protected String host;
    protected int port;
    protected String path;
    protected String file;
    
    private Status status = Status.WAITING;
    
    public AbstractDownload(IVideoPage video) {
        this.videoPage = video;
        setSource(video.getVideoUri());
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

    public Throwable getException() {
        return exception;
    }

    public void setException(Throwable exception) {
        this.exception = exception;
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
    
    public IVideoPage getVideoPage() {
        return videoPage;
    }
}
