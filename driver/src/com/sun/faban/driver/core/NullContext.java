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
 * $Id: NullContext.java,v 1.5 2007/09/07 15:49:05 noahcampbell Exp $
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
     * @see com.sun.faban.driver.DriverContext#getScale()
     */
    @Override
	public int getScale() {
        return 0;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getThreadId()
     */
    @Override
	public int getThreadId() {
        return 0;
    }


    /**
     * @see com.sun.faban.driver.DriverContext#getAgentId()
     */
    @Override
	public int getAgentId() {
        return 0;
    }


    /**
     * @see com.sun.faban.driver.DriverContext#getDriverName()
     */
    @Override
	public String getDriverName() {
        return "DriverTestNullContext";
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getLogger()
     */
    @Override
	public Logger getLogger() {
        if (logger == null) {
			logger = Logger.getLogger("faban.test");
		}
        return logger;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#attachMetrics(com.sun.faban.driver.CustomMetrics)
     */
    @Override
	public void attachMetrics(CustomMetrics metrics) {
        // Noop.
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getCurrentOperation()
     */
    @Override
	public String getCurrentOperation() {
        return "DriverTestNullContext";
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getOperationId()
     */
    @Override
	public int getOperationId() {
        return 0;
    }


    /**
     * @see com.sun.faban.driver.DriverContext#getOperationCount()
     */
    @Override
	public int getOperationCount() {
        return 1;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getRandom()
     */
    @Override
	public Random getRandom() {
        if (random == null) {
			random = new Random();
		}
        return random;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#recordTime()
     */
    @Override
	public void recordTime() {
        // Noop
    }

    /**
     * @see com.sun.faban.driver.DriverContext#pauseTime()
     */
    @Override
	public void pauseTime() {
        //Noop
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getTime()
     */
    @Override
	public int getTime() {
        return (int) (System.currentTimeMillis() - baseTime);
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getSteadyStateStart()
     */
    @Override
	public int getSteadyStateStart() {
        return (int) (System.currentTimeMillis() - baseTime + 5000l);
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getRampUp()
     */
    @Override
	public int getRampUp() {
        return 0;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getSteadyState()
     */
    @Override
	public int getSteadyState() {
        // Just provide a very high number.
        return Integer.MAX_VALUE / 2;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getRampDown()
     */
    @Override
	public int getRampDown() {
        return 0;
    }

    /**
     * @see com.sun.faban.driver.DriverContext#resetMix()
     */
    @Override
	public void resetMix() {
        //noop
    }

    /**
     * @see com.sun.faban.driver.DriverContext#isTxSteadyState()
     */
    @Override
	public boolean isTxSteadyState() {
        return true;
    }
    /**
     * @see com.sun.faban.driver.DriverContext#getProperty(java.lang.String)
     */
    @Override
	public String getProperty(String name) {
        return System.getProperty(name);
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getPropertyValues(java.lang.String)
     */
    @Override
	public String[] getPropertyValues(String name) {
        return new String[0];
    }

    /**
     * @see com.sun.faban.driver.DriverContext#getPropertiesNode()
     */
    @Override
	public Element getPropertiesNode() {
        return null;
    }

    
    /**
     * @see com.sun.faban.driver.DriverContext#getXPathValue(java.lang.String)
     */
    @SuppressWarnings("unused")
	@Override
	public String getXPathValue(String xPath) throws XPathExpressionException {
        return null;
    }
}
