package net.ewant.jmqttd.codec.message;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.UnsupportedEncodingException;

import net.ewant.jmqttd.codec.MqttCodecUtils;
import net.ewant.jmqttd.codec.MqttDecodeException;
import net.ewant.jmqttd.codec.MqttEncodeException;

import io.netty.buffer.ByteBuf;

/**
 * mqtt协议包：固定头部  + 可变头部  + 消息体 
 * @author huangzh
 * @date 2017年4月13日
 */
public abstract class MqttWireMessage {

	private static int MAX_FRAME_LENGTH = 0xFFFFFFF;// 268435455
	
	public static int MAX_MSG_ID = 0xFFFF;// 65535
	
	private MqttFixedHeader fixedHeader;// first byte [fixed header]
	
	private int remainingLength;//max 4 byte 剩余长度
	
	private byte[] remainingData;// 剩余字节数据（除开固定头部1字节，剩余长度<=4字节）

	private int messageId = 0;
	
	private int offset;
	
	public MqttWireMessage() {
	}
	
	public MqttWireMessage(MqttMessageType type) {
		this.fixedHeader = new MqttFixedHeader(type, MqttQoS.AT_MOST_ONCE, false, false);
	}
	
	public MqttWireMessage(MqttFixedHeader fixedHeader, int remainingLength, byte[] remainingData) {
		this.fixedHeader = fixedHeader;
		this.remainingLength = remainingLength;
		this.remainingData = remainingData;
	}
	
	public static MqttWireMessage decode(byte[] data){
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		return decode(bais);
	}
	
	public static MqttWireMessage decode(ByteBuf buf){
		byte[] bytes = new byte[buf.readableBytes()];
		buf.readBytes(bytes);
		return decode(bytes);
	}
	
