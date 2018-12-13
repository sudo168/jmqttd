package com.ewant.jmqttd.server.mqtt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import io.netty.util.ResourceLeakDetector;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ewant.jmqttd.cluster.Peer;
import com.ewant.jmqttd.cluster.PeerListener;
import com.ewant.jmqttd.codec.MqttException;
import com.ewant.jmqttd.codec.message.MqttPubRel;
import com.ewant.jmqttd.codec.message.MqttPublish;
import com.ewant.jmqttd.codec.message.MqttTopic;
import com.ewant.jmqttd.config.AccessControlConfig;
import com.ewant.jmqttd.config.ConfigParseException;
import com.ewant.jmqttd.config.ConfigParseResult;
import com.ewant.jmqttd.config.HostPortSslConfiguration;
import com.ewant.jmqttd.config.ServerConfiguration;
import com.ewant.jmqttd.config.ServerConfigurationParser;
import com.ewant.jmqttd.config.impl.AclPermissionAccess;
import com.ewant.jmqttd.config.impl.ListenersConfig;
import com.ewant.jmqttd.config.impl.ServerConfig.Store;
import com.ewant.jmqttd.core.AbstractServer;
import com.ewant.jmqttd.core.ServerProtocol;
import com.ewant.jmqttd.interceptor.AccessControlChain;
import com.ewant.jmqttd.interceptor.AccessControlInterceptor;
import com.ewant.jmqttd.interceptor.ConnectionAuthChain;
import com.ewant.jmqttd.interceptor.ConnectionAuthInterceptor;
import com.ewant.jmqttd.interceptor.MessageFilterChain;
import com.ewant.jmqttd.listener.MqttSessionListener;
import com.ewant.jmqttd.persistent.FileSystemMqttPubRelPersistence;
import com.ewant.jmqttd.persistent.FileSystemMqttPublishPersistence;
import com.ewant.jmqttd.persistent.MqttPubRelPersistence;
import com.ewant.jmqttd.persistent.MqttPublishPersistence;
import com.ewant.jmqttd.scheduler.HashedTimeoutScheduler;
import com.ewant.jmqttd.scheduler.SchedulerKey;
import com.ewant.jmqttd.utils.PersistenceUtil;
import com.ewant.jmqttd.utils.ReflectUtil;

public class MqttServer extends AbstractServer<HostPortSslConfiguration> implements PeerListener{

	private static final Logger logger = LoggerFactory.getLogger(MqttServer.class);

	/**
	 * 默认配置文件
	 */
	private static String DEFAULT_CONFIG_NAME = "jmqttd.conf";

	/**
	 * 客户端监听器
	 */
	private MqttSessionListener sessionListener;
	/**
	 * 发布订阅权限控制链
	 */
	private AccessControlChain accessControlChain;
	/**
	 * 客户端连接验证链
	 */
	private ConnectionAuthChain connectionAuthChain;
	/**
	 * 消息过滤器链
	 */
	private MessageFilterChain messageFilterChain;
	/**
	 * qos 大于 0 的消息重发调度器
	 */
	private HashedTimeoutScheduler<MqttPublish> serverPublishScheduler;
	/**
	 * qos 大于 0 的消息重发调度器
	 */
	private HashedTimeoutScheduler<MqttPubRel> serverPubRelScheduler;
	
	private static MqttPublishPersistence publishPersistor;
	
	private static MqttPubRelPersistence pubrelPersistor;
	
    public MqttServer(ServerConfiguration configuration, HostPortSslConfiguration subConfig) {
    	super(configuration, subConfig);
    	this.serverPublishScheduler = new HashedTimeoutScheduler<>(SchedulerKey.Type.SERVER_PUBLISH_SCHEDULER.name());
    	this.serverPubRelScheduler = new HashedTimeoutScheduler<>(SchedulerKey.Type.SERVER_PUBREL_SCHEDULER.name());
    }
    
    public static void startup(String[] args){
    	// 开启内存泄漏提示
		ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.ADVANCED);
		final ServerConfiguration configuration;
		try {
			boolean hasConfigOption = false;
			for (String arg : args){
				if(hasConfigOption){
					DEFAULT_CONFIG_NAME = arg;
					break;
				}
				if("-c".equals(arg)){
					hasConfigOption = true;
				}
			}
			configuration = parseConfig();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return;
		}
		
