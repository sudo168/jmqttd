package net.ewant.jmqttd.config.impl;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Map;

import net.ewant.jmqttd.config.HostPortSslConfiguration;
import net.ewant.jmqttd.config.InitializingConfig;
import net.ewant.jmqttd.core.ServerProtocol;
import net.ewant.jmqttd.utils.ProtocolUtil;
import net.ewant.jmqttd.utils.ReflectUtil;

public class ClusterConfig implements InitializingConfig{
	
	private String discovery;
	
	private Discovery protocol;
	
	private String groupId;
	
	private String host;
	
	private int port = 40008;
	
	private TTL ttl = TTL.SAME_SITE;
	
	public Discovery getProtocol() {
		return protocol;
	}

	public void setProtocol(Discovery protocol) {
		this.protocol = protocol;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getHost() {
		if(host == null){
			return ProtocolUtil.getLocalHost();
		}
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public TTL getTtl() {
		return ttl;
	}

	public void setTtl(TTL ttl) {
		this.ttl = ttl;
	}
	
	public boolean valid(){
		return this.groupId != null && this.host != null && this.port > 0;
	}

	public static enum Discovery{
		MCAST, STATIC, ZK
	}
	
	public static enum TTL{
		SAME_HOST(0), SAME_SUBNET(1), SAME_SITE(32), SAME_REGION(64), SAME_CONTINENT(128), UNRESTRICTED(255);
		private int value;
		TTL(int value){
			this.value = value;
		}
		public int getValue() {
			return value;
		}
	}
	
	public HostPortSslConfiguration toHostPortConfig(){
		return new ClusterHostPortConfiguration(this);
	}
	
	public static class ClusterHostPortConfiguration extends HostPortSslConfiguration{
		public ClusterHostPortConfiguration(ClusterConfig config){
			this.setProtocol(ServerProtocol.CLUSTER);
			this.setEnable(true);
			this.setHost(config.getHost());
			this.setPort(config.getPort());
		}
	}
	
	public void setDiscovery(String discovery) {
		this.discovery = discovery;
		init();
	}
	
	public void init() {
		if(discovery == null){
			return ;
		}
		try {
			URL url = new URL(null, this.discovery, new URLStreamHandler(){
				@Override
				protected URLConnection openConnection(URL u) throws IOException {
					return null;
				}
			});
			this.protocol = Discovery.valueOf(url.getProtocol().toUpperCase());
			if(protocol == Discovery.MCAST){
				this.host = url.getHost();
				this.port = url.getPort();
				Map<String, String> paramsMap = ProtocolUtil.parseQueryString(url.getQuery());
				this.ttl = ReflectUtil.getEnum(TTL.class, paramsMap.get("ttl"));
			}else if(protocol == Discovery.STATIC){
				// TODO not implement
			}else if(protocol == Discovery.ZK){
				// TODO not implement
			}
		} catch (Exception e) {
			// NOOP
		}
	}
	
	public static void main(String[] args) throws Exception {
		URL url = new URL(null, "STATIC://127.0.0.1:40008/127.0.0.1:40008/127.0.0.1:40008?ttl=32", new URLStreamHandler(){
			@Override
			protected URLConnection openConnection(URL u) throws IOException {
				return null;
			}
		});
		Map<String, String> paramsMap = ProtocolUtil.parseQueryString(url.getQuery());
		TTL ttl2 = ReflectUtil.getEnum(TTL.class, paramsMap.get("ttl"));
		System.out.println(ttl2);
	}
}
