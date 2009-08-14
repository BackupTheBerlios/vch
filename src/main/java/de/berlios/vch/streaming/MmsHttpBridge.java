package de.berlios.vch.streaming;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.hampelratte.net.mms.asf.io.ASFInputStream;
import org.hampelratte.net.mms.asf.objects.ASFFilePropertiesObject;
import org.hampelratte.net.mms.asf.objects.ASFToplevelHeader;
import org.hampelratte.net.mms.client.MMSClient;
import org.hampelratte.net.mms.client.MMSNegotiator;
import org.hampelratte.net.mms.client.listeners.MMSMessageListener;
import org.hampelratte.net.mms.client.listeners.MMSPacketListener;
import org.hampelratte.net.mms.data.MMSHeaderPacket;
import org.hampelratte.net.mms.data.MMSMediaPacket;
import org.hampelratte.net.mms.data.MMSPacket;
import org.hampelratte.net.mms.messages.MMSMessage;
import org.hampelratte.net.mms.messages.client.Connect;
import org.hampelratte.net.mms.messages.client.ConnectFunnel;
import org.hampelratte.net.mms.messages.client.OpenFile;
import org.hampelratte.net.mms.messages.client.ReadBlock;
import org.hampelratte.net.mms.messages.client.StreamSwitch;
import org.hampelratte.net.mms.messages.server.ReportStreamSwitch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.net.httpserver.HttpExchange;

// TODO use MMSDownload
public class MmsHttpBridge implements MMSMessageListener, MMSPacketListener, IoHandler, StreamBridge {

    private static transient Logger logger = LoggerFactory.getLogger(MmsHttpBridge.class);
    
    private String host;
    private int port = 1755;
    private String path;
    private String file;
    
    private MMSClient client;
    
    private MMSNegotiator negotiator;
    
    private MMSHeaderPacket hp;
    
    int progress;
    
    private long packetCount;
    
    private long packetReadCount;
    
    private long contentLength;
    
    private URI uri;
    
    private HttpExchange exchange;
    
    private boolean running = false;
    
    public MmsHttpBridge(URI uri, HttpExchange exchange) {
        this.uri = uri;
        this.exchange = exchange;
    }
    
    private MMSNegotiator createNegotiator() {
        MMSNegotiator negotiator = new MMSNegotiator();
        
        // connect
        Connect connect = new Connect();
        connect.setPlayerInfo("NSPlayer/7.0.0.1956");
        connect.setGuid(UUID.randomUUID().toString());
        connect.setHost(host);
        negotiator.setConnect(connect);
        
        // connect funnel
        ConnectFunnel cf = new ConnectFunnel();
        cf.setIpAddress("192.168.0.1");
        cf.setProtocol("TCP");
        cf.setPort("1037");
        negotiator.setConnectFunnel(cf);
        
        // open file
        OpenFile of = new OpenFile();
        of.setFileName(path + file);
        negotiator.setOpenFile(of);
        
        // read block
        ReadBlock rb = new ReadBlock();
        negotiator.setReadBlock(rb);
        
        // stream switch
        StreamSwitch ss = new StreamSwitch();
        ss.addStreamSwitchEntry(ss.new StreamSwitchEntry(0xFFFF, 1, 0));
        ss.addStreamSwitchEntry(ss.new StreamSwitchEntry(0xFFFF, 2, 0));
        negotiator.setStreamSwitch(ss);
        
        return negotiator;
    }

    private void extractConnectInfo() {
        // host
        host = uri.getHost();
        
        // port
        port = uri.getPort();
        
        // directory path
        path = uri.getPath();
        if(path.startsWith("/")) {
            path = path.substring(1);
        }
        path = path.substring(0, path.lastIndexOf('/'));
        
        // file
        String p = uri.getPath();
        file = p.substring(p.lastIndexOf('/'));
        if(uri.getQuery() != null) {
            file += "?" + uri.getQuery();
        }
        
        logger.debug(host);
        logger.debug(Integer.toString(port));
        logger.debug(path);
        logger.debug(file);
    }

