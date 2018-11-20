package com.ewant.jmqttd.interceptor;

import java.util.ArrayList;
import java.util.List;

import com.ewant.jmqttd.config.AccessControlConfig;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.core.ServerProtocol;
import com.ewant.jmqttd.server.mqtt.MqttServer;
import com.ewant.jmqttd.server.mqtt.MqttServerContext;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public class ConnectionAuthChain {
	
	private List<ConnectionAuthInterceptor> connectionAuthInterceptors;
	private MqttServer server;
	private AclPermissionAccess defaultPermissionAccess;

    public ConnectionAuthChain(ServerProtocol serverProtocol, List<ConnectionAuthInterceptor> connectionAuthInterceptors){
    	this.server = MqttServerContext.getServer(serverProtocol);
    	AccessControlConfig aclConfig = this.server.getConfiguration().getAclConfig();
    	if(aclConfig != null){
    		this.defaultPermissionAccess = aclConfig.getDefaultConnectPermissionAccess();
    	}
    	this.connectionAuthInterceptors = connectionAuthInterceptors;
    	if(this.connectionAuthInterceptors == null){
            this.connectionAuthInterceptors = new ArrayList<>();
        }
    	if(this.connectionAuthInterceptors.isEmpty()){
    		DefaultConnectionAuthInterceptor defaultConnectionAuthInterceptor = new DefaultConnectionAuthInterceptor(server.getConfiguration().getServerConfig().getUsername(), server.getConfiguration().getServerConfig().getPassword(), server.getConfiguration().getServerConfig().isAllowAnonymous());
    		this.connectionAuthInterceptors.add(defaultConnectionAuthInterceptor);
    	}
    }

    public boolean validClientId(MqttSession client){
    	if(defaultPermissionAccess != null){
    		return connectionAuthInterceptors.get(0).validClientId(client, defaultPermissionAccess);
    	}
    	AccessControlConfig aclConfig = this.server.getConfiguration().getAclConfig();
    	if(aclConfig == null || connectionAuthInterceptors == null || connectionAuthInterceptors.isEmpty()){
    		return true;
    	}
    	boolean result = true;
    	for (ConnectionAuthInterceptor connectionAuthInterceptor : connectionAuthInterceptors) {
    		if(connectionAuthInterceptor.matchSession(client, aclConfig.getPermision(connectionAuthInterceptor))){
    			result = connectionAuthInterceptor.validClientId(client, aclConfig.getPermision(connectionAuthInterceptor));
    		}
		}
        return result;
    }

    public boolean validUsernamePassword(MqttSession client){
    	if(defaultPermissionAccess != null){
    		return connectionAuthInterceptors.get(0).validUsernamePassword(client, defaultPermissionAccess);
    	}
    	AccessControlConfig aclConfig = this.server.getConfiguration().getAclConfig();
    	if(aclConfig == null || connectionAuthInterceptors == null || connectionAuthInterceptors.isEmpty()){
    		return true;
    	}
    	boolean result = true;
    	for (ConnectionAuthInterceptor connectionAuthInterceptor : connectionAuthInterceptors) {
    		if(connectionAuthInterceptor.matchSession(client, aclConfig.getPermision(connectionAuthInterceptor))){
    			result = connectionAuthInterceptor.validUsernamePassword(client, aclConfig.getPermision(connectionAuthInterceptor));
    		}
		}
        return result;
    }

    public boolean authentication(MqttSession client){
    	if(defaultPermissionAccess != null){
    		return connectionAuthInterceptors.get(0).authentication(client, defaultPermissionAccess);
    	}
    	AccessControlConfig aclConfig = this.server.getConfiguration().getAclConfig();
    	if(aclConfig == null || connectionAuthInterceptors == null || connectionAuthInterceptors.isEmpty()){
    		return true;
    	}
    	boolean result = true;
    	for (ConnectionAuthInterceptor connectionAuthInterceptor : connectionAuthInterceptors) {
    		if(connectionAuthInterceptor.matchSession(client, aclConfig.getPermision(connectionAuthInterceptor))){
    			result = connectionAuthInterceptor.authentication(client, aclConfig.getPermision(connectionAuthInterceptor));
    		}
		}
        return result;
    }

}
