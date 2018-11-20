package com.ewant.jmqttd.codec.message;

public class MqttPing extends MqttWireMessage {

	public MqttPing() {
		super(MqttMessageType.PING);
	}

	@Override
	protected byte[] getVariableHeader() {
		return new byte[0];
	}

}
