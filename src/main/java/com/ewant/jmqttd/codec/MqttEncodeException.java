package com.ewant.jmqttd.codec;

public class MqttEncodeException extends MqttException {

	private static final long serialVersionUID = -9018061178078203348L;

	public MqttEncodeException() {
    }

    public MqttEncodeException(String message) {
        super(message);
    }

    public MqttEncodeException(String message, Throwable cause) {
        super(message, cause);
    }
    
    public MqttEncodeException(Throwable cause) {
        super(cause);
    }
}
