package net.ewant.jmqttd.codec.message;

public class MqttPubComp extends MqttAck {

	public MqttPubComp(int messageId) {
		super(MqttMessageType.PUBCOMP);
		this.setMessageId(messageId);
	}
	
	public MqttPubComp(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		this.setMessageId(readShort());
	}

	@Override
	protected byte[] getVariableHeader() {
		return writeShort(getMessageId());
	}

}
