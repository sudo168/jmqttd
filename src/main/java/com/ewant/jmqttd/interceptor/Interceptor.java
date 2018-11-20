package com.ewant.jmqttd.interceptor;

import com.ewant.jmqttd.codec.MqttException;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public interface Interceptor {
	boolean matchSession(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException ;
}
