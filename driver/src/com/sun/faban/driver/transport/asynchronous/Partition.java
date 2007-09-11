package com.sun.faban.driver.transport.asynchronous;

/**
 * Describes a partition.
 * 
 * @author ncampbell
 */
public enum Partition {
	/**
	 * Thread partition designates that a trace result will be safely referenced
	 * by the same thread. 
	 */
	THREAD,
	/**
	 * JVM partition designates that a trace result will be available within a JVM
	 * by any thread.
	 */
	JVM,
	/** HOST partition designates that a trace result will be available within
	 * a host via shared memory or similar construct. */
	HOST,
	/** 
	 * SUBNET partition designates that a trace result will be available within a subnet 
	 */
	SUBNET
}
