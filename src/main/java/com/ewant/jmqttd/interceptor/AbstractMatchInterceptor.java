package com.ewant.jmqttd.interceptor;

import com.ewant.jmqttd.codec.MqttException;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.config.impl.AclPermissionAccess.AclType;
import com.ewant.jmqttd.server.mqtt.MqttSession;

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
