/**
 * 
 */
package com.sun.faban.driver.transport.asynchronous;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

import javax.jms.Message;
import javax.management.MBeanServer;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.management.ManagementService;

import org.apache.commons.math.stat.Frequency;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;

import com.sun.faban.driver.DriverContext;

/**
 * @author Noah Campbell
 * @param <T> result
 * @param <K> message
 *
 */
public class MessageCacheTraceRegistry<T, K extends Message> implements TraceRegistry<T, K> {
	
	static private AtomicLong traceCounter = new AtomicLong(100000);
	static private ScheduledExecutorService scheduler;
	static private DescriptiveStatistics cacheStatistics = DescriptiveStatistics.newInstance();
	static private Frequency roundtripFrequence = new Frequency();	
	static private CacheManager responseCacheManager;
	
	private Cache cache;
	private DriverContext context;
	
	/**
	 * @param ctx
	 * @throws RegistryConfigurationException 
	 */
	public MessageCacheTraceRegistry(DriverContext ctx) throws RegistryConfigurationException {
		this.context = ctx;
		
		scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors() * 4);
		
		synchronized (MessageCacheTraceRegistry.class) {
			if(responseCacheManager == null) {
				InputStream ehcacheConfig= getClass().getResourceAsStream("/ehcache.xml");
				if(ehcacheConfig == null) {
					throw new IllegalStateException("Unable to get cache configuration.");
				}
				responseCacheManager = CacheManager.create(ehcacheConfig);
				try {
					ehcacheConfig.close();
				} catch (Exception e) {
					ctx.getLogger().warning("Unable to close cache configuration.  May not affect run");
				}
				StringBuilder builder = new StringBuilder();
				builder.append("Configured Caches:").append('\n');
				for(String name : responseCacheManager.getCacheNames()) {
					builder.append('\t').append(name).append('\n');
				}
				ctx.getLogger().info(builder.toString());
				ctx.getLogger().info("Registered MBean Server: " + responseCacheManager.getName());
				MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
				ManagementService.registerMBeans(responseCacheManager, mBeanServer, false, false, false, true);
				
			}
		}
		
		Cache cache = responseCacheManager.getCache("JMSDriverDistributedCache");
		if(cache == null) {
			throw new RegistryConfigurationException("Unable to get cache");
		}
		
		this.cache = cache;
	}

	/**
	 * @see com.sun.faban.driver.transport.asynchronous.TraceRegistry#compileResults(int, java.util.concurrent.TimeUnit)
	 */
	public Object compileResults(int time, TimeUnit unit) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see com.sun.faban.driver.transport.asynchronous.TraceRegistry#isComplete(com.sun.faban.driver.transport.asynchronous.Trace)
	 */
	public boolean isComplete(Trace<K> trace) {
		return cache.isKeyInCache(trace.getIdentifier());
	}
	
	/**
	 * @param payload The message payload
	 * @return trace A trace object
	 * @throws DyeingException 
	 * @see com.sun.faban.driver.transport.asynchronous.TraceRegistry#registerAndDye(java.lang.Object)
	 */
	public Trace<K> registerAndDye(K payload) throws DyeingException {
		try {
			String correlationId = payload.getJMSCorrelationID();
			// set the JMSCorrelationID if it's not set.
			if(correlationId == null) {
				correlationId = context.getDriverName() + ":" + 
				context.getAgentId() + ":" + 
				Long.toString(traceCounter.incrementAndGet());
				payload.setJMSCorrelationID(correlationId);
			} 
			return new MessageTrace(correlationId, payload);
		} catch (Exception e) {
			throw new DyeingException(e);
		}
	}

	/**
	 * @see com.sun.faban.driver.transport.asynchronous.TraceRegistry#waitForCompletion(com.sun.faban.driver.transport.asynchronous.Trace, int, java.util.concurrent.TimeUnit)
	 */
	@SuppressWarnings("unchecked")
	public T waitForCompletion(Trace<K> trace, int time, TimeUnit unit) throws InterruptedException, ExecutionException {
		Future<T> future;
		T result = null;
		Element element = cache.get(trace);
		if(element == null) {
			future = scheduler.submit(new PollingCacheMonitor(trace, 5));
			try { 
				result = future.get(time, unit); // wait!!!
			} catch (TimeoutException e) {
				context.getLogger().log(Level.FINE, e.getMessage(), e);	
			}
		} else {
			result = (T)element.getObjectValue(); // No support for generics
		}
		return result;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("================== Frequency Statistics ==================").append('\n');
		builder.append(cacheStatistics.toString()).append('\n');
		builder.append("================== Frequency Distribution ================").append('\n');
		builder.append(roundtripFrequence.toString()).append('\n');
		builder.append("==========================================================").append('\n');
		return builder.toString();
	}

	/**
	 * @see com.sun.faban.driver.transport.asynchronous.TraceRegistry#acknowledge(java.lang.String, java.lang.Object)
	 */
	public void acknowledge(String traceId, T response)
			throws MissingDyeException {
		if(traceId == null || traceId.length() == 0) {
			throw new MissingDyeException();
		}
		cache.put(new Element(traceId, response));
		
	}
	
	/** Message Trace **/
	private final class MessageTrace implements Trace<K> {

		private String id;
		private K msg;
		
		private MessageTrace(String id, K msg) {
			this.id = id;
			this.msg = msg;
		}
		
		/**
		 * @see com.sun.faban.driver.transport.asynchronous.Trace#getIdentifier()
		 */
		public String getIdentifier() {
			return id;
		}

		/**
		 * @see com.sun.faban.driver.transport.asynchronous.Trace#getPayload()
		 */
		public K getPayload() {
			return msg;
		}
		
	}
	
	/**
	 * Pooling response class.
	 * 
	 * @author Noah Campbell
	 */
	final class PollingCacheMonitor implements Callable<T> {
		private final String correlationId;
		private int waitTime;
		
		/**
		 * Construct a {@link PollingCacheMonitor}
		 * 
		 * @param correlationId
		 * @param time in milliseconds
		 */
		private PollingCacheMonitor(Trace<K> trace, int time) {
			this.correlationId = trace.getIdentifier();
			this.waitTime = time;
		}

		/**
		 * @see java.util.concurrent.Callable#call()
		 */
		@SuppressWarnings("unchecked")
		public T call() throws Exception {
			long start = System.currentTimeMillis();
			while(!cache.isKeyInCache(correlationId)) {
				Thread.sleep(waitTime);
			}
			Element e = cache.get(correlationId);
			long duration = System.currentTimeMillis() - start;
			synchronized(PollingCacheMonitor.class) {
				cacheStatistics.addValue(duration);
				roundtripFrequence.addValue(duration);
			}
			if(e == null) return null;
			return (T) e.getObjectValue();
		}
	}

	
	private static final Set<Partition> SUPPORTED_PARTITIONS = new HashSet<Partition>();
	static {
		Collections.addAll(SUPPORTED_PARTITIONS, Partition.THREAD, Partition.JVM, Partition.HOST);
	}
	
	/**
	 * @see com.sun.faban.driver.transport.asynchronous.TraceRegistry#getPartitionTolerance()
	 */
	public Set<Partition> getPartitionTolerance() {
		return SUPPORTED_PARTITIONS;
	}

	/**
	 * @see com.sun.faban.driver.transport.asynchronous.TraceRegistry#isSafe(com.sun.faban.driver.transport.asynchronous.Partition)
	 */
	public boolean isSafe(Partition partition) {
		return SUPPORTED_PARTITIONS.contains(partition);
	}

}