	public static MqttWireMessage decode(InputStream data){
		try {
			DataInputStream in = new DataInputStream(data);
			//1. parse Fixed header
			MqttFixedHeader fixedHeader = MqttCodecUtils.isValidFixedHeader(new MqttFixedHeader(in.readByte()));
			MqttMessageType type = fixedHeader.getMessageType();
		    //2. parse Remaining Length
		    int multiplier = 1;
			int length = 0;
			byte digit = 0;
			do {
				digit = in.readByte();// 读取一个字节
				length += (digit & 0x7f) * multiplier;
				multiplier *= 128;
			} while ((digit & 0x80) != 0);
			
			if(length > MAX_FRAME_LENGTH)throw new InvalidObjectException("Remaining Length ["+length+"] great than MAX_FRAME_LENGTH ["+MAX_FRAME_LENGTH+"]");
			
			MqttWireMessage result = null;
			byte[] remainingData = new byte[length];
			in.readFully(remainingData, 0, length);
			
			if (type == MqttMessageType.CONNECT) {
				result = new MqttConnect(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.CONNACK) {
				result = new MqttConnAck(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.PUBLISH) {
				result = new MqttPublish(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.PUBACK) {
				result = new MqttPubAck(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.PUBREC) {
				result = new MqttPubRec(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.PUBREL) {
				result = new MqttPubRel(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.PUBCOMP) {
				result = new MqttPubComp(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.SUBSCRIBE) {
				result = new MqttSubscribe(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.SUBACK) {
				result = new MqttSubAck(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.UNSUBSCRIBE) {
				result = new MqttUnsubscribe(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.UNSUBACK) {
				result = new MqttUnsubAck(fixedHeader, length, remainingData);
			}else if (type == MqttMessageType.PING) {
				result = new MqttPing();
			}else if (type == MqttMessageType.PONG) {
				result = new MqttPong();
			}else if (type == MqttMessageType.DISCONNECT) {
				result = new MqttDisconnect();
			}else {
				throw new MqttDecodeException("MQTT packet decode error ! not support type [" + type + "]");
			}
			return result;
		} catch (MqttDecodeException e) {
			throw e;
		} catch (Exception e) {
			throw new MqttDecodeException("MQTT packet decode error ! " + e.getMessage(), e);
		}
	}

	/**
	 * @return
	 */
	public MqttQoS getQos() {
		return this.fixedHeader.getQos();
	}

	/**
	 * 是否为第 2+ 次发送的
	 * publish,subscribe,unsubscribe 及其ack消息。
	 * 以上这些消息如果需要回执，但是在没有收到回执的情况下进行重发时，需要标记duplicate为true
	 * @return
	 */
	public boolean isDuplicate() {
		return this.fixedHeader.isDuplicate();
	}

	/**
	 * MQTT 消息类型
	 * @return
	 */
	public MqttMessageType getType() {
		return this.fixedHeader.getMessageType();
	}

	/**
	 * 可变头部 + 消息体
	 * @return
	 */
	public int getRemainingLength() {
		if (remainingLength > 0) {
			return remainingLength;
		}
		return remainingLength = getVariableHeader().length + getPayload().length;
	}
	
	/**
	 * MQTT 数据包总长(header 1 byte + remaining length size bytes + remaining length bytes)
	 * @return
	 */
	public int getPacketSize() {
		int remL = getRemainingLength();
		return (1 + remainingLength2Bytes(remL).length + remL);
	}

	/**
	 * 消息是否需要持久化。
	 * @description 对于需要持久化的消息，新的订阅者只会获取最新一条
	 * @return
	 */
	public boolean isRetain() {
		return this.fixedHeader.isRetain();
	}


	/**
	 * 获取消息ID
	 * @return
	 */
	public int getMessageId() {
		return messageId;
	}

	/**
	 * 消息ID不能大于65535
	 * @param messageId
	 */
	public void setMessageId(int messageId) {
		if (messageId > MAX_MSG_ID) throw new IllegalArgumentException("invalid messageId : " + messageId);
		this.messageId = messageId;
	}

	private void checkIndex(int readSize){
		if (readSize < 0 || readableBytes() - readSize < 0) {
			throw new IndexOutOfBoundsException("remaining length is [" + remainingLength + "], but current index is ["+offset+"]");
		}
	}
	
	/**
	 * 读取数据包2个字节，读指针下移2
	 * @return
	 */
	protected int readShort() {
		checkIndex(2);
		return ((remainingData[offset++] & 0xFF) << 8) + (remainingData[offset++] & 0xFF);
	}
	
	/**
	 * 数据包剩余未读字节数
	 * @return
	 */
	protected int readableBytes() {
		return remainingLength - offset;
	}
	
	/**
	 * 将int类型写为2个字节
	 * @return
	 */
	protected byte[] writeShort (int length){
		byte[] b = new byte[2];
		b[0] = (byte) ((length >> 8) & 0xFF);
		b[1] = (byte) (length & 0xFF);
		return b;
	}
	
	/**
	 * 使用UTF-8编码，将字符串转化为byte
	 * @param data
	 * @return
	 */
	protected byte[] transUTF8Bytes (String data){
		try {
			if(data == null)return new byte[0];
			return data.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	/**
	 * 从数据包中读取指定长度的数据，并转换为UTF-8编码字符串
	 * @param length
	 * @return
	 */
	protected String readUTF8 (int length){
		try {
			return new String(readBytes(length), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}
	
	/**
	 * 从数据包中读取指定长度的数据
	 * @param length
	 * @return
	 */
	protected byte[] readBytes (int length){
		if (length < 1) return new byte[0];
		checkIndex(length);
		byte[] bytes = new byte[length];
		for (int i = 0; i < length; i++) {
			bytes[i] = remainingData[offset++];
		}
		return bytes;
	}
	
	/**
	 * 从数据包中读取单字节数据
	 * @return
	 */
	protected byte readByte() {
		checkIndex(1);
		return remainingData[offset++];
	}
	
	/**
	 * 当前MQTT数据包是否必须有messageId
	 * @return
	 */
	public boolean isMessageIdRequired() {
		return true;
	}
	
	/**
	 * 获取消息体
	 * @return
	 */
	public byte[] getPayload() {
		return new byte[0];
	}
	
	/**
	 * 获取可变头部
	 * @return
	 */
	protected abstract byte[] getVariableHeader();
	
	public byte[] encode(){
		
		try {
			// check Fixed header
			MqttCodecUtils.isValidFixedHeader(this.fixedHeader);

			byte header = this.fixedHeader.toByte();

			byte[] variableHeader = getVariableHeader();
			
			byte[] payload = getPayload();
			
			int remLen = variableHeader.length + payload.length;
			
			byte[] remainingBytes = remainingLength2Bytes(remLen);
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream((1 + remainingBytes.length + remLen));
			
			baos.write(header);
			
			baos.write(remainingBytes);
			
			if(variableHeader.length > 0)baos.write(variableHeader);

			if(payload.length > 0)baos.write(payload);
			
			baos.flush();
			
			return baos.toByteArray();
			
		} catch (MqttEncodeException e) {
			throw e;
		} catch (Exception e) {
			throw new MqttEncodeException(e);
		}
	}
	
	protected byte[] remainingLength2Bytes(int remainingLength){
		ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
		int val = remainingLength;
		do {
		     byte digit = (byte) (val % 128);
		     val = val / 128;
		     if (val > 0)
		         digit = (byte) (digit | 0x80);
		     baos.write(digit);
		} while (val > 0);
		return baos.toByteArray();
	}
	
}
