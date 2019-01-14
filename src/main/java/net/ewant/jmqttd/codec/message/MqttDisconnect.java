package net.ewant.jmqttd.codec.message;

public class MqttDisconnect extends MqttWireMessage {

	public MqttDisconnect() {
		super(MqttMessageType.DISCONNECT);
	}

	@Override
	protected byte[] getVariableHeader() {
		return new byte[0];
	}

}
