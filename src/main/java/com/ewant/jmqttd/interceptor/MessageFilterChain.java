package com.ewant.jmqttd.interceptor;

import java.util.List;

import com.ewant.jmqttd.codec.message.MqttTopic;
import com.ewant.jmqttd.config.AccessControlConfig;
import com.ewant.jmqttd.core.ServerProtocol;
import com.ewant.jmqttd.server.mqtt.MqttServer;
import com.ewant.jmqttd.server.mqtt.MqttServerContext;
import com.ewant.jmqttd.server.mqtt.MqttSession;

public class MessageFilterChain {
	
	private List<MessageFilterInterceptor> messageFilterInterceptors;
	private MqttServer server;

    public MessageFilterChain(ServerProtocol serverProtocol, List<MessageFilterInterceptor> messageFilterInterceptors){
    	this.server = MqttServerContext.getServer(serverProtocol);
    	this.messageFilterInterceptors = messageFilterInterceptors;
    }

    public String doFilter(MqttSession client, MqttTopic topic, String message){
    	AccessControlConfig aclConfig = this.server.getConfiguration().getAclConfig();
    	if(aclConfig == null || messageFilterInterceptors == null || messageFilterInterceptors.isEmpty()){
    		return message;
    	}
    	String filterMessage = message;
    	for (MessageFilterInterceptor messageFilterInterceptor : messageFilterInterceptors) {
    		filterMessage = messageFilterInterceptor.doFilter(client, topic, filterMessage, aclConfig.getPermission(messageFilterInterceptor));
			if(filterMessage == null){
				return null;
			}
		}
    	return filterMessage;
    }
}
