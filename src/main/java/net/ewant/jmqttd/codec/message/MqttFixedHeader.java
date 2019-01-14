package net.ewant.jmqttd.codec.message;

public class MqttFixedHeader {

	private boolean retain;// 1bit
	
	private MqttQoS qos;// 2bit (0 <= 1; 1 >= 1; 2 = 1)
	
	private boolean duplicate;//1bit default 0, 1 means this is a republish message. PUBLISH, PUBREL, SUBSCRIBE or UNSUBSCRIBE 消息中，并且QoS>0有效
	
	private MqttMessageType type;//4 bit
	
	public MqttFixedHeader(byte header){
		this.retain = (header & 0x01) == 1;
		this.qos = MqttQoS.valueOf((header >> 1) & 0x03);
		this.duplicate = (header & 0x08) == 1;
		this.type = MqttMessageType.valueOf((header >> 4) & 0x0F);
	}
	
	public MqttFixedHeader(MqttMessageType type, MqttQoS qos, boolean duplicate, boolean retain){
		this.retain = retain;
		this.qos = qos;
		this.duplicate = duplicate;
		this.type = type;
	}
	
	public byte toByte(){
		byte header = (byte) (type.value() << 4);
		if (duplicate) header |= 0x08; 
		if (qos.value() > 0) header |= (qos.value() << 1);
		if (retain) header |= 0x01;
		
		return header;
	}

	public boolean isRetain() {
		return retain;
	}

	public MqttQoS getQos() {
		return qos;
	}

	public boolean isDuplicate() {
		return duplicate;
	}

	public MqttMessageType getMessageType() {
		return type;
	}
	
}
