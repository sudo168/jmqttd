package net.ewant.jmqttd.listener;

import net.ewant.jmqttd.cluster.Peer;
import net.ewant.jmqttd.codec.message.MqttPublish;
import net.ewant.jmqttd.server.mqtt.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class MqttMessageDispatcher {

    public static void dispatch(MqttPublish message, boolean isFromPeer){
        // TODO 待优化，采用多线程 多任务并行分发消息
        List<TopicMapping> topicMappings = TopicManager.clientMatch(message.getTopic().getName());
        if(topicMappings != null && !topicMappings.isEmpty()){
            for(TopicMapping tm : topicMappings){
                Map<String, Integer> subscribers = tm.getSubscribers();
                if(subscribers != null && !subscribers.isEmpty()){
                    for(String clientId : subscribers.keySet()){
                        MqttSession session = MqttSessionManager.getSession(clientId);
                        if(session != null){// TODO qos1、qos2消息 单独维护messageId
                            session.send(message);
                        }
                    }
                }
                if(!isFromPeer){
                    Collection<Peer> routeTable = tm.getRouteTable();
                    for(Peer peer : routeTable){// 其他集群节点的订阅者
                        // TODO 将消息分发到其他节点（不用过滤，直接发送）
                        System.out.println();
                    }
                }
            }
        }
    }
}
