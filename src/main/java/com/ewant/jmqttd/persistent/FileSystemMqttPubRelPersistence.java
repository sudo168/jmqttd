package com.ewant.jmqttd.persistent;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ewant.jmqttd.codec.message.MqttPubRel;
import com.ewant.jmqttd.codec.message.MqttPublish;

/**
 * 定期或者定量存储文件即可，不用实时
 * 1天 或者 100M
 * @author hoey
 */
public class FileSystemMqttPubRelPersistence implements MqttPubRelPersistence, Runnable {

	Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * 每隔多久（秒）写一次硬盘
	 */
	private long flushIntervalMillis = 24 * 60 * 60 * 1000;
	/**
	 * 每多少字节（byte）写一次硬盘
	 */
	private long flushPerBytes = 100 * 1024 * 1024;
	
	private boolean run = true;
	private long lastFlushMillis = System.currentTimeMillis();
	private long messageByteSizes;
	private String path;
	private File storePath;
	private Thread writer;
	private Map<String, MqttPubRel> messageQueue;
	
	public FileSystemMqttPubRelPersistence(String path){
		this.path = path;
	}
	
	@Override
	public void open() throws MqttPersistentException {
		storePath = new File(path);
		if(!storePath.exists()){
			storePath.mkdirs();
		}
		messageQueue = new HashMap<>();
		writer = new Thread(this, "FileSystem-Persistence-thread");
		writer.setDaemon(false);
		writer.start();
	}

	@Override
	public void close() throws MqttPersistentException {
		this.run = false;
	}

	@Override
	public void put(String key, MqttPubRel persistable) throws MqttPersistentException {
		messageQueue.put(key, persistable);
		messageByteSizes += persistable.getPacketSize();
		if(messageByteSizes >= flushPerBytes){
			synchronized (storePath) {
				storePath.notify();
			}
		}
	}

	@Override
	public MqttPubRel get(String key) throws MqttPersistentException {
		return messageQueue.get(key);
	}

	@Override
	public MqttPubRel remove(String key) throws MqttPersistentException {
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

	public void setFlushPerBytes(long flushPerBytes) {
		this.flushPerBytes = flushPerBytes;
	}

	@Override
	public void run() {
		try{
			while (run) {
				if(canWriter() && !messageQueue.isEmpty()){
					lastFlushMillis = System.currentTimeMillis();
					Map<String, MqttPubRel> storeMessages = messageQueue;
					messageQueue = new HashMap<>();
					// TODO do write, 主题-客户端-时间(月)
					Set<String> keySet = storeMessages.keySet();
					for(String key : keySet){
						MqttPubRel message = storeMessages.get(key);
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
		return messageByteSizes >= flushPerBytes || System.currentTimeMillis() - lastFlushMillis >= flushIntervalMillis;
	}

}
