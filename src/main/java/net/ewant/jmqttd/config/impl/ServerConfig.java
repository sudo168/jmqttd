package net.ewant.jmqttd.config.impl;

import java.io.File;

public class ServerConfig {
	private String username;
	private String password;
	/**
	 * 接收客户端连接线程数
	 */
	private int accptors = 2;
	/**
	 * 处理客户端读写线程数
	 */
	private int workers = 8;
	/**
	 * 整个消息队列长度
	 */
	private int windowSize = -1;
	/**
	 * 发送但未收到回执消息队列长度
	 */
	private int maxInfight = 10240;
	/**
	 * 允许客户端最大连接数（tcp、ws、http之和，-1 表示不限制）
	 */
	private int maxClient = 1000000;
	/**
	 * 客户端socket通道打开后，多久没法送connect消息（秒）
	 */
	private int connectTimeout = 10;
	/**
	 * 客户端间隔多久发没有消息超时时间（秒）
	 */
	private int clientIdleTimeout = 120;
	/**
	 * 客户端连接成功后，首次发送数据时间（含ping，秒）
	 */
	private int firstDataTimeout = 120;
	/**
	 * 是否允许匿名连接
	 */
	private boolean allowAnonymous;
	/**
	 * 是否启用acl权限控制
	 */
	private boolean aclEnable;
	/**
	 * acl权限控制配置文件
	 */
	private File aclFile;
	
	/**
	 * publish 消息存储配置
	 */
	private Store store;

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public int getAccptors() {
		return accptors;
	}

	public void setAccptors(int accptors) {
		this.accptors = accptors;
	}

	public int getWorkers() {
		return workers;
	}

	public void setWorkers(int workers) {
		this.workers = workers;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public void setWindowSize(int windowSize) {
		this.windowSize = windowSize;
	}

	public int getMaxInfight() {
		return maxInfight;
	}

	public void setMaxInfight(int maxInfight) {
		this.maxInfight = maxInfight;
	}

	public int getMaxClient() {
		return maxClient;
	}

	public void setMaxClient(int maxClient) {
		this.maxClient = maxClient;
	}

	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getClientIdleTimeout() {
		return clientIdleTimeout;
	}

	public void setClientIdleTimeout(int clientIdleTimeout) {
		this.clientIdleTimeout = clientIdleTimeout;
	}

	public int getFirstDataTimeout() {
		return firstDataTimeout;
	}

	public void setFirstDataTimeout(int firstDataTimeout) {
		this.firstDataTimeout = firstDataTimeout;
	}

	public boolean isAllowAnonymous() {
		return allowAnonymous;
	}

	public void setAllowAnonymous(boolean allowAnonymous) {
		this.allowAnonymous = allowAnonymous;
	}

	public boolean isAclEnable() {
		return aclEnable;
	}

	public void setAclEnable(boolean aclEnable) {
		this.aclEnable = aclEnable;
	}

	public File getAclFile() {
		return aclFile;
	}

	public void setAclFile(File aclFile) {
		this.aclFile = aclFile;
	}
	
	public Store getStore() {
		return store;
	}

	public void setStore(Store store) {
		this.store = store;
	}

	public static class Store{
		private String path;
		private String publishPersistor;
		private String pubrelPersistor;
		private String flushInterval;
		private String flushPerBytes;
		
		public String getPath() {
			return path;
		}
		public void setPath(String path) {
			this.path = path;
		}
		public String getPublishPersistor() {
			return publishPersistor;
		}
		public void setPublishPersistor(String publishPersistor) {
			this.publishPersistor = publishPersistor;
		}
		public String getPubrelPersistor() {
			return pubrelPersistor;
		}
		public void setPubrelPersistor(String pubrelPersistor) {
			this.pubrelPersistor = pubrelPersistor;
		}
		public String getFlushInterval() {
			return flushInterval;
		}
		public void setFlushInterval(String flushInterval) {
			this.flushInterval = flushInterval;
		}
		public String getFlushPerBytes() {
			return flushPerBytes;
		}
		public void setFlushPerBytes(String flushPerBytes) {
			this.flushPerBytes = flushPerBytes;
		}
	}
}
