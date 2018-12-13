package com.ewant.jmqttd.persistent;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * 对收到的所有publish消息进行存储。
 * publish按年份分库。pdb库 ：tc表、uc表、ic表、cm表
 * 索引关系
 * topic <1-N> clientId
 * user <1-N> clientId
 * ip <1-N> clientId
 * 
 * clientId folder：Integer.toHexString(clientId.hashCode())
 * 
 * clientId（hash + month） <1-N> messageId（message）
 * 
 * 1st level：clientId.hashCode() & 0xff
 * 2nd level：clientId.hashCode() >> 8 & 0xff
 * 3rd level：clientId.hashCode() >> 16 & 0xff
 * 4th level：Integer.toHexString(clientId.hashCode())
 * 5th level：month
 * 
 * 256^3 = 16777216 个文件夹，即1600多万个客户端
 * 
 * message：ip、user、topic、clientId、messageId，ackState、index、qos、retain、duplicate、payload、time
 * 
 * ext4支持 1EB（1,048,576TB， 1EB=1024PB， 1PB=1024TB）的文件系统，以及 16TB 的单个文件。
 * ext4无限数量的子目录。 Ext3 目前只支持 32,000 个子目录，而 Ext4 支持无限数量的子目录
 * mkdir创建一个目录时，目录下默认就会创建两个子目录的，一个是.目录（代表当前目录），另一个是..目录（代表上级目录）。
 * 这两个子目录是删除不掉的，因此Ext3 实际有效目录是 32000 - 2 个。
 * ext3文件系统下单个目录里的最大文件数无特别的限制，是受限于所在文件系统的inode数
 * ext3文件系统下filename最大字符长度（默认255个英文字符）
 * 
 * http://www.mamicode.com/info-detail-325269.html
 * @author hoey
 */
public class PersistMessage {
	private String ip;
	private String user;
	private String topic;
	private String clientId;
	private String messageId;
	private int ackState;
	private long index;
	private int qos;
	private boolean retain;
	private boolean duplicate;
	private byte[] payload;
	private long time;
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public String getUser() {
		return user;
	}
	public void setUser(String user) {
		this.user = user;
	}
	public String getTopic() {
		return topic;
	}
	public void setTopic(String topic) {
		this.topic = topic;
	}
	public String getClientId() {
		return clientId;
	}
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}
	public String getMessageId() {
		return messageId;
	}
	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
	public int getAckState() {
		return ackState;
	}
	public void setAckState(int ackState) {
		this.ackState = ackState;
	}
	public long getIndex() {
		return index;
	}
	public void setIndex(long index) {
		this.index = index;
	}
	public int getQos() {
		return qos;
	}
	public void setQos(int qos) {
		this.qos = qos;
	}
	public boolean isRetain() {
		return retain;
	}
	public void setRetain(boolean retain) {
		this.retain = retain;
	}
	public boolean isDuplicate() {
		return duplicate;
	}
	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}
	public byte[] getPayload() {
		return payload;
	}
	public void setPayload(byte[] payload) {
		this.payload = payload;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	
	
	public static void main(String[] args) throws Exception {
		
		String sql = "create table message("
 + "message_id varchar(32) primary key comment '消息ID',"
 + "ip varchar(32) NOT NULL comment '发送者ip',"
 + "user varchar(32) NOT NULL comment '发送者账号',"
 + "topic varchar(64) NOT NULL comment '发送主题',"
 + "client_id varchar(32) NOT NULL comment '发送者客户端ID',"
 + "ack_state int NOT NULL comment '消息回执状态',"
 + "qos tinyint NOT NULL comment '消息质量级别',"
 + "idx bigint NOT NULL comment '消息在主题中的index',"
 + "retain tinyint NOT NULL comment '是否持久消息',"
 + "duplicate tinyint NOT NULL comment '是否2+次发送消息',"
 + "payload varchar(8192) NOT NULL comment '内容',"
 + "time timestamp NOT NULL comment '发送时间'"
 + ")engine=InnoDB charset=UTF8;";
		
		// List<SQLStatement> parseStatements = SQLUtils.parseStatements(sql , JdbcConstants.MYSQL);
		
	}
	
}
