package net.ewant.jmqttd.persistent;

public class MqttPersistentException extends RuntimeException {
	
	private static final long serialVersionUID = -4816034580027147242L;

	public MqttPersistentException() {
    }

    public MqttPersistentException(String message) {
        super(message);
    }

    public MqttPersistentException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttPersistentException(Throwable cause) {
        super(cause);
    }
}
