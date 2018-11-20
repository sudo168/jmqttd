package com.ewant.jmqttd.codec;

import com.ewant.jmqttd.core.ServerProtocol;
import com.ewant.jmqttd.handler.ProtocolMessageWrapper;
import io.netty.channel.CombinedChannelDuplexHandler;

public class MqttCodec extends CombinedChannelDuplexHandler<MqttDecoder, MqttEncoder> {

	public MqttCodec(ServerProtocol protocol, ProtocolMessageWrapper wraper) {
		super(new MqttDecoder(protocol), new MqttEncoder(protocol, wraper));
	}
	
}
