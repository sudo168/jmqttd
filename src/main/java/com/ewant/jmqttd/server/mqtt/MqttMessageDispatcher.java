package com.ewant.jmqttd.server.mqtt;

import com.ewant.jmqttd.codec.message.MqttPublish;

/**
 * 消息分发器
 */
public interface MqttMessageDispatcher {
    void dispatch(MqttPublish publish);
}
