package com.sun.faban.driver.transport.asynchronous;

import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Noah Campbell
 * @param <T> result
 * @param <K> payload
 */
public interface TraceRegistry<T, K> {
	
	/**
	 * {@link #registerAndDye(Object)} creates an id for tracing the request across the SUT to 
	 * correlate the result.  This method may modify the payload by adding a
	 * dye so the message can be tracked.  The SUT must be able to correlate this
	 * dyed request with the response.  
	 * 
	 * @param payload
	 * @return id The identifier for tracking the payload across the SUT.
	 * @throws DyeingException
	 * 
	 * @see {@link #acknowledge(String, Object)}
	 */
	Trace<K> registerAndDye(K payload) throws DyeingException;
	
	/**
	 * {@link #acknowledge(String, Object)} the response message associated with a specific traceid.
	 * 
	 * @param traceId 
	 * @param response
	 * @throws MissingDyeException 
	 */
	void acknowledge(String traceId, T response) throws MissingDyeException;
	
	/**
	 * {@link #waitForCompletion(Trace, int, TimeUnit)} provides a method for the caller
	 * to wait for a result for a specified period of time.
	 * 
	 * @param trace The corresponding trace returned from {@link #registerAndDye(Object)}.
	 * @param time
	 * @param unit
	 * @return result The resulting message
	 * @throws ExecutionException 
	 * @throws InterruptedException 
	 */
	T waitForCompletion(Trace<K> trace, int time, TimeUnit unit) throws InterruptedException, ExecutionException;
	
	/**
	 * Check if the trace has received the corresponding response.
	 * 
	 * @param trace
	 * @return
	 */
	boolean isComplete(Trace<K> trace);
	
	/**
	 * Provide the metrics for all traces.
	 * 
	 * @param time
	 * @param unit
	 * @return
	 */
	Object compileResults(int time, TimeUnit unit);
	
	/**
	 * What safety guarantee does this partition provide.
	 * 
	 * @param partition
	 * @return
	 */
	boolean isSafe(Partition partition);
	
	/**
	 * What tolerance does this partition support.
	 * 
	 * @return
	 */
	Set<Partition> getPartitionTolerance();
}
