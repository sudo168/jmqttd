package net.ewant.jmqttd.server.mqtt;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import net.ewant.jmqttd.cluster.Peer;

public class TopicMapping {

	/**
	 * topic name 订阅的主题名
	 */
    private String name;

    /**
     * client and subscribe QoS
     */
    private transient Map<String, Integer> subscribers = new ConcurrentHashMap<String, Integer>();

    /**
     * 该主题订阅的节点路由表
     */
    private Collection<Peer> routeTable = new ConcurrentSkipListSet<Peer>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Integer> getSubscribers() {
        return subscribers;
    }

    public Collection<Peer> getRouteTable() {
		return routeTable;
	}
}
