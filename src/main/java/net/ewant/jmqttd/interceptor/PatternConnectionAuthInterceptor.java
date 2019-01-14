package net.ewant.jmqttd.interceptor;

import java.util.regex.Pattern;

import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.config.impl.AclPermissionAccess;
import net.ewant.jmqttd.server.mqtt.MqttSession;

public final class PatternConnectionAuthInterceptor extends AbstractMatchInterceptor implements ConnectionAuthInterceptor {
	
	private Pattern[] values;
	
	public PatternConnectionAuthInterceptor(String[] regex){
	    this.setValues(regex);
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
		for (Pattern pattern : values) {
			if (pattern.matcher(value).matches()) {
				return true;
			}
		}
		return false;
	}
    
    private void setValues(String[] regex){
    	if(values == null){
    		return;
    	}
    	values = new Pattern[regex.length];
    	for (int i = 0; i < regex.length; i++) {
    		values[i] = Pattern.compile(regex[i]);
		}
    }

}
