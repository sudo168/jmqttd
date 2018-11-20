package com.ewant.jmqttd.interceptor;

import com.ewant.jmqttd.codec.MqttException;
import com.ewant.jmqttd.codec.message.MqttTopic;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public interface MessageFilterInterceptor extends Interceptor {
    String doFilter(MqttSession client, MqttTopic topic, String message, AclPermissionAccess permissionAccess) throws MqttException;
}
