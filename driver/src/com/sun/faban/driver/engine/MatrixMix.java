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
import com.sun.faban.driver.Row;
import com.sun.faban.driver.util.Random;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.lang.annotation.Annotation;

/**
 * The implementation of the matrix mix annotation, provides all
 * utilities for matrix mix.
 *
 * @author Akara Sucharitakul
 */
public class MatrixMix extends Mix {

	private static final long serialVersionUID = 1L;

	double[][] mix;

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
        com.sun.faban.driver.MatrixMix matrixMix =
                (com.sun.faban.driver.MatrixMix) a;
        operations = BenchmarkDefinition.getOperations(driverClass,
                matrixMix.operations());
        Row[] lines = matrixMix.mix();
        mix = new double[lines.length][];
        for (int i = 0; i < lines.length; i++) {
			mix[i] = lines[i].value();
		}
        deviation = matrixMix.deviation();
        validate();
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
        MatrixMix clone = (MatrixMix) super.clone();
        clone.mix = new double[mix.length][];
        for (int i = 0; i < mix.length; i++) {
            clone.mix[i] = new double[mix[i].length];
            System.arraycopy(mix[i], 0, clone.mix[i], 0, mix[i].length);
        }
        return clone;
    }

    /**
     * Configures/overrides the mix from the driverConfig DOM node
     * read from the configuration file.
     *
     * @param driverConfigNode The driverConfig DOM node
     * @throws ConfigurationException If there is a configuration error
     */
	public void configure(Element driverConfigNode)
            throws ConfigurationException {
        /* The format is as follows:
        <operationMix>
            <name>operationA</name>
            <r>20</r><r>20</r><r>30</r><r>10</r><r>20</r>
        </operationMix>
        <operationMix>
            <name>operationB</name>
            <r>20</r><r>20</r><r>30</r><r>10</r><r>20</r>
        </operationMix>
        <operationMix>
            <name>operationC</name>
            <r>20</r><r>20</r><r>30</r><r>10</r><r>20</r>
        </operationMix>
        <operationMix>
            <name>operationD</name>
            <r>20</r><r>20</r><r>30</r><r>10</r><r>20</r>
        </operationMix>
        <operationMix>
            <name>operationE</name>
            <r>20</r><r>20</r><r>30</r><r>10</r><r>20</r>
        </operationMix>
        */
        HashMap<String, Integer> positionMap =
                new HashMap<String, Integer>(operations.length);
        HashMap<String, double[]> ratioMap =
                new HashMap<String, double[]>(operations.length);
        NodeList operationList = driverConfigNode.
                getElementsByTagNameNS(RunInfo.DRIVERURI, "operationMix");
        int size0 = operationList.getLength();

        // First we walk the DOM tree and put all ratios in a map with the name
        // as the key. We also keep track of the position of each name.
        for (int i = 0; i < size0; i++) {
            Element opMix = (Element) operationList.item(i);
            NodeList nl = opMix.getElementsByTagNameNS(
                                                    RunInfo.DRIVERURI, "name");
            if (nl.getLength() == 0) {
				throw new ConfigurationException("Element <name> not found " +
                        "inside <operationMix>.");
			}

            if (nl.getLength() > 1) {
				throw new ConfigurationException("Only one operation name " +
                        "allowed in each operation mix");
			}

            String name = nl.item(0).getFirstChild().getNodeValue();
            positionMap.put(name, i);
            nl = opMix.getElementsByTagNameNS(RunInfo.DRIVERURI, "r");
            int size1 = nl.getLength();
            if (size1 != operations.length) {
                throw new ConfigurationException("@MatrixMix for " + name +
                        " requires " + operations.length +
                        " ratio <r> elements, found " + size1 + '.');
            }
            double[] ratios = new double[size1];
            for (int j = 0; j < ratios.length; j++) {
				ratios[j] = Double.parseDouble(nl.item(j).
                        getFirstChild().getNodeValue());
			}
            ratioMap.put(name, ratios);
        }

        // If no configuration specified, we just don't override
        if (ratioMap.size() <= 0) {
			return;
		}

        // Then we have to extract the ratio map based on the defined name list
        double[][] ratios = new double[operations.length][];
        for (int i = 0; i < operations.length; i++) {
            ratios[i] = ratioMap.remove(operations[i].name);
            if (ratios[i] == null) {
				throw new ConfigurationException("Configured ratio for " +
                        "operation " + operations[i].name + " not found");
			}
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
            throw new ConfigurationException("Invalid operation name(s) in " +
                    "operationMix configuration: " + msg + '.');
        }

        // But the ratios are still sorted by the configured list which
        // may not be in the same order as the defined list. So we need to
        // sort through each row.
        for (int i = 0; i < ratios.length; i++) {
			for (int j = 0; j < operations.length; j++) {
                int position = positionMap.get(operations[j].name);
                mix[i][j] = ratios[i][position];
            }
		}
    }

    /**
     * Validates the mix spec in the benchmark definitions to ensure all
     * the rows and columns are valid.
     * @throws com.sun.faban.driver.DefinitionException Found missing or
     *                                                  invalid row/column
     */
    public void validate() throws DefinitionException {
        if (mix.length != operations.length) {
            String msg = "Mix matrix must be " + operations.length + " x " +
                    operations.length + " (#ops x #ops) in size!\nFound " +
                    mix.length + " x " + mix[0].length + ".";
            getLogger().severe(msg);
            DefinitionException e = new DefinitionException(msg);
            getLogger().throwing(className, "validate", e);
            throw e;
        }
        for (int i = 0; i < operations.length; i++) {
			if (mix[i].length != operations.length) {
                String msg = "All rows in mix matrix must be " +
                        operations.length + " (#ops) in size.\nFound row " +
                        i + " to be of size " + mix[i].length + ".";
                getLogger().severe(msg);
                DefinitionException e = new DefinitionException(msg);
                getLogger().throwing(className, "validate", e);
                throw e;
            }
		}
    }

    /**
     * Normalizes the mix so each row will have a sum of 1.0d. This is
     * important as we do not require the spec input to sum to this
     * amount but the selector (doMenu) will base the random number
     * generator to 1.
     */
	public void normalize() {

        for (int i = 0; i < mix.length; i++) {
            double rowTotal = 0;
            for (int j = 0; j < mix.length; j++) {
				rowTotal += mix[i][j];
			}
            for (int j = 0; j < mix.length; j++) {
				mix[i][j] /= rowTotal;
			}
        }
        // The following is called only to dump out the resulting mix info.
        // If the log level is not fine enough, we just do not calculate.
        // It is not necessary in the logic.
        if (getLogger().isLoggable(Level.FINER)) {
			flatMix();
		}
    }

    /**
     * Calculates flat mix ratios from a matrix mix. The matrix must be
     * checked and adjusted before calculating the flat mix or otherwise
     * runtime exceptions or divergence (infinite loops) may occur.
     * @return the flat mix equivalent of this mix
     */
	public FlatMix flatMix() {

         getLogger().finer("flatMix - before\n" + toString());

        /* This method keeps increasing the power of the matrix until
         * each column becomes stable - the values are equal all equal
         * in each column. Then we take the values in these columns and
         * put it in a 1xOps matrix (1-dimensional array) and return it.
         *
         * The precision is the number of decimal digits used to compare
         * and determine that the value is stable. The value of 4 means
         * the values are the same up to the 4th digit after the decimal point.
         * Percentage-wise we can expect the variation be less than 0.01%
         * (0.0001 - four digits behind the point)
         */
        int precision = 4;

        // For purposes of memory, we use 2 intermediate result (iResults)
        // matrixes and switch between the two.
        double[][][] iResults = new double[2][mix.length][mix.length];
        double[] results = new double[mix.length];
        double multiplier = Math.pow(10d, precision);
        double threshold = 1d/multiplier;

        // First we do a power of 2 to populate an intermediate result (iResult)
        for (int i = 0; i < mix.length; i++) {
			for (int j = 0; j < mix.length; j++) {
				for (int k = 0; k < mix.length; k++) {
					iResults[0][i][j] += mix[i][k] * mix[k][j];
				}
			}
		}

        // We just don't think it has converged yet, start power 3 without check
        int power = 3;

        incrementPower:
        for (; power < 10000; power++) { // Limit to 9999 iters
            int srcIdx = (power - 1) & 1;
            int resIdx = power & 1; // Just need to know the idx 0 or 1

            // Do the next multiplication
            for (int i = 0; i < mix.length; i++) {
				for (int j = 0; j < mix.length; j++) {
                    iResults[resIdx][i][j] = 0d;
                    for (int k = 0; k < mix.length; k++) {
						iResults[resIdx][i][j] +=
                                iResults[srcIdx][i][k] * mix[k][j];
					}
                }
			}

            // Now we check the results
            for (int j = 0; j < mix.length; j++) {// j for column
                // we know we're talking about a low number (4 digits or so),
                // int should be more than adequate
                int comparator = -1;
                for (int i = 0; i < mix.length - 1; i++) { // i for row
                    comparator = -1; // Uninitialize comparator every row
                    for (int k = i + 1; k < mix.length; k++) {
                        // We first do the inexact comparison, low cost
                        double diff = Math.abs(iResults[resIdx][i][j] -
                                               iResults[resIdx][k][j]);
                        if (diff > threshold) {
							continue incrementPower;
						}

                        // If we past that, we do the exact version
                        if (comparator == -1) {
							comparator = (int) Math.round(
                                    iResults[resIdx][i][j] * multiplier);
						}
                        int comparatee = (int) Math.round(
                                iResults[resIdx][k][j] * multiplier);
                        if (comparator != comparatee) {
							continue incrementPower;
						}
                    }
                }
                // Once we past all checks for a row, we use the final
                // comparator which is rounded to <precision> digits
                results[j] = comparator * threshold;
            }
            getLogger().finer("Obtained stable mix at matrix power " + power +
                              '.');
            break;
        }
        if (power == 10000) {
            throw new ArithmeticException("Mix probability did not converge " +
                    "after 9999 iterations, please check mix for validity." +
                    " Aborting.");
        }
        FlatMix flatMix = new FlatMix();
        flatMix.operations = operations;
        flatMix.deviation = deviation;
        flatMix.mix = results;

        getLogger().finer("flatMix - after\n" + flatMix.toString());

        return flatMix;
    }

    /**
     * Provides a string representation of this MatrixMix.
     * @return The string representation
     * @see java.lang.Object#toString()
     */
    @Override
	public String toString() {
        StringBuffer buffer = new StringBuffer();
        buffer.append("MatrixMix\n");
        buffer.append("operations: ");
        buffer.append(operations[0].name);
        for (int i = 1; i < operations.length; i++) {
            buffer.append(", ");
            buffer.append(operations[i].name);
        }
        buffer.append("\nmix:\n");
        for (int i = 0; i < mix.length; i++) {
            buffer.append(mix[i][0]);
            for (int j = 1; j < mix[i].length; j++) {
                buffer.append(", ");
                buffer.append(mix[i][j]);
            }
            buffer.append('\n');
        }
        buffer.append("deviation :");
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
        getLogger().finest("Get selector: " + toString());
        Selector s = new Selector(random, mix);
        getLogger().finest(s.toString());
        return s;
    }

    /**
     * Selector implementation for the matrix mix.
     */
    public static class Selector extends Mix.Selector {
        private int op = -1;
        private Random random;
        private double[][] selectMix;

        Selector(Random random, double[][] mix) {
            this.random = random;
            selectMix = new double[mix.length][mix.length];

            for (int i = 0; i < mix.length; i++) {
                selectMix[i][0] = mix[i][0];
                for (int j = 1; j < selectMix.length; j++) {
                    selectMix[i][j] = mix[i][j] + selectMix[i][j - 1];
                }
            }
        }

        /**
         * The select method selects the operation to run next.
         *
         * @return The operation index selected to run next
         */
		public int select() {
            if (op == -1) { // first selection
                op = 0;
            } else { // Any subsequent selection
                double val = random.drandom(0, 1);
                int i;
                for (i = 0; i < selectMix.length; i++) {
					if (val <= selectMix[op][i]) {
						break;
					}
				}
                op = i;
            }
            return op;
        }

        /**
         * Resets the selector's state to start at the first op,
         * if applicable.
         */
		public void reset() {
            op = -1;
        }

        /**
         * Provides a string representation of this selector.
         * @return The string representation
         * @see java.lang.Object#toString()
         */
        @Override
		public String toString() {
            StringBuffer buffer = new StringBuffer();
            buffer.append("MatrixMix.Selector\n");
            for (int i = 0; i < selectMix.length; i++) {
                buffer.append(selectMix[i][0]);
                for (int j = 1; j < selectMix[i].length; j++) {
                    buffer.append(", ");
                    buffer.append(selectMix[i][j]);
                }
                buffer.append('\n');
            }
            buffer.append('\n');

            return buffer.toString();
        }
    }
}
