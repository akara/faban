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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: NullContext.java,v 1.1 2006/06/29 18:51:33 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.core;

import com.sun.faban.driver.DriverContext;
import com.sun.faban.driver.CustomMetrics;
import com.sun.faban.driver.util.Random;

import java.util.logging.Logger;

import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;

/**
 * The null context is used for testing driver code for the Faban driver
 * framework without actually running the agents. It ensures that the same
 * driver code can be tested in it's own main method.
 *
 * @author Akara Sucharitakul
 */
public class NullContext extends DriverContext {
    private Logger logger;
    private Random random;
    private long baseTime = System.currentTimeMillis();

    /**
     * Obtains the scale or scaling rate of the current run.
     * The scale is always 0 for the NullContext.
     * @return the current run's scaling rate
     */
    public int getScale() {
        return 0;
    }

    /**
     * Obtains the global agent thread id for this context's thread.
     *
     * @return the global agent thread id
     */
    public int getThreadId() {
        return 0;
    }

    /**
     * Obtains the agent id for this agent.
     *
     * @return the current agent's id
     */
    public int getAgentId() {
        return 0;
    }

    /**
     * Obtains the driver's name as annotated in the driver class.
     *
     * @return the driver name
     */
    public String getDriverName() {
        return "DriverTestNullContext";
    }

    /**
     * Obtains the logger to be used by the calling driver.
     *
     * @return the appropriate logger
     */
    public Logger getLogger() {
        if (logger == null)
            logger = Logger.getLogger("faban.test");
        return logger;
    }

    /**
     * Attaches a custom metrics object to the primary metrics.
     * This should be done by the driver at initialization time.
     * Only one custom metrics can be attached. Subsequent calls
     * to this method replaces the previously attached metrics.
     *
     * @param metrics The custom metrics to be replaced
     */
    public void attachMetrics(CustomMetrics metrics) {
        // Noop.
    }

    /**
     * Obtains the name of the operation currently executing.
     *
     * @return the current operation's name
     */
    public String getCurrentOperation() {
        return "DriverTestNullContext";
    }

    /**
     * Obtains the per-thread random value generator. Drivers
     * should use this random value generator and not instantiate
     * their own.
     *
     * @return The random value generator
     */
    public Random getRandom() {
        if (random == null)
            random = new Random();
        return random;
    }

    /**
     * Records the start time and end of the critical section of an operation.
     * This operation may block until the appropriate start time for the
     * operation has arrived. There is no blocking for the end time.
     *
     * @throws IllegalStateException if the operation uses auto timing
     */
    public void recordTime() {
        // Noop
    }

    /**
     * Pauses the critical section so that operations made during the pause
     * do not count into the response time. If Timing.AUTO is used, the pause
     * ends automatically when the next request is sent to the server. For
     * manual timing, the next call to recordTime ends the pause.
     */
    public void pauseTime() {
        //Noop
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
        return (int) (System.currentTimeMillis() - baseTime);
    }

    /**
     * Resets the state of the current mix to start off at the beginning
     * of the mix. For stateless mixes such as FlatMix, this operation
     * does nothing.
     */
    public void resetMix() {
        //noop
    }

    /**
     * Checks whether the driver is currently in steady state or not.
     * In NullContext, isTxSteadyState always returns true to excercise
     * the code.
     * @return True
     */
    public boolean isTxSteadyState() {
        return true;
    }
    /**
     * Obtains a single-value property from the configuration. If the name
     * of a multi-value property is given, only one value is returned.
     * It is undefined as to which value in the list is returned.
     *
     * @param name The property name
     * @return The property value
     */
    public String getProperty(String name) {
        return System.getProperty(name);
    }

    /**
     * Obtains a multiple-value property from the configuration. A
     * single-value property will be returned as an array of dimension 1.
     *
     * @param name The property name
     * @return The property values
     */
    public String[] getPropertyValues(String name) {
        return new String[0];
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
        return null;
    }

    /**
     * Reads the element or attribute by it's XPath. The XPath is evaluated
     * from the root of the configuration file.
     *
     * @param xPath The XPath to evaluate.
     * @return The element or attribute value defined by the XPath
     * @throws javax.xml.xpath.XPathExpressionException
     *          If the given XPath has an error
     */
    public String getXPathValue(String xPath) throws XPathExpressionException {
        return null;
    }
}
