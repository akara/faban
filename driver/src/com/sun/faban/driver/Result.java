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
 * $Id: Result.java,v 1.4 2007/09/05 23:32:28 noahcampbell Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver;

/**
 * The driver representation of the results, mainly used for fetching data
 * from the main metrics for reporting in CustomMetrics.<br>
 *
 * Note that operations can and will be added as needed to query different
 * aspects of the results.
 *
 * @author Akara Sucharitakul
 */
public abstract class Result {

    /**
     * @return instance
     */
    public static Result getInstance() {
        return com.sun.faban.driver.core.Result.getInstance();
    }

    /**
     * Obtains the steady state time or cycles of the run, dependent on the
     * run control. Times are returned in seconds.
     * @return The run's steady state
     */
    public abstract int getSteadyState();

    /**
     * Obtains the scale of the run.
     * @return The scale of the run.
     */
    public abstract int getScale();

    /**
     * Obtains the throughput metric the run has achieved. This is the same as
     * the operation rate or transaction rate.
     * @return The metric
     */
    public abstract double getMetric();

    /**
     * Obtains the defined operation names, in sequence.
     * @return The operation names.
     */
    public abstract String[] getOpsNames();

    /**
     * Obtains the number of operations of a certain type successfully
     * executed during steady state.
     *
     * @param opsName The name of the operation to query
     * @return The number of successful operations
     */
    public abstract int getOpsCountSteady(String opsName);

    /**
     * Obtains the number of operations of each type successfully
     * executed during steady state. The index into the array returned
     * corresponds to the index of getOpsNames().
     * @return The number of successful operations for each type
     */
    public abstract int[] getOpsCountSteady();


    /**
     * Obtains the number of operations of a certain type successfully executed
     * during the whole run.
     *
     * @param opsName The name of the operation to query
     * @return The number of successful operations
     */
    public abstract int getOpsCountTotal(String opsName);

    /**
     * Obtains the number of operatioins of each type successfully executed
     * during the whole run. The index into the array returned corresponds
     * to the index of getOpsNames().
     * @return The number of successful operations for each type
     */
    public abstract int[] getOpsCountTotal();

    /**
     * Obtains the actual mix of the operation. The return value ranges
     * between 0 and 1. To obtain the percent mix, multiply the mix by 100.
     * @param opsName The name of the operation to query
     * @return The mix ratio of the operation during steady state.
     */
    public abstract double getOpsMix(String opsName);

    /**
     * Obtains the actual mix of each operation. The return values range
     * between 0 and 1. To obtain the percent mix, multiply the mix by 100.
     * The index into the array returned corresponds to the index of
     * getOpsNames().
     * @return The mix ratio of all operations during steady state.
     */ 
    public abstract double[] getOpsMix();
}
