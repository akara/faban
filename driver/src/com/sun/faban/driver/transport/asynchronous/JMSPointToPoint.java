package com.sun.faban.driver.transport.asynchronous;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;

import net.sf.ehcache.CacheException;

import com.sun.faban.driver.DriverContext;

/**
 * The {@link JMSPointToPoint} class will take messages sent in a point to point fashion.  This transport
 * assumes that the message may not be delivered back to the same thread, jvm, or host
 * that originated the request.
 * 
 * A global cache is implemented that provide the mechanism to coordinate responses.
 * 
 * @author Noah Campbell
 * @param <T> response
 * @param <K> payload
 *
 */
public class JMSPointToPoint<T> implements AsynchronousTransport<T, Message>, MessageListener {

	private MessageProducer producer;
	private MessageConsumer consumer;
	private DriverContext context;
	
	
	private final TraceRegistry<T, Message> traceRegistry;
	
	static private Logger logger = Logger.getLogger("ASYNCHRONOUS_TRANSPORT");	
	
	// background task management and tracking
	
	/**
	 * Construct a {@link JMSPointToPoint} object.
	 * 
	 * 
	 * @param ctx 
	 * @param producer 
	 * @param consumer 
	 * @throws JMSException 
	 * @throws RegistryConfigurationException 
	 */
	public JMSPointToPoint(DriverContext ctx, MessageProducer producer, MessageConsumer consumer) throws JMSException, RegistryConfigurationException {   

		assert producer != null;
		assert consumer != null;
		assert ctx != null;
		
		this.traceRegistry = new MessageCacheTraceRegistry<T, Message>(ctx);
		
		this.context = ctx;
		
		this.producer = producer;
		this.consumer = consumer;
		this.consumer.setMessageListener(this);
		
	}


	/**
 	 * Send a JMS Message.  It is up to the caller to construct a JMS message and set the 
	 * appropriate header fields.  The resolution for timeout is determined by <code>time</code> and <code>unit</code>.  
	 * This method polls to see if the message as arrived.<p>
	 * 
	 * The JMSCorrelationID is used to track responses.  This id must be unique across JVMs and host 
	 * boundaries.  The method will create a JMSCorrelationID if one is not set that meets this
	 * requirement.
	 * 
	 * Send the message and wait for the response.
	 * 
	 * @see #putAndWait(Message, int, TimeUnit)
	 * @see #putAndWait(Serializable, int, TimeUnit)
	 * @see #putAndWait(Serializable)
	 * 
	 * @param message JMS message payload
	 * @param time The time coefficient to wait for a response.
	 * @param unit The {@link TimeUnit} to wait for a response.
	 * @return response The response to the request.
	 * @throws JMSException
	 * @throws InterruptedException
	 * @throws ExecutionException
	 * @throws DyeingException 
	 */
	@SuppressWarnings("unchecked") 
	private T sendMessage(Message msg, int time, TimeUnit unit) throws JMSException,
			InterruptedException, ExecutionException, DyeingException {
		
		Trace<Message> trace = traceRegistry.registerAndDye(msg);
		
		context.recordTime();
		producer.send(msg);
		
		Thread.yield(); // we'll be lucky if the message is ready right after it sent so give another thread a chance.
		
		T result = traceRegistry.waitForCompletion(trace, time, unit);
		context.recordTime();

		return result;
	}

	/**
	 * @see javax.jms.MessageListener#onMessage(javax.jms.Message)
	 */
	@SuppressWarnings("unchecked")
	public void onMessage(Message msg) {
		try {
			final String correlationID = msg.getJMSCorrelationID();
			if(correlationID != null && msg instanceof BytesMessage) {
				BytesMessage bytesMsg = (BytesMessage) msg;
				long realLength = bytesMsg.getBodyLength();
				if(realLength <= Integer.MAX_VALUE) {
					int shortLength = (int)realLength;
					byte[] bytes = new byte[shortLength];
					int read = bytesMsg.readBytes(bytes);
					if(read == shortLength) {
						try {
							ObjectInputStream bais = new ObjectInputStream(new ByteArrayInputStream(bytes));
							traceRegistry.acknowledge(correlationID, (T)bais.readObject());
							logger.finest("Event Received, putting in cache: " + correlationID);
							bais.close();
						} catch (IllegalArgumentException e) {
							logger.log(Level.WARNING, e.getMessage(), e);
						} catch (IllegalStateException e) {
							logger.log(Level.WARNING, e.getMessage(), e);
						} catch (CacheException e) {
							logger.log(Level.WARNING, e.getMessage(), e);
						} catch (IOException e) {
							logger.log(Level.WARNING, e.getMessage(), e);
						} catch (ClassNotFoundException e) {
							logger.log(Level.WARNING, e.getMessage(), e);
						} catch (MissingDyeException e) {
							logger.log(Level.WARNING, e.getMessage(), e);						}
					} else {
						logger.log(Level.WARNING, "Unable to read the entire byte payload.  Giving up.");
					}
				} else {
					logger.log(Level.WARNING, "Payload greater than " + Integer.MAX_VALUE + ".  Ignoring.");
				}
				
			} else if (correlationID != null && msg instanceof ObjectMessage) {
				ObjectMessage om = (ObjectMessage)msg;
				try {
					traceRegistry.acknowledge(correlationID, (T)om.getObject());
				} catch (MissingDyeException e) {
					logger.log(Level.WARNING, e.getMessage(), e);
				}
				logger.finest("Event Received, putting in cache: " + correlationID);
			}
			logger.fine("Ack Message: " + correlationID);
			msg.acknowledge(); // we'll take the message no matter what.
		} catch (JMSException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}

	/**
	 * @param payload 
	 * @param time 
	 * @param unit 
	 * @return 
	 * @throws InterruptedException 
	 * @throws ExecutionException 
	 * @throws DyeingException 
	 * @see com.sun.faban.driver.transport.asynchronous.AsynchronousTransport#putAndWait(java.lang.Object, int, java.util.concurrent.TimeUnit)
	 */
	public T putAndWait(Message payload, int time, TimeUnit unit)
			throws InterruptedException, ExecutionException, DyeingException {
		
		
		if( (payload instanceof ObjectMessage) || 
				(payload instanceof BytesMessage) ) {
			try {
				return this.sendMessage(payload, time, unit);
			} catch (JMSException e) {
				throw new ExecutionException(e);
			}
		}
		
		throw new IllegalArgumentException("Payload cannot be cast to the appropriate message type");
	}


	/**
	 * @param payload 
	 * @return 
	 * @see com.sun.faban.driver.transport.asynchronous.AsynchronousTransport#put(java.lang.Object)
	 */
	public T put(Message payload) {
		// TODO Auto-generated method stub
		return null;
	}


	/**
	 * @see com.sun.faban.driver.transport.asynchronous.AsynchronousTransport#getTracingRegistry()
	 */
	public TraceRegistry<T, Message> getTracingRegistry() {
		return this.traceRegistry;
	}
}
