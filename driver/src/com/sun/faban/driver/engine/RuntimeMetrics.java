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
 * $Id: RuntimeMetrics.java,v 1.1 2009/04/01 19:11:10 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.engine;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * The runtime metrics is used for communicating runtime stats from
 * the agents to the master.
 *
 * @author akara
 */
public class RuntimeMetrics implements Serializable {

    private static final long serialVersionUID = 33009l;

    public static final int C_THRUPUT = 0;
    public static final int O_THRUPUT = 1;
    public static final int C_ERRORS = 2;
    public static final int C_RESP = 3;
    public static final int O_RESP = 4;
    public static final int C_RESP90 = 5;
    public static final int O_RESP90 = 6;

    public static final String[] LABELS = { "CThru", "OThru", "CErr", "CResp",
                                            "OResp", "C90%Resp", "O90%Resp" };


    int sequence = 0; // The sequence number of this runtime stats.

    /**
     * The timestamp of this RuntimeMetrics in ms
     * from the start of rampup.
     */
    int timestamp;

    int driverType; // The type of this driver.

    int txTypes; // The tx types for the current metrics.

    protected long fineRespBucketSize;  // Size of the fine and coarse
    protected long coarseRespBucketSize; // response time buckets, in ns.
    protected long fineRespHistMax; // Max fine response time
    protected long coarseRespHistMax; // Max coarse response time

	/**
     * Number of successful transactions during steady state.
     * This is used for final reporting and in-flight reporting of averages.
     */
    protected int[] txCntStdy;

    /**
     * Number of successful transactions total.
     * This is used for in-flight reporting only.
     */
    protected int[] txCntTotal;

    /**
     * Number of failed transactions during steady state.
     * This is used for final reporting and in-flight reporting of averages.
     */
    protected int[] errCntStdy;

    /**
     * Number of failed transactions total.
     * This is used for in-flight reporting only.
     */
    protected int[] errCntTotal;

    /**
     * Sum of response times during steady state.
     * This is used for final reporting and in-flight reporting of averages.
     */
    protected double[] respSumStdy;

    /**
     * Sum of response times total.
     * This is used for in-flight reporting only.
     */
    protected double[] respSumTotal;

    /** Response time histogram. */
    protected int[][] respHist;

    /**
     * Only classes in this package can instantiate the RuntimeMetrics.
     */
    RuntimeMetrics() {
    }

    /**
     * Adds a metrics to this RuntimeMetrics.
     * @param m The metrics to add
     */
    public void add(Metrics m) {

        if (txCntTotal == null) { // Needs initialization
            driverType = m.driverType;
            txTypes = m.txCntTotal.length;
            fineRespBucketSize = m.fineRespBucketSize;
            coarseRespBucketSize = m.coarseRespBucketSize;
            fineRespHistMax = m.fineRespHistMax;
            coarseRespHistMax = m.coarseRespHistMax;

            txCntStdy = new int[txTypes];
            txCntTotal = new int[txTypes];
            errCntStdy = new int[txTypes];
            errCntTotal = new int[txTypes];
            respSumStdy = new double[txTypes];
            respSumTotal = new double[txTypes];
            respHist = new int[txTypes][m.respHist[0].length];
        }

        for (int i = 0; i < txTypes; i++) {
            txCntStdy[i] += m.txCntStdy[i];
            txCntTotal[i] += m.txCntTotal[i];
            errCntStdy[i] += m.errCntStdy[i];
            errCntTotal[i] += m.errCntTotal[i];
            respSumStdy[i] += m.respSumStdy[i];
            respSumTotal[i] += m.respSumTotal[i];
            for (int j = 0; j < m.respHist[i].length; j++)
                respHist[i][j] += m.respHist[i][j];
        }
    }

    /**
     * Adds a metrics to this RuntimeMetrics.
     * @param m The metrics to add
     */
    public void add(RuntimeMetrics m) {
        if (m.timestamp > timestamp)
            timestamp = m.timestamp;
        
        for (int i = 0; i < txTypes; i++) {
            txCntStdy[i] += m.txCntStdy[i];
            txCntTotal[i] += m.txCntTotal[i];
            errCntStdy[i] += m.errCntStdy[i];
            errCntTotal[i] += m.errCntTotal[i];
            respSumStdy[i] += m.respSumStdy[i];
            respSumTotal[i] += m.respSumTotal[i];
            for (int j = 0; j < m.respHist[i].length; j++)
                respHist[i][j] += m.respHist[i][j];
        }
    }

