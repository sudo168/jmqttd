package com.ewant.jmqttd.codec.message;

public class MqttPubRec extends MqttAck {

	public MqttPubRec(int messageId) {
		super(MqttMessageType.PUBREC);
		this.setMessageId(messageId);
	}
	
	public MqttPubRec(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		this.setMessageId(readShort());
	}

	@Override
	protected byte[] getVariableHeader() {
		return writeShort(getMessageId());
	}

}
