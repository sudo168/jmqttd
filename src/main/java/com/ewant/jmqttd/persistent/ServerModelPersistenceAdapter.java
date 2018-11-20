package com.ewant.jmqttd.persistent;

import java.util.Collection;

import com.ewant.jmqttd.codec.message.MqttPublish;

public class ServerModelPersistenceAdapter implements MqttPublishPersistence{
	
	protected MqttPublishPersistence publishPersistence;

	@Override
	public void open() throws MqttPersistentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() throws MqttPersistentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void put(String key, MqttPublish persistable) throws MqttPersistentException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public MqttPublish get(String key) throws MqttPersistentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MqttPublish remove(String key) throws MqttPersistentException {
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
