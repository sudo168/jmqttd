package net.ewant.jmqttd.server.mqtt;

import java.util.List;

import net.ewant.jmqttd.codec.message.MqttTopic;

public class TopicManager {
	
	/**
	 * 服务器内部通信专用主题
	 * $SYS/cluster/sync 集群数据同步
	 * $SYS/cluster/cmd 集群管理控制信息
	 * $SYS/peer/cmd 节点管理命令
	 * $SYS/monitor 集群监控管理
	 */
	private static final String SERVER_TRANSFER_TOPIC_PREFIX = "$SYS";
	
	private static final String CLUSTER_DATA_SYNC_TOPIC = SERVER_TRANSFER_TOPIC_PREFIX + "/cluster/sync";
	
	private static final String CLUSTER_MANAGER_TOPIC = SERVER_TRANSFER_TOPIC_PREFIX + "/cluster/cmd";
	
	private static final String PEER_MANAGER_TOPIC = SERVER_TRANSFER_TOPIC_PREFIX + "/peer/cmd";
	
	/**
	 * 系统主题订阅树（$SYS）
	 */
	private static TopicTrie systemTopicTrie = new TopicTrie();
	
	/**
	 * 客户端主题订阅树，$QUEUE 要实现
	 */
	private static TopicTrie clientTopicTrie = new TopicTrie();
	
	public static boolean isSystemTopic(String topicName){
		return topicName.startsWith(SERVER_TRANSFER_TOPIC_PREFIX);
	}
	
	public static boolean clientSubscribe(MqttSession client, MqttTopic subTopic){
		clientTopicTrie.insert(subTopic.getName(), subTopic.getQos().value(), client.getId());
		return true;
	}
	
	public static boolean clientUnSubscribe(MqttSession client, String unsubTopic){
		clientTopicTrie.remove(unsubTopic, client.getId());
		return true;
	}

	/**
	 * 匹配订阅指定主题的客户端
	 * @param topic
	 * @return
	 */
	public static List<TopicMapping> clientMatch(String topic){
		return clientTopicTrie.search(topic);
	}
	
	public static boolean systemSubscribe(MqttSession client, MqttTopic subTopic){
		systemTopicTrie.insert(subTopic.getName(), subTopic.getQos().value(), client.getId());
		return true;
	}
	
	public static boolean systemUnSubscribe(MqttSession client, String unsubTopic){
		systemTopicTrie.remove(unsubTopic, client.getId());
		return true;
	}
	
	/**
	 * 集群中，匹配订阅指定系统主题的客户端
	 * @param topic
	 * @return
	 */
	public static List<TopicMapping> systemMatch(String topic){
		return systemTopicTrie.search(topic);
	}
	
	public static boolean isClusterDataSync(String topic){
		return CLUSTER_DATA_SYNC_TOPIC.equals(topic);
	}
	
	public static boolean isClusterCommand(String topic){
		return CLUSTER_MANAGER_TOPIC.equals(topic);
	}
	
	public static boolean isPeerCommand(String topic){
		return PEER_MANAGER_TOPIC.equals(topic);
	}
	
}
