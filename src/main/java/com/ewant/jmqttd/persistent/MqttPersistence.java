package com.ewant.jmqttd.persistent;

import java.util.Collection;

import com.ewant.jmqttd.codec.message.MqttPersistableMessage;

/**
 * server model: 收到的public消息要存储（全部存盘）
 * client model: 发送的public、pubrel要存储（选择性的drop掉一些），从上次登录时间节点往后的要重发（离线的除外，因为离线将有离线推送）
 * 重发频率：6次1/3客户端keepalive时间调度，3次1分钟，1次5分钟，1次10分钟，1次30分钟，1次1小时。重发指定次数后，仍没有收到的，则持久化（在这之间的数据如果服务器宕机了如何处理）
 * 客户端重连时（是否是订阅后？），要将这些消息重发回去（考虑集群：在集群中广播该客户端连接了。）
 * 所有客户端公用一个重发调度器
 * 
 * 消息分客户端存储，调度key为clientId+messageId。往客户端发消息时，所用的messageId必须是不在调度列表里
 * 消息流程：
 * 1.服务端未收到（即没回执，发送失败；qos0不在讨论范围）
 * 2.服务端已收到（发端显示“未读”，群消息则为“n人未读”）
 * 3.接收端已收到（发端显示“未读”，群消息则为“n人未读”）
 * 4.接收端已读（群消息要做到哪些人已读）
 * 
 * 如果服务器收到消息刚发完回执，宕机了如何处理？
 * 每100ms写一次数据。
 * 在内存中缓存一定量的未收到回执的public、pubrel，如果超过了量，就要持久化到硬盘，调度器在调度到的时候发现内存中没有该消息应当到硬盘取，再没有则取消调度
 * 收到的public消息也是按量（按字节还是条数呢？）缓存在内存。这部分数据是海量的！！！
 * @author hoey
 */
public interface MqttPersistence<T extends MqttPersistableMessage> {
	
	public void open() throws MqttPersistentException;

	public void close() throws MqttPersistentException;

	public void put(String key, T persistable) throws MqttPersistentException;
	
	public T get(String key) throws MqttPersistentException;
	
	public T remove(String key) throws MqttPersistentException;

	public Collection<String> keys() throws MqttPersistentException;
	
	public void clear() throws MqttPersistentException;
	
	public boolean containsKey(String key) throws MqttPersistentException;

}
