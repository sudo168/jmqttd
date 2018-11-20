package com.ewant.jmqttd.codec.message;

public class MqttConnAck extends MqttAck {
	
	private int returnCode;
	
	private boolean sessionPresent;
	
	public static final int ACCEPTED = 0;
	public static final int UNACCEPTABLE_PROTOCOL_VERSION = 1;
	public static final int IDENTIFIER_REJECTED = 2;
	public static final int SERVER_UNAVAILABLE = 3;
	public static final int BAD_USER_NAME_OR_PASSWORD = 4;
	public static final int NOT_AUTHORIZED = 5;

	public MqttConnAck(int returnCode, boolean sessionPresent) {
		super(MqttMessageType.CONNACK);
		this.returnCode = returnCode;
		this.sessionPresent = sessionPresent;
	}
	
	public MqttConnAck(MqttFixedHeader fixedHeader, int length, byte[] remainingData) {
		super(fixedHeader, length, remainingData);
		this.sessionPresent = (readByte() & 0x01) == 1;
		this.returnCode = readByte();
	}

	private static final String[] CODE_MSG = new String[]{
			"Connection Accepted",
			"Connection Refused: unacceptable protocol version",
	   		"Connection Refused: identifier rejected",
	   		"Connection Refused: server unavailable",
	   		"Connection Refused: bad user name or password",
	   		"Connection Refused: not authorized"};
		
	public static String getCodeMsg(int code) {
		if(code < 0 || code > 6) throw new IllegalArgumentException("invalid code:" + code);
		return CODE_MSG[code];
	}
	
	@Override
	protected byte[] getVariableHeader() {
		byte[] ackBytes = new byte[2];
		// Byte 1 is the "Connect Acknowledge Flags". Bits 7-1 are reserved and MUST be set to 0
		// Bit 0 (SP1) is the Session Present Flag
		// 
		// 如果CleanSession = 1 并且 return Code = 0 时，此值必须为0 
		// 如果CleanSession = 0 并且 return Code = 0 时，如果服务端已经存有当前clientID相关信息的返回1，否则0
		// 当 return Code != 0 时，此值必须为0 
		ackBytes[0] = (byte) (sessionPresent ? 1 : 0);
		ackBytes[1] = (byte) returnCode;
		return ackBytes;
	}
	
	public int getReturnCode() {
		return returnCode;
	}
	
	public boolean isSessionPresent() {
		return sessionPresent;
	}
	
	@Override
	public boolean isMessageIdRequired() {
		return false;
	}
	
}
