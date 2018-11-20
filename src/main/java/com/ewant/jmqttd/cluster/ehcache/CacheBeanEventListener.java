package com.ewant.jmqttd.cluster.ehcache;

public interface CacheBeanEventListener<T extends Cachingable>{

	void onRemoved(T element);
	 
    void onPut(T element);
 
    void onUpdated(T element);
 
    void onExpired(T element);
    
    void onEvicted(T element);
 
    void onRemoveAll();
 
    void onRelease();
    
    void onPropertyChange(EventBean event);
    
    void onEvent(EventBean event);
}
