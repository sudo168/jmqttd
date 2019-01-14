package net.ewant.jmqttd.server.mqtt;

import java.util.Collection;
import java.util.Map;

import io.netty.channel.Channel;
import io.netty.util.internal.PlatformDependent;


public class MqttSessionManager {
	
	/**
	 * 正在连接中的客户端管道映射
	 */
	private static Map<String,MqttSession> channelClients = PlatformDependent.newConcurrentHashMap();
	/**
	 * 正在连接中的客户端id映射
	 */
	private static Map<String,MqttSession> idClients = PlatformDependent.newConcurrentHashMap();

	/**
	 * cleanSession为false时，存储离线session。暂存内存中 TODO
	 */
	private static Map<String,MqttSession> presentClients = PlatformDependent.newConcurrentHashMap();
	
	public static Collection<MqttSession> getClients() {
		return idClients.values();
	}

	public static void addSession(MqttSession client) {
		if (client != null) {
			idClients.put(client.getId(), client);
			channelClients.put(client.getChannel().id().asShortText(), client);
		}
	}

	public static void retain(MqttSession client) {
		if (client != null) {
			presentClients.put(client.getId(), client);
		}
	}

	public static MqttSession unRetain(String id) {
		if (id != null) {
			return presentClients.remove(id);
		}
		return null;
	}
	
	public static MqttSession getSession(String id) {
		if (id != null) {
			return idClients.get(id);
		}
		return null;
	}
	
	public static MqttSession getSession(Channel channel) {
		if (channel != null) {
			return channelClients.get(channel.id().asShortText());
		}
		return null;
	}
	
	public static MqttSession remove(String id) {
		if (id != null) {
			MqttSession mqttSession = idClients.remove(id);
			if(mqttSession != null){
				channelClients.remove(mqttSession.getChannel().id().asShortText());
			}
			return mqttSession;
		}
		return null;
	}

	public static MqttSession remove(Channel channel) {
		if (channel != null) {
			MqttSession mqttSession = channelClients.remove(channel.id().asShortText());

			if(mqttSession != null){
				idClients.remove(mqttSession.getId());
			}
			return mqttSession;
		}
		return null;
	}
	
}
