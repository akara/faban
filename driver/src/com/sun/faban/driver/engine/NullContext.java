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
package com.sun.faban.driver.engine;

import com.sun.faban.driver.CustomMetrics;
import com.sun.faban.driver.CustomTableMetrics;
import com.sun.faban.driver.DriverContext;
import com.sun.faban.driver.util.Random;
import org.w3c.dom.Element;

import javax.xml.xpath.XPathExpressionException;
import java.util.logging.Logger;

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
    private long baseMillis = System.currentTimeMillis();
    private long baseNanos = System.nanoTime();


    /**
     * Obtains the scale of this run. This implementation always return 0.
     * @return Always 0
     * @see com.sun.faban.driver.DriverContext#getScale()
     */
	public int getScale() {
        return 0;
    }

    /**
     * Obtains the number of client threads in this agent.
     * @return the number of client threads
     */
    public int getClientsInAgent() {
        return 0;
    }

    /**
     * Obtains the total number of clients threads for this driver.
     * @return the number of client threads for this driver
     */
    public int getClientsInDriver() {
        return 0;
    }

    /**
     * Obtains the current thread id. This implementation always returns 0.
     * @return Always 0
     * @see com.sun.faban.driver.DriverContext#getThreadId()
     */
	public int getThreadId() {
        return 0;
    }


    /**
     * Obtains the agent id. This implementation always returns 0.
     * @return Always returns 0
     * @see com.sun.faban.driver.DriverContext#getAgentId()
     */
	public int getAgentId() {
        return 0;
    }


    /**
     * Returns the driver name. In this case a dummy driver name
     * "DriverTestNullContext".
     * @return The dummy driver name.
     * @see com.sun.faban.driver.DriverContext#getDriverName()
     */
	public String getDriverName() {
        return "DriverTestNullContext";
    }

    /**
     * Obtains the logger for this driver thread. The logger name
     * is always "faban.test."
     * @return The logger for this thread.
     * @see com.sun.faban.driver.DriverContext#getLogger()
     */
	public Logger getLogger() {
        if (logger == null) {
			logger = Logger.getLogger("faban.test");
		}
        return logger;
    }

    /**
     * Attaches a custom metrics as the miscellaneous stats.
     * This implementation does nothing.
     * @param metrics The metrics to attach
     * @see com.sun.faban.driver.DriverContext#attachMetrics(com.sun.faban.driver.CustomMetrics)
     */
	public void attachMetrics(CustomMetrics metrics) {
        // Noop.
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
    @Override
    public void attachMetrics(String name, CustomMetrics metrics) {
        // Noop.
    }

    /**
     * Attaches a custom table metrics object to the primary metrics,
     * given a name or description. The name/description must be unique.
     * This should be done by the driver at initialization time.
     * Only one custom metrics can be attached. Subsequent calls
     * to this method replaces the previously attached metrics.
     * @param name    The name or description of this metrics
     * @param metrics The custom table metrics to be replaced
     */

    @Override
    public void attachMetrics(String name, CustomTableMetrics metrics) {
        // Noop.
    }

    /**
     * Returns the current operation. For the null context, it is a dummy
     * name "DriverTestNullContext."
     * @return The dummy name of the current operation.
     * @see com.sun.faban.driver.DriverContext#getCurrentOperation()
     */
	public String getCurrentOperation() {
        return "DriverTestNullContext";
    }

    /**
     * Obtains the operation id of the current operation. This implementation
     * always returns 0.
     * @return Always 0
     * @see com.sun.faban.driver.DriverContext#getOperationId()
     */
	public int getOperationId() {
        return 0;
    }


    /**
     * Gets the current count of the operations done by this thread.
     * @return Always 1 for this implementation
     * @see com.sun.faban.driver.DriverContext#getOperationCount()
     */
	public int getOperationCount() {
        return 1;
    }

    /**
     * Obtains the random value generator for this thread.
     * @return Always the same random value generator
     * @see com.sun.faban.driver.DriverContext#getRandom()
     */
	public Random getRandom() {
        if (random == null) {
			random = new Random();
		}
        return random;
    }

    /**
     * Records the start and end of time measurement. This implemenation does
     * nothing.
     * @see com.sun.faban.driver.DriverContext#recordTime()
     */
	public void recordTime() {
        // Noop
    }

    /**
     * Pauses the time measurement. This implementation does nothing.
     * @see com.sun.faban.driver.DriverContext#pauseTime()
     */
	public void pauseTime() {
        //Noop
    }

    /**
     * Obtains the relative current time.
     * @return The current relative time
     * @see com.sun.faban.driver.DriverContext#getTime()
     */
	public int getTime() {
        return (int) (System.currentTimeMillis() - baseMillis);
    }

    /**
     * Obtains the relative time steady state starts. This implementation
     * always returns the relative time, 5 seconds from the current time
     * which is in the future.
     * @return The steady state start time
     * @see com.sun.faban.driver.DriverContext#getSteadyStateStart()
     */
	public int getSteadyStateStart() {
        return (int) (System.currentTimeMillis() - baseMillis + 5000l);
    }

    /**
     * Obtain the current nano time offset from the base time.
     * @return The current nano time
     * @see com.sun.faban.driver.DriverContext#getNanoTime()
     */
    public long getNanoTime() {
        return System.nanoTime() - baseNanos;
    }

    /**
     * Obtain the nano time offset where steady state starts.
     * @return A dummy time 5 seconds after the base time
     * @see com.sun.faban.driver.DriverContext#getSteadyStateStartNanos()
     */
    public long getSteadyStateStartNanos() {
        return System.nanoTime() - baseNanos + 5000000000l;
    }

    /**
     * Obtains the rampup time. This implementation always returns 0.
     * @return The rampup time which is 0
     * @see com.sun.faban.driver.DriverContext#getRampUp()
     */
	public int getRampUp() {
        return 0;
    }

    /**
     * Obtains the steady state time. This implementation returns a very large
     * dummy number of Integer.MAX_VALUE / 2.
     * @return A dummy steady state time
     * @see com.sun.faban.driver.DriverContext#getSteadyState()
     */
	public int getSteadyState() {
        // Just provide a very high number.
        return Integer.MAX_VALUE / 2;
    }

    /**
     * Obtains the ramp down time. This implementation returns 0.
     * @return A dummy ramp down time of 0.
     * @see com.sun.faban.driver.DriverContext#getRampDown()
     */
	public int getRampDown() {
        return 0;
    }

    /**
     * Resets the mix. We don't use mixes with the null context so
     * this implementation does nothing.
     * @see com.sun.faban.driver.DriverContext#resetMix()
     */
	public void resetMix() {
        //noop
    }

    /**
     * Checks whether we're in the steady state. This implementation
     * always return true.
     * @return Always true
     * @see com.sun.faban.driver.DriverContext#isTxSteadyState()
     */
	public boolean isTxSteadyState() {
        return true;
    }
    /**
     * Gets the driver property. This implementation gets the property from
     * the java properties.
     * @param name The name of the property
     * @return The property value, or null if it does not exist
     * @see com.sun.faban.driver.DriverContext#getProperty(java.lang.String)
     */
	public String getProperty(String name) {
        return System.getProperty(name);
    }

    /**
     * Gets the driver properties by name. This implementation gets the
     * property from the java properties and returns an array of size 1 at
     * most. If the property is not set, returns null.
     * @param name The name of the property
     * @return An array of size 1 containing the value, or null if not exist
     * @see com.sun.faban.driver.DriverContext#getPropertyValues(java.lang.String)
     */
	public String[] getPropertyValues(String name) {
        String[] ret = null;
        String v = System.getProperty(name);
        if (v != null) {
            ret = new String[1];
            ret[0] = v;
        }
        return ret;
    }

    /**
     * Gets the properties node from the dom tree. NullContext does not
     * use a run configuration so this will always return null.
     * @return Always null.
     * @see com.sun.faban.driver.DriverContext#getPropertiesNode()
     */
	public Element getPropertiesNode() {
        return null;
    }

    
    /**
     * Returns a value matching the given xpath in the configuration file.
     * NullContext does not use a run configuration so this will always return
     * null.
     * @param xPath The xpath
     * @return Always null
     * @see com.sun.faban.driver.DriverContext#getXPathValue(java.lang.String)
     */
    @SuppressWarnings("unused")
	public String getXPathValue(String xPath) {
        return null;
    }


    /**
     * Obtains the base directory where the benchmark currently being run
     * is installed.
     *
     * @return The benchmark's base directory
     */
    public String getBaseDir() {
        return System.getProperty("faban.driver.base");
    }

    /**
     * Obtains the resource directory used for this benchmark, if exists.
     * @return The resource directory for this benchmark
     */
    public String getResourceDir() {
        String driverBase = System.getProperty("faban.driver.base");
        if (driverBase == null)
            return null;
        return driverBase + "/resources";
    }
}
