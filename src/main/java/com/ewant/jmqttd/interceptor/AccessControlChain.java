package com.ewant.jmqttd.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.ewant.jmqttd.codec.MqttCodecUtils;
import com.ewant.jmqttd.codec.MqttException;
import com.ewant.jmqttd.codec.message.MqttPublish;
import com.ewant.jmqttd.codec.message.MqttSubAck;
import com.ewant.jmqttd.codec.message.MqttSubscribe;
import com.ewant.jmqttd.codec.message.MqttTopic;
import com.ewant.jmqttd.config.AccessControlConfig;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.core.ServerProtocol;
import com.ewant.jmqttd.server.mqtt.MqttServer;
import com.ewant.jmqttd.server.mqtt.MqttServerContext;
import com.ewant.jmqttd.server.mqtt.MqttSession;
import com.ewant.jmqttd.server.mqtt.TopicManager;

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
    	AclPermissionAccess denyPermision = null;
    	boolean result = true;
    	for (AccessControlInterceptor accessControlInterceptor : accessControlInterceptors) {
    		AclPermissionAccess permision = aclConfig.getPermision(accessControlInterceptor);
    		if(permision.getAction() != AclPermissionAccess.AclAction.SUB && accessControlInterceptor.matchSession(client, permision)){
    			try {
					result = accessControlInterceptor.canPublish(client, publish.getTopic(), permision);
				} catch (Exception e) {
					result = false;
					logger.error("session: " + client.getId() + "ip: " + client.getIP() + ", execute Method:" + accessControlInterceptor.getClass().getName() + ".canPublish(); cause: " + e.getMessage() , e);
				}
    			if(!result){
    				denyPermision = permision;
    			}
    		}
		}
    	if(!result){
			logger.info("permission deny session: {} ip:{}, publish topic:{}. acl:{}", client.getId(), client.getIP(), publish.getTopic().getName(), denyPermision);
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
    	AclPermissionAccess denyPermision = null;
    	int notAllowSub = 0;
    	for (MqttTopic subTopic : subscribe.getSubTopic()) {
    		if(hasAcl){
    			for (AccessControlInterceptor accessControlInterceptor : accessControlInterceptors) {
            		AclPermissionAccess permision = aclConfig.getPermision(accessControlInterceptor);
            		if(permision.getAction() != AclPermissionAccess.AclAction.PUB && accessControlInterceptor.matchSession(client, permision)){
            			boolean canSubscribe;
    					try {
    						canSubscribe = accessControlInterceptor.canSubscribe(client, subTopic, permision);
    					} catch (Exception e) {
    						canSubscribe = false;
    						logger.error("session: " + client.getId() + "ip: " + client.getIP() + ", execute Method:" + accessControlInterceptor.getClass().getName() + ".canSubscribe(); cause: " + e.getMessage() , e);
    					}
            			if(canSubscribe){
            				notAllowSub = 0;
            				client.sub(subTopic);
            			}else{
            				notAllowSub = MqttSubAck.SUB_NOT_ALLOW;
            				denyPermision = permision;
            			}
            		}
        		}
    		}
    		if(notAllowSub == MqttSubAck.SUB_NOT_ALLOW){
    			resultCodes.add(notAllowSub);
    			notAllowSub = 0;
    			logger.info("permission deny session: {} ip:{}, subscribe topic:{}. acl:{}", client.getId(), client.getIP(), subscribe.getSubTopic(), denyPermision);
    		}else{
    			resultCodes.add(subTopic.getQos().value());
    			// init sub trie
    			if(this.server.getProtocol() == ServerProtocol.CLUSTER){
    				TopicManager.systemSubscribe(client, subTopic);
    			}else{
    				TopicManager.clientSubscribe(client, subTopic);
    			}
    		}
		}
    	logger.info("client: {}, subscribe: {}, resultCodes: {}", client.getId(), subscribe, JSON.toJSONString(resultCodes));
        return resultCodes;
    }

}
