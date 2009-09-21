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

import java.util.HashMap;
import java.util.Iterator;
import java.lang.annotation.Annotation;

/**
 * The implementation of the flat mix annotation.
 *
 * @author Akara Sucharitakul
 */
public class FlatMix extends Mix {

	private static final long serialVersionUID = 1L;

	double[] mix;

    /**
     * Initializes this mix according to the annotation.
     *
     * @param driverClass The driver class annotating this mix
     * @param a           The annotation
     * @throws DefinitionException If there is an error in the annotation
     */
	public void init(Class<?> driverClass, Annotation a)
            throws DefinitionException {
        com.sun.faban.driver.FlatMix flatMix =
                (com.sun.faban.driver.FlatMix) a;
        operations = BenchmarkDefinition.getOperations(driverClass,
                flatMix.operations());
        mix = flatMix.mix();
        deviation = flatMix.deviation();
        validate();
    }

    /**
     * Initializes the default FlatMix without an annotation. 
     * @param driverClass The driver class in question
     * @throws DefinitionException 
     */
    public void init(Class<?> driverClass) throws DefinitionException {
        operations = BenchmarkDefinition.getOperations(driverClass);
        mix = new double[operations.length];
        for (int i = 0; i < mix.length; i++) {
			mix[i] = 1d;
		}
        deviation = 2d; // Default allowed deviation is 2%
    }

    /**
     * Creates and returns a copy of this object.  The precise meaning
     * of "copy" may depend on the class of the object. The general
     * intent is that, for any object <tt>x</tt>, the expression:
     * <blockquote>
     * <pre>
     * x.clone() != x</pre></blockquote>
     * will be true, and that the expression:
     * <blockquote>
     * <pre>
     * x.clone().getClass() == x.getClass()</pre></blockquote>
     * will be <tt>true</tt>, but these are not absolute requirements.
     * While it is typically the case that:
     * <blockquote>
     * <pre>
     * x.clone().equals(x)</pre></blockquote>
     * will be <tt>true</tt>, this is not an absolute requirement.
     * <p/>
     * By convention, the returned object should be obtained by calling
     * <tt>super.clone</tt>.  If a class and all of its superclasses (except
     * <tt>Object</tt>) obey this convention, it will be the case that
     * <tt>x.clone().getClass() == x.getClass()</tt>.
     * <p/>
     * By convention, the object returned by this method should be independent
     * of this object (which is being cloned).  To achieve this independence,
     * it may be necessary to modify one or more fields of the object returned
     * by <tt>super.clone</tt> before returning it.  Typically, this means
     * copying any mutable objects that comprise the internal "deep structure"
     * of the object being cloned and replacing the references to these
     * objects with references to the copies.  If a class contains only
     * primitive fields or references to immutable objects, then it is usually
     * the case that no fields in the object returned by <tt>super.clone</tt>
     * need to be modified.
     * <p/>
     * The method <tt>clone</tt> for class <tt>Object</tt> performs a
     * specific cloning operation. First, if the class of this object does
     * not implement the interface <tt>Cloneable</tt>, then a
     * <tt>CloneNotSupportedException</tt> is thrown. Note that all arrays
     * are considered to implement the interface <tt>Cloneable</tt>.
     * Otherwise, this method creates a new instance of the class of this
     * object and initializes all its fields with exactly the contents of
     * the corresponding fields of this object, as if by assignment; the
     * contents of the fields are not themselves cloned. Thus, this method
     * performs a "shallow copy" of this object, not a "deep copy" operation.
     * <p/>
     * The class <tt>Object</tt> does not itself implement the interface
     * <tt>Cloneable</tt>, so calling the <tt>clone</tt> method on an object
     * whose class is <tt>Object</tt> will result in throwing an
     * exception at run time.
     *
     * @return a clone of this instance.
     * @see Cloneable
     */
    @Override
	public Object clone() {
        FlatMix clone = (FlatMix) super.clone();
        clone.mix = new double[mix.length];
        System.arraycopy(mix, 0, clone.mix, 0, mix.length);
        return clone;
    }

