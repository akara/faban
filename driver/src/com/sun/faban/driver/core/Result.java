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
 * $Id: Result.java,v 1.3 2007/02/23 06:47:12 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.core;

/**
 * The implementation of the Result abstract class.
 *
 * @author Akara Sucharitakul
 */
public class Result extends com.sun.faban.driver.Result {

    private static Result instance;
    private Metrics m;

    public static Result getInstance() {
        return instance;
    }

    static void init(Metrics m) { // Only called by Metrics
        instance = new Result(m);
    }

    private Result(Metrics m) {
        this.m = m;
    }

    private int getOpsIdx(String opsName) {
        int idx = 0;
        for (; idx < m.txNames.length; idx++)
            if (m.txNames[idx].equals(opsName))
                break;
        return idx;
    }

    /**
     * Obtains the steady state time or cycles of the run, dependent on the
     * run control. Times are returned in seconds.
     *
     * @return The run's steady state
     */
    public int getSteadyState() {
        return m.stdyState;
    }

    /**
     * Obtains the scale of the run.
     *
     * @return The scale of the run.
     */
    public int getScale() {
        return RunInfo.getInstance().scale;
    }

    /**
     * Obtains the throughput metric the run has achieved. This is the same as
     * the operation rate or transaction rate.
     *
     * @return The metric
     */
    public double getMetric() {
        return m.metric;
    }

    /**
     * Obtains the number of operations of a certain type successfully
     * executed during steady state.
     *
     * @param opsName The name of the operation to query
     * @return The number of successful operations
     */
    public int getOpsCountSteady(String opsName) {
        return m.txCntStdy[getOpsIdx(opsName)];
    }

    /**
     * Obtains the number of operations of a certain type successfully executed
     * during the whole run.
     *
     * @param opsName The name of the operation to query
     * @return The number of successful operations
     */
    public int getOpsCountTotal(String opsName) {
        return m.txCntTotal[getOpsIdx(opsName)];
    }

    /**
     * Obtains the actual mix of the operation. The return value ranges
     * between 0 and 1. To obtain the percent mix, multiply the mix by 100.
     *
     * @param opsName The name of the operation to query
     * @return The mix ratio of the operation during steady state.
     */
    public double getOpsMix(String opsName) {
        return m.mixRatio[getOpsIdx(opsName)];
    }
}