    	ListenersConfig listenersConfig = configuration.getListenersConfig();
		List<HostPortSslConfiguration> configActives = listenersConfig.getActives();
		if(configActives.isEmpty()){
			logger.error("Not listeners active , Bye ...");
		}
		for (HostPortSslConfiguration config: configActives) {
			if(!config.isSsl() && config.isSslRequired()){
				logger.error("invalid [" + config.getProtocol() + "] security keyStore config.");
			}else{
				MqttServerContext.addServer(new MqttServer(configuration, config));
			}
		}

		Thread clusterStartWatcher = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					MqttServer.CLUSTER_START_WATCH.await();// wait all server complete
					MqttServer.initCluster(configuration);
				} catch (InterruptedException e) {
					// NOOP
				}
			}
		}, "Cluster-Start-Watcher");
		
		Collection<MqttServer> servers = MqttServerContext.getServers();
		if(servers != null && !servers.isEmpty()){
			MqttServer.CLUSTER_START_WATCH = new CountDownLatch(servers.size());
			clusterStartWatcher.start();
			for (MqttServer mqttServer : servers) {
				mqttServer.start();
			}
		}
    }

    public static ServerConfiguration parseConfig() throws ConfigParseException {

		ConfigParseResult<ServerConfiguration> configParseResult = new ServerConfigurationParser().parse(DEFAULT_CONFIG_NAME);
		for(Throwable cause : configParseResult.getCauses()){
			logger.error(cause.getMessage() + ". cause: " + cause.getClass().getName() + ", at " + ReflectUtil.getAvailableStack(cause));
		}

		ServerConfiguration serverConfiguration = configParseResult.getResult();
		if(serverConfiguration == null){
			throw new ConfigParseException("parse server configuration error.");
		}
		return serverConfiguration;
	}

	@Override
    public void initListeners(){
		sessionListener = new MqttSessionListener(getProtocol());
	}

	@Override
	public void initInterceptors() {
		ServerProtocol protocol = getProtocol();
		if(protocol == ServerProtocol.CLUSTER){
			List<AccessControlInterceptor> accessControlInterceptors = new ArrayList<>();
			accessControlInterceptors.add(new AccessControlInterceptor(){
				@Override
				public boolean matchSession(MqttSession client, AclPermissionAccess permissionAccess)
						throws MqttException {
					return true;
				}
				@Override
				public boolean canPublish(MqttSession client, MqttTopic pubTopic,
						AclPermissionAccess permissionAccess) {
					return TopicManager.isSystemTopic(pubTopic.getName());
				}
				@Override
				public boolean canSubscribe(MqttSession client, MqttTopic subTopic,
						AclPermissionAccess permissionAccess) {
					return TopicManager.isSystemTopic(subTopic.getName());
				}
			});
			accessControlChain = new AccessControlChain(protocol, accessControlInterceptors);
			List<ConnectionAuthInterceptor> connectionAuthInterceptors = new ArrayList<>();
			connectionAuthInterceptors.add(new ConnectionAuthInterceptor() {
				@Override
				public boolean matchSession(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
					Collection<Peer> remotePeers = peerDiscover.listRemotePeers();
					if(remotePeers != null){
						for (Peer peer : remotePeers) {
							if(peer.getHost().equals(client.getIP())){
								return true;
							}
						}
					}
					return false;
				}
				@Override
				public boolean validUsernamePassword(MqttSession client, AclPermissionAccess permissionAccess)
						throws MqttException {
					return true;
				}
				@Override
				public boolean validClientId(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
					return true;
				}
				@Override
				public boolean authentication(MqttSession client, AclPermissionAccess permissionAccess) throws MqttException {
					return true;
				}
			});
			connectionAuthChain = new ConnectionAuthChain(protocol, connectionAuthInterceptors);
			messageFilterChain = new MessageFilterChain(protocol, null);
		}else{
			AccessControlConfig aclConfig = configuration.getAclConfig();
			accessControlChain = new AccessControlChain(protocol, aclConfig == null ? null : aclConfig.getAccessControlInterceptors());
			connectionAuthChain = new ConnectionAuthChain(protocol, aclConfig == null ? null : aclConfig.getConnectionAuthInterceptors());
			messageFilterChain = new MessageFilterChain(protocol, aclConfig == null ? null : aclConfig.getMessageFilterInterceptors());
		}

	}
	
	@Override
	public void initStorage() {
		if(publishPersistor != null){
			return;
		}
		Store store = configuration.getServerConfig().getStore();
		try {
			if(store != null){
				if(StringUtils.isNotBlank(store.getPublishPersistor())){
					Class<?> persistClass = Class.forName(store.getPublishPersistor());
					if(MqttPublishPersistence.class.isAssignableFrom(persistClass)){
						publishPersistor = (MqttPublishPersistence) persistClass.newInstance();
						publishPersistor.open();
					}
				}
				if(StringUtils.isNotBlank(store.getPubrelPersistor())){
					Class<?> persistClass = Class.forName(store.getPubrelPersistor());
					if(MqttPubRelPersistence.class.isAssignableFrom(persistClass)){
						pubrelPersistor = (MqttPubRelPersistence) persistClass.newInstance();
						pubrelPersistor.open();
					}
				}
			}
		} catch (Exception e) {}
		
		if(publishPersistor == null){
			String path = store != null ? store.getPath() : "";
			FileSystemMqttPublishPersistence fileSystemMqttPersistence = new FileSystemMqttPublishPersistence(path);
			fileSystemMqttPersistence.setFlushIntervalMillis(PersistenceUtil.parseMillis(store.getFlushInterval()));
			fileSystemMqttPersistence.setCacheBytes(PersistenceUtil.parseBytes(store.getFlushPerBytes()));
			publishPersistor = fileSystemMqttPersistence;
			publishPersistor.open();
		}
		
		if(pubrelPersistor == null){
			String path = store != null ? store.getPath() : "";
			FileSystemMqttPubRelPersistence fileSystemMqttPersistence = new FileSystemMqttPubRelPersistence(path);
			fileSystemMqttPersistence.setFlushIntervalMillis(PersistenceUtil.parseMillis(store.getFlushInterval()));
			fileSystemMqttPersistence.setFlushPerBytes(PersistenceUtil.parseBytes(store.getFlushPerBytes()));
			pubrelPersistor = fileSystemMqttPersistence;
			pubrelPersistor.open();
		}
		logger.info("MQTT Publish message persist use: {}", publishPersistor.getClass());
		logger.info("MQTT PubRel message persist use: {}", pubrelPersistor.getClass());
	}

	@Override
	public void stop() {
		super.stop();
		sessionListener.close();
		MqttServerContext.removeServer(getProtocol());
	}

	public MqttSessionListener getSessionListener() {
		return sessionListener;
	}

	public AccessControlChain getAccessControlChain() {
		return accessControlChain;
	}

	public ConnectionAuthChain getConnectionAuthChain() {
		return connectionAuthChain;
	}

	public MessageFilterChain getMessageFilterChain() {
		return messageFilterChain;
	}
	
	public HashedTimeoutScheduler<MqttPublish> getServerPublishScheduler() {
		return serverPublishScheduler;
	}
	
	public HashedTimeoutScheduler<MqttPubRel> getServerPubRelScheduler() {
		return serverPubRelScheduler;
	}
	
	public MqttPublishPersistence getPublishPersist(){
		return publishPersistor;
	}
	
	public MqttPubRelPersistence getPubRelPersist(){
		return pubrelPersistor;
	}

	public static void shutdown(){
    	for (MqttServer mqttServer : MqttServerContext.getServers()) {
    		mqttServer.stop();
		}
    	if(MqttServer.peerDiscover != null){
    		MqttServer.peerDiscover.dispose();
    	}
    }

	@Override
	public void peerJoin(Peer peer) {
		// TODO 第一次加入时，应当把当前节点的主题信息同步过去。
	}

	@Override
	public void peerLeave(Peer peer) {
		// TODO 节点下线时，应当从主题路由表移除
	}
}