    /**
     * Configures the flat mix based on the ratios given in the
     * configuration file.
     * @param driverConfigNode The DOM node holding the driver config
     * @throws ConfigurationException   If the configuration is invalid
     *                                  for the mix
     */
	public void configure(Element driverConfigNode)
            throws ConfigurationException {

        /* The format is as follows:
        <operationMix>
            <name>operationF</name><r>40</r>
        </operationMix>
        <operationMix>
            <name>operationG</name><r>30</r>
        </operationMix>
        <operationMix>
            <name>operationH</name><r>30</r>
        </operationMix>
        */

        HashMap<String, Double> ratioMap =
                new HashMap<String, Double>(operations.length);
        NodeList operationList = driverConfigNode.
                getElementsByTagNameNS(RunInfo.DRIVERURI, "operationMix");
        int size0 = operationList.getLength();
        for (int i = 0; i < size0; i++) {
            Element opMix = (Element) operationList.item(i);
            NodeList nl = opMix.getElementsByTagNameNS(
                                                    RunInfo.DRIVERURI, "name");
            if (nl.getLength() > 1) {
                String msg = "Only one operation name allowed in each " +
                        "operation mix.";
                getLogger().severe(msg);
                ConfigurationException e = new ConfigurationException(msg);
                getLogger().throwing(className, "configure", e);
                throw e;
            }
            String name = nl.item(0).getFirstChild().getNodeValue();
            nl = opMix.getElementsByTagNameNS(RunInfo.DRIVERURI, "r");
            if (nl.getLength() > 1) {
                String msg = "Only one ratio allowed for @FlatMix";
                getLogger().severe(msg);
                ConfigurationException e = new ConfigurationException(msg);
                getLogger().throwing(className, "configure", e);
                throw e;
            }
            Double ratio = new Double(nl.item(0).getFirstChild().
                    getNodeValue());
            ratioMap.put(name, ratio);
        }

        if (ratioMap.size() <= 0) {
			return;
		}

        for (int i = 0; i < operations.length; i++) {
            Double value = ratioMap.remove(operations[i].name);
            if (value == null) {
                String msg = "Configured ratio for operation " +
                        operations[i].name + " not found";
                getLogger().severe(msg);
                ConfigurationException e = new ConfigurationException(msg);
                getLogger().throwing(className, "configure", e);
                throw e;
            }
            mix[i] = value.doubleValue();
        }

        // By now, the map should be empty
        if (ratioMap.size() > 0) {
            String msg = "";
            for (Iterator<String> i = ratioMap.keySet().iterator();
                 i.hasNext();) {
                if (msg.length() > 0) {
					msg += ", ";
				}
                msg += i.next();
            }
            msg = "Invalid operation name(s) in operationMix " +
                    "configuration: " + msg + '.';
            getLogger().severe(msg);
            ConfigurationException e = new ConfigurationException(msg);
            getLogger().throwing(className, "configure", e);
            throw e;
        }
    }

    /**
     * Validates the mix spec in the benchmark definitions to ensure all
     * the rows and columns are valid.
     * @throws DefinitionException Found missing or
     *                                                  invalid row/column
     */
    public void validate() throws DefinitionException {
        if (mix.length != operations.length) {
            String msg = "Mix ratios must be " + operations.length +
                    "(#ops) in size!\nFound " + mix.length + ".";
            getLogger().severe(msg);
            DefinitionException e = new DefinitionException(msg);
            getLogger().throwing(className, "validate", e);
            throw e;
        }
    }

    /**
     * Normalizes the mix so each row will have a sum of 1.0d. This is
     * important as we do not require the spec input to sum to this
     * amount but the selector (doMenu) will base the random number
     * generator to 1.
     */
	public void normalize() {
        // if (logger.isLoggable(Level.FINEST))
            getLogger().finest("normalize - before\n" + toString());

        double rowTotal = 0d;
        for (int i = 0; i < mix.length; i++) {
			rowTotal += mix[i];
		}
        for (int i = 0; i < mix.length; i++) {
			mix[i] /= rowTotal;
		}

        // if (logger.isLoggable(Level.FINEST))
            getLogger().finest("normalize - after\n" + toString());
    }

    /**
     * Returns the flat mix representation of this mix. This is used for
     * consistent reporting.
     *
     * @return The flat mix representation.
     */
	public FlatMix flatMix() {
        return this;
    }

    /**
     * Provides a string representation of this FlatMix.
     * @return The string representation
     * @see java.lang.Object#toString()
     */
    @Override
	public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("FlatMix\n");
        buffer.append("operations: ");
        buffer.append(operations[0].name);
        for (int i = 1; i < operations.length; i++) {
            buffer.append(", ");
            buffer.append(operations[i].name);
        }
        buffer.append("\nmix: ");
        buffer.append(mix[0]);
        for (int i = 1; i < mix.length; i++) {
            buffer.append(", ");
            buffer.append(mix[i]);
        }
        buffer.append("\ndeviation :");
        buffer.append(deviation);
        buffer.append('\n');

        return buffer.toString();
    }

    /**
     * Obtains the per-thread and per-driver instance selector.
     *
     * @param random The per-thread random value generator
     * @return The selector to be used by the driver
     */
	public Selector selector(Random random) {
        return new Selector(random, mix);
    }

    /**
     * The selector implementation for the FlatMix.
     */
    public static class Selector extends Mix.Selector {

        private Random random;
        private double[] selectMix;

        Selector(Random random, double[] mix) {
            this.random = random;
            selectMix = new double[mix.length];
            selectMix[0] = mix[0];
            for (int i = 1; i < selectMix.length; i++) {
                selectMix[i] = mix[i] + selectMix[i - 1];
            }
        }

        /**
         * The select method selects the operation to run next.
         *
         * @return The operation index selected to run next
         */
		public int select() {
            double val = random.drandom(0, 1);
            for (int i = 0; i < selectMix.length; i++) {
				if (val <= selectMix[i]) {
					return i;
				}
			}
            return -1;
        }

        /**
         * Resets the selector's state to start at the first op,
         * if applicable.
         */
		public void reset() {
            // Noop... there's nothing to reset in a flatmix as it does
            // not contain any state.
        }
    }
}
