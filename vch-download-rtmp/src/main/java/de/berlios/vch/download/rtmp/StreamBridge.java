package de.berlios.vch.download.rtmp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.concurrent.Executors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.osgi.service.log.LogService;

import com.flazr.rtmp.client.ClientOptions;

import de.berlios.vch.web.servlets.BundleContextServlet;

public class StreamBridge extends BundleContextServlet {

    public static final String PATH = "/stream/rtmp";
    
    @Override
    protected void get(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //String uri = req.getParameter("uri");
        String host = req.getParameter("host");
        String appName = req.getParameter("app");
        String streamName = req.getParameter("stream");
        String scheme = req.getParameter("scheme");
        String swfUri = req.getParameter("swfUri");
        logger.log(LogService.LOG_INFO, "StreamBridge params: " + req.getParameterMap());

        resp.setContentType("video");
        
        // add a filename 
        int lastSlash = streamName.lastIndexOf('/');
        String filename = lastSlash > 0 ? streamName.substring(lastSlash+1) : streamName;
        resp.addHeader("Content-disposition", "attachment; filename=\"" + filename + ".flv\"");
        
        ClientOptions co;
        if("rtmpe".equals(scheme)) {
            logger.log(LogService.LOG_INFO, "Trying to establish encrypted connection over rtmpe");
            co = new ClientOptions(host, 1935, appName, streamName, "/tmp/dummy", true, null);
        } else {
            co = new ClientOptions(host, appName, streamName,"/tmp/dummy");
        }
        if(swfUri != null) {
            try {
                RTMP.initSwfVerification(co, new URI(swfUri));
                logger.log(LogService.LOG_INFO, "SWF verification initialized");
            } catch (Exception e) {
                logger.log(LogService.LOG_ERROR, "Couldn't initialize SWF verification", e);
            }
        }
        
        OutputStreamFlvWriter writer = new OutputStreamFlvWriter(0, resp.getOutputStream(), new DownloadListener() {
            @Override
            public void setProgress(int percent) {}

            @Override
            public void downloadFailed(Exception e) {}

            @Override
            public void downloadFinished() {}

            @Override
            public void downloadStarted() {}
        });
        
        co.setWriterToSave(writer);
        logger.log(LogService.LOG_INFO, "Starting streaming: " +scheme + " " + host + " " + appName + " " + streamName);
        final ClientBootstrap bootstrap = RtmpDownload.getBootstrap(Executors.newCachedThreadPool(), new BandwidthMeterHandler(), co);
        final ChannelFuture future = bootstrap.connect(new InetSocketAddress(co.getHost(), co.getPort()));
        future.awaitUninterruptibly();
        if(!future.isSuccess()) {
            logger.log(LogService.LOG_ERROR, "Error creating client connection",future.getCause());
            error(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error creating client connection", future.getCause());
        }
        future.getChannel();
        future.getChannel().getCloseFuture().awaitUninterruptibly();
        bootstrap.getFactory().releaseExternalResources();
    }

    @Override
    protected void post(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
        get(req, resp);
    }
}
