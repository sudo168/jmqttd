package com.ewant.jmqttd.persistent;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ewant.jmqttd.codec.message.MqttPublish;

/**
 * 定期或者定量存储文件即可，不用实时
 * 1天 或者 100M
 * @author hoey
 */
public class FileSystemMqttPublishPersistence implements MqttPublishPersistence, Runnable {

	Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * 每隔多久（毫秒）写一次硬盘
	 */
	private long flushIntervalMillis = 100;
	/**
	 * 内存中缓存字节（byte）
	 */
	private long cacheBytes = 100 * 1024 * 1024;
	
	private boolean run = true;
	private long lastFlushMillis = System.currentTimeMillis();
	private long messageByteSizes;
	private String path;
	private File storePath;
	private File topicStorePath;
	private File clientStorePath;
	private Thread writer;
	private Map<String, MqttPublish> messageQueue;
	private List<String> persistIndexHolder;
	
	public FileSystemMqttPublishPersistence(String path){
		this.path = path;
	}
	
	@Override
	public void open() throws MqttPersistentException {
		storePath = new File(path);
		if(!storePath.exists()){
			storePath.mkdirs();
		}
		topicStorePath = new File(storePath, "topic");
		if(!topicStorePath.exists()){
			topicStorePath.mkdir();
		}
		clientStorePath = new File(storePath, "client");
		if(!clientStorePath.exists()){
			clientStorePath.mkdir();
		}
		
		messageQueue = new HashMap<>();
		writer = new Thread(this, "FileSystem-Publish-Persistence-Thread");
		writer.setDaemon(false);
		writer.start();
	}

	@Override
	public void close() throws MqttPersistentException {
		this.run = false;
	}

	@Override
	public void put(String key, MqttPublish persistable) throws MqttPersistentException {
		messageQueue.put(key, persistable);
		persistIndexHolder.add(key);
		messageByteSizes += persistable.getPacketSize();
		if(messageByteSizes >= cacheBytes){
			shrink();
		}
	}
	
	/**
	 * 数据超过cacheBytes时，清除一部分缓存
	 */
	private void shrink(){
		
	}

	@Override
	public MqttPublish get(String key) throws MqttPersistentException {
		return messageQueue.get(key);
	}

	@Override
	public MqttPublish remove(String key) throws MqttPersistentException {
		return messageQueue.remove(key);
	}

	@Override
	public Collection<String> keys() throws MqttPersistentException {
		return messageQueue.keySet();
	}

	@Override
	public void clear() throws MqttPersistentException {
		messageQueue.clear();
	}

	@Override
	public boolean containsKey(String key) throws MqttPersistentException {
		return messageQueue.containsKey(key);
	}

	public void setFlushIntervalMillis(long flushIntervalMillis) {
		this.flushIntervalMillis = flushIntervalMillis;
	}

	public void setCacheBytes(long cacheBytes) {
		this.cacheBytes = cacheBytes;
	}

	@Override
	public void run() {
		try{
			while (run) {
				if(canWriter() && !messageQueue.isEmpty()){
					lastFlushMillis = System.currentTimeMillis();
					Map<String, MqttPublish> storeMessages = messageQueue;
					messageQueue = new HashMap<>();
					// TODO do write, 主题-客户端-时间(月)
					Set<String> keySet = storeMessages.keySet();
					for(String key : keySet){
						MqttPublish message = storeMessages.get(key);
						String clientId = key.split("@")[0];
					}
				}
				synchronized (storePath) {
					storePath.wait(flushIntervalMillis);
				}
			}
		}catch(Exception e){
			logger.error("persitnce error: " + e.getMessage(), e);
		}
	}
	
	private boolean canWriter(){
		return System.currentTimeMillis() - lastFlushMillis >= flushIntervalMillis;
	}

}
