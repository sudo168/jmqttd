package com.ewant.jmqttd.interceptor;

import com.ewant.jmqttd.codec.MqttCodecUtils;
import com.ewant.jmqttd.codec.MqttException;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public class DefaultConnectionAuthInterceptor implements ConnectionAuthInterceptor {
	
	private String username;
	private String password;
	private boolean allowAnonymous;
	
	public DefaultConnectionAuthInterceptor(String username, String password, boolean allowAnonymous){
		this.username = username;
		this.password = password;
		this.allowAnonymous = allowAnonymous;
	}
	
    @Override
    public boolean validClientId(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
        return MqttCodecUtils.isValidClientId(client.getVersion(), client.getId());
    }

    @Override
    public boolean validUsernamePassword(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
		String connectUserName = client.getUserName();
		String connectPassword = client.getPassword();
		if(this.allowAnonymous){
			return true;
		}
    	return (
    			  (connectUserName == null && username == null) || (username != null && username.equals(connectUserName))
    		   )
    			&& 
    		   (
    			  (connectPassword == null && password == null) || (password != null && password.equals(password))
    		   );
    		
    }

    @Override
    public boolean authentication(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
        return true;
    }

	@Override
	public boolean matchSession(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
		return true;
	}
    
}
