package net.ewant.jmqttd.codec;

public class MqttDecodeException extends MqttException {

	private static final long serialVersionUID = -9018061178078203348L;

	public MqttDecodeException() {
    }

    public MqttDecodeException(String message) {
        super(message);
    }

    public MqttDecodeException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public MqttDecodeException(Throwable cause) {
        super(cause);
    }
}
