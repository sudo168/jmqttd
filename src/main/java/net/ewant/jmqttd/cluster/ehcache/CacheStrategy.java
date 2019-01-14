package net.ewant.jmqttd.cluster.ehcache;

public interface CacheStrategy extends Cachingable{
	
	boolean isEmpty();

    int size();

    void clear();

}
