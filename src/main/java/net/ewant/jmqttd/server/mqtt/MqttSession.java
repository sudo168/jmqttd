package net.ewant.jmqttd.server.mqtt;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import net.ewant.jmqttd.codec.MqttException;
import net.ewant.jmqttd.codec.message.MqttConnect;
import net.ewant.jmqttd.codec.message.MqttTopic;
import net.ewant.jmqttd.codec.message.MqttWireMessage;
import net.ewant.jmqttd.core.Closeable;
import net.ewant.jmqttd.core.ServerProtocol;
import net.ewant.jmqttd.utils.ProtocolUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.ewant.jmqttd.codec.message.MqttPublish;
import net.ewant.jmqttd.codec.message.MqttVersion;

import io.netty.channel.Channel;
import io.netty.util.AttributeKey;

public class MqttSession implements Closeable {
	
	private static final Logger logger = LoggerFactory.getLogger(MqttSession.class);

	public static final AttributeKey<String> IP_KEY = AttributeKey.valueOf("_real_ip_");
	
	private AtomicReference<State> sessionState = new AtomicReference<State>(State.UN_ACTIVE);

	private AtomicInteger messageIdHolder = new AtomicInteger();
	
	public enum State{
		UN_ACTIVE,
		ACTIVE,
		UN_AUTH,
		ERROR,
		DISCONNECT,
		CONNECT_TIMEOUT,
		CLIENT_TIMEOUT,
		UNEXPECTED_HALT,
		CLOSE,
	}
	
	private ServerProtocol protocol;
	private String id;
	private Channel channel;
	private MqttVersion version;
	private int keepAlive;
	private String userName;
	private String password;
	private boolean cleanSession;
	private MqttPublish willMessage;
	
	private Map<String, MqttTopic> subTopics;
	private ClientType clientType = ClientType.UNKNOWN;
	private String clientVersion;

	public MqttSession(Channel channel) {
		this.channel = channel;
		this.id = channel.id().asLongText();
	}

	public void start(MqttConnect conn){
		if(sessionState.compareAndSet(State.UN_ACTIVE, State.ACTIVE)){
			this.id = conn.getClientId() != null ? conn.getClientId() : id;
			this.version = conn.getVersion();
			this.keepAlive = conn.getKeepAlive();
			this.userName = conn.getUserName();
			this.password = conn.getPassword();
			this.cleanSession = conn.isCleanSession();
			this.willMessage = conn.getWillMessage();
			this.subTopics = new HashMap<>();
		}else{
			throw new IllegalStateException("can not start session. cause an unavailable state " + sessionState.get());
		}
	}
	
	public Map<String, MqttTopic> getSubTopics() {
		return subTopics;
	}
	
	public boolean sub(MqttTopic topic) {
		if(this.protocol == ServerProtocol.CLUSTER){
			TopicManager.systemSubscribe(this, topic);
		}else{
			TopicManager.clientSubscribe(this, topic);
		}
		subTopics.put(topic.getName(), topic);
		MqttServerContext.getServer(protocol).getSessionListener().onSubscribe(this, topic);
		return true;
	}
	
	public MqttTopic unsub(String topic) {
		if(this.protocol == ServerProtocol.CLUSTER){
			TopicManager.systemUnSubscribe(this, topic);
		}else{
			TopicManager.clientUnSubscribe(this, topic);
		}
		MqttTopic mqttTopic = subTopics.remove(topic);
		MqttServerContext.getServer(protocol).getSessionListener().onUnSubscribe(this, mqttTopic);
		return mqttTopic;
	}
	
	public State getState() {
		return sessionState.get();
	}
	
	public State updateState(State newState){
		return sessionState.getAndSet(newState);
	}
	
	protected AttributeKey<MqttSession> channelAttrKey(){
		return AttributeKey.valueOf(channel.id().asLongText());
	}

	public Channel getChannel() {
		return channel;
	}

	public void setChannel(Channel channel) {
		if(this.channel instanceof ChannelWrapper && !(channel instanceof ChannelWrapper)){
			((ChannelWrapper) this.channel).setChannel(channel);
		}else{
			this.channel = channel;
		}
	}

	public String getId() {
		return id;
	}

