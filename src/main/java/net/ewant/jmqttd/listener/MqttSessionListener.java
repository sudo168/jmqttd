package net.ewant.jmqttd.listener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import net.ewant.jmqttd.codec.message.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ewant.jmqttd.codec.MqttException;
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

    static String NOTIFY_TOPIC = "$sys/clients";

    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private static AtomicInteger SESSION_COUNT = new AtomicInteger();

    private MqttServer server;
    private HashedTimeoutScheduler<MqttSession> scheduler;
    private MqttMessageListener messageListener;
    private ClientSessionListener clientSessionListener;

    public MqttSessionListener(ServerProtocol protocol){
        this.server = MqttServerContext.getServer(protocol);
        this.scheduler = new HashedTimeoutScheduler<>(SchedulerKey.Type.CONNECT_TIMEOUT.name());
        this.messageListener = new MqttMessageListener(protocol);
        this.clientSessionListener = new ClientSessionListenerWrapper(protocol);
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
            this.clientSessionListener.onSessionOpen(mqttSession);
            logger.info("MQTT session start [{}@{}] ", mqttSession.getId(), ProtocolUtil.toSessionId(channel));
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

    public void onSessionClose(MqttSession client, State state){
        if(client != null){
            SESSION_COUNT.decrementAndGet();
            client.updateState(state);
            client.close();
            this.clientSessionListener.onSessionClose(client);
        }
    }

    public void onSessionException(Channel channel, MqttSession client, Throwable cause){
        logger.error("MQTT server cause an exception : " + cause.getClass().getName()+ "[" + cause.getMessage() + "] at " + ReflectUtil.getAvailableStack(cause) + " [client in " + ProtocolUtil.toSessionId(channel) + " is " + (client == null ? "NULL" : client.getId()) + "]", cause);
        try {
            if(!(cause instanceof MqttException)){
                this.onSessionClose(client, State.ERROR);
            }
        } catch (Exception e) {
            logger.error("MQTT session close error: " + e.getMessage(), e);
        }
    }

    public void onMessage(Channel channel, MqttSession client, MqttWireMessage message){
        logger.debug("MQTT message[{}-{}] arrived from client[{}]", message.getClass().getSimpleName(), message.getMessageId(), client == null ? ProtocolUtil.toSessionId(channel) : client);
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
            this.onSessionClose(client, State.DISCONNECT);
        }
    }

    public void onSend(MqttSession client, MqttWireMessage message) {
        logger.debug("send MQTT message[{}-{}] to client[{}]", message.getClass().getSimpleName(), message.getMessageId(), client);
    }

    @Override
    public void close() {
        this.scheduler.shutdown();
    }

    public void onSubscribe(MqttSession session, MqttTopic subTopic){
        if(subTopic.getName().equals(NOTIFY_TOPIC)){
            // TODO 注意占用订阅线程
            for(MqttSession client : MqttSessionManager.getClients()){
                if(session != client){
                    session.send(generaSessionNotifyMessage(client, false));
                }
            }
        }
    }

    public void onUnSubscribe(MqttSession session, MqttTopic subTopic){
    }

    public static MqttPublish generaSessionNotifyMessage(MqttSession session, boolean isClose){
        StringBuilder sb = new StringBuilder();
        sb.append(session.getId());
        if(!isClose){
            sb.append(",");
            sb.append(session.getIP());
            sb.append(",");
            sb.append(session.getUserName() == null ? "" : session.getUserName());
            sb.append(",");
            sb.append(session.getPassword() == null ? "" : session.getPassword());
        }
        return new MqttPublish(new MqttTopic(NOTIFY_TOPIC, MqttQoS.AT_LEAST_ONCE), false, sb.toString().getBytes());
    }
}
