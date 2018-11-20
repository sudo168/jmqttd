package com.ewant.jmqttd.cluster.ehcache;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.CacheConfiguration.BootstrapCacheLoaderFactoryConfiguration;
import net.sf.ehcache.config.CacheConfiguration.CacheEventListenerFactoryConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.FactoryConfiguration;
import net.sf.ehcache.distribution.RMIBootstrapCacheLoaderFactory;
import net.sf.ehcache.distribution.RMICacheManagerPeerListenerFactory;
import net.sf.ehcache.distribution.RMICacheManagerPeerProviderFactory;
import net.sf.ehcache.distribution.RMICacheReplicatorFactory;
import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

public abstract class ApplicationEhcache<K extends Serializable, V extends Cachingable> implements Closeable{
	
	private static final Logger logger = LoggerFactory.getLogger(ApplicationEhcache.class);
	
	protected static final int DEFAULT_CACHE_TIMEOUT_SECONDS = 3600;
	
	private FactoryConfiguration<?> managerListenerFactory;
	private FactoryConfiguration<?> managerProviderFactory;
	
	private CacheEventListenerFactoryConfiguration cacheReplicatorFactory;
	private BootstrapCacheLoaderFactoryConfiguration bootstrapCacheLoaderFactory;
	
	private CacheEventListenerFactoryConfiguration peerListenerFactory;
	
	// private CacheConfiguration cacheConfiguration;
	
	private String ehcachePeerListenerHost = "localhost";
	
	private int ehcachePeerListenerPort = 40001;
	
	private boolean ehcacheEnableCluster = false;
	
	private boolean diskStore = false;
	
	protected String cacheName;
	
	private static CacheManager ehCacheManager;
	
	public static ProxyFactory proxyFactory = new ProxyFactory();
	
	private static boolean useMethodStackReplication = true;
	
	/**
	 * 构造方法
	 * @param isCluster
	 * @param host
	 * @param port
	 * @param diskStore
	 */
	public ApplicationEhcache(String cacheName, boolean isCluster, String host, int port, boolean diskStore) {
		this.ehcacheEnableCluster = isCluster;
		this.diskStore = diskStore;
		this.cacheName = cacheName;
		if(port > 0){
			this.ehcachePeerListenerPort = port;
		}
		if (StringUtils.isNotBlank(host)) {
			this.ehcachePeerListenerHost = host;
		}
		init();
	}
	
	private void addToProxyFactory() {
		proxyFactory.addApplicationEhcache(this);
	}
	
	private CacheManager createCacheManager() {
		if (ehCacheManager == null) {
			if (ehcacheEnableCluster) {
				ehCacheManager = CacheManager.create(new Configuration()
						.cacheManagerPeerProviderFactory(managerProviderFactory)
						.cacheManagerPeerListenerFactory(managerListenerFactory));
			}else{
				ehCacheManager = CacheManager.getInstance();
			}
		}
		return ehCacheManager;
	}
	