    public void startStream() {
        running = true;
        extractConnectInfo();
        
        // create negotiator
        negotiator = createNegotiator();
        
        // create client
        port = port < 1 ? 1755 : port;
        client = new MMSClient(host, port, negotiator);
        negotiator.setClient(client);
        
        // register message listeners
        client.addMessageListener(this);
        client.addPacketListener(this);
        client.addAdditionalIoHandler(this);
        
        // open the connection
        try {
            client.connect();
        } catch (Exception e1) {
            logger.error("Couldn't connect to host", e1);
        }
        
        // since mina runs in another thread, we have to wait for
        // mina to finish, otherwise the outputstream to the client would be
        // closed immediately
        while(running) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    
    public void stop() {
        if(client != null) {
            client.disconnect(new IoFutureListener<IoFuture>() {
                @Override
                public void operationComplete(IoFuture arg0) {
                    running = false;
                }
            });
        }
    }
    
    @Override
    public void messageReceived(MMSMessage mmscommand) {
        if(Thread.currentThread().isInterrupted()) {
            stop();
        }
        
        if(mmscommand instanceof ReportStreamSwitch) {
            // great, we can start the streaming
            long startPacket = 0;
            
            // check if resuming is supported
            if(negotiator.isResumeSupported()) {
                startPacket = packetReadCount;
            }
            
            // start the streaming
            client.startStreaming(startPacket);
        }
    }

    @Override
    public void packetReceived(MMSPacket mmspacket) {
        if(Thread.currentThread().isInterrupted()) {
            stop();
        }
        
        if(mmspacket instanceof MMSHeaderPacket) {
            hp = (MMSHeaderPacket) mmspacket;
            try {
                ByteArrayInputStream bin = new ByteArrayInputStream(hp.getData());
                ASFInputStream asfin = new ASFInputStream(bin);
                ASFToplevelHeader asfHeader = (ASFToplevelHeader) asfin.readASFObject();
                ASFFilePropertiesObject fileprops = (ASFFilePropertiesObject) asfHeader.getNestedHeader(ASFFilePropertiesObject.class);
                if(fileprops != null) {
                    packetCount = fileprops.getDataPacketCount();
                    logger.debug(fileprops.toString());
                    contentLength = fileprops.getFileSize();
                } else {
                    packetCount = -1;
                    contentLength = 1000; // FIXME can we set 1000 as a dummy value to avoid chuncked encoding?
                }
            } catch (Exception e) {
                logger.error("Couldn't analyze ASF header", e);
            }

            // send http header
            try {
                exchange.getResponseHeaders().add("Content-type", "video/wmv");
                exchange.sendResponseHeaders(200, contentLength);
            } catch (IOException e) {
                logger.error("Couldn't send response headers", e);
                stop();
                return;
            }
            
            forward(hp);
        } else if(mmspacket instanceof MMSMediaPacket) {
            packetReadCount++;
            progress = (int) ((double)packetReadCount / (double)packetCount * 100);
            forward(mmspacket);
        }
    }
    
    private void forward(MMSPacket packet) {
        try {
            exchange.getResponseBody().write(packet.getData());
        } catch (IOException e) {
            logger.warn("Couldn't write MMS packet to stream. Stopping bridging.", e);
            stop();
        }
    }

    @Override
    public void exceptionCaught(IoSession arg0, Throwable arg1) throws Exception {
        logger.warn("MMS protocol error occured. Restarting from last valid packet");
        client.disconnect(new IoFutureListener<IoFuture>() {
            @Override
            public void operationComplete(IoFuture arg0) {
                startStream();
            }
        });
    }

    @Override
    public void messageReceived(IoSession arg0, Object msg) throws Exception {}

    @Override
    public void messageSent(IoSession arg0, Object arg1) throws Exception {}

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        running = false;
    }

    @Override
    public void sessionCreated(IoSession arg0) throws Exception {}

    @Override
    public void sessionIdle(IoSession arg0, IdleStatus arg1) throws Exception {}

    @Override
    public void sessionOpened(IoSession arg0) throws Exception {}
}