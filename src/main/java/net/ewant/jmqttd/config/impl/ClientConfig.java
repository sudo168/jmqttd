package net.ewant.jmqttd.config.impl;

public class ClientConfig {
	
	/**
	 * 客户端发出ping消息后，没收到服务端响应pong的超时时间（秒）
	 */
	private int pingTimeout = 20;
	/**
	 * 两次ping消息的时间间隔（秒）
	 */
	private int pingInterval = 30;
	public int getPingTimeout() {
		return pingTimeout;
	}
	public void setPingTimeout(int pingTimeout) {
		this.pingTimeout = pingTimeout;
	}
	public int getPingInterval() {
		return pingInterval;
	}
	public void setPingInterval(int pingInterval) {
		this.pingInterval = pingInterval;
	}
	
}
