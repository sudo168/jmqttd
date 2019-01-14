package net.ewant.jmqttd.cluster.ehcache;

import java.io.Serializable;

public interface Cachingable extends Serializable{
	
	/**
	 * 缓存名，get set 一体。
	 * 传null为get值，names不空且length>0为set值
	 * @param names
	 * @return
	 */
	Serializable cacheName(Serializable... names);
	
}
