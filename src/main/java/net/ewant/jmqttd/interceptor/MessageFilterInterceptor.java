package net.ewant.jmqttd.interceptor;

import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.codec.message.MqttTopic;
import net.ewant.jmqttd.config.impl.AclPermissionAccess;
import net.ewant.jmqttd.server.mqtt.MqttSession;

public interface MessageFilterInterceptor extends Interceptor {
    byte[] doFilter(MqttSession client, MqttTopic topic, byte[] message, AclPermissionAccess permissionAccess) throws MqttException;
}
