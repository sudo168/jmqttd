package com.ewant.jmqttd.codec.message;

public abstract class MqttPersistableMessage extends MqttWireMessage {
	
	private long messageTime = System.currentTimeMillis();
	
	public MqttPersistableMessage(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
	}

	public long getMessageTime() {
		return messageTime;
	}

	public void setMessageTime(long messageTime) {
		this.messageTime = messageTime;
	}
	
}
