/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.sun.com/cddl/cddl.html or
 * install_dir/legal/LICENSE
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at install_dir/legal/LICENSE.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id$
 *
 * Copyright 2005-2010 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.engine;

import com.sun.faban.common.FabanNamespaceContext;
import com.sun.faban.driver.CustomMetrics;
import com.sun.faban.driver.CustomTableMetrics;
import com.sun.faban.driver.Timing;
import static com.sun.faban.driver.engine.AgentThread.TIME_NOT_SET;
import com.sun.faban.driver.util.Random;
import com.sun.faban.driver.util.Timer;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Logger;

/**
 * DriverContext is the point of communication between the
 * developer-provided driver and the Faban driver framework.
 * Each thread has it's own context.<p>
 * This class provides the actual implementation and is
 * located in the engine package to get direct access to all
 * the restricted/needed classes and members.
 *
 * @author Akara Sucharitakul
 */
public class DriverContext extends com.sun.faban.driver.DriverContext {

    /** Thread local used for obtaining the context. */
    private static ThreadLocal<DriverContext> localContext = new InheritableThreadLocal<DriverContext>();

    /** The thread associated with this context. */
    AgentThread agentThread;
    
    /** The timing structure for this thread/context. */
    TimingInfo timingInfo = new TimingInfo();

    /** The central timer. */
    Timer timer;

    /**
     * Flag whether pause is supported with the current protocol.
     * Default is true. Protocols that may not be request/response but
     * may have concurrent inbound and outbound traffic, AND wishes to
     * support auto timing should set this flag to false. The default is true.
     */
    boolean pauseSupported = true;

    /** Context-specific logger. */
    Logger logger;

    /** The properties hashmap. It is lazy initialized. */
    static HashMap<String, String[]> properties;

    /** Class name of this class. */
    private String className;

    /** The XPath instance used to evaluate the XPaths. */
    private XPath xPathInstance;

	/** Desired upload speed of this context */
	private int kbpsUpload = -1;

	/** Desired download speed of this context */
	private int kbpsDownload = -1;

    /**
     * Obtains the DriverContext associated with this thread.
     * @return the associated DriverContext
     */
    public static DriverContext getContext() {
        return localContext.get();
    }

    /**
     * Constructs a DriverContext. Called only from AgentThread.
     * @param thread The AgentThread used by this context
     * @param timer The timer used by this thread
     */
    DriverContext(AgentThread thread, Timer timer) {
        className = getClass().getName();
        agentThread = thread;
        this.timer = timer;
        localContext.set(this);
    }

    /**
     * Obtains the scale or scaling rate of the current run.
     *
     * @return the current run's scaling rate
     */
	public int getScale() {
        return agentThread.runInfo.scale;
    }

    /**
     * Obtains the number of client threads in this agent.
     *
     * @return the number of client threads
     */
    public int getClientsInAgent() {
        return agentThread.runInfo.agentInfo.threads;
    }

    /**
     * Obtains the total number of clients threads for this driver.
     *
     * @return the number of client threads for this driver
     */
    public int getClientsInDriver() {
        return agentThread.runInfo.driverConfig.numThreads;
    }

    /**
     * Obtains the global thread id for this context's thread. The thread id
     * is unique for each driver type.
     * @return the global agentImpl thread id
     */
	public int getThreadId() {
        return agentThread.id;
    }

    /**
     * Obtains the agent id for this agentImpl.
     * @return the current agentImpl's id
     */
	public int getAgentId() {
        return agentThread.runInfo.agentInfo.agentNumber;
    }

    /**
     * Obtains the driver's name as annotated in the driver class.
     * @return the driver name
     */
	public String getDriverName() {
        return agentThread.driverConfig.name;
    }

