package net.ewant.jmqttd.codec.message;

public class MqttPubAck extends MqttAck {
	
	public MqttPubAck(int messageId) {
		super(MqttMessageType.PUBACK);
		this.setMessageId(messageId);
	}
	
	public MqttPubAck(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		this.setMessageId(readShort());
	}

	@Override
	protected byte[] getVariableHeader() {
		return writeShort(getMessageId());
	}

}
