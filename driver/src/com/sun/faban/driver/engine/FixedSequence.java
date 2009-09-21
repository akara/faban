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

import com.sun.faban.driver.ConfigurationException;
import com.sun.faban.driver.DefinitionException;
import com.sun.faban.driver.util.Random;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.lang.annotation.Annotation;

/**
 * The implementation of the fixed sequence annotation.
 *
 * @author Akara Sucharitakul
 */
public class FixedSequence extends Mix {

	private static final long serialVersionUID = 1L;

	/**
     * Initializes this mix according to the annotation.
     *
     * @param driverClass The driver class annotating this mix
     * @param a           The annotation
     * @throws com.sun.faban.driver.DefinitionException
     *          If there is an error in the annotation
     */
	public void init(Class<?> driverClass, Annotation a)
            throws DefinitionException {
        com.sun.faban.driver.FixedSequence fixedMix =
                (com.sun.faban.driver.FixedSequence) a;
        operations = BenchmarkDefinition.getOperations(
                driverClass, fixedMix.value());
        deviation = fixedMix.deviation();
    }

    /**
     * Configures/overrides the mix from the driverConfig DOM node
     * read from the configuration file. The FixedSequence does not
     * allow mix configurations so any operationMix element will
     * cause this method to throw a ConfigurationException
     *
     * @param driverConfigNode The driverConfig DOM node
     * @throws ConfigurationException If operationMix element exists
     */
	public void configure(Element driverConfigNode)
            throws ConfigurationException {
        NodeList operationList = driverConfigNode.
                getElementsByTagNameNS(RunInfo.DRIVERURI, "operationMix");
        if (operationList.getLength() > 0) {
            String msg = "Illegal operationMix configuration - " +
                    "not allowed for @FixedSequence driver.";
            getLogger().severe(msg);
            ConfigurationException e = new ConfigurationException(msg);
            getLogger().throwing(className, "configure", e);
            throw e;
        }
    }

    /**
     * Normalizes the mix so it is ready for use in selections.
     * The fixed sequence has no selection and does not need
     * normalization so this method is a noop.
     */
	public void normalize() {
    	// noop
    }

    /**
     * Returns the flat mix representation of this mix. This is used for
     * consistent reporting.
     *
     * @return The flat mix representation.
     */
	public FlatMix flatMix() {
        FlatMix flatMix = new FlatMix();
        flatMix.operations = operations;
        flatMix.deviation = deviation;
        flatMix.mix = new double[operations.length];
        double ratio = 1d / operations.length;
        for (int i = 0; i < flatMix.mix.length; i++) {
			flatMix.mix[i] = ratio;
		}
        return flatMix;
    }

    /**
     * Obtains the per-thread and per-driver instance selector.
     *
     * @param random The per-thread random value generator
     * @return The selector to be used by the driver
     */
	public Selector selector(Random random) {
        return new Selector(operations);
    }

    /**
     * The selector implementation for the fixed sequence.
     */
    public static class Selector extends Mix.Selector {

        private int currentOp = -1;
        private int totalOps;

        Selector(BenchmarkDefinition.Operation[] operations) {
            totalOps = operations.length;
        }

        /**
         * The select method selects the operation to run next.
         *
         * @return The operation index selected to run next
         */
		public int select() {
            ++currentOp;
            if (currentOp >= totalOps) {
				currentOp = 0;
			}
            return currentOp;
        }

        /**
         * Resets the selector's state to start at the first op,
         * if applicable.
         */
		public void reset() {
            currentOp = -1;
        }
    }
}
