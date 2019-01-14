package net.ewant.jmqttd.cluster.ehcache;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public interface CacheMapStrategy<K,V> extends CacheStrategy {
	
	boolean containsKey(Object key);
    
	boolean containsValue(Object value);
	
	V get(Object key);
	
	V put(K key, V value);
    
	V remove(Object key);
	
	void putAll(Map<? extends K, ? extends V> m);
	
	Set<K> keySet();
	
	Collection<V> values();
	
	Map<K, V> getAll();
	
	Set<Entry<K, V>> entrySet();

}
