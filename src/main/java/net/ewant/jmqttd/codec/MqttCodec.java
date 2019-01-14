package net.ewant.jmqttd.codec;

import net.ewant.jmqttd.core.ServerProtocol;
import net.ewant.jmqttd.handler.ProtocolMessageWrapper;
import io.netty.channel.CombinedChannelDuplexHandler;

public class MqttCodec extends CombinedChannelDuplexHandler<MqttDecoder, MqttEncoder> {

	public MqttCodec(ServerProtocol protocol, ProtocolMessageWrapper wrapper) {
		super(new MqttDecoder(protocol), new MqttEncoder(protocol, wrapper));
	}
	
}
