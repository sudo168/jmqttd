package net.ewant.jmqttd.interceptor;

import com.alibaba.fastjson.JSON;
import net.ewant.jmqttd.codec.MqttCodecUtils;
import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.codec.message.MqttPublish;
import net.ewant.jmqttd.codec.message.MqttSubAck;
import net.ewant.jmqttd.codec.message.MqttSubscribe;
import net.ewant.jmqttd.codec.message.MqttTopic;
import net.ewant.jmqttd.config.AccessControlConfig;
import net.ewant.jmqttd.config.impl.AclPermissionAccess;
import net.ewant.jmqttd.core.ServerProtocol;
import net.ewant.jmqttd.server.mqtt.MqttServer;
import net.ewant.jmqttd.server.mqtt.MqttServerContext;
import net.ewant.jmqttd.server.mqtt.MqttSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AccessControlChain {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private List<AccessControlInterceptor> accessControlInterceptors;
	private MqttServer server;

    public AccessControlChain(ServerProtocol serverProtocol, List<AccessControlInterceptor> accessControlInterceptors){
    	this.server = MqttServerContext.getServer(serverProtocol);
    	this.accessControlInterceptors = accessControlInterceptors;
    }

    public boolean canPublish(MqttSession client, MqttPublish publish){
    	try {
			MqttCodecUtils.isValidPublishTopicName(publish.getTopic().getName());
		} catch (MqttException e) {
			return false;
		}
    	AccessControlConfig aclConfig = this.server.getConfiguration().getAclConfig();
    	if(aclConfig == null || accessControlInterceptors == null || accessControlInterceptors.isEmpty()){
    		return true;
    	}
    	AclPermissionAccess denyPermission = null;
    	boolean result = true;
    	for (AccessControlInterceptor accessControlInterceptor : accessControlInterceptors) {
    		AclPermissionAccess permission = aclConfig.getPermission(accessControlInterceptor);
    		if(permission.getAction() != AclPermissionAccess.AclAction.SUB && accessControlInterceptor.matchSession(client, permission)){
    			try {
					result = accessControlInterceptor.canPublish(client, publish.getTopic(), permission);
				} catch (Exception e) {
					result = false;
					logger.error("session: " + client.getId() + "ip: " + client.getIP() + ", execute Method:" + accessControlInterceptor.getClass().getName() + ".canPublish(); cause: " + e.getMessage() , e);
				}
    			if(!result){
    				denyPermission = permission;
    			}
    		}
		}
    	if(!result){
			logger.info("permission deny session: {} ip:{}, publish topic:{}. acl:{}", client.getId(), client.getIP(), publish.getTopic().getName(), denyPermission);
		}
        return result;
    }

    public List<Integer> canSubscribe(MqttSession client, MqttSubscribe subscribe){
    	AccessControlConfig aclConfig = this.server.getConfiguration().getAclConfig();
    	boolean hasAcl = true;
    	if(aclConfig == null || accessControlInterceptors == null || accessControlInterceptors.isEmpty()){
    		hasAcl = false;
    	}
    	List<Integer> resultCodes = new ArrayList<>();
    	AclPermissionAccess denyPermission = null;
    	int notAllowSub = 0;
    	for (MqttTopic subTopic : subscribe.getSubTopic()) {
    		if(hasAcl){
    			for (AccessControlInterceptor accessControlInterceptor : accessControlInterceptors) {
            		AclPermissionAccess permission = aclConfig.getPermission(accessControlInterceptor);
            		if(permission.getAction() != AclPermissionAccess.AclAction.PUB && accessControlInterceptor.matchSession(client, permission)){
            			boolean canSubscribe;
    					try {
    						canSubscribe = accessControlInterceptor.canSubscribe(client, subTopic, permission);
    					} catch (Exception e) {
    						canSubscribe = false;
    						logger.error("session: " + client.getId() + "ip: " + client.getIP() + ", execute Method:" + accessControlInterceptor.getClass().getName() + ".canSubscribe(); cause: " + e.getMessage() , e);
    					}
            			if(canSubscribe){
            				notAllowSub = 0;
            			}else{
            				notAllowSub = MqttSubAck.SUB_NOT_ALLOW;
            				denyPermission = permission;
            			}
            		}
        		}
    		}
    		if(notAllowSub == MqttSubAck.SUB_NOT_ALLOW){
    			resultCodes.add(notAllowSub);
    			notAllowSub = 0;// reset state
    			logger.info("permission deny session: {} ip:{}, subscribe topic:{}. acl:{}", client.getId(), client.getIP(), subscribe.getSubTopic(), denyPermission);
    		}else{
    			resultCodes.add(subTopic.getQos().value());
    			// init sub trie
				client.sub(subTopic);
    		}
		}
    	logger.info("client: {}, subscribe: {}, resultCodes: {}", client.getId(), subscribe, JSON.toJSONString(resultCodes));
        return resultCodes;
    }

}
