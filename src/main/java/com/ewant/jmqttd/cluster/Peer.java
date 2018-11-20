package com.ewant.jmqttd.cluster;

public class Peer {
	
	private String groupId;
	
	private String id;
	
	private String host;
	
	private int port;
	
	private long latestUpdateTime = System.currentTimeMillis();
	
	public Peer(){}
	
	public Peer(String groupId, String host, int port){
		this.groupId = groupId;
		this.host = host;
		this.port = port;
	}

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getId() {
		if(id == null){
			id = host + ":" + port;
		}
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getHost() {
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

	public long getLatestUpdateTime() {
		return latestUpdateTime;
	}

	public void setLatestUpdateTime(long latestUpdateTime) {
		this.latestUpdateTime = latestUpdateTime;
	}
	
}
