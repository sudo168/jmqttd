package net.ewant.jmqttd.listener;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.ewant.jmqttd.cluster.Peer;
import net.ewant.jmqttd.interceptor.MessageFilterChain;
import net.ewant.jmqttd.server.mqtt.*;
import net.ewant.jmqttd.codec.message.MqttConnect;
import net.ewant.jmqttd.codec.message.MqttPing;
import net.ewant.jmqttd.codec.message.MqttQoS;
import net.ewant.jmqttd.codec.message.MqttSubscribe;
import net.ewant.jmqttd.core.Closeable;
import net.ewant.jmqttd.persistent.MqttPublishPersistence;
import net.ewant.jmqttd.utils.ProtocolUtil;
import net.ewant.jmqttd.utils.ReflectUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.codec.message.MqttConnAck;
import net.ewant.jmqttd.codec.message.MqttPong;
import net.ewant.jmqttd.codec.message.MqttPubAck;
import net.ewant.jmqttd.codec.message.MqttPubComp;
import net.ewant.jmqttd.codec.message.MqttPubRec;
import net.ewant.jmqttd.codec.message.MqttPubRel;
import net.ewant.jmqttd.codec.message.MqttPublish;
import net.ewant.jmqttd.codec.message.MqttSubAck;
import net.ewant.jmqttd.codec.message.MqttUnsubAck;
import net.ewant.jmqttd.codec.message.MqttUnsubscribe;
import net.ewant.jmqttd.core.ServerProtocol;
import net.ewant.jmqttd.interceptor.AccessControlChain;
import net.ewant.jmqttd.interceptor.ConnectionAuthChain;
import net.ewant.jmqttd.server.mqtt.MqttSession.State;
import net.ewant.jmqttd.utils.PersistenceUtil;

public class MqttMessageListener implements MqttAckReceiveListener, MqttMessageArriveListener, Closeable {

	Logger logger = LoggerFactory.getLogger(getClass());

	private MqttServer server;

	public MqttMessageListener(ServerProtocol protocol){
		this.server = MqttServerContext.getServer(protocol);
	}

	public void onPublish(MqttSession client, MqttPublish message) {
		AccessControlChain accessControlChain = this.server.getAccessControlChain();
		boolean canPublish = accessControlChain.canPublish(client, message);
		if(!canPublish){
			return;
		}
		MqttPublishPersistence mqttPersistence = this.server.getPublishPersist();
		String storeKey = PersistenceUtil.getTempStoreKey(client.getId(), message.getTopic().getName(), message.getMessageId());
		MqttPublish oldMessage = mqttPersistence.get(storeKey);
		if(!message.isDuplicate() && oldMessage == null){
			// TODO 存储消息。收到的所有消息都要存储，不管QoS
			//mqttPersistence.put(storeKey, message);
		}
		// 当QoS为0，(将消息发给订阅者) 不需要回复
		// 当QoS为1，(保存PUBLISH消息) (将消息发给订阅者) 回复PUBACK，发送者收到PUBACK后丢弃发送的PUBLISH消息
		// 当QoS为2，(保存PUBLISH消息) 回复PUBREC，待再收到PUBREL（QoS=1）后(将消息发给订阅者) 回复PUBCOMP，发送者收到PUBCOMP后丢弃发送的PUBLISH消息
		// 当QoS > 0 , 需要带上MessageID
		boolean canDispatch = true;
		MqttQoS qos = message.getQos();
		if(qos == MqttQoS.AT_LEAST_ONCE){
			client.send(new MqttPubAck(message.getMessageId()));
			if(oldMessage != null){
				oldMessage.countUpAckState();
			}else{
				message.countUpAckState();
			}
		}else if(qos == MqttQoS.EXACTLY_ONCE){
			canDispatch = false;
			client.send(new MqttPubRec(message.getMessageId()));
			if(message.getAckState() == 0){
				message.setAckState(100000);
			}else{
				message.countUpAckState();
			}
		}
		if(!canDispatch){
			return;
		}
		dispatchMessage(client, message);

	}

	private void dispatchMessage(MqttSession client, MqttPublish message){
		// 消息分发，当没有人订阅时，消息是否保存？保存多少？保存多久？

		//1. 集群消息
		boolean isFromPeer = false;
		if(this.server.getProtocol() == ServerProtocol.CLUSTER){
			boolean clusterDataSyn = TopicManager.isClusterDataSync(message.getTopic().getName());
			if(!clusterDataSyn){// 其他集群控制消息
				// TODO
				return;
			}
			isFromPeer = true;
		}else{// 非集群同步消息需要过滤
			MessageFilterChain messageFilterChain = this.server.getMessageFilterChain();
			byte[] bytes = messageFilterChain.doFilter(client, message.getTopic(), message.getPayload());
			if(bytes == null){
				// TODO 不用下发
				return;
			}
			message.setPayload(bytes);
		}

		//2. 需要发往对端的消息（应当另起线程）
		MqttMessageDispatcher.dispatch(message, isFromPeer);
	}

