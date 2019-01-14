package net.ewant.jmqttd.config;

public class ConfigParseException extends RuntimeException {

	private static final long serialVersionUID = -9018061178078203348L;

	public ConfigParseException() {
    }

    public ConfigParseException(String message) {
        super(message);
    }

    public ConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigParseException(Throwable cause) {
        super(cause);
    }
}
