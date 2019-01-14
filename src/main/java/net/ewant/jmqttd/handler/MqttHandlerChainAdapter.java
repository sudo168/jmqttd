package net.ewant.jmqttd.handler;

import net.ewant.jmqttd.codec.MqttCodec;
import net.ewant.jmqttd.config.HostPortSslConfiguration;
import net.ewant.jmqttd.core.AbstractHandlerChainAdapter;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;

public class MqttHandlerChainAdapter extends AbstractHandlerChainAdapter<HostPortSslConfiguration>{


	@Override
	public void addSocketHandlers(ChannelPipeline pipeline) {
		pipeline.addLast("MqttCodec", new MqttCodec(this.hostPortSsl.getProtocol(), new ProtocolMessageWrapper() {
			@Override
			public Object wrapperMessage(ByteBuf mqttBuffer) {
				return mqttBuffer;
			}
		}));
		//pipeline.addLast("MqttEncoder", new MqttEncoder());
    	//pipeline.addLast("MqttDecoder", new MqttDecoder(new MqttMessageListener()));
	}

}
