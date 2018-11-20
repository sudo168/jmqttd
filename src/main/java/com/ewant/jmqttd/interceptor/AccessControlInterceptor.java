package com.ewant.jmqttd.interceptor;

import com.ewant.jmqttd.codec.message.MqttTopic;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public interface AccessControlInterceptor extends Interceptor {

    boolean canPublish(MqttSession client, MqttTopic pubTopic, AclPermissionAccess permissionAccess);

    boolean canSubscribe(MqttSession client, MqttTopic subTopic, AclPermissionAccess permissionAccess);

}
