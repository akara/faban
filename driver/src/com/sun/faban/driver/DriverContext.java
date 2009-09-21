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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver;

import com.sun.faban.driver.util.Random;
import com.sun.faban.driver.engine.NullContext;

import java.util.logging.Logger;

import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;

/**
 * DriverContext is the point of communication between the
 * developer-provided driver and the Faban driver framework.
 * Each thread has it's own context.<p>
 * The only concrete implementation of this abstract class
 * is in com.sun.faban.driver.engine.DriverContext.
 *
 * @author Akara Sucharitakul
 */
public abstract class DriverContext {

    /**
     * Obtains the DriverContext associated with this thread.
     * @return the associated DriverContext
     */
    public static DriverContext getContext() {
        DriverContext ctx =
                com.sun.faban.driver.engine.DriverContext.getContext();
        if (ctx != null)
            return ctx;
        return new NullContext();
    }

    /**
     * Obtains the scale or scaling rate of the current run.
     * @return the current run's scaling rate
     */
    public abstract int getScale();

    /**
     * Obtains the number of client threads in this agent.
     *
     * @return the number of client threads for this agent
     */
    public abstract int getClientsInAgent();

    /**
     * Obtains the total number of clients threads for this driver.
     * @return the number of client threads for this driver
     */
    public abstract int getClientsInDriver();

    /**
     * Obtains the global agent thread id for this context's thread.
     * @return the global agent thread id
     */
    public abstract int getThreadId();

    /**
     * Obtains the agent id for this agent.
     * @return the current agent's id
     */
    public abstract int getAgentId();

    /**
     * Obtains the driver's name as annotated in the driver class.
     * @return the driver name
     */
    public abstract String getDriverName();

    /**
     * Obtains the logger to be used by the calling driver.
     * @return the appropriate logger
     */
    public abstract Logger getLogger();

    /**
     * Attaches a custom metrics object to the primary metrics.
     * This should be done by the driver at initialization time.
     * Only one custom metrics can be attached. Subsequent calls
     * to this method replaces the previously attached metrics.
     * @param metrics The custom metrics to be replaced
     */
    public abstract void attachMetrics(CustomMetrics metrics);

    /**
     * Attaches a custom metrics object to the primary metrics,
     * given a name or description. The name/description must be unique.
     * This should be done by the driver at initialization time.
     * Only one custom metrics can be attached. Subsequent calls
     * to this method replaces the previously attached metrics.
     * @param name    The name or description of this metrics
     * @param metrics The custom metrics to be replaced
     */
    public abstract void attachMetrics(String name, CustomMetrics metrics);

    /**
     * Attaches a custom table metrics object to the primary metrics,
     * given a name or description. The name/description must be unique.
     * This should be done by the driver at initialization time.
     * Only one custom metrics can be attached. Subsequent calls
     * to this method replaces the previously attached metrics.
     * @param name    The name or description of this metrics
     * @param metrics The custom table metrics to be replaced
     */
    public abstract void attachMetrics(String name, CustomTableMetrics metrics);

    /**
     * Obtains the name of the operation currently executing.
     * @return the current operation's name
     */
    public abstract String getCurrentOperation();

    /**
     * Obtains the unique id assigned to the current operation type.
     * This id is commonly used to index into array structures containing
     * operation-specific information such as stats. The id ranges from 0 to
     * n where n is the number of operations in the driver less one.
     * @return The unique id assigned to this operation type.
     */
    public abstract int getOperationId();

    /**
     * Obtains the number of operations active in this driver.
     * @return The number of active operations
     */
    public abstract int getOperationCount();

    /**
     * Obtains the per-thread random value generator. Drivers
     * should use this random value generator and not instantiate
     * their own.
     * @return The random value generator
     */
    public abstract Random getRandom();

    /**
     * Checks whether the driver is currently in steady state or not.
     * This method needs to be called after the critical section of the
     * operation. The transaction times must have been recorded in order
     * to establish whether or not the transaction is in steady state.
     * @return True if in steady state, false if not.
     */
    public abstract boolean isTxSteadyState();

    /**
     * Resets the state of the current mix to start off at the beginning
     * of the mix. For stateless mixes such as FlatMix, this operation
     * does nothing.
     */
    public abstract void resetMix();

    /**
     * Records the start time and end of the critical section of an operation.
     * This operation may block until the appropriate start time for the
     * operation has arrived. There is no blocking for the end time.
     * @throws IllegalStateException if the operation uses auto timing
     */
    public abstract void recordTime();

    /**
     * Pauses the critical section so that operations made during the pause
     * do not count into the response time. If Timing.AUTO is used, the pause
     * ends automatically when the next request is sent to the server. For
     * manual timing, the next call to recordTime ends the pause.
     */
    public abstract void pauseTime();

    /**
     * Obtains a relative time, in milliseconds. This time is relative to
     * a certain time at the beginning of the benchmark run and does not
     * represent a wall clock time. All agents will have the same reference
     * time. Use this time to check time durations during the benchmark run.
     * @return The relative time of the benchmark run
     */
    public abstract int getTime();

    /**
     * Obtains the relative time - in milliseconds - that steady state starts,
     * if set. The if the time is not yet set, it will return 0.
     * @return The relative time steady state starts
     */
    public abstract int getSteadyStateStart();

    /**
     * Obtains a relative time, in nanosecs. This time is relative to
     * a certain time at the beginning of the benchmark run and does not
     * represent a wall clock time. All agents will have the same reference
     * time. Use this time to check time durations during the benchmark run.
     * @return The relative time of the benchmark run
     */
    public abstract long getNanoTime();

    /**
     * Obtains the relative time - in nanosecs - that steady state starts,
     * if set. The if the time is not yet set, it will return 0.
     * @return The relative time steady state starts
     */
    public abstract long getSteadyStateStartNanos();

    /**
     * Obtains the configured ramp up time.
     * @return The configured ramp up time, in seconds
     */
    public abstract int getRampUp();

    /**
     * Obtains the configured steady state time.
     * @return The configured steady state time, in seconds
     */
    public abstract int getSteadyState();

    /**
     * Obtains the configured ramp down time.
     * @return The configured ramp down time, in seconds
     */
    public abstract int getRampDown();



    /**
     * Obtains a single-value property from the configuration. If the name
     * of a multi-value property is given, only one value is returned.
     * It is undefined as to which value in the list is returned.
     * @param name The property name
     * @return The property value
     */
    public abstract String getProperty(String name);

    /**
     * Obtains a multiple-value property from the configuration. A
     * single-value property will be returned as an array of dimension 1.
     * @param name The property name
     * @return The property values
     */
    public abstract String[] getPropertyValues(String name);

    /**
     * Obtains the reference to the whole properties element as configured
     * in the driverConfig element of this driver in the config file. This
     * method allows custom free-form structures but the driver will need
     * to spend the effort walking the DOM tree.
     * @return The DOM tree representing the properties node
     */
    public abstract Element getPropertiesNode();

    /**
     * Reads the element or attribute by it's XPath. The XPath is evaluated
     * from the root of the configuration file.
     * @param xPath The XPath to evaluate.
     * @return The element or attribute value defined by the XPath
     * @exception XPathExpressionException If the given XPath has an error
     */
    public abstract String getXPathValue(String xPath)
            throws XPathExpressionException;

    /**
     * Obtains the base directory where the benchmark currently being run
     * is installed.
     * @return The benchmark's base directory
     */
    public abstract String getBaseDir();

    /**
     * Obtains the resource directory used for this benchmark, if exists.
     * @return The resource directory for this benchmark
     */ 
    public abstract String getResourceDir();
}