	private void init(){
		
		this.managerListenerFactory = new FactoryConfiguration<>();
		this.managerListenerFactory.setClass(RMICacheManagerPeerListenerFactory.class.getName());
		this.managerListenerFactory.setProperties("hostName="+ehcachePeerListenerHost+",port="+ehcachePeerListenerPort+", socketTimeoutMillis=20000");
		
		/**
		 * peerDiscovery 方式：atutomatic 为自动 ；mulicastGroupAddress 广播组地址：230.0.0.1；mulicastGroupPort 广播组端口：40001；timeToLive是指搜索范围：0是同一台服务器，1是同一个子网，32是指同一站点，64是指同一块地域，128是同一块大陆，还有个256，我就不说了；hostName：主机名或者ip，用来接受或者发送信息的接口
		 */
		this.managerProviderFactory = new FactoryConfiguration<>();
		this.managerProviderFactory.setClass(RMICacheManagerPeerProviderFactory.class.getName());
		this.managerProviderFactory.setProperties("peerDiscovery=automatic, multicastGroupAddress=230.0.0.1,multicastGroupPort=4446, timeToLive=32");
	
		this.cacheReplicatorFactory = new CacheEventListenerFactoryConfiguration();
		this.cacheReplicatorFactory.setClass(RMICacheReplicatorFactory.class.getName());
		/*RMI缓存分布同步查找 class使用net.sf.ehcache.distribution.RMICacheReplicatorFactory
		        这个工厂支持以下属性：
		replicatePuts=true | false – 当一个新元素增加到缓存中的时候是否要复制到其他的peers。默认是true。
		replicateUpdates=true | false – 当一个已经在缓存中存在的元素被覆盖时是否要进行复制。默认是true。
		replicateRemovals= true | false – 当元素移除的时候是否进行复制。默认是true。
		replicateAsynchronously=true | false – 复制方式是异步的指定为true时，还是同步的，指定为false时。默认是true。
		replicatePutsViaCopy=true | false – 当一个新增元素被拷贝到其他的cache中时是否进行复制指定为true时为复制，默认是true。
		replicateUpdatesViaCopy=true | false – 当一个元素被拷贝到其他的cache中时是否进行复制指定为true时为复制，默认是true。
		asynchronousReplicationIntervalMillis=1000*/
		this.cacheReplicatorFactory.setProperties("replicateAsynchronously=true, replicatePuts=true, replicateUpdates=true, replicateRemovals=true,asynchronousReplicationIntervalMillis=1000");
		
		this.bootstrapCacheLoaderFactory = new BootstrapCacheLoaderFactoryConfiguration();
		this.bootstrapCacheLoaderFactory.setClass(RMIBootstrapCacheLoaderFactory.class.getName());
		this.bootstrapCacheLoaderFactory.setProperties("bootstrapAsynchronously=true, maximumChunkSizeBytes=5000000");
		
		this.peerListenerFactory = new CacheEventListenerFactoryConfiguration();
		this.peerListenerFactory.setClass(PeerListenerFactory.class.getName());
		Class<?> listenerClass = getPeerListenerClass();
		if (listenerClass != null) {
			this.peerListenerFactory.setProperties("listener=" + listenerClass.getName());
		}
		
		createCacheManager();
		ehCacheManager.addCache(buildCache(cacheName, getIdleTimeout()));
		ehCacheManager.addCache(buildCache(getEventCacheName(), 20));
		addToProxyFactory();
	}
	
	/*private Class<?> getActualType(Class<?> clazz) {
		Type genericSuperclass = clazz.getGenericSuperclass();
		if (genericSuperclass instanceof ParameterizedType) {
			return (Class<?>) ((ParameterizedType) genericSuperclass).getActualTypeArguments()[1];
		}else if(!Object.class.equals(clazz)){
			return getActualType((Class<?>)genericSuperclass);
		}
		return clazz;
	}*/
	
	protected V get(String cacheName, K key) {
		return get(cacheName, key, true);
	}
	
	@SuppressWarnings("unchecked")
	private V get(String cacheName, K key , boolean useProxy) {
		try {
			Cache cache = ehCacheManager.getCache(cacheName);
			if(cache == null){
				return null;
			}
			Element element = cache.get(key);
			if(element != null){
				Object value = element.getObjectValue();
				if(value != null){
					return (V) (useProxy ? proxyWrap(value) : value);
				}
			}
		} catch (Exception e) {
			logger.error("从缓存【"+cacheName+"】中获取【"+key+"】异常！", e);
		}
		return null;
	}
	
	private Object proxyWrap(Object target){
		if (proxyFactory != null) {
			return proxyFactory.create(cacheName, target);
		}
		return target;
	}
	
	private String getEventCacheName() {
		return cacheName + "Event";
	}
	
	protected V get(K key) {
		return get(cacheName, key);
	}
	
