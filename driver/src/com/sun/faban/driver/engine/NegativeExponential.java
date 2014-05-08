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

import com.sun.faban.common.Utilities;
import com.sun.faban.driver.util.Random;
import com.sun.faban.driver.ConfigurationException;
import com.sun.faban.driver.CycleType;
import com.sun.faban.driver.DefinitionException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.lang.annotation.Annotation;

/**
 * Implements the negative exponential distribution/selection.
 *
 * @author Akara Sucharitakul
 */
public class NegativeExponential extends Cycle {

	private static final long serialVersionUID = 1L;

    long cycleMin = 0;
	long cycleMean = 1000 * Utilities.TO_NANOS;
    long cycleMax = 5000 * Utilities.TO_NANOS;
    boolean truncate = true;

    /**
     * Initializes this cycle according to the annotation.
     * @param a The annotation
     * @throws DefinitionException If there is an error in the annotation
     */
	public void init(Annotation a) throws DefinitionException {
        com.sun.faban.driver.NegativeExponential cycleDef =
                (com.sun.faban.driver.NegativeExponential) a;
        cycleType = cycleDef.cycleType();
        cycleDeviation = cycleDef.cycleDeviation();
        cycleMin = cycleDef.cycleMin();
        cycleMean = cycleDef.cycleMean();
        cycleMax = cycleDef.cycleMax();
        truncate = cycleDef.truncateAtMin();
        if (cycleMax == -1) {
			cycleMax = 5 * cycleMean;
		}

        // Now check parameters for validity.
        if (cycleMin < 0) {
			throw new DefinitionException("@NegativeExponential cycleMin < 0");
		}
        if (cycleMean < 0) {
			throw new DefinitionException("@NegativeExponential cycleMean < 0");
		}
        if (cycleMax < 0) {
			throw new DefinitionException("@NegativeExponential cycleMax < 0");
		}
        if (cycleMin > cycleMean) {
			throw new DefinitionException(
                    "@NegativeExponential cycleMin > cycleMean");
		}
        if (cycleMean > cycleMax) {
			throw new DefinitionException(
                    "@NegativeExponential cycleMean > cycleMax");
		}
        if (cycleMax == 0 && cycleType == CycleType.CYCLETIME) {
			throw new DefinitionException(
                    "@NegativeExponential CYCLETIME cycleMax cannot be 0");
		}
        // Adjust time to nanosec.
        cycleMin  *= Utilities.TO_NANOS;
        cycleMean *= Utilities.TO_NANOS;
        cycleMax  *= Utilities.TO_NANOS;
    }

    /**
     * Randoms/calculates the delay time for a thread based on its
     * supplied random number generator and the actual conditions in the
     * distribution.
     *
     * @param random The random number generator used
     * @return The delay time
     */
	public long getDelay(Random random) {
        long delay = 0;
        long mean = cycleMean;
        long shift = 0;
        
        if (!truncate) {
            shift = cycleMin;
            mean -= shift;
        }

        if (cycleMean > 0) {
            double x = random.drandom(0.0, 1.0);
            if (x == 0) {
				x = 1e-20d;
			}
            delay = shift + (long)(mean * -Math.log(x));
            if (delay < cycleMin) {
                delay = cycleMin;
            } else if (delay > cycleMax) {
				delay = cycleMax;
			}
        }
        return delay;
    }

    /**
     * Provides the maximum value to be represented inside a histogram.
     *
     * @return The max reasonable delay to be presented in the output histogram.
     */
	public double getHistogramMax() {
        if (cycleMax > 0) {
			return cycleMax;
		}

        // We know it takes very little time to prepare the data to submit.
        // This case can only happen for think time. Giving a histogram
        // max of 2 seconds for data prep should be enough.
        return 2d;
    }

	/**
	 * Configure the cycle based on an XML fragment from the configuration
	 * file. The format of the fragment is:
	 *
	 * <pre>
	 * <cycleType>CYCLETIME</cycleType>  -- or THINKTIME
	 * <cycleMin>min</cycleMin>
	 * <cycleMean>mean</cycleMean>
	 * <cycleMax>max</cycleMax>
	 * <cycleDeviation>deviation</cycleDeviation>
	 * <truncateAtMin>true</truncateAtMin> -- or false
	 * </pre>
	 */
    protected void configureSubclass(Element e) throws ConfigurationException {
	    NodeList nl = e.getElementsByTagNameNS(
						RunInfo.DRIVERURI, "cycleMin");
		if (nl.getLength() > 1) {
		    String msg = "Bad cycleMin definition; must have only one per cycleTime";
			getLogger().severe(msg);
			ConfigurationException ce = new ConfigurationException(msg);
			getLogger().throwing(getClass().getName(), "configure", ce);
			throw ce;
		}
		if (nl.getLength() == 1) {
		    cycleMin = Long.parseLong(nl.item(0).getFirstChild().getNodeValue()) * Utilities.TO_NANOS;
		}
	    nl = e.getElementsByTagNameNS(
						RunInfo.DRIVERURI, "cycleMean");
		if (nl.getLength() > 1) {
		    String msg = "Bad cycleMean definition; must have only one per cycleTime";
			getLogger().severe(msg);
			ConfigurationException ce = new ConfigurationException(msg);
			getLogger().throwing(getClass().getName(), "configure", ce);
			throw ce;
		}
		if (nl.getLength() == 1) {
		    cycleMean = Long.parseLong(nl.item(0).getFirstChild().getNodeValue()) * Utilities.TO_NANOS;
		}
	    nl = e.getElementsByTagNameNS(
						RunInfo.DRIVERURI, "cycleMax");
		if (nl.getLength() > 1) {
		    String msg = "Bad cycleMax definition; must have only one per cycleTime";
			getLogger().severe(msg);
			ConfigurationException ce = new ConfigurationException(msg);
			getLogger().throwing(getClass().getName(), "configure", ce);
			throw ce;
		}
		if (nl.getLength() == 1) {
		    cycleMax = Long.parseLong(nl.item(0).getFirstChild().getNodeValue()) * Utilities.TO_NANOS;
		}
	    nl = e.getElementsByTagNameNS(
						RunInfo.DRIVERURI, "cycleDeviation");
		if (nl.getLength() > 1) {
		    String msg = "Bad cycleDeviation definition; must have only one per cycleTime";
			getLogger().severe(msg);
			ConfigurationException ce = new ConfigurationException(msg);
			getLogger().throwing(getClass().getName(), "configure", ce);
			throw ce;
		}
		if (nl.getLength() == 1) {
		    truncate = Boolean.parseBoolean(nl.item(0).getFirstChild().getNodeValue());
		}
	}
}
