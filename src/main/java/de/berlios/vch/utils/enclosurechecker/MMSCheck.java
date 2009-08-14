package de.berlios.vch.utils.enclosurechecker;

import java.net.URI;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.hampelratte.net.mms.data.MMSMediaPacket;
import org.hampelratte.net.mms.data.MMSPacket;
import org.hampelratte.net.mms.io.RemoteException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.berlios.vch.download.MMSDownload;

public class MMSCheck extends MMSDownload {
    
    private static transient Logger logger = LoggerFactory.getLogger(MMSCheck.class);
    
    private boolean available = false;
    
    public MMSCheck(URI uri) {
        super(uri);
    }
    
    public void packetReceived(MMSPacket mmspacket) {
        if(Thread.currentThread().isInterrupted()) {
            cancel();
        }
        
        if(mmspacket instanceof MMSMediaPacket) {
            available = true;
            cancel();
        }
    }

    public boolean isAvailable() {
        return available;
    }
    
    @Override
    public void exceptionCaught(IoSession session, Throwable t) throws Exception {
        if(t.getCause() instanceof RemoteException) {
            RemoteException re = (RemoteException) t.getCause();
            switch (re.getHr()) {
            case 0x80070002:
                logger.warn("Enclosure doesn't seem to be available anymore", re);
                break;
            default:
                logger.warn("MMS protocol error occured. Enclosure doesn't seem to be available anymore");
                break;
            }
        }
        cancel();
    }
    
    @Override
    public String getLocalFile() {
        return null; // don't create a file, but discard the downloaded data
    }
    
    @Override
    public void stop() {
        setStatus(Status.STOPPED);
        if(client != null) {
            client.disconnect(new IoFutureListener<IoFuture>() {
                @Override
                public void operationComplete(IoFuture arg0) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }
}
