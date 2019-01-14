package net.ewant.jmqttd.interceptor;

import net.ewant.jmqttd.codec.MqttCodecUtils;
import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.config.impl.AclPermissionAccess;
import net.ewant.jmqttd.server.mqtt.MqttSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultConnectionAuthInterceptor implements ConnectionAuthInterceptor {

	Logger logger = LoggerFactory.getLogger(getClass());
	
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
		try {
			return MqttCodecUtils.isValidClientId(client.getVersion(), client.getId());
		} catch (MqttException e) {
			logger.error(e.getMessage());
		}
		return false;
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