    /**
     * Obtains the logger to be used by the calling driver.
     * @return the appropriate logger
     */
	public Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger(agentThread.driverConfig.
                    className + '.' + agentThread.id);
            if (agentThread.runInfo.logHandler != null) {
				logger.addHandler(agentThread.runInfo.logHandler);
			}
        }
        return logger;
    }

    /**
     * Obtains the name of the operation currently executing.
     * @return the current operation's name,
     *         or null if called from outside an operation.
     */
	public String getCurrentOperation() {
        if (agentThread.currentOperation == -1)
            return null;
        return agentThread.driverConfig.operations[
                agentThread.currentOperation].name;
    }

    /**
     * Obtains the unique id assigned to the current operation type.
     * This id is commonly used to index into array structures containing
     * operation-specific information such as stats. The id ranges from 0 to
     * n where n is the number of operations in the driver less one.
     *
     * @return The unique id assigned to this operation type,
     *         or -1 if called from outside an operation.
     */
	public int getOperationId() {
        return agentThread.currentOperation;
    }

    /**
     * Obtains the number of operations active in this driver.
     *
     * @return The number of active operations
     */
	public int getOperationCount() {
        return agentThread.driverConfig.operations.length;
    }

    /**
     * Obtains the per-thread random value generator. Drivers
     * should use this random value generator and not instantiate
     * their own.
     * @return The random value generator
     */
	public Random getRandom() {
        return agentThread.random;
    }

    /**
     * Resets the state of the current mix to start off at the beginning
     * of the mix. For stateless mixes such as FlatMix, this operation
     * does nothing.
     */
	public void resetMix() {
        agentThread.selector[agentThread.mixId].reset();
    }

    /**
     * Attaches a custom metrics object to the primary metrics.
     * This should be done by the driver at initialization time.
     * Only one custom metrics can be attached. Subsequent calls
     * to this method replaces the previously attached metrics.
     * @param metrics The custom metrics to be replaced
     */
	public void attachMetrics(CustomMetrics metrics) {
        attachMetrics("Miscellaneous Statistics", metrics);
    }

    /**
     * Attaches a custom metrics object to the primary metrics,
     * given a name or description. The name/description must be unique.
     * This should be done by the driver at initialization time.
     * Only one custom metrics can be attached. Subsequent calls
     * to this method replaces the previously attached metrics.
     * @param name    The name or description of this metrics
     * @param metrics The custom metrics to be replaced
     */
    public void attachMetrics(String name, CustomMetrics metrics) {
        if (agentThread.metrics.metricAttachments == null)
            agentThread.metrics.metricAttachments =
                    new LinkedHashMap<String, CustomMetrics>();
        agentThread.metrics.metricAttachments.put(name, metrics);
    }

    /**
     * Attaches a custom table metrics object to the primary metrics,
     * given a name or description. The name/description must be unique.
     * This should be done by the driver at initialization time.
     * Only one custom metrics can be attached. Subsequent calls
     * to this method replaces the previously attached metrics.
     * @param name    The name or description of this metrics
     * @param metrics The custom metrics to be replaced
     */
    public void attachMetrics(String name, CustomTableMetrics metrics) {
        if (agentThread.metrics.tableAttachments == null)
            agentThread.metrics.tableAttachments =
                    new LinkedHashMap<String, CustomTableMetrics>();
        agentThread.metrics.tableAttachments.put(name, metrics);
    }

    /**
     * Parses the properties DOM tree in puts the output into a HashMap.
     * Returns properties so that we do not have the effect of double-checks.
     * @param propertiesElement The DOM element containing the properties
     * @return The resulting map
     */
    private static synchronized HashMap<String, String[]> parseProperties(
            Element propertiesElement) {
        if (properties == null) {
            NodeList list = propertiesElement.getElementsByTagNameNS(
                                            RunInfo.DRIVERURI, "property");
            int length = list.getLength();
            HashMap<String, String[]> props =
                    new HashMap<String, String[]>(length);
            for (int i = 0; i < length; i++) {
                Element propertyElement = (Element) list.item(i);
                Attr attr = propertyElement.getAttributeNodeNS(null, "name");
                if (attr != null) {
					props.put(attr.getValue(), getValue(propertyElement));
				}
                NodeList nameList =
                        propertyElement.getElementsByTagNameNS(
                                                RunInfo.DRIVERURI, "name");
                if (nameList.getLength() != 1) {
					continue;
				}
                Element nameElement = (Element) nameList.item(0);
                String name = nameElement.getFirstChild().getNodeValue();
                if (name != null) {
					props.put(name, getValue(propertyElement));
				}
            }
            properties = props;
        }
        return properties;
    }

    /**
     * Gets the value of a property DOM element.
     * @param propertyElement The DOM element
     * @return The list of associated values
     */
    private static String[] getValue(Element propertyElement) {
        NodeList valueList = propertyElement.getElementsByTagNameNS(
                                                RunInfo.DRIVERURI, "value");
        String[] values;
        int length = valueList.getLength();
        if (length >= 1) {
            values = new String[length];
            for (int i = 0; i < length; i++) {
                Node valueNode = valueList.item(i).getFirstChild();
                values[i] = valueNode == null ? "" : valueNode.getNodeValue();
            }
        } else {
            values = new String[1];
            Node valueNode = propertyElement.getFirstChild();
            values[0] = valueNode == null ? "" : valueNode.getNodeValue();
        }
        return values;
    }

    /**
     * Obtains a single-value property from the configuration. If the name
     * of a multi-value property is given, only one value is returned.
     * It is undefined as to which value in the list is returned.
     *
     * @param name The property name
     * @return The property value, or null if there is no such property
     */
	public String getProperty(String name) {
        if (properties == null) {
			properties = parseProperties(getPropertiesNode());
		}
        String[] value = properties.get(name);
        if (value == null) {
			return null;
		}
        return value[0];
    }

    /**
     * Obtains a multiple-value property from the configuration. A
     * single-value property will be returned as an array of dimension 1.
     *
     * @param name The property name
     * @return The property values
     */
	public String[] getPropertyValues(String name) {
        if (properties == null) {
			properties = parseProperties(getPropertiesNode());
		}
        return properties.get(name);
    }

    /**
     * Obtains the reference to the whole properties element as configured
     * in the driverConfig element of this driver in the config file. This
     * method allows custom free-form structures but the driver will need
     * to spend the effort walking the DOM tree.
     *
     * @return The DOM tree representing the properties node
     */
	public Element getPropertiesNode() {
        return agentThread.driverConfig.properties;
    }

    /**
     * Checks whether the driver is currently in steady state or not.
     * This method needs to be called after the critical section of the
     * operation. The transaction times must have been recorded in order
     * to establish whether or not the transaction is in steady state.
     * @return True if in steady state, false if not.
     */
	public boolean isTxSteadyState() {
        return agentThread.isSteadyState();
    }

    /**
     * Reads the element or attribute by it's XPath. The XPath is evaluated
     * from the root of the configuration file.
     *
     * @param xPath The XPath to evaluate.
     * @return The element or attribute value defined by the XPath
     * @exception XPathExpressionException If the given XPath has an error
     */
	public String getXPathValue(String xPath) throws XPathExpressionException {
        if (xPathInstance == null) {
            XPathFactory xf = XPathFactory.newInstance();
            FabanNamespaceContext nsCtx = new FabanNamespaceContext();            
            xPathInstance = xf.newXPath();
            xPathInstance.setNamespaceContext(nsCtx);
        }
        return xPathInstance.evaluate(xPath,
                agentThread.driverConfig.rootElement);
    }

    /**
     * Records the start and end time of the critical section of an operation.
     * This operation may block until the appropriate start time for the
     * operation has arrived. There is no blocking for the end time.
     * This method is for use in the driver code to demarcate critical
     * sections.
     * @throws IllegalStateException if the operation uses auto timing
     */
	public void recordTime() {
        if (agentThread.currentOperation == -1)
            throw new IllegalStateException("DriverContext.recordTime called " +
                                            "outside an operation");
        if (agentThread.driverConfig.operations[agentThread.currentOperation].
                timing != Timing.MANUAL) {
            String msg = "Driver: " + getDriverName() + ", Operation: " +
                    getCurrentOperation() + ", timing: MANUAL illegal call " +
                    "to recordTime() in driver code.";
            logger.severe(msg);
            IllegalStateException e = new IllegalStateException(msg);
            logger.throwing(className, "recordTime", e);
            throw e;
        }
        if (timingInfo != null) {
			if (timingInfo.invokeTime == TIME_NOT_SET) {
                timer.wakeupAt(timingInfo.intendedInvokeTime);
                // But since sleep may not be exact, we get the time again here.
                timingInfo.invokeTime = System.nanoTime();
            } else if (timingInfo.lastRespondTime != TIME_NOT_SET) {
                // The critical section was paused.
                timingInfo.pauseTime +=
                        System.nanoTime() - timingInfo.lastRespondTime;
                timingInfo.lastRespondTime = TIME_NOT_SET;
            } else {
                timingInfo.respondTime = System.nanoTime();
            }
		}
    }

    /**
     * Pauses the critical section so that operations made during the pause
     * do not count into the response time. If Timing.AUTO is used, the pause
     * ends automatically when the next request is sent to the server. For
     * manual timing, the next call to recordTime ends the pause. Calls
     * pauseTime when the critical section is already paused are simply ignored. 
     */
	public void pauseTime() {
        if (agentThread.currentOperation == -1)
            throw new IllegalStateException("DriverContext.pauseTime called " +
                                            "outside an operation");
        if (agentThread.driverConfig.operations[agentThread.currentOperation].
                timing != Timing.MANUAL) {
            String msg = "Driver: " + getDriverName() + ", Operation: " +
                    getCurrentOperation() + ", timing: MANUAL illegal call " +
                    "to pauseTime() in driver code.";
            logger.severe(msg);
            IllegalStateException e = new IllegalStateException(msg);
            logger.throwing(className, "recordTime", e);
            throw e;
        }
        if (timingInfo.lastRespondTime == TIME_NOT_SET) {
            timingInfo.lastRespondTime = System.nanoTime();
		}
    }

    /**
     * Obtains a relative time, in milliseconds. This time is relative to
     * a certain time at the beginning of the benchmark run and does not
     * represent a wall clock time. All agents will have the same reference
     * time. Use this time to check time durations during the benchmark run.
     *
     * @return The relative time of the benchmark run
     */
	public int getTime() {
        return timer.getTime();
    }

    /**
     * Wakes up closest to a system nanosec time.
     * @param time The time to wake up
     */
    public void wakeupAt(long time) {
        timer.wakeupAt(time);
    }

    /**
     * Obtains the relative time - in milliseconds - that steady state starts,
     * if set. The if the time is not yet set, it will return 0.
     *
     * @return The relative time steady state starts
     */
	public int getSteadyStateStart() {
        return (int) (timer.toRelTime(agentThread.endRampUp) / 1000000l);
    }

    /**
     * Obtains a relative time, in nanosecs. This time is relative to
     * a certain time at the beginning of the benchmark run and does not
     * represent a wall clock time. All agents will have the same reference
     * time. Use this time to check time durations during the benchmark run.
     *
     * @return The relative time of the benchmark run
     */
    public long getNanoTime() {
       return timer.toRelTime(System.nanoTime());
    }

    /**
     * Obtains the relative time - in nanosecs - that steady state starts,
     * if set. The if the time is not yet set, it will return 0.
     *
     * @return The relative time steady state starts
     */
    public long getSteadyStateStartNanos() {
        return timer.toRelTime(agentThread.endRampUp);
    }

    /**
     * Obtains the configured ramp up time.
     *
     * @return The configured ramp up time, in seconds
     */
	public int getRampUp() {
        return agentThread.runInfo.rampUp;
    }

    /**
     * Obtains the configured steady state time.
     *
     * @return The configured steady state time, in seconds
     */
	public int getSteadyState() {
        return agentThread.runInfo.stdyState;
    }

    /**
     * Obtains the configured ramp down time.
     *
     * @return The configured ramp down time, in seconds
     */
	public int getRampDown() {
        return agentThread.runInfo.rampDown;
    }

    /**
     * Property whether pause is supported with the current protocol.
     * Default is true. Protocols that may not be request/response but
     * may have concurrent inbound and outbound traffic, AND wishes to
     * support auto timing should set this flag to false. The default is true.
     *
     * @return The current setting of the pauseSupported property.
     */
    public boolean isPauseSupported() {
        return pauseSupported;
    }

    /**
     * Property whether pause is supported with the current protocol.
     * Default is true. Protocols that may not be request/response but
     * may have concurrent inbound and outbound traffic, AND wishes to
     * support auto timing should set this flag to false. The default is true.
     *
     * @param pause The new setting of the pauseSupported property.
     */
    public void setPauseSupported(boolean pause) {
        pauseSupported = pause;
    }

    /**
     * Records the start time of an operation. This method is not
     * exposed through the interface and is only used by the transport
     * facilities.
     * @return The recorded time - system nanotime, or TIME_NOT_SET if not set
     */
    public long recordStartTime() {
        // Not in an operation, don't record time.
        if (agentThread.currentOperation == -1)
            return TIME_NOT_SET;
        if (timingInfo != null && agentThread.driverConfig.operations[
                agentThread.currentOperation].timing == Timing.AUTO) {
            if (timingInfo.invokeTime == TIME_NOT_SET) {
                if (timingInfo.respondTime != TIME_NOT_SET)
                    logger.warning("Respond time already set before " +
                                   "sleeping. Please report a bug.");
                timer.wakeupAt(timingInfo.intendedInvokeTime);
                // But since sleep may not be exact, we get the time again here.
                timingInfo.invokeTime = System.nanoTime();
                return timingInfo.invokeTime;
            } else if (pauseSupported && timingInfo.respondTime != TIME_NOT_SET) {
                if (timingInfo.respondTime < timingInfo.invokeTime)
                    logger.warning("Respond time (" + timingInfo.respondTime +
                            ") less than invoke time (" +
                            timingInfo.invokeTime + "). Please report a bug.");

                // Some response already read, then transmit again.
                // In this case the time from last receive to this transmit
                // is the pause time ...
                timingInfo.lastRespondTime = timingInfo.respondTime;

                // We set the pause time only on the first byte transmitted.
                timingInfo.respondTime = TIME_NOT_SET;

                long time = System.nanoTime();
                timingInfo.pauseTime += time - timingInfo.lastRespondTime;
                return time;
            }
            // Otherwise this can be a subsequent write.
            // Invoke time already set and respond time not set.
        }
        return TIME_NOT_SET;
    }

    /**
     * Records the end time of an operation. This method is not
     * exposed through the interface and is only used by the transport
     * facilities.
     * @return The recorded time - system nanotime, or TIME_NOT_SET if not set
     */
    public long recordEndTime() {
        long tstamp = TIME_NOT_SET;
        // Not in an operation, don't record time.
        if (agentThread.currentOperation != -1) {
            if (timingInfo != null && agentThread.driverConfig.operations[
                    agentThread.currentOperation].timing == Timing.AUTO ) {
                // Some stacks clear the connection by doing a read before a
                // write in a request, normally a read of 0 bytes. We need to
                // make sure such reads are not part of the response time.
                if (timingInfo.invokeTime == TIME_NOT_SET) {
                    int[] previousOps = agentThread.previousOperation;
                    String name = agentThread.driverConfig.mix[0].
                            operations[previousOps[0]].name;
                    if (previousOps.length > 1)
                        name += ',' + agentThread.driverConfig.mix[1].
                                operations[previousOps[1]].name;
                    logger.warning("Read before write! Some input may still " +
                            "be in the buffer from previous operation " +
                            name + ". Ignoring such input.");
                } else {
                    timingInfo.respondTime = tstamp = System.nanoTime();
                }
            }
        }
        return tstamp;
    }

    /**
     * Sets the intended invocation time for the next invocation
     * on this thread. This is called from AgentThread only.
     * @param time The time to invoke
     */
    void setInvokeTime(long time) {

        // Then set the intended start time.
        timingInfo.intendedInvokeTime = time;
        // And set the other times to invalid.
        timingInfo.invokeTime = TIME_NOT_SET;
        timingInfo.respondTime = TIME_NOT_SET;
        timingInfo.lastRespondTime = TIME_NOT_SET;
        timingInfo.pauseTime = 0l;
    }

    /**
     * TimingInfo is a value object that contains individual
     * timing records for each operation.
     */
    public static class TimingInfo {

    	/** Intended Invoke Time. */
        public long intendedInvokeTime = TIME_NOT_SET;

        /** Actual Invoke Time. */
        public long invokeTime = TIME_NOT_SET;

        /** Respond Time. */
        public long respondTime = TIME_NOT_SET;

        /** Last respond time, if any. */
        public long lastRespondTime = TIME_NOT_SET;

        /** Pause Time. */
        public long pauseTime = 0l;
    }

    /**
     * Obtains the base directory where the benchmark currently being run
     * is installed.
     *
     * @return The benchmark's base directory
     */
    public String getBaseDir() {
        return agentThread.agent.driverBase;
    }

    /**
     * Obtains the resource directory used for this benchmark, if exists.
     * @return The resource directory for this benchmark
     */
    public String getResourceDir() {
        return agentThread.agent.driverBase + File.separator + "resources";
    }

    /**
     * Set the desired upload speed for the thread using this context.
     * This method is intended for use only by transport classes; drivers
     * should call an apporpriate method on the transport to set this
     * value. Note that not all transports support bandwidth throttling
     * (so drivers that do call this method will have no idea if the value
     * is used or not).
     *
     * @param kbps desired speed in kilobytes per second. If kbps is < 0,
     * speed will be unlimited.
     */
    public void setUploadSpeed(int kbps) {
        this.kbpsUpload = kbps;
    }

    /**
     * Return the desired upload speed for the thread using this context.
     *
     * @return desired speed in kilobytes per second
     */
    public int getUploadSpeed() {
        return kbpsUpload;
    }

    /**
     * Set the desired downoad speed for the thread using this context.
     * This method is intended for use only by transport classes; drivers
     * should call an apporpriate method on the transport to set this
     * value. Note that not all transports support bandwidth throttling
     * (so drivers that do call this method will have no idea if the value
     * is used or not).
     *
     * @param kbps desired speed in kilobytes per second. If kbps is < 0,
     * speed will be unlimited.
     */
    public void setDownloadSpeed(int kbps) {
        this.kbpsDownload = kbps;
    }

    /**
     * Return the desired download speed for the thread using this context.
     *
     * @return desired speed in kilobytes per second
     */
    public int getDownloadSpeed() {
        return kbpsDownload;
    }
}
