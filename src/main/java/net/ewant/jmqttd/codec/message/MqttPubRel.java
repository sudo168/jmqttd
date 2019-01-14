package net.ewant.jmqttd.codec.message;

public class MqttPubRel extends MqttPersistableMessage {

	public MqttPubRel(int messageId) {
		// Bits 3,2,1 and 0 of the fixed header in the PUBREL Control Packet are reserved and MUST be set to 0,0,1 and 0 respectively
		super(new MqttFixedHeader(MqttMessageType.PUBREL, MqttQoS.AT_LEAST_ONCE, false, false), 0, null);
		this.setMessageId(messageId);
	}
	
	public MqttPubRel(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		this.setMessageId(readShort());
	}

	@Override
	protected byte[] getVariableHeader() {
		return writeShort(getMessageId());
	}

}
