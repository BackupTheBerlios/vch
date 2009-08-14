package de.berlios.vch.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.Config;

public class RtmpDownload extends AbstractDownload {

    private static transient Logger logger = LoggerFactory.getLogger(RtmpDownload.class);
    
    private File file;
    
    private float speed;
    
    public RtmpDownload(URI uri) {
        super(uri);
    }

    private int progress = -1;
    
    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public void run() {
        // try to start flvstreamer
        String flvstreamerPath = Config.getInstance().getProperty("flvstreamer.path");
        Runtime rt = Runtime.getRuntime();
        
        // initialize output stream
        if(getOutputStream() == null) {
            try {
                File localFile = new File(getLocalFile());
                setOutputStream(new FileOutputStream(localFile));
                
                // create the nfo file
                try {
                    createInfoFile(localFile, getItem());
                } catch (IOException e) {
                    logger.warn("Couldn't write .nfo file for " + getLocalFile(), e);
                }
            } catch (FileNotFoundException e) {
                logger.error("Couldn't open file", e);
                setStatus(Status.FAILED);
                setException(e);
                return;
            }
        }
        
        try {
            Process p = rt.exec(flvstreamerPath + " -r " + getSource().toString());

            // start streaming into the outputstream
            SpeedometerInputStream in = new SpeedometerInputStream(p.getInputStream());
            logger.debug("Download started for " + getSource().toString() + " at position " + getLoadedBytes());
            setStatus(Status.DOWNLOADING);
            byte[] b = new byte[10240];
            int length = -1;
            while( (length = in.read(b)) > -1) {
                if(Thread.currentThread().isInterrupted() || getStatus() == Status.STOPPED) {
                    setStatus(Status.STOPPED);
                    p.destroy();
                    return;
                }
                
                getOutputStream().write(b, 0, length);
                increaseLoadedBytes(length);
                
                // TODO calculate the progress
//            if(contentLength > -1) {
//                progress = (int) ((double)getLoadedBytes() / (double)contentLength * 100);
//            }
                
                // get the speed
                speed = in.getSpeed();
            }
            
            int result = p.waitFor();
            if(result == 0) {
                // TODO genauere Fehlermeldung
                logger.debug("AbstractDownload finished " + getSource().toString());
                setStatus(Status.FINISHED);
            } else {
                throw new Exception("flvstreamer finished with errors");
            }
        } catch (Exception e) {
            logger.error("Couldn't download " + getSource(), e);
            setStatus(Status.FAILED);
            setException(e);
        }
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
        return false;
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