	public String getIP() {
		String ip = channel.attr(IP_KEY).get();
		if(ip != null){
			return ip;
		}
		return ((InetSocketAddress) channel.remoteAddress()).getAddress().getHostAddress();
	}
	
	public MqttVersion getVersion() throws MqttException {
		checkSessionStart();
		return version;
	}

	public int getKeepAlive() throws MqttException {
		checkSessionStart();
		return keepAlive;
	}

	public ServerProtocol getProtocol() {
		return protocol;
	}

	public void setProtocol(ServerProtocol protocol) {
		this.protocol = protocol;
	}

	public String getUserName() throws MqttException {
		checkSessionStart();
		return userName;
	}

	public String getPassword() throws MqttException {
		checkSessionStart();
		return password;
	}

	public ClientType getClientType() {
		return clientType;
	}

	public void setClientType(ClientType clientType) {
		this.clientType = clientType;
	}

	public String getClientVersion() {
		return clientVersion;
	}

	public void setClientVersion(String clientVersion) {
		this.clientVersion = clientVersion;
	}

	public boolean isCleanSession() throws MqttException {
		checkSessionStart();
		return cleanSession;
	}

	public MqttWireMessage getWillMessage() throws MqttException {
		checkSessionStart();
		return willMessage;
	}

	public void send(MqttWireMessage message, boolean resetMessageId) throws MqttException {
		checkSessionStart();
		if(resetMessageId && message instanceof MqttPublish){
            message.setMessageId(getMessageId());
		}
		// TODO 统计发送数
		this.channel.writeAndFlush(message);
	}

	public void send(MqttWireMessage message) throws MqttException {
		this.send(message, false);
	}

	private int getMessageId(){
        int msgId = messageIdHolder.incrementAndGet();
        if(msgId > MqttWireMessage.MAX_MSG_ID){
            messageIdHolder.compareAndSet(msgId, 0);
            return getMessageId();
        }
        return msgId;
    }
	
	public void setAttr(String key, Object value){
		this.channel.attr(AttributeKey.newInstance(key)).set(value);
	}
	
	public Object getAttr(String key){
		return this.channel.attr(AttributeKey.newInstance(key)).get();
	}
	
	public Object removeAttr(String key){
		return this.channel.attr(AttributeKey.newInstance(key)).getAndSet(null);
	}

	private void checkSessionStart(){
		if(sessionState.get() != State.ACTIVE){
			throw new MqttException("cat not operation a " + sessionState.get() + " session !");
		}
	}

	/**
	 * https://stackoverflow.com/questions/21240981/in-netty-4-whats-the-difference-between-ctx-close-and-ctx-channel-close
	 */
	public void close(){
		
		MqttSessionManager.remove(id);
		
		switch (sessionState.get()) {
			case DISCONNECT: // 客户端正常关闭
				logger.info("close session: [{}] clearSession flag: {}", this, cleanSession);
				// store QoS 1 , QoS 2 message and all subscriptions after client disconnect
				if(!cleanSession){
					MqttSessionManager.retain(this);
				}
				break;
			case ERROR:// 服务器异常导致关闭
				logger.info("close an ERROR session: [{}]", this);
				break;
			case UNEXPECTED_HALT:// 未知原因意外关闭
				logger.info("close an UNEXPECTED_HALT session: [{}]", this);
				if(this.willMessage != null){
					// TODO send will message to target topic
				}
				break;
			case CONNECT_TIMEOUT:
			case CLIENT_TIMEOUT:
			case UN_AUTH:
			default:
				logger.info("close an {} session: [{}]", sessionState.get(), this);
				break;
		}
		this.updateState(State.CLOSE);
		if(this.channel != null){
			this.channel.close();
			this.channel = null;
		}
		Iterator<String> iterator = subTopics.keySet().iterator();
		while(iterator.hasNext()){
			String next = iterator.next();
			iterator.remove();
			this.unsub(next);
		}
	}

	@Override
	public String toString() {
		return id + "@" + ProtocolUtil.toSessionId(channel);
	}

	enum ClientType{
		UNKNOWN(0), ANDROID(1), IOS(2), WEB(3), H5(4), PC(5);
		private int value;
		ClientType(int value){
			this.value = value;
		}
		public int value() {
			return value;
		}
	}
}
