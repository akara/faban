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
import com.sun.faban.driver.OperationSequence;
import com.sun.faban.driver.util.Random;
import org.w3c.dom.Element;

import java.util.HashMap;
import java.util.Iterator;
import java.lang.annotation.Annotation;

/**
 * The implementation of the flat sequence mix annotation.
 *
 *
 * This mix allows you to specify a set of sequences of operations. For
 * example, a workload may define two scenarios: shopping and browsing.
 * The shopping operation itself is made up of three ordered operations (login,
 * buy, home) and the browsing operation is made up of two operations
 * (search, home). Say that 75% of the workload is browsing and 25% of
 * the workload is shopping. Such a mix of operations would be represented
 * like this:
 * <br>
 * <pre>
 &#064;FlatSequenceMix (
 sequences = {
 &#064;OperationSequence({"login", "buy", "home" }),
 &#064;OperationSequence({"search", "home" })
 },
 mix = { 25, 75 },
 deviation = 2
 )</pre>
 * The resulting metric will be the number of the 4 operations (login, buy,
 * search, home) in the time defined for each operation. The driver will
 * expect 45.75% of the operations to be home (50% of 75% of the operations
 * from browsing and 33% of 25% of the operations from shopping), 37.5% to
 * be search, and 8.25% each for login and buy.
 *
 * @author Scott Oaks
 */
public class FlatSequenceMix extends Mix {

	private static final long serialVersionUID = 1L;

	private String[] operationNames;
    private int[][] operationSequences;
    double[] mix;
    double[] normalizedMix;

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
        com.sun.faban.driver.FlatSequenceMix fsMix =
                (com.sun.faban.driver.FlatSequenceMix) a;
        OperationSequence[] seq = fsMix.sequences();
        mix = fsMix.mix();
        init(seq);
        operations = BenchmarkDefinition.getOperations(
                driverClass, operations());
        deviation = fsMix.deviation();
        validate();
    }

    /**
     * Initialized the mix based on the defined sequences from the driver
     * annotations.
     * @param seq An array of {@link OperationSequence}
     */
    public void init(OperationSequence[] seq) {
        // We're assuming mix[] adds to 100%, which is okay because
        // validate() told us that.
        HashMap<String, Double> set = new HashMap<String, Double>();

        // Put all the operations into a map. For each operation,
        // calculate it's percentage in the particular sequence and
        // multiply by the percentage of the sequence in the mix. That's
        // the total expected for that operation.
        for (int i = 0; i < seq.length; i++) {
            String[] s = seq[i].value();
            for (int j = 0; j < s.length; j++) {
                double factor = 1. / s.length;
                factor *= (mix[i] / 100.);
                Double d = set.get(s[j]);
                if (d == null) {
					d = new Double(factor);
				} else {
					d = new Double(d.doubleValue() + factor);
				}
                set.put(s[j], d);
            }
        }

        // Now we can calculate:
        //   1) The entire set of URLs and store them in operationNames
        //   2) The expected mix of those URLs based on how many times
        //      they appeared.
        Iterator<String> it = set.keySet().iterator();
        operationNames = new String[set.size()];
        normalizedMix = new double[set.size()];
        for (int i = 0; i < normalizedMix.length; i++) {
            String s = it.next();
            operationNames[i] = s;
            normalizedMix[i] = set.get(s).doubleValue();
        }

        // Finally, we calculate a map for the selector. The selector
        // will randomally pick an operationalSequence[x] and then
        // cycle through the array of URLs in that sequence.
        //
        // Pretty badly performing logic, but it's only done once at
        // startup, and it's easier than keeping another map.
        operationSequences = new int[seq.length][];
        for (int i = 0; i < seq.length; i++) {
            String[] s = seq[i].value();
            operationSequences[i] = new int[s.length];
            for (int j = 0; j < s.length; j++) {
                operationSequences[i][j] = -1;
                for (int k = 0; k < operationNames.length; k++) {
                    if (s[j].equals(operationNames[k])) {
                        operationSequences[i][j] = k;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Returns an array of the operation names used in this FlatMix.
     * @return operations The names of the operations
     */
    public String[] operations() {
        return operationNames;
    }

    /**
     * The clone operation for this object is a deep copy.
     *
     * @return a clone of this instance.
     * @see Cloneable
     */
    @Override
	public Object clone() {
        FlatSequenceMix clone = (FlatSequenceMix) super.clone();
        if (mix != null) {
            clone.mix = new double[mix.length];
            System.arraycopy(mix, 0, clone.mix, 0, mix.length);
        } else {
			clone.mix = null;
		}

        if (operationNames != null) {
            clone.operationNames = new String[operationNames.length];
            System.arraycopy(operationNames, 0, clone.operationNames,
                    0, operationNames.length);
        } else {
			clone.operationNames = null;
		}

        if (operationSequences != null) {
            clone.operationSequences = new int[operationSequences.length][];
            for (int i = 0; i < operationSequences.length; i++) {
                clone.operationSequences[i] = new int[operationSequences[i].length];
                System.arraycopy(operationSequences[i], 0, clone.operationSequences[i],
                        0, operationSequences[i].length);
            }
        } else {
			clone.operationSequences = null;
		}
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
    	// noop
    }

    /**
     * Validates the mix spec in the benchmark definitions to ensure all
     * the rows and columns are valid.
     * @throws com.sun.faban.driver.DefinitionException Found missing or
     *                                                  invalid row/column
     */
	public void validate() throws DefinitionException {
    	// noop
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
        FlatMix fm = new FlatMix();
        fm.operations = operations;
        fm.deviation = deviation;
        fm.mix = normalizedMix;
        return fm;
    }

    /**
     * Provides a string representation of this FlatSequenceMix.
     * @return The string representation
     * @see java.lang.Object#toString()
     */
    @Override
	public String toString() {
        StringBuilder buffer = new StringBuilder();
        buffer.append("FlatSequenceMix\n");
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
        return new Selector(random, mix, operationSequences);
    }

    /**
     * The selector implementation for the FlatSequenceMix.
     */
    public static class Selector extends Mix.Selector {

        private Random random;
        private double[] selectMix;
        private int curSequence;
        private int curIndex;
        private int[][] operationSequences;

        Selector(Random random, double[] mix, int[][] operationSequences) {
            this.operationSequences = operationSequences;
            this.random = random;
            selectMix = new double[mix.length];
            selectMix[0] = mix[0];
            for (int i = 1; i < selectMix.length; i++) {
                selectMix[i] = mix[i] + selectMix[i - 1];
            }
            // Resets the selector to starting position.
            reset();
        }

        /**
         * The select method selects the operation to run next.
         *
         * @return The operation index selected to run next
         */
		public int select() {
            if (curIndex == operationSequences[curSequence].length) {
                curIndex = 0;
                double val = random.drandom(0, 1);
                for (curSequence = 0; curSequence < selectMix.length; curSequence++) {
					if (val <= selectMix[curSequence]) {
						break;
					}
				}
            }
            return operationSequences[curSequence][curIndex++];
        }

        /**
         * Resets the selector's state to start at the first op,
         * if applicable.
         */
		public void reset() {
            curSequence = 0;
            curIndex = operationSequences[0].length;
        }
    }
}
