package com.ewant.jmqttd.codec;

public class MqttException extends RuntimeException {

	private static final long serialVersionUID = -9018061178078203348L;

	public MqttException() {
    }

    public MqttException(String message) {
        super(message);
    }

    public MqttException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttException(Throwable cause) {
        super(cause);
    }
}
