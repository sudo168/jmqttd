package net.ewant.jmqttd.codec.message;

public abstract class MqttAck extends MqttWireMessage {
	
	public MqttAck() {
	}
	
	public MqttAck(MqttMessageType type) {
		super(type);
	}
	
	public MqttAck(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
	}

}
