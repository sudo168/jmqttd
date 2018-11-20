package com.ewant.jmqttd.cluster.ehcache;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class EhcacheMapCacheStractegy<K,V> implements CacheMapStrategy<K,V>{

	private static final long serialVersionUID = -2307296999771156691L;
	
	ApplicationEhcache<Serializable, Cachingable> cache;
	
	public EhcacheMapCacheStractegy(String cacheName, boolean isCluster, String host, int port, boolean diskStore) {
		this.cache = new ApplicationEhcache<Serializable, Cachingable>(cacheName,isCluster,host,port,diskStore) {
			
			@Override
			protected Class<? extends CacheBeanEventListener<Cachingable>> getPeerListenerClass() {
				return null;
			}
			
			@Override
			protected int getIdleTimeout() {
				return 0;
			}
		};
	}

	@Override
	public boolean isEmpty() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void clear() {
		this.cache.clear();
	}

	@Override
	public Serializable cacheName(Serializable... names) {
		if(names == null || names.length == 0){
			return this.cache.cacheName;
		}else{
			String name = "";
			for (Serializable n : names) {
				name += n;
			}
			this.cache.cacheName = name;
		}
		return null;
	}

	@Override
	public boolean containsKey(Object key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public V get(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V put(K key, V value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public V remove(Object key) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> m) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Set<K> keySet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<V> values() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<K, V> getAll() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		// TODO Auto-generated method stub
		return null;
	}

}
