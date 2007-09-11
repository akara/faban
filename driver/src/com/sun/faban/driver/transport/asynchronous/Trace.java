package com.sun.faban.driver.transport.asynchronous;

/**
 * 
 * @author Noah Campbell
 * @param <T> payload
 */
public interface Trace<T> {
	
	/**
	 * @return identifier
	 */
	public String getIdentifier();
	
	/**
	 * @return payload
	 */
	public T getPayload();
}
