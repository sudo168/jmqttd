package net.ewant.jmqttd.codec.message;

public class MqttUnsubAck extends MqttAck {

	public MqttUnsubAck(int messageId) {
		super(MqttMessageType.UNSUBACK);
		this.setMessageId(messageId);
	}
	
	public MqttUnsubAck(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		this.setMessageId(readShort());
	}

	@Override
	protected byte[] getVariableHeader() {
		return writeShort(getMessageId());
	}

}
