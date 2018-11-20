package com.ewant.jmqttd.cluster;

public class DiscoveryException extends RuntimeException {

	private static final long serialVersionUID = -9018061178078203348L;

	public DiscoveryException() {
    }

    public DiscoveryException(String message) {
        super(message);
    }

    public DiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public DiscoveryException(Throwable cause) {
        super(cause);
    }
}
