package de.berlios.vch.download.rtmp;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;

import com.flazr.rtmp.client.ClientOptions;
import com.flazr.rtmp.client.ClientPipelineFactory;

public class RtmpPipelineFactory implements ChannelPipelineFactory {

    private ClientOptions options;
    
    private BandwidthMeterHandler bmh;
    
    public RtmpPipelineFactory(BandwidthMeterHandler bmh, ClientOptions options) {
        this.options = options;
        this.bmh = bmh;
    }
    
    @Override
    public ChannelPipeline getPipeline() throws Exception {
        ClientPipelineFactory cpf = new ClientPipelineFactory(options);
        ChannelPipeline pipeline = cpf.getPipeline();
        pipeline.addFirst("BandwidthMeter", bmh);
        return pipeline;
    }
    

}
