package net.ewant.jmqttd.interceptor;

import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.config.impl.AclPermissionAccess;
import net.ewant.jmqttd.server.mqtt.MqttSession;

public interface Interceptor {
	boolean matchSession(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException;
}
