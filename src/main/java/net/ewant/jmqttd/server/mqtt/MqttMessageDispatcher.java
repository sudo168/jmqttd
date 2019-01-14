package net.ewant.jmqttd.server.mqtt;

import net.ewant.jmqttd.codec.message.MqttPublish;

/**
 * 消息分发器
 */
public interface MqttMessageDispatcher {
    void dispatch(MqttPublish publish);
}
