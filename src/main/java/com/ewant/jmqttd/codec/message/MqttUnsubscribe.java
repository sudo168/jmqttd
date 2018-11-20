package com.ewant.jmqttd.codec.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MqttUnsubscribe extends MqttWireMessage {

	private List<String> unSubTopic;

	public MqttUnsubscribe(int messageId, List<String> unSubTopic) {
		super(new MqttFixedHeader(MqttMessageType.UNSUBSCRIBE, MqttQoS.AT_LEAST_ONCE, false, false), 0, null);
		this.setMessageId(messageId);
		if(unSubTopic == null || unSubTopic.size() == 0)throw new IllegalArgumentException("unsubscript Topic requrid !");
		this.unSubTopic = unSubTopic;
	}
	
	public MqttUnsubscribe(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		this.setMessageId(readShort());
		unSubTopic = new ArrayList<String>();
		do {
			unSubTopic.add(readUTF8(readShort()));
		} while (readableBytes() > 0);
	}

	@Override
	protected byte[] getVariableHeader() {
		return writeShort(getMessageId());
	}
	
	public List<String> getUnSubTopic() {
		return unSubTopic;
	}
	
	@Override
	public byte[] getPayload() {
		if(unSubTopic == null || unSubTopic.size() == 0)throw new IllegalArgumentException("unsubscript Topic requrid in payload!");
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			for (String st : unSubTopic) {
				byte[] unsub = transUTF8Bytes(st);
				baos.write(writeShort(unsub.length));
				baos.write(unsub);
			}
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalArgumentException("get Payload error!",e);
		}
	}
	
}
