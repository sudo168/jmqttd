package net.ewant.jmqttd.interceptor;

import java.util.HashMap;
import java.util.Map;

import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.config.impl.AclPermissionAccess;
import net.ewant.jmqttd.server.mqtt.MqttSession;

public final class SimpleConnectionAuthInterceptor extends AbstractMatchInterceptor implements ConnectionAuthInterceptor {
	
	private Map<String, Boolean> values;
	
	public SimpleConnectionAuthInterceptor(String[] values){
        this.values = new HashMap<>();
        for (String v: values) {
            this.values.put(v, true);
        }
    }
	
    @Override
    public boolean validClientId(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
        return true;
    }

    @Override
    public boolean validUsernamePassword(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
        return true;
    }

    @Override
    public boolean authentication(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
        return permissionAccess.getPermission() == AclPermissionAccess.AclPermission.DENY ? false : true;
    }
    
	protected boolean matchValue(String value) throws MqttException {
		return this.values.get(value) != null;
	}
}
