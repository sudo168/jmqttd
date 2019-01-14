package net.ewant.jmqttd.codec.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MqttSubscribe extends MqttWireMessage {
	
	private List<MqttTopic> subTopic;
	
	public MqttSubscribe(int messageId, List<MqttTopic> subTopic) {
		super(new MqttFixedHeader(MqttMessageType.SUBSCRIBE, MqttQoS.AT_LEAST_ONCE, false, false), 0, null);
		this.setMessageId(messageId);
		if(subTopic == null || subTopic.size() == 0)throw new IllegalArgumentException("subscript Topic requrid !");
		this.subTopic = subTopic;
	}
	
	public MqttSubscribe(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		this.setMessageId(readShort());
		subTopic = new ArrayList<MqttTopic>();
		do {
			subTopic.add(new MqttTopic(readUTF8(readShort()), MqttQoS.valueOf(readByte())));
		} while (readableBytes() > 0);
	}

	@Override
	protected byte[] getVariableHeader() {
		return writeShort(getMessageId());
	}
	
	public List<MqttTopic> getSubTopic() {
		return subTopic;
	}

	@Override
	public byte[] getPayload() {
		if(subTopic == null || subTopic.size() == 0)throw new IllegalArgumentException("subscript Topic requrid in payload!");
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (MqttTopic st : subTopic) {
				byte[] topicNameBytes = st.getName().getBytes();
				baos.write(writeShort(topicNameBytes.length));
				baos.write(topicNameBytes);
				baos.write(new byte[]{(byte) st.getQos().value()}, 0, 1);
			}
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalArgumentException("get Payload error!",e);
		}
	}

	@Override
	public String toString() {
		return "[subTopic=" + subTopic + "]";
	}
	
}
