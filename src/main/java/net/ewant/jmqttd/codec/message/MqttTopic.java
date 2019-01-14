package net.ewant.jmqttd.codec.message;

public class MqttTopic implements Comparable<MqttTopic>{
	private String name;
	private MqttQoS qos;
	
	public MqttTopic(String name,MqttQoS qos) {
		this.name = name;
		this.qos = qos;
	}
	
	public int compareTo(MqttTopic other) {
		return this.name.compareTo(other.name);
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public MqttQoS getQos() {
		return qos;
	}
	public void setQos(MqttQoS qos) {
		this.qos = qos;
	}

	@Override
	public String toString() {
		return "{name=" + name + ", qos=" + qos + "}";
	}
	
}
