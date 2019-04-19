package net.ewant.jmqttd.listener;

import net.ewant.jmqttd.server.mqtt.MqttSession;

/**
 * Created by admin on 2019/4/18.
 */
public interface ClientSessionListener {
    void onSessionOpen(MqttSession session);
    void onSessionClose(MqttSession session);
}
