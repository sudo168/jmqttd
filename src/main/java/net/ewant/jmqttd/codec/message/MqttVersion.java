package net.ewant.jmqttd.codec.message;

import java.io.UnsupportedEncodingException;
/**
 * //MQTT protocol and version                                    0    6    M    Q    I    s    d    p    3
 * protected static byte[] PROTOCOL_IDENTIFIER_V31 = new byte[]{0x00,0x06,0x4d,0x51,0x49,0x73,0x64,0x70,0x03}; // 3 or 3.1
 * protected static byte[] PROTOCOL_IDENTIFIER_V311 = new byte[]{0x00,0x04,0x4d,0x51,0x54,0x54,0x04}; // 3.1.1
 * @author hoey
 */
public enum MqttVersion {
    MQTT_31("MQIsdp", (byte) 3),// version 3 or 3.1
    MQTT_311("MQTT", (byte) 4);// version 3.1.1

    private final String name;
    private final byte level;

    MqttVersion(String protocolName, byte protocolLevel) {
        name = protocolName;
        level = protocolLevel;
    }

    public String protocolName() {
        return name;
    }

    public byte[] encodeBytes() {
        try {
        	int length = name.length();
        	byte[] encodeBuf = new byte[3 + length];// 3 = 2 bytes name length + 1 byte version level
        	int index = 0;
        	encodeBuf[index++] = (byte) ((length >> 8) & 0xFF);
        	encodeBuf[index++] = (byte) (length & 0xFF);
        	
			byte[] nameBytes = name.getBytes("UTF-8");
			for (int i = 0; i < nameBytes.length; i++, index++) {
				encodeBuf[index] = nameBytes[i];
			}
			encodeBuf[index] = level;
			
			return encodeBuf;
		} catch (UnsupportedEncodingException e) {
			// NOOP
		}
        return null;
    }

    public byte protocolLevel() {
        return level;
    }

    public static MqttVersion fromProtocolNameAndLevel(String protocolName, byte protocolLevel) {
        for (MqttVersion mv : values()) {
            if (mv.name.equals(protocolName) && mv.level == protocolLevel) {
            	return mv;
            }
        }
        return null;
    }
}
