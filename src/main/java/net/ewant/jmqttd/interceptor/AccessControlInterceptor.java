package net.ewant.jmqttd.interceptor;

import net.ewant.jmqttd.codec.message.MqttTopic;
import net.ewant.jmqttd.config.impl.AclPermissionAccess;
import net.ewant.jmqttd.server.mqtt.MqttSession;

public interface AccessControlInterceptor extends Interceptor {

    boolean canPublish(MqttSession client, MqttTopic pubTopic, AclPermissionAccess permissionAccess);

    boolean canSubscribe(MqttSession client, MqttTopic subTopic, AclPermissionAccess permissionAccess);

}
