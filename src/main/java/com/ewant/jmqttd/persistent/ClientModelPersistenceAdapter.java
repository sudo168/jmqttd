package com.ewant.jmqttd.persistent;

import java.util.Collection;

import com.ewant.jmqttd.codec.message.MqttPersistableMessage;
import com.ewant.jmqttd.codec.message.MqttPubRel;
import com.ewant.jmqttd.codec.message.MqttPublish;

public class ClientModelPersistenceAdapter implements MqttPersistence<MqttPersistableMessage>{
	
	protected MqttPublishPersistence publishPersistence;
	protected MqttPubRelPersistence pubRelPersistence;
	
	@Override
	public void open() throws MqttPersistentException {
		// TODO 将最近的消息反序列化回来，进行重发
		publishPersistence.open();
		pubRelPersistence.open();
	}
	@Override
	public void close() throws MqttPersistentException {
		// TODO Auto-generated method stub
		publishPersistence.close();
		pubRelPersistence.close();
	}
	@Override
	public void put(String key, MqttPersistableMessage persistable) throws MqttPersistentException {
		if(persistable instanceof MqttPublish){
			publishPersistence.put(key, (MqttPublish)persistable);
		}else if(persistable instanceof MqttPubRel){
			pubRelPersistence.put(key, (MqttPubRel)persistable);
		}
		
	}
	@Override
	public MqttPersistableMessage get(String key) throws MqttPersistentException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public MqttPersistableMessage remove(String key) throws MqttPersistentException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public Collection<String> keys() throws MqttPersistentException {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public void clear() throws MqttPersistentException {
		// TODO Auto-generated method stub
		
	}
	@Override
	public boolean containsKey(String key) throws MqttPersistentException {
		// TODO Auto-generated method stub
		return false;
	}
}
