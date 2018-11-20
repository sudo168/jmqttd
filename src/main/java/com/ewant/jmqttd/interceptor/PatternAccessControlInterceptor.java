package com.ewant.jmqttd.interceptor;

import java.util.regex.Pattern;

import com.ewant.jmqttd.codec.MqttException;
import com.ewant.jmqttd.codec.message.MqttTopic;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public final class PatternAccessControlInterceptor extends AbstractMatchInterceptor implements AccessControlInterceptor {
	
	private Pattern[] values;
	
	public PatternAccessControlInterceptor(String[] regex){
	    this.setValues(regex);
    }
	

    private void setValues(String[] regex){
    	if(regex == null){
    		return;
    	}
    	values = new Pattern[regex.length];
    	for (int i = 0; i < regex.length; i++) {
    		values[i] = Pattern.compile(regex[i]);
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
		for (Pattern pattern : values) {
			if (pattern.matcher(value).matches()) {
				return true;
			}
		}
		return false;
	}
}
