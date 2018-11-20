package com.ewant.jmqttd.interceptor;

import com.ewant.jmqttd.codec.MqttException;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public interface ConnectionAuthInterceptor extends Interceptor {

    boolean validClientId(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException;

    boolean validUsernamePassword(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException;

    boolean authentication(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException;

}