	@SuppressWarnings("unchecked")
	protected Collection<V> getAll(String cacheName) {
		try {
			Cache cache = ehCacheManager.getCache(cacheName);
			if(cache == null){
				return null;
			}
			Map<Object, Element> all = cache.getAll(cache.getKeys());
			if(all != null){
				List<V> res = new ArrayList<>();
				for (Element element : all.values()) {
					Object value = element.getObjectValue();
					if(value != null){
						res.add((V) proxyWrap(value));
					}
				}
				return res;
			}
		} catch (Exception e) {
			logger.error("从缓存【"+cacheName+"】中获取所有元素异常！", e);
		}
		return null;
	}
	
	protected Collection<V> getAll() {
		return getAll(cacheName);
	}
	
	/**
	 * @param cacheName
	 * @param key
	 * @param value
	 * @param expireMills 0 不过期
	 * @return
	 */
	protected V put(String cacheName, K key, V value) {
		try {
			Cache cache = ehCacheManager.getCache(cacheName);
			if(cache == null){
				cache = buildCache(cacheName,DEFAULT_CACHE_TIMEOUT_SECONDS);
				ehCacheManager.addCache(cache);
			}
			Element element = new Element(key, value);
			cache.put(element);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error("将key【"+key+"】value【"+value+"】加入缓存【"+cacheName+"】异常！", e);
		}
		return value;
	}
	
	protected V put(K key, V value) {
		return put(cacheName, key, value);
	}
	
	@SuppressWarnings("unchecked")
	protected V remove(String cacheName, K key) {
		try {
			Cache cache = ehCacheManager.getCache(cacheName);
			if(cache == null){
				return null;
			}
			Element element = cache.removeAndReturnElement(key);
			if(element != null){
				Object value = element.getObjectValue();
				if(value != null){
					return (V) value;
				}
			}
		} catch (Exception e) {
			logger.error("从缓存【"+cacheName+"】中移除【"+key+"】异常！", e);
		}
		return null;
	}
	
	protected V remove(K key) {
		return remove(cacheName, key);
	}
	
	public void clear(String cacheName) {
		Cache cache = ehCacheManager.getCache(cacheName);
		if(cache == null){
			return ;
		}
		cache.removeAll();
	}
	
	public void clear() {
		clear(cacheName);
	}
	
	public void sendEvent(EventBean event){
		ehCacheManager.getCache(getEventCacheName()).put(new Element(event.cacheName(), event));
	}
	
	/**
	 * defined cache idle timeout. if client never acccess an object over this time . system will remove it.
	 * @return
	 * 		a less than 0 value means use default 36000, 0 means never check
	 */
	protected abstract int getIdleTimeout();
	
	@SuppressWarnings("deprecation")
	protected Cache buildCache(String cacheName ,int timeout) {
		CacheConfiguration config = new CacheConfiguration(cacheName, 1000000)  
			    .memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LFU)  
			    .overflowToDisk(diskStore)  
			    .eternal(false)  
			    .timeToLiveSeconds(0)  
			    .timeToIdleSeconds(timeout < 0 ? DEFAULT_CACHE_TIMEOUT_SECONDS : timeout) 
			    .diskPersistent(diskStore)
			    .statistics(true);
		if (ehcacheEnableCluster) {
			config.addBootstrapCacheLoaderFactory(this.bootstrapCacheLoaderFactory);
			config.cacheEventListenerFactory(this.cacheReplicatorFactory);
			config.cacheEventListenerFactory(this.peerListenerFactory);
		}
		return new net.sf.ehcache.Cache(config);
	}
	
	protected CacheManager getCacheManager() {
		return ehCacheManager;
	}
	
	@Override
	public void close() throws IOException {
		ehCacheManager.shutdown();
	}
	
	protected abstract Class<? extends CacheBeanEventListener<V>> getPeerListenerClass();
	
	public static class PropertyChangeMessage implements Serializable{

		private static final long serialVersionUID = 4417695937089954815L;
		
		private Serializable rootId;
		private String cacheName;
		private List<String> methodStack;
		private List<Object[]> argStack;
		private List<Class<?>[]> argTypeStack;
		
		public Serializable getRootId() {
			return rootId;
		}
		public String getCacheName() {
			return cacheName;
		}
		public List<String> getMethodStack() {
			return methodStack;
		}
		public List<Object[]> getArgStack() {
			return argStack;
		}
		public List<Class<?>[]> getArgTypeStack() {
			return argTypeStack;
		}
		public PropertyChangeMessage(String cacheName, Serializable rootId, List<String> methodStack, List<Object[]> argStack, List<Class<?>[]> argTypeStack) {
			this.rootId = rootId;
			this.cacheName = cacheName;
			this.methodStack = methodStack;
			this.argStack = argStack;
			this.argTypeStack = argTypeStack;
		}
		
	}
	
	@SuppressWarnings("rawtypes")
	public static class ProxyFactory{
		
		private Map<String,ApplicationEhcache> applicationEhcache = new HashMap<String,ApplicationEhcache>();
		
		public ProxyFactory() {
		}
		
		public ProxyFactory(ApplicationEhcache cache) {
			addApplicationEhcache(cache);
		}

		public Object create(String cacheName, Class<?> type, Object target, Object root, ProxyMethodInterceptor parentHolder){
			if (type != null && target != null) {
				if (List.class.isAssignableFrom(type)) {
					type = ArrayList.class;
				}else if(Map.class.isAssignableFrom(type)){
					type = HashMap.class;
				}else if(Collection.class.isAssignableFrom(type)){
					type = HashSet.class;
				}
				return Enhancer.create(type, new ProxyMethodInterceptor(cacheName, target, root, parentHolder));
			}
			return null;
		}
		
		public Object create(String cacheName, Object target){
			if (target != null) {
				return create(cacheName, target.getClass(), target, target, null);
			}
			return null;
		}
		
		public ApplicationEhcache getApplicationEhcache(String name) {
			return this.applicationEhcache.get(name);
		}
		
		public void addApplicationEhcache(ApplicationEhcache cache) {
			this.applicationEhcache.put(cache.cacheName, cache);
		}
	}
	
	public static class ProxyMethodInterceptor implements MethodInterceptor{
		
		private static final Pattern UPDATE_REG_NEED_PARAM = Pattern.compile("^(set|add|remove|update|delete|put).*");
		
		private static final Pattern UPDATE_REG_NOT_PARAM = Pattern.compile("^(clear).*");
		
		Object target;
		Object root;
		
		String pMethodName;
		Object[] pMethodArgs;
		Class<?>[] pArgsType;
		
		String cacheName;
		
		ProxyMethodInterceptor parentHolder;
		
		public ProxyMethodInterceptor(String cacheName, Object target, Object root) {
			this.target = target;
			this.root = root;
			this.cacheName = cacheName;
		}
		
		public ProxyMethodInterceptor(String cacheName, Object target, Object root,ProxyMethodInterceptor parentHolder) {
			this(cacheName, target, root);
			this.parentHolder = parentHolder;
		}
		
		private void getExecuteStack(List<String> methodStack, List<Object[]> argStack, List<Class<?>[]> argTypeStack, ProxyMethodInterceptor holder){
			if(holder != null){
				methodStack.add(holder.pMethodName);
				argStack.add(holder.pMethodArgs);
				argTypeStack.add(holder.pArgsType);
				if (holder.target != holder.root) {
					getExecuteStack(methodStack, argStack, argTypeStack, holder.parentHolder);
				}
			}
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
			Object result = method.invoke(target, args);
			//result = proxy.invokeSuper(obj, args);
			if (root != null && needUpdateChange(target, method)) {
				// update value
				Cachingable b = (Cachingable)root;
				if (useMethodStackReplication) {
					List<String> methodStack = new ArrayList<String>();
					List<Object[]> argStack = new ArrayList<Object[]>();
					List<Class<?>[]> argTypeStack = new ArrayList<Class<?>[]>();
					this.pMethodName = method.getName();
					this.pMethodArgs = args;
					this.pArgsType = method.getParameterTypes();
					getExecuteStack(methodStack, argStack, argTypeStack, this);
					proxyFactory.getApplicationEhcache(cacheName).sendEvent(new EventBean(new PropertyChangeMessage(cacheName, b.cacheName(), methodStack, argStack, argTypeStack)));
				}else{
					proxyFactory.getApplicationEhcache(cacheName).put(b.cacheName(), b);
				}
			}
			// Void.TYPE.isAssignableFrom((Class<?>) returnType
			Type returnType = method.getGenericReturnType();
			Class<?> proxyClass = null;
			if (result != null) {
				if (returnType instanceof ParameterizedType) {
					proxyClass = (Class<?>) ((ParameterizedType) returnType).getRawType();
				}else if(returnType instanceof TypeVariable){
					proxyClass = result.getClass();
				}else if(supportProxyType((Class<?>) returnType)){
					proxyClass = (Class<?>) returnType;
				}
			}
			
			if (proxyClass != null) {
				this.pMethodName = method.getName();
				this.pMethodArgs = args;
				this.pArgsType = method.getParameterTypes();
				return proxyFactory.create(cacheName, proxyClass, result, root, this);
			}
			return result;
		}
		
		private boolean needUpdateChange(Object target, Method method){
			return (UPDATE_REG_NEED_PARAM.matcher(method.getName()).matches() && method.getGenericParameterTypes().length > 0) 
					|| UPDATE_REG_NOT_PARAM.matcher(method.getName()).matches();
		}
		
	}
	
	public static class PeerListenerFactory<V extends Cachingable> extends CacheEventListenerFactory{
		
		private static String DEFAULT_LISTENER_KEY = "listener";

		@SuppressWarnings("unchecked")
		@Override
		public CacheEventListener createCacheEventListener(Properties properties) {
			PeerListenerAdapter<V> listenerAdapter = new PeerListenerAdapter<V>();
			if (properties != null) {
				String listener = properties.getProperty(DEFAULT_LISTENER_KEY);
				if (StringUtils.isNotBlank(listener)) {
					try {
						listenerAdapter.setListener((CacheBeanEventListener<V>) Class.forName(listener).newInstance());
					} catch (Exception e) {
						logger.error("create EhcacheEventListener error by name ["+listener+"].", e);
					}
				}
			}
			return listenerAdapter;
		}
		
	}
	
	@SuppressWarnings("unchecked")
	public static class PeerListenerAdapter<V extends Cachingable> implements CacheEventListener{
		
		private CacheBeanEventListener<V> listener;
		
		private void handerPropertyChange(EventBean event){
			Object eventData = event.getEventData();
			if (eventData instanceof PropertyChangeMessage) {
				try {
					PropertyChangeMessage msg = (PropertyChangeMessage) eventData;
					Cachingable cacheBean = proxyFactory.getApplicationEhcache(msg.getCacheName()).get(msg.getCacheName(), msg.getRootId(), false);
					List<String> methodStack = msg.getMethodStack();
					Object tmpObj = cacheBean;
					if (methodStack != null && cacheBean != null) {
						int size = methodStack.size() - 1;
						for (int i = size ; i > -1; i--) {
							String methodName = methodStack.get(i);
							tmpObj = tmpObj.getClass().getMethod(methodName, msg.getArgTypeStack().get(i)).invoke(tmpObj, msg.getArgStack().get(i));
						}
					}
					logger.info("handerPropertyChange cache name ["+msg.getCacheName()+"] methodStack : " + methodStack);
					listener.onPropertyChange(event);
				} catch (Exception e) {
					logger.error("handerPropertyChange error!", e);
				}
			}else{
				listener.onEvent(event);
			}
		}
		
		@Override
		public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
			if (listener != null) {
				Object objectValue = element.getObjectValue();
				if(objectValue != null){
					if (objectValue instanceof EventBean){
						handerPropertyChange((EventBean) objectValue);
					}else{
						listener.onPut((V) objectValue);
					}
				}else{
					logger.info("put an empty element . key is ["+element.getObjectKey()+"]");
				}
			}
		}

		@Override
		public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
			if (listener != null) {
				Object objectValue = element.getObjectValue();
				if(objectValue != null){
					if (objectValue instanceof EventBean){
						handerPropertyChange((EventBean) objectValue);
					}else{
						listener.onUpdated((V) objectValue);
					}
				}else{
					logger.info("updated an empty element . key is ["+element.getObjectKey()+"]");
				}
			}
		}
		
		@Override
		public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
			if (listener != null) {
				Object objectValue = element.getObjectValue();
				if(objectValue != null){
					if (!(objectValue instanceof EventBean))listener.onRemoved((V) objectValue);
				}else{
					logger.info("remove an empty element . key is ["+element.getObjectKey()+"]");
				}
			}
		}

		@Override
		public void notifyElementExpired(Ehcache cache, Element element) {
			if (listener != null) {
				Object objectValue = element.getObjectValue();
				if(objectValue != null){
					if (!(objectValue instanceof EventBean))listener.onExpired((V) objectValue);
				}else{
					logger.info("an empty element expired . key is ["+element.getObjectKey()+"]");
				}
			}
		}

		@Override
		public void notifyElementEvicted(Ehcache cache, Element element) {
			if (listener != null) {
				Object objectValue = element.getObjectValue();
				if(objectValue != null){
					if (!(objectValue instanceof EventBean))listener.onEvicted((V) objectValue);
				}else{
					logger.info("an empty element evicted by ehcache system . key is ["+element.getObjectKey()+"]");
				}
			}
		}

		@Override
		public void notifyRemoveAll(Ehcache cache) {
			if (listener != null) {
				listener.onRemoveAll();
			}
		}

		@Override
		public void dispose() {
			if (listener != null) {
				listener.onRelease();
			}
		}
		
		@Override
	    public Object clone() throws CloneNotSupportedException {
	        return super.clone();
	    }
		
		public void setListener(CacheBeanEventListener<V> listener) {
			this.listener = listener;
		}
	}
	
	/**  
	  * 判断一个类是否为基本数据类型。  
	  * @param clazz 要判断的类。  
	  * @return true 表示为基本数据类型。  
	  */ 
	private static boolean isBaseDataType(Class<?> clazz) {   
	     return (  
  		 clazz.isPrimitive() ||
	         clazz.equals(String.class) ||
	         clazz.equals(Integer.class)||   
	         clazz.equals(Long.class) ||   
	         clazz.equals(Double.class) ||   
	         clazz.equals(Float.class) ||   
	         clazz.equals(Boolean.class) ||   
	         clazz.equals(Character.class) ||  
	         clazz.equals(Short.class) || 
	         clazz.equals(Byte.class) ||
	         clazz.equals(BigDecimal.class) ||   
	         clazz.equals(BigInteger.class)   
	     ); 
	 }
	 
	 private static boolean supportProxyType(Class<?> clazz) {
		 return !(isBaseDataType(clazz) || Void.TYPE.isAssignableFrom(clazz) ||
				 Modifier.isInterface(clazz.getModifiers()) || Date.class.isAssignableFrom(clazz) ||
				 Object.class.equals(clazz) || Class.class.equals(clazz) || 
				 clazz.isArray() || Modifier.isAbstract(clazz.getModifiers()) 
				 );
	 }
}
