package com.ewant.jmqttd.codec.message;

import com.ewant.jmqttd.codec.MqttDecodeException;

public enum MqttQoS {
	AT_MOST_ONCE(0),
	AT_LEAST_ONCE(1),
	EXACTLY_ONCE(2),
	;
	
	private int value;
	
	MqttQoS(int value){
		this.value = value;
	}
	
	public int value(){
		return this.value;
	}
	
	public static MqttQoS valueOf(int value){
		for (MqttQoS qos : values()) {
			if(qos.value == value){
				return qos;
			}
		}
		throw new MqttDecodeException("MQTT QoS support 0,1,2 only. by " + value);
	}
}
