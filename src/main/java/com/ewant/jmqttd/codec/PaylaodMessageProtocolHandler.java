package com.ewant.jmqttd.codec;

public interface PaylaodMessageProtocolHandler {
	
	boolean supportMessage(Object message);
	
	byte[] messageToBytes(Object message);
	
	Object bytesToMessage(byte[] message);
}