    /**
     * Resets this RuntimeMetrics for another round of runtime stats collection.
     */
    public void reset() {
        if (txCntTotal != null) {
            for (int i = 0; i < txTypes; i++) {
            txCntStdy[i] = 0;
            txCntTotal[i] = 0;
            errCntStdy[i] = 0;
            errCntTotal[i] = 0;
            respSumStdy[i] = 0;
            respSumTotal[i] = 0;
            for (int j = 0; j < respHist[i].length; j++)
                respHist[i][j] = 0;
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("seq:").append(sequence);
        b.append("\ntimestamp:").append(timestamp);
        b.append("\ndrivertype:").append(driverType);
        b.append("\ntxCntStdy:").append(txCntStdy[0]);
        for (int i = 1; i < txTypes; i++) {
            b.append('/').append(txCntStdy[i]);
        }
        b.append("\ntxCntTotal:").append(txCntTotal[0]);
        for (int i = 1; i < txTypes; i++) {
            b.append('/').append(txCntTotal[i]);
        }
        b.append("\nerrCntStdy:").append(errCntStdy[0]);
        for (int i = 1; i < txTypes; i++) {
            b.append('/').append(errCntStdy[i]);
        }
        b.append("\nerrCntTotal:").append(errCntTotal[0]);
        for (int i = 1; i < txTypes; i++) {
            b.append('/').append(errCntTotal[i]);
        }
        b.append("\nrespSumStdy:").append(respSumStdy[0]);
        for (int i = 1; i < txTypes; i++) {
            b.append('/').append(respSumStdy[i]);
        }
        b.append("\nrespSumTotal:").append(respSumTotal[0]);
        for (int i = 1; i < txTypes; i++) {
            b.append('/').append(respSumTotal[i]);
        }
        b.append("\nrespHist:").append('[').append(respHist[0][0]);

        for (int j = 1; j < respHist[0].length; j++) {
            b.append(',').append(respHist[0][j]);            
        }
        b.append(']');
        for (int i = 1; i < respHist.length; i++) {
            b.append("/[").append(respHist[i][0]);
            for (int j = 1; j < respHist[i].length; j++) {
                b.append(',').append(respHist[i][j]);
            }
            b.append(']');
        }
        return b.toString();
    }

    /**
     * Synthesizes and provides the results from this runtime stats. The 
     * results are first indexed by the stat type, then by the tx type.
     * @param runInfo The RunInfo for this benchmark run
     * @param prev The previous RuntimeMetrics
     * @return The synthesized results
     */
    public double[][] getResults(RunInfo runInfo, RuntimeMetrics prev) {

        BenchmarkDefinition.Driver driver = runInfo.driverConfigs[driverType];
        double precision = driver.responseTimeUnit.toNanos(1l);

        int stdyStart = runInfo.rampUp * 1000;
        int stdyEnd = (runInfo.rampUp + runInfo.stdyState) * 1000;

        int timeElapsed;
        if (timestamp > stdyEnd)
            timeElapsed = runInfo.stdyState * 1000;
        else if (timestamp > stdyStart)
            timeElapsed = timestamp - stdyStart;
        else
            timeElapsed = Integer.MIN_VALUE;

        double[][] s = new double[7][txTypes];

        int timeDiff = timestamp - prev.timestamp;

        for (int i = 0; i < txTypes; i++) {
            int nTx = txCntTotal[i] - prev.txCntTotal[i];
            // Current Thruput (last n secs)
            s[C_THRUPUT][i] = nTx * 1000d / timeDiff;
            // Overall thruput
            if (timeElapsed > 0) {
                s[O_THRUPUT][i] = txCntStdy[i] * 1000d / timeElapsed;
            }
            // Current error rate (last n secs)
            s[C_ERRORS][i] = (errCntTotal[i] - prev.errCntTotal[i]) *
                    1000d / timeDiff;
            // Current avg response time (last n secs)
            if (respSumTotal[i] > prev.respSumTotal[i]) {
                s[C_RESP][i] = (respSumTotal[i] - prev.respSumTotal[i]) /
                        (nTx * precision);
            }
            // Overall avg response time
            if (txCntStdy[i] > 0) {
                s[O_RESP][i] = respSumStdy[i] / (txCntStdy[i] * precision);
                // Current 90th% response time (last n secs)
                int sumtx = 0;
                int cnt90 = (int) ((txCntStdy[i] - prev.txCntStdy[i]) * .90d);
                int j = 0;
                for (; j < respHist[i].length; j++) {
                    sumtx += respHist[i][j] - prev.respHist[i][j];
                    if (sumtx >= cnt90) {	/* 90% of tx. got */
                        break;
                    }
                }
                // We report the base of the next bucket.
                ++j;
                s[C_RESP90][i] = getBucketValue(j) / precision;

                // Overall 90th% response time
                sumtx = 0;
                cnt90 = (int) (txCntStdy[i] * .90d);
                j = 0;
                for (; j < respHist[i].length; j++) {
                    sumtx += respHist[i][j];
                    if (sumtx >= cnt90) {	/* 90% of tx. got */
                        break;
                    }
                }
                // We report the base of the next bucket.
                ++j;
                s[O_RESP90][i] = getBucketValue(j) / precision;
            }
        }
        return s;
    }

    private long getBucketValue(int bucketId) {
        long resp;
        if (bucketId < Metrics.FINE_RESPBUCKETS) {
            resp = bucketId * fineRespBucketSize;
        } else if (bucketId < Metrics.RESPBUCKETS) {
            resp = (bucketId - Metrics.FINE_RESPBUCKETS) *
                    coarseRespBucketSize + fineRespHistMax;
        } else {
            resp = coarseRespHistMax;
        }
        return resp;
    }
}