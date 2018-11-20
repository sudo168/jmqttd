package com.ewant.jmqttd.codec.message;

import com.ewant.jmqttd.codec.MqttCodecUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MqttPublish extends MqttPersistableMessage {
	
	private MqttTopic topic;
	
	private byte[] payload = new byte[0];
	
	/**
	 * qos0: ack 始终为0
	 * qos1: ack 值累加
	 * qos2: ack 为100000+或者200000+，分两部分ack
	 */
	private int ackState = 0;
	
	public MqttPublish(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		String topicName = readUTF8(readShort());
		if(this.isMessageIdRequired()){
			this.setMessageId(readShort());
		}
		this.payload = readBytes(readableBytes());
		topic = new MqttTopic(topicName, getQos());
	}
	
	public MqttPublish(MqttTopic topic, boolean retain, byte[] payload) {
		super(new MqttFixedHeader(MqttMessageType.PUBLISH, topic.getQos(), false, retain), 0, null);
		this.topic = topic;
		this.payload = payload;
	}

	@Override
	protected byte[] getVariableHeader() {
		String topicName = topic.getName();
		MqttCodecUtils.isValidPublishTopicName(topicName);
		byte[] top = transUTF8Bytes(topicName);
		byte[] topLength = writeShort(top.length);
		
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(top.length + 4);// + 4 包含了message id 的2位长度
			baos.write(topLength);
			baos.write(top);
			
			if (this.isMessageIdRequired()) {
				baos.write(writeShort(getMessageId()));
			}
			
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalArgumentException("get Variable Header error!",e);
		}
	}
	
	public MqttTopic getTopic() {
		return topic;
	}
	
	@Override
	public byte[] getPayload() {
		return this.payload;
	}
	
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}

	@Override
	public boolean isMessageIdRequired() {
		return this.getQos().value() > 0;
	}

	public int getAckState() {
		return ackState;
	}

	public void setAckState(int ackState) {
		this.ackState = ackState;
	}
	
	public void countUpAckState() {
		this.ackState++;
	}
}
