package com.ewant.jmqttd.handler;

import com.ewant.jmqttd.codec.MqttCodec;
import com.ewant.jmqttd.config.HostPortSslConfiguration;
import com.ewant.jmqttd.core.AbstractHandlerChainAdapter;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelPipeline;

public class MqttHandlerChainAdapter extends AbstractHandlerChainAdapter<HostPortSslConfiguration>{


	@Override
	public void addSocketHandlers(ChannelPipeline pipeline) {
		pipeline.addLast("MqttCodec", new MqttCodec(this.hostPortSsl.getProtocol(), new ProtocolMessageWrapper() {
			@Override
			public Object wraperMessage(ByteBuf mqttBuffer) {
				return mqttBuffer;
			}
		}));
		//pipeline.addLast("MqttEncoder", new MqttEncoder());
    	//pipeline.addLast("MqttDecoder", new MqttDecoder(new MqttMessageListener()));
	}

}
