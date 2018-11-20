package com.ewant.jmqttd.interceptor;

import java.util.HashMap;
import java.util.Map;

import com.ewant.jmqttd.codec.MqttException;
import com.ewant.jmqttd.codec.message.MqttTopic;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public final class SimpleAccessControlInterceptor extends AbstractMatchInterceptor implements AccessControlInterceptor {
	
private Map<String, Boolean> values;
	
	public SimpleAccessControlInterceptor(String[] values){
        this.values = new HashMap<>();
        for (String v: values) {
            this.values.put(v, true);
        }
    }


    @Override
    public boolean canPublish(MqttSession client, MqttTopic pubTopic, AclPermissionAccess permissionAccess) {
    	return permissionAccess.getPermission() == AclPermissionAccess.AclPermission.DENY ? false : true;
    }

    @Override
    public boolean canSubscribe(MqttSession client, MqttTopic subTopic, AclPermissionAccess permissionAccess) {
    	return permissionAccess.getPermission() == AclPermissionAccess.AclPermission.DENY ? false : true;
    }
    
	protected boolean matchValue(String value) throws MqttException {
		return this.values.get(value) != null;
	}
}
