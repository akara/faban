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
 * $Id: NullContext.java,v 1.2 2009/05/17 20:02:02 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
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
     * @see com.sun.faban.driver.DriverContext#getScale()
     */
	public int getScale() {
        return 0;
    }

    /**
     * Obtains the number of client threads in this agent.
     *
     * @return the number of client threads
     */
    public int getClientsInAgent() {
        return 0;
    }

    /**
     * Obtains the total number of clients threads for this driver.
     *
     * @return the number of client threads for this driver
     */
    public int getClientsInDriver() {
        return 0;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getThreadId()
     */
	public int getThreadId() {
        return 0;
    }


    /**
     * @see com.sun.faban.driver.DriverContext#getAgentId()
     */
	public int getAgentId() {
        return 0;
    }


    /**
     * @see com.sun.faban.driver.DriverContext#getDriverName()
     */
	public String getDriverName() {
        return "DriverTestNullContext";
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getLogger()
     */
	public Logger getLogger() {
        if (logger == null) {
			logger = Logger.getLogger("faban.test");
		}
        return logger;
    }

    /**
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
     * @see com.sun.faban.driver.DriverContext#getCurrentOperation()
     */
	public String getCurrentOperation() {
        return "DriverTestNullContext";
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getOperationId()
     */
	public int getOperationId() {
        return 0;
    }


    /**
     * @see com.sun.faban.driver.DriverContext#getOperationCount()
     */
	public int getOperationCount() {
        return 1;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getRandom()
     */
	public Random getRandom() {
        if (random == null) {
			random = new Random();
		}
        return random;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#recordTime()
     */
	public void recordTime() {
        // Noop
    }

    /**
     * @see com.sun.faban.driver.DriverContext#pauseTime()
     */
	public void pauseTime() {
        //Noop
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getTime()
     */
	public int getTime() {
        return (int) (System.currentTimeMillis() - baseMillis);
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getSteadyStateStart()
     */
	public int getSteadyStateStart() {
        return (int) (System.currentTimeMillis() - baseMillis + 5000l);
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getNanoTime()
     */
    public long getNanoTime() {
        return System.nanoTime() - baseNanos;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getSteadyStateStartNanos()
     */
    public long getSteadyStateStartNanos() {
        return System.nanoTime() - baseNanos + 5000000000l;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getRampUp()
     */
	public int getRampUp() {
        return 0;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getSteadyState()
     */
	public int getSteadyState() {
        // Just provide a very high number.
        return Integer.MAX_VALUE / 2;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getRampDown()
     */
	public int getRampDown() {
        return 0;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#resetMix()
     */
	public void resetMix() {
        //noop
    }

    /**
     * @see com.sun.faban.driver.DriverContext#isTxSteadyState()
     */
	public boolean isTxSteadyState() {
        return true;
    }
    /**
     * @see com.sun.faban.driver.DriverContext#getProperty(java.lang.String)
     */
	public String getProperty(String name) {
        return System.getProperty(name);
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getPropertyValues(java.lang.String)
     */
	public String[] getPropertyValues(String name) {
        return new String[0];
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getPropertiesNode()
     */
	public Element getPropertiesNode() {
        return null;
    }

    
    /**
     * @see com.sun.faban.driver.DriverContext#getXPathValue(java.lang.String)
     */
    @SuppressWarnings("unused")
	public String getXPathValue(String xPath) throws XPathExpressionException {
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
