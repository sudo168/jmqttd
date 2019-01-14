package net.ewant.jmqttd.codec.message;

import java.util.List;

public class MqttSubAck extends MqttAck {
	public static final int SUB_NOT_ALLOW = 8;
	//Allowed return codes:
	//0x00 - Success - Maximum QoS 0 
	//0x01 - Success - Maximum QoS 1 
	//0x02 - Success - Maximum QoS 2 
	//0x80 - Failure 
	private byte[] returnCodes;

	public MqttSubAck(MqttSubscribe subscribe, List<Integer> returnCodes) {
		super(MqttMessageType.SUBACK);
		this.setMessageId(subscribe.getMessageId());
		List<MqttTopic> subTopic = subscribe.getSubTopic();
		int size = subTopic.size();
		this.returnCodes = new byte[size];
		for (int i = 0; i < size; i++) {
			int allowQos = (returnCodes == null || returnCodes.isEmpty()) ? subTopic.get(i).getQos().value() : returnCodes.get(i);
			if(allowQos > -1 && allowQos < 3){
				this.returnCodes[i] = (byte) allowQos;
			}else{
				this.returnCodes[i] = SUB_NOT_ALLOW;
			}
		}
	}
	
	public MqttSubAck(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		this.setMessageId(readShort());
		returnCodes = readBytes(readableBytes());
	}

	@Override
	protected byte[] getVariableHeader() {
		return writeShort(getMessageId());
	}

	@Override
	public byte[] getPayload() {
		return returnCodes;
	}
}
