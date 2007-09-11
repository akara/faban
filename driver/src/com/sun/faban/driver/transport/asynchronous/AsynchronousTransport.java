package com.sun.faban.driver.transport.asynchronous;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * {@link AsynchronousTransport} provides a transport for sending and receiving responses that
 * are not on the same thread.  A transport that implements this interface makes the guarantee to deliver the
 * message to the same JVM that originated the request.  Sometimes the transport relies on the addressing
 * of the message to ensure correct semantics.
 * 
 * Depending on the specific implementation, their may be additional overhead on the
 * response time.  In this case, it is important to include a control or baseline to
 * determine the overhead.
 * 
 * @author Noah Campbell
 * @param <T> response
 * @param <K> payload
 */
public interface AsynchronousTransport<T, K> {

	/**
	 * Send the message.  The resolution for timeout is determined by <code>time</code> and <code>unit</code>.  
	 * This method polls to see if the message as arrived.
	 * 
	 * The JMSCorrelationID is used to track responses.  This id must be unique across JVMs and host 
	 * boundaries.  The method will create a JMSCorrelationID if one is not set that meets this
	 * requirement.
	 * 
	 * @param payload An object payload.
	 * @param time The time coefficient to wait for a response.
	 * @param unit The {@link TimeUnit} to wait for a response.
	 * 
	 * @return response The response for the specific request or null. 
	 * @throws InterruptedException 
	 * @throws ExecutionException  
	 * @throws DyeingException 
	 * @throws Exception
	 */
	T putAndWait(K payload, int time, TimeUnit unit)
			throws InterruptedException, ExecutionException, DyeingException;
	
	/**
	 * @param payload
	 * @return
	 */
	T put(K payload);
	
	/**
	 * @return tracingRegistry The underlying tracing registry
	 */
	TraceRegistry<T, K> getTracingRegistry();

}
