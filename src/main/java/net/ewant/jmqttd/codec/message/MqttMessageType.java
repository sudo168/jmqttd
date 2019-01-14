package net.ewant.jmqttd.codec.message;

import net.ewant.jmqttd.codec.MqttDecodeException;

/**
 * 消息类型  0与15为保留（Reserved）
 * @author huangzh
 * @date 2016年12月15日
 */
public enum MqttMessageType {
	CONNECT(1),
    CONNACK(2),
    PUBLISH(3),
    PUBACK(4),
    PUBREC(5),
    PUBREL(6),
    PUBCOMP(7),
    SUBSCRIBE(8),
    SUBACK(9),
    UNSUBSCRIBE(10),
    UNSUBACK(11),
    PING(12),
    PONG(13),
    DISCONNECT(14);

    private final int value;

    MqttMessageType(int value) {
        this.value = value;
    }

    public int value() {
        return value;
    }

    public static MqttMessageType valueOf(int type) {
        for (MqttMessageType t : values()) {
            if (t.value == type) {
                return t;
            }
        }
        throw new MqttDecodeException("unknown message type: " + type);
    }
}
