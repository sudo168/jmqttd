package net.ewant.jmqttd.interceptor;

import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.config.impl.AclPermissionAccess;
import net.ewant.jmqttd.server.mqtt.MqttSession;

public interface ConnectionAuthInterceptor extends Interceptor {

    boolean validClientId(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException;

    boolean validUsernamePassword(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException;

    boolean authentication(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException;

}
