package de.berlios.vch.download.rtmp;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URI;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.osgi.service.log.LogService;

import com.flazr.rtmp.RtmpHandshake;
import com.flazr.rtmp.client.ClientOptions;
import com.flazr.rtmp.client.ClientPipelineFactory;
import com.flazr.util.Utils;

import de.berlios.vch.download.AbstractDownload;
import de.berlios.vch.parser.IVideoPage;


public class RtmpDownload extends AbstractDownload  {

    private LogService logger;
    
    private String host;
    private String app;
    private String streamName;
    private File localFile;
    private URI swfUri;
    
    private int progress;
    private Channel channel;
    
    public RtmpDownload(IVideoPage video, LogService logger) {
        super(video);
        this.logger = logger;
        
        // file
        URI uri = video.getVideoUri();
        String p = uri.getPath();
        file = p.substring(p.lastIndexOf('/'));
        if(uri.getQuery() != null) {
            file += "?" + uri.getQuery();
        }
        
        // host
        host = uri.getHost();
        
        // app
        streamName = (String) video.getUserData().get("streamName");
        app = uri.getPath() + (uri.getQuery() != null ? "?" + uri.getQuery() : "")
                + (uri.getFragment() != null ? "#" + uri.getFragment() : "");
        app = app.substring(1); // cut off the leading /
        int pos = app.indexOf(streamName);
        app = app.substring(0, pos);
        
        // swf verification
        swfUri = (URI) video.getUserData().get("swfUri");
    }
    
    @Override
    public int getProgress() {
        return progress;
    }

    @Override
    public boolean isPauseSupported() {
        return false;
    }

    @Override
    public float getSpeed() {
//        if(getStatus() == Status.DOWNLOADING) {
//            return (float)client.getSpeed();
//        } else {
//            return -1;
//        }
        return -1;
    }

    @Override
    public boolean isRunning() {
        return getStatus() == Status.DOWNLOADING || getStatus() == Status.STARTING;
    }

    @Override
    public void stop() {
        setStatus(Status.STOPPED);
        if(channel != null) {
            channel.close().awaitUninterruptibly();
        }
    }
    
    @Override
    public void cancel() {
        setStatus(Status.CANCELED);
        
        // delete the video file
        if(localFile != null && localFile.exists()) {
            boolean deleted = localFile.delete();
            if(!deleted) {
                logger.log(LogService.LOG_WARNING, "Couldn't delete file " + localFile.getAbsolutePath());
            }
        }
    }

    @Override
    public synchronized String getLocalFile() {
        String filename = file.substring(1);
        
        // cut off query parameters
        if(filename.contains("?")) {
            filename = filename.substring(0, filename.indexOf('?'));
        }
        
        // replace anything other than a-z, A-Z or 0-9 with _
        String title = getVideoPage().getTitle().replaceAll("[^a-zA-z0-9]", "_");
        
        return getDestinationDir() + File.separator + title + "_" + filename;
    }

    @Override
    public void run() {
        try {
            localFile = new File(getLocalFile());
            FileOutputStream fos = new FileOutputStream(localFile);
            OutputStreamFlvWriter writer = new OutputStreamFlvWriter(0, fos, new DownloadListener() {
                @Override
                public void setProgress(int percent) {
                    progress = percent;
                }

                @Override
                public void downloadFailed(Exception e) {
                    stop();
                    setStatus(Status.FAILED);
                    setException(e);
                }

                @Override
                public void downloadFinished() {
                    setStatus(Status.FINISHED);
                }

                @Override
                public void downloadStarted() {
                    setStatus(Status.DOWNLOADING);
                }
            });
            
            ClientOptions options = new ClientOptions(host, app, streamName, getLocalFile());
            if(swfUri != null) {
                try {
                    initSwfVerification(options, swfUri);
                } catch (Exception e) {
                    logger.log(LogService.LOG_ERROR, "Couldn't initialize SWF verification", e);
                }
            }
            options.setWriterToSave(writer);
            logger.log(LogService.LOG_INFO, "Starting download: " + host + " " + app + " " + streamName);
            final ClientBootstrap bootstrap = getBootstrap(Executors.newCachedThreadPool(), options);
            final ChannelFuture future = bootstrap.connect(new InetSocketAddress(options.getHost(), options.getPort()));
            future.awaitUninterruptibly();
            if(!future.isSuccess()) {
                logger.log(LogService.LOG_ERROR, "Error creating client connection",future.getCause());
                setStatus(Status.FAILED);
                setException(future.getCause());
            }
            channel = future.getChannel();
            future.getChannel().getCloseFuture().awaitUninterruptibly();
            if(getProgress() == 100) {
                setStatus(Status.FINISHED);
            } else {
                setStatus(Status.STOPPED);
            }
            bootstrap.getFactory().releaseExternalResources();
            
        } catch (FileNotFoundException e) {
            logger.log(LogService.LOG_ERROR, "Couldn't start download to file " + getLocalFile(), e);
        }
    }
    
    private void initSwfVerification(ClientOptions options, URI swfUri) throws MalformedURLException, IOException, InvalidKeyException, NoSuchAlgorithmException {
        InputStream in = swfUri.toURL().openStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        SwfFile.decompressSwf(in, bos);
        byte[] data = bos.toByteArray();
        byte[] hmacSha256 = Utils.sha256(data, RtmpHandshake.CLIENT_CONST);
        options.setSwfHash(hmacSha256);
        options.setSwfSize(data.length);
    }

    public static ClientBootstrap getBootstrap(final Executor executor, final ClientOptions options) {
        final ChannelFactory factory = new NioClientSocketChannelFactory(executor, executor);
        final ClientBootstrap bootstrap = new ClientBootstrap(factory);
        bootstrap.setPipelineFactory(new ClientPipelineFactory(options));
        bootstrap.setOption("tcpNoDelay" , true);
        bootstrap.setOption("keepAlive", true);
        return bootstrap;
    }
}