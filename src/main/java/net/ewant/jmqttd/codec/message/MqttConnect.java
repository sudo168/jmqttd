package net.ewant.jmqttd.codec.message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.ewant.jmqttd.codec.MqttCodecUtils;

public class MqttConnect extends MqttWireMessage{
	
	private MqttVersion version;
	private int keepAlive = 60;
	private String clientId;
	private String userName;
	private String password;
	private boolean cleanSession;

	private MqttPublish willMessage;

	
	public MqttConnect(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		this.version = MqttVersion.fromProtocolNameAndLevel(readUTF8(readShort()), readByte());
		byte connectFlags = readByte();
		cleanSession = (connectFlags >> 1 & 0x01) > 0;
		int willFlag = (connectFlags >> 2 & 0x01);
		byte passFlag = (byte) (connectFlags >> 6 & 0x01);
		byte userFlag = (byte) (connectFlags >> 7 & 0x01);
		keepAlive = readShort();
		clientId = readUTF8(readShort());
		if(willFlag == 1){
			MqttQoS willQos = MqttQoS.valueOf(connectFlags >> 3 & 0x03);
			boolean willRetain = (connectFlags >> 5 & 0x01) == 1;
			String willTopic = readUTF8(readShort());
			MqttCodecUtils.isValidPublishTopicName(willTopic);
			byte[] willPayload = readBytes(readShort());
			this.willMessage = new MqttPublish(new MqttTopic(willTopic, willQos), willRetain, willPayload);
		}
		// If the User Name Flag is set to 0, the Password Flag MUST be set to 0
		if(userFlag == 1){
			userName = readUTF8(readShort());
			if(passFlag == 1){
				password = readUTF8(readShort());
			}
		}
	}
	
	public MqttConnect(MqttVersion mqttVersion, String clientId, boolean cleanSession, int keepAliveInterval, String userName, String password, MqttPublish willMessage) {
		super(MqttMessageType.CONNECT);
		this.clientId = clientId;
		this.cleanSession = cleanSession;
		this.keepAlive = keepAliveInterval;
		this.userName = userName;
		this.password = password;
		this.version = mqttVersion;
		this.willMessage = willMessage;
	}

	public MqttVersion getVersion() {
		return this.version;
	}

	public int getKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(int keepAlive) {
		this.keepAlive = keepAlive;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public boolean isCleanSession() {
		return cleanSession;
	}

	public void setCleanSession(boolean cleanSession) {
		this.cleanSession = cleanSession;
	}

	public void setVersion(MqttVersion version) {
		this.version = version;
	}

	public MqttPublish getWillMessage() {
		return willMessage;
	}

	public void setWillMessage(MqttPublish willMessage) {
		this.willMessage = willMessage;
	}

	@Override
	public boolean isMessageIdRequired() {
		return false;
	}

	@Override
	protected byte[] getVariableHeader() {
		
		if (version == null) {
			throw new IllegalArgumentException("not support MQTT version : " + version);
		}
		byte[] protoVersion = version.encodeBytes();
		byte connectFlag = 0;
		if (cleanSession) {
			connectFlag |= 0x02;
		}
		if(willMessage != null){
			String willTopic = willMessage.getTopic().getName();
			MqttCodecUtils.isValidPublishTopicName(willTopic);
			MqttQoS willQos = willMessage.getQos();
			if(willTopic != null && willTopic.length() > 0){
				connectFlag |= 0x04;
				connectFlag |= (willQos.value() << 3);
				if (willMessage.isRetain()) {
					connectFlag |= 0x20;
				}
			}
		}
		if(userName != null){
			connectFlag |= 0x80;
			if(password != null){
				connectFlag |= 0x40;
			}
		}else{
			if(password != null){
				throw new IllegalArgumentException("not need password !");
			}
		}
		
		byte[] keepAliveInterval = writeShort(keepAlive);
		
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(protoVersion.length + 3);
			baos.write(protoVersion);
			baos.write(connectFlag);
			baos.write(keepAliveInterval);
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalArgumentException("get Variable Header error!",e);
		}
	}

	@Override
	public byte[] getPayload() {
		
		byte[] cid = transUTF8Bytes(clientId);
		byte[] cidLength = writeShort(cid.length);
		
		byte[] willTp = transUTF8Bytes(this.willMessage.getTopic().getName());
		
		byte[] willTpLength = writeShort(willTp.length);
		
		byte[] willPl = this.willMessage.getPayload();
		
		byte[] willPlLength = writeShort(willPl.length);
		
		byte[] u = transUTF8Bytes(userName);
		byte[] uLength = writeShort(u.length);
		
		byte[] p = transUTF8Bytes(password);
		byte[] pLength = writeShort(p.length);
		
		try {
			int payloadLength = (cid.length + 2) + (willTp.length + 2) + (willPl.length + 2) + (u.length + 2) + (p.length + 2);
			ByteArrayOutputStream baos = new ByteArrayOutputStream(payloadLength);
			baos.write(cidLength);
			baos.write(cid);
			
			baos.write(willTpLength);
			baos.write(willTp);
			
			baos.write(willPlLength);
			baos.write(willPl);
			
			baos.write(uLength);
			baos.write(u);
			
			baos.write(pLength);
			baos.write(p);
			
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalArgumentException("get Payload error!",e);
		}
	}
}