	public void onPubRel(MqttSession client, MqttPubRel message) {
		// server maybe received this message more than 1 times
		client.send(new MqttPubComp(message.getMessageId()));
		
		MqttPublishPersistence mqttPersistence = this.server.getPublishPersist();
		// TODO
		String storeKey = PersistenceUtil.getTempStoreKey(client.getId(), "", message.getMessageId());
		MqttPublish publish = mqttPersistence.get(storeKey);
		if(publish != null){
			if(publish.getAckState() < 200000){
				publish.setAckState(200000);
			}else{
				publish.countUpAckState();
			}
			dispatchMessage(client, publish);
		}
	}

	public void onSubscribe(MqttSession client, MqttSubscribe message) {
		AccessControlChain accessControlChain = this.server.getAccessControlChain();
		client.send(new MqttSubAck(message, accessControlChain.canSubscribe(client, message)));
	}

	public void onUnsubscribe(MqttSession client, MqttUnsubscribe message) {
		// 客户端取消订阅
		List<String> unSubTopic = message.getUnSubTopic();
		for (String topic : unSubTopic) {
			client.unsub(topic);
		}
		logger.info("client: {}, unsubscribe: {}", client.getId(), JSON.toJSONString(unSubTopic));
	}

	public void onPing(MqttSession client, MqttPing message) {
		client.send(new MqttPong());
	}

	public void onConnect(MqttSession client, MqttConnect connect) {
		processConnection(client);
	}
	private void processConnection(MqttSession current){
		
		MqttSession sessionToSave = current;
		
		// 如果CleanSession = 1 并且 return Code = 0 时，此值必须为0 
		// 如果CleanSession = 0 并且 return Code = 0 时，如果服务端已经存有当前clientID相关信息的返回1，否则0
		// 当 return Code != 0 时，此值必须为0
		boolean sessionPresent = false;
		int returnCode = MqttConnAck.ACCEPTED;
		try {
			if(current == null){
				throw new MqttException("MQTT connect packet arrived after connection timeout[" + (server.getConfiguration().getServerConfig().getConnectTimeout()) + "s]");
			}
			MqttSession oldSession = MqttSessionManager.unRetain(current.getId());
			if(current.isCleanSession()){
				if(oldSession != null){
					oldSession.close();
				}
			}else{
				MqttSession prevSession = MqttSessionManager.getSession(current.getId());
				if(prevSession != null && prevSession.getChannel() != null && prevSession.getChannel().isActive()){
					// multiple clients with the same client id 
					returnCode = MqttConnAck.IDENTIFIER_REJECTED;
					logger.info("multiple clients[exists:{} new:{}] with the same client id[{}] rejected.", ProtocolUtil.toSessionId(prevSession.getChannel()), ProtocolUtil.toSessionId(current.getChannel()), prevSession.getId());
				}
				if(oldSession != null){// restore to use
					oldSession.setChannel(current.getChannel());
					oldSession.updateState(State.ACTIVE);
					sessionToSave = oldSession;
					sessionPresent = true;
				}
			}
			if(current.getVersion() == null){
				returnCode = MqttConnAck.UNACCEPTABLE_PROTOCOL_VERSION;
			}
			
			// invoke ConnectionAuthChain
			if(returnCode == MqttConnAck.ACCEPTED){
				ConnectionAuthChain connectionAuthChain = server.getConnectionAuthChain();
				if(!connectionAuthChain.validClientId(sessionToSave)){
					returnCode = MqttConnAck.IDENTIFIER_REJECTED;
				}else if(!connectionAuthChain.validUsernamePassword(sessionToSave)){
					returnCode = MqttConnAck.BAD_USER_NAME_OR_PASSWORD;
				}else if(!connectionAuthChain.authentication(sessionToSave)){
					returnCode = MqttConnAck.NOT_AUTHORIZED;
				}
			}
			
		} catch (Exception e) {
			returnCode = MqttConnAck.SERVER_UNAVAILABLE;
			logger.error(e.getMessage() + ". cause: " + e.getClass().getName() + ", at " + ReflectUtil.getAvailableStack(e));
		}
		
		if(returnCode != MqttConnAck.ACCEPTED){
			sessionPresent = false;
			sessionToSave = null;
		}else{
			MqttSessionManager.addSession(sessionToSave);
		}
		
		current.send(new MqttConnAck(returnCode, sessionPresent));
		
		if(sessionToSave == null){
			current.updateState(State.UN_AUTH);
			current.close();
		}
	}

	public void onDisconnect(MqttSession client) {
		// Nothing to do
	}

	public void onPubAck(MqttSession client, MqttPubAck message) {
		// TODO in client mode, discard the publish message
	}

	public void onPubRec(MqttSession client, MqttPubRec message) {
		// TODO in client mode, acknowledge PUBREL(repeat if not PubComp received)
		
	}

	public void onPubComp(MqttSession client, MqttPubComp message) {
		// TODO in client mode QoS = 2 message finish. discard the publish message
		
	}

	public void onSubAck(MqttSession client, MqttSubAck message) {
		// TODO in client mode
		
	}

	public void onUnsubAck(MqttSession client, MqttUnsubAck message) {
		// TODO in client mode
		
	}

	public void onPong(MqttSession client, MqttPong message) {
		// TODO in client mode
		
	}

	public void onConnAck(MqttSession client, MqttConnAck ack) {
		// TODO in client mode, notify listener connected
		
	}

	@Override
	public void close() {
	}
	
}
