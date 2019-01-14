package net.ewant.jmqttd.listener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.ewant.jmqttd.codec.message.MqttSubscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.codec.message.MqttConnAck;
import net.ewant.jmqttd.codec.message.MqttConnect;
import net.ewant.jmqttd.codec.message.MqttMessageType;
import net.ewant.jmqttd.codec.message.MqttPing;
import net.ewant.jmqttd.codec.message.MqttPong;
import net.ewant.jmqttd.codec.message.MqttPubAck;
import net.ewant.jmqttd.codec.message.MqttPubComp;
import net.ewant.jmqttd.codec.message.MqttPubRec;
import net.ewant.jmqttd.codec.message.MqttPubRel;
import net.ewant.jmqttd.codec.message.MqttPublish;
import net.ewant.jmqttd.codec.message.MqttSubAck;
import net.ewant.jmqttd.codec.message.MqttUnsubAck;
import net.ewant.jmqttd.codec.message.MqttUnsubscribe;
import net.ewant.jmqttd.codec.message.MqttWireMessage;
import net.ewant.jmqttd.core.Closeable;
import net.ewant.jmqttd.core.ServerProtocol;
import net.ewant.jmqttd.scheduler.HashedTimeoutScheduler;
import net.ewant.jmqttd.scheduler.SchedulerKey;
import net.ewant.jmqttd.scheduler.TimeoutDataHolder;
import net.ewant.jmqttd.server.mqtt.MqttServer;
import net.ewant.jmqttd.server.mqtt.MqttServerContext;
import net.ewant.jmqttd.server.mqtt.MqttSession;
import net.ewant.jmqttd.server.mqtt.MqttSession.State;
import net.ewant.jmqttd.server.mqtt.MqttSessionManager;
import net.ewant.jmqttd.utils.ProtocolUtil;
import net.ewant.jmqttd.utils.ReflectUtil;

import io.netty.channel.Channel;

public class MqttSessionListener implements Closeable{

    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private static AtomicInteger SESSION_COUNT = new AtomicInteger();

    private MqttServer server;
    private HashedTimeoutScheduler<MqttSession> scheduler;
    private MqttMessageListener messageListener;

    public MqttSessionListener(ServerProtocol protocol){
        this.server = MqttServerContext.getServer(protocol);
        this.scheduler = new HashedTimeoutScheduler<MqttSession>(SchedulerKey.Type.CONNECT_TIMEOUT.name());
        messageListener = new MqttMessageListener(protocol);
    }

    /**
     * 客户端发送connect消息时触发
     * @param channel
     * @param connect
     * @return
     */
    public MqttSession onSessionStart(Channel channel, MqttConnect connect){
        TimeoutDataHolder<MqttSession> sessionTimeoutDataHolder = scheduler.cancel(SchedulerKey.forKey(SchedulerKey.Type.CONNECT_TIMEOUT, channel));
        if(sessionTimeoutDataHolder != null){
            sessionTimeoutDataHolder.getTimeout().cancel();
            MqttSession mqttSession = sessionTimeoutDataHolder.getData();
            mqttSession.start(connect);
            return mqttSession;
        }
        return null;
    }

    /**
     * 客户端连接进来时触发
     * @param channel
     */
    public void onSessionOpen(final Channel channel){
    	int maxClient = server.getConfiguration().getServerConfig().getMaxClient();
    	if(SESSION_COUNT.get() + 1 > maxClient){
    		logger.error("Too many client, max client: {}", maxClient);
    		channel.close();
    		return;
    	}
    	SESSION_COUNT.incrementAndGet();
        final MqttSession session = new MqttSession(channel);
        session.setProtocol(server.getProtocol());
        final int connectTimeout = server.getConfiguration().getServerConfig().getConnectTimeout();
        TimeoutDataHolder<MqttSession> sessionTimeoutDataHolder = scheduler.schedule(SchedulerKey.forKey(SchedulerKey.Type.CONNECT_TIMEOUT, channel), new Runnable() {
            @Override
            public void run() {
                logger.info("MQTT session [{}] {} {}s", ProtocolUtil.toSessionId(channel), SchedulerKey.Type.CONNECT_TIMEOUT, connectTimeout);
                session.updateState(State.CONNECT_TIMEOUT);
                session.close();
            }
        }, connectTimeout, TimeUnit.SECONDS);

        sessionTimeoutDataHolder.setData(session);
        logger.info("MQTT session open [{}] ", ProtocolUtil.toSessionId(channel));
    }

