package net.ewant.jmqttd.codec.message;

public class MqttPong extends MqttAck {

	public MqttPong() {
		super(MqttMessageType.PONG);
	}

	@Override
	protected byte[] getVariableHeader() {
		return new byte[0];
	}

}
