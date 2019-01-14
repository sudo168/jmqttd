package net.ewant.jmqttd.interceptor;

import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.config.impl.AclPermissionAccess;
import net.ewant.jmqttd.config.impl.AclPermissionAccess.AclType;
import net.ewant.jmqttd.server.mqtt.MqttSession;

public abstract class AbstractMatchInterceptor implements Interceptor{
	
	@Override
	public boolean matchSession(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
		AclType aclType = permissionAccess.getType();
		switch (aclType) {
		case ALL:
			return true;
		case USER:
			return matchValue(client.getUserName());
		case CLIENT:
			return matchValue(client.getId());
		case IP:
			return matchValue(client.getIP());
		default:
			break;
		}
		return false;
	}
	
	protected abstract boolean matchValue(String value) throws MqttException ;
}