    public void onSessionClose(Channel channel){
        MqttSession client = MqttSessionManager.getSession(channel);
        if(client != null){
        	if(client.getState() == State.ACTIVE){
        		client.updateState(State.UNEXPECTED_HALT);
        	}
        	SESSION_COUNT.decrementAndGet();
            client.close();
        }
    }

    public void onSessionException(Channel channel, MqttSession client, Throwable cause){
        logger.error("MQTT server cause an exception : " + cause.getClass().getName()+ "[" + cause.getMessage() + "] at " + ReflectUtil.getAvailableStack(cause) + " [client in " + ProtocolUtil.toSessionId(channel) + " is " + (client == null ? "NULL" : client.getId()) + "]", cause);
        try {
            if(!(cause instanceof MqttException)){
                if(client != null){
                	client.updateState(State.ERROR);
                    client.close();
                }
            }
        } catch (Exception e) {
            logger.error("MQTT session close error: " + e.getMessage(), e);
        }
    }

    public void onMessage(Channel channel, MqttSession client, MqttWireMessage message){
        logger.info("MQTT message[{}-{}] arrived from client[{}]", message.getClass().getSimpleName(), message.getMessageId(), client == null ? ProtocolUtil.toSessionId(channel) : client);
        MqttMessageType type = message.getType();
        // TODO 统计接收数
        if (type == MqttMessageType.CONNECT) {
            client = this.onSessionStart(channel, (MqttConnect) message);
            messageListener.onConnect(client, (MqttConnect) message);
        }else if (type == MqttMessageType.CONNACK) {
            messageListener.onConnAck(client, (MqttConnAck) message);
        }else if (type == MqttMessageType.PUBLISH) {
            messageListener.onPublish(client, (MqttPublish) message);
        }else if (type == MqttMessageType.PUBACK) {
            messageListener.onPubAck(client, (MqttPubAck) message);
        }else if (type == MqttMessageType.PUBREC) {
            messageListener.onPubRec(client, (MqttPubRec) message);
        }else if (type == MqttMessageType.PUBREL) {
            messageListener.onPubRel(client, (MqttPubRel) message);
        }else if (type == MqttMessageType.PUBCOMP) {
            messageListener.onPubComp(client, (MqttPubComp) message);
        }else if (type == MqttMessageType.SUBSCRIBE) {
            messageListener.onSubscribe(client, (MqttSubscribe) message);
        }else if (type == MqttMessageType.SUBACK) {
            messageListener.onSubAck(client, (MqttSubAck) message);
        }else if (type == MqttMessageType.UNSUBSCRIBE) {
            messageListener.onUnsubscribe(client, (MqttUnsubscribe) message);
        }else if (type == MqttMessageType.UNSUBACK) {
            messageListener.onUnsubAck(client, (MqttUnsubAck) message);
        }else if (type == MqttMessageType.PING) {
            messageListener.onPing(client, (MqttPing) message);
        }else if (type == MqttMessageType.PONG) {
            messageListener.onPong(client, (MqttPong) message);
        }else if (type == MqttMessageType.DISCONNECT) {
            messageListener.onDisconnect(client);
        }
    }

    public void onSend(MqttSession client, MqttWireMessage message) {
        logger.info("send MQTT message[{}-{}] to client[{}]", message.getClass().getSimpleName(), message.getMessageId(), client);
    }

    @Override
    public void close() {
        this.scheduler.shutdown();
    }
}
