package de.berlios.vch.download;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpDownload extends AbstractDownload {

    private static transient Logger logger = LoggerFactory.getLogger(HttpDownload.class);
    
    private File file;
    
    private boolean supportsPause;
    
    private float speed;
    
    public HttpDownload(URI uri) {
        super(uri);
    }

    private int progress = -1;
    
    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public void run() {
        supportsPause = checkSupportsPause();
        
        HttpURLConnection con = null;
        RandomAccessFile out = null;
        try {
            file = new File(getLocalFile());
            
            // create nfo file
            try {
                createInfoFile(file, getItem());
            } catch (Throwable t) {
                logger.warn("Couldn't write .nfo file for " + getLocalFile(), t);
            }
            
            // try to restart download if the file already exists
            long fileLength = 0;
            if(file.exists()) {
                fileLength = file.length();
            }
            out = new RandomAccessFile(file, "rw");
            URI uri = getSource();
            con = (HttpURLConnection) uri.toURL().openConnection();
            con.addRequestProperty("Range", "bytes=" + fileLength + "-");

            logger.debug("Server responded with: " + con.getResponseCode() + " " + con.getResponseMessage());
            if(con.getResponseCode() == 206) { // partial content
                // partial content is supported, we can append to the file
                setLoadedBytes(fileLength);
                out.seek(getLoadedBytes());
            } else {
                // partial content is not supported
                // make a normal request
                setLoadedBytes(0);
                con = (HttpURLConnection) uri.toURL().openConnection();
            }
            
            long contentLength = -1;
            if(con.getHeaderField("Content-Length") != null) {
                // if it is a partial download, we have to add the already 
                // downloaded bytes to get the original size.
                // otherwise getLoadedBytes is 0 and doesn't affect the value
                contentLength = Long.parseLong(con.getHeaderField("Content-Length")) + getLoadedBytes();
            }
            
            // check if the file is complete already 
            if(contentLength == fileLength) {
                logger.debug("File is complete already. No need to download anything");
                setStatus(Status.FINISHED);
                progress = 100;
                return;
            }

            
            SpeedometerInputStream in = new SpeedometerInputStream(con.getInputStream());
            logger.debug("Download started for " + uri.toString() + " at position " + getLoadedBytes());
            setStatus(Status.DOWNLOADING);
            byte[] b = new byte[10240];
            int length = -1;
            while( (length = in.read(b)) > -1) {
                if(Thread.currentThread().isInterrupted() || getStatus() == Status.STOPPED) {
                    setStatus(Status.STOPPED);
                    return;
                }
                
                out.write(b, 0, length);
                increaseLoadedBytes(length);
                
                // calculate the progress
                if(contentLength > -1) {
                    progress = (int) ((double)getLoadedBytes() / (double)contentLength * 100);
                }
                
                // get the speed
                speed = in.getSpeed();
            }
            logger.debug("AbstractDownload finished " + uri.toString());
            setStatus(Status.FINISHED);
        } catch (MalformedURLException e) {
            error("Not a valid URL " + getSource().toString(), e);
            setStatus(Status.FAILED);
            setException(e);
        } catch (IOException e) {
            error("Couldn't download file from " + getSource().toString(), e);
            setStatus(Status.FAILED);
            setException(e);
        } finally {
            if(con != null) {
                con.disconnect();
            }
            
            if(out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    logger.warn("Couldn't close file", e);
                }
            }
        }
    }

    private boolean checkSupportsPause() {
        boolean support = false;
        URI uri = getSource();
        HttpURLConnection con = null;
        try {
            con = (HttpURLConnection) uri.toURL().openConnection();
            con.addRequestProperty("Range", "bytes=1-");
            support = con.getResponseCode() == 206;
            logger.debug("Pause download supported: " + support);
        } catch (Exception e) {
            logger.warn("Failed to check support for pausing the download", e);
        } finally {
            if(con != null) {
                con.disconnect();
            }
        }
        
        return support;
    }

    @Override
    public void cancel() {
        // delete the video file
        if(file != null && file.exists()) {
            boolean deleted = file.delete();
            if(!deleted) {
                logger.warn("Couldn't delete file " + file.getAbsolutePath());
            }
        }
        
        // delete the nfo file
        deleteInfoFile(file);
    }

    @Override
    public boolean isPauseSupported() {
        return supportsPause;
    }

    @Override
    public float getSpeed() {
        if(getStatus() == Status.DOWNLOADING) {
            return speed;
        } else {
            return -1;
        }
    }

    @Override
    public void stop() {
        setStatus(Status.STOPPED);
    }

    @Override
    public String getLocalFile() {
        URI uri = getSource();
        String path = uri.getPath();
        String _file = path.substring(path.lastIndexOf('/') + 1);
        String title = getItem().getTitle().replaceAll("[^a-zA-z0-9]", "_");
        return getDestinationDir() + File.separator + title + "_" + _file;
    }
    
    
}