package net.ewant.jmqttd.core;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ExecutorFactory {
	
	public static Executor getExecutor(String threadPrefix, int poolSize) {
		poolSize = poolSize < 1 ? Runtime.getRuntime().availableProcessors() * 2 : poolSize;
		return Executors.newScheduledThreadPool(poolSize , new NameGroupThreadFactory(threadPrefix));
	}
	
	public static ExecutorService getExecutorService(String threadPrefix, int poolSize) {
		return (ExecutorService) getExecutor(threadPrefix, poolSize);
	}
	
	public static Executor getExecutor(String threadPrefix) {
		return getExecutor(threadPrefix, 0);
	}
	
	public static ExecutorService getExecutorService(String threadPrefix) {
		return getExecutorService(threadPrefix, 0);
	}

}
