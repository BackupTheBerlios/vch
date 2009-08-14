package de.berlios.vch.download;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SpeedometerInputStream extends FilterInputStream {

    private int bytesRead;
    private long lastTime;
    private float speed;
    private int interval = 1;
    
    public SpeedometerInputStream(InputStream in) {
        super(in);
    }

    @Override
    public int read() throws IOException {
        int read = super.read();
        bytesRead++;
        calculateSpeed();
        return read;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = super.read(b, off, len);
        bytesRead += read;
        calculateSpeed();
        return read;
    }

    @Override
    public int read(byte[] b) throws IOException {
        int read = super.read(b);
        // dont do speed calculation here, because
        // this method calls read(byte[] b, int off, int len)
        // calculation will be done there
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = super.skip(n);
        bytesRead += skipped;
        calculateSpeed();
        return skipped;
    }
    
    private void calculateSpeed() {
        long now = System.nanoTime();
        if(lastTime == 0) {
            speed = -1;
            lastTime = now;
            return;
        }
        
        long diff = now - lastTime;
        if(diff > (interval * 1000000000l)) {
            double bytesPerNanoSec = (double)bytesRead / (double)diff;
            double bytesPerSec = bytesPerNanoSec * 1000000000l;
            float kilobytesPerSec = (float)bytesPerSec / 1024f;
            speed = kilobytesPerSec;
            
            // reset values
            bytesRead = 0;
            lastTime = now;
        }
    }
    
    /**
     * 
     * @return the speed in KiB/s for the last {@link #interval} or -1, if no
     *         speed is available currently
     */
    public float getSpeed() {
        return speed;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }
}
