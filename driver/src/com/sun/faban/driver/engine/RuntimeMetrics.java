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

import com.sun.faban.driver.util.PairwiseAggregator;
import java.io.Serializable;
import java.util.Formatter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The runtime metrics is used for communicating runtime stats from
 * the agents to the master.
 *
 * @author akara
 */
public class RuntimeMetrics implements Serializable, 
        PairwiseAggregator.Aggregable<RuntimeMetrics> {

    private static final long serialVersionUID = 33009l;

    /** Index for current thruput label. */
    public static final int C_THRUPUT = 0;

    /** Index for overall thruput label. */
    public static final int O_THRUPUT = 1;

    /** Index for current error rate label. */
    public static final int C_ERRORS = 2;

    /** Index for current response time label. */
    public static final int C_RESP = 3;

    /** Index for overall response time label. */
    public static final int O_RESP = 4;

    /** Index for current standard deviation label. */
    public static final int C_SD = 5;

    /** Index for overall standard deviation label. */
    public static final int O_SD = 6;

    /** Index for current 90th% response time label. */
    public static final int C_RESP90 = 7;

    /** Index for overall 90th% response time label. */
    public static final int O_RESP90 = 8;

    /** The output labels for the runtime metrics. */
    public static final String[] LABELS = { "CThru", "OThru", "CErr",
                                            "CResp", "OResp", "CSD",
                                            "OSD", "C90%Resp", "O90%Resp"};


    int sequence = 0; // The sequence number of this runtime stats.

    /**
     * The timestamp of this RuntimeMetrics in ms
     * from the start of rampup.
     */
    int timestamp;

    int driverType; // The type of this driver.

    int txTypes; // The tx types for the current metrics.

    /** Size of the fine response time buckets. */
    protected long fineRespBucketSize;  // Size of the fine and coarse

    /** Size of the coarse response time buckets. */
    protected long coarseRespBucketSize; // response time buckets, in ns.

    /** Max fine response time. */
    protected long fineRespHistMax;

    /** Max coarse response time. */
    protected long coarseRespHistMax;

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

    /** Sum of response times during steady state. */
    protected double[] respSumStdy;

    /** Sum of response times total. */
    protected double[] respSumTotal;

    /** Sum of the overflow response time. */
    protected double[] hiRespSumStdy;

    /** The sum squares of the deviations in steady state. */
    protected double[] sumSquaresStdy;

    /** The sum squares of the deviations total. */
    protected double[] sumSquaresTotal;

    /** Response time histogram. */
    protected int[][] respHist;

    /**
     * Only classes in this package can instantiate the RuntimeMetrics.
     */
    RuntimeMetrics() {
    }

    /**
     * Copies the necessary members of Metrics into this RuntimeMetrics.
     * @param m The metrics to copy
     */
    public void copy(Metrics m) {
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
            hiRespSumStdy = new double[txTypes];
            sumSquaresStdy = new double[txTypes];
            sumSquaresTotal = new double [txTypes];
            respHist = new int[txTypes][m.respHist[0].length];
        }

        for (int i = 0; i < txTypes; i++) {
            // Add the sum squares before adding the count and response sum.
            // The values of count and sum have to be unchanged at this point.
            sumSquaresStdy[i] = m.sumSquaresStdy[i];
            sumSquaresTotal[i] = m.sumSquaresTotal[i];
            txCntStdy[i] = m.txCntStdy[i];
            txCntTotal[i] = m.txCntTotal[i];
            errCntStdy[i] = m.errCntStdy[i];
            errCntTotal[i] = m.errCntTotal[i];
            respSumStdy[i] = m.respSumStdy[i];
            respSumTotal[i] = m.respSumTotal[i];
            hiRespSumStdy[i] = m.hiRespSumStdy[i];
            for (int j = 0; j < m.respHist[i].length; j++)
                respHist[i][j] = m.respHist[i][j];
        }
    }

    /**
     * Adds a metrics to this RuntimeMetrics.
     * @param m The metrics to add
     */
    public void add(Metrics m) {

        for (int i = 0; i < txTypes; i++) {
            // Add the sum squares before adding the count and response sum.
            // The values of count and sum have to be unchanged at this point.
            sumSquaresStdy[i] = Metrics.addSumSquare(
                    sumSquaresStdy[i], txCntStdy[i], respSumStdy[i],
                    m.sumSquaresStdy[i], m.txCntStdy[i], m.respSumStdy[i]);
            sumSquaresTotal[i] = Metrics.addSumSquare(
                    sumSquaresTotal[i], txCntTotal[i], respSumTotal[i],
                    m.sumSquaresTotal[i], m.txCntTotal[i], m.respSumTotal[i]);
            txCntStdy[i] += m.txCntStdy[i];
            txCntTotal[i] += m.txCntTotal[i];
            errCntStdy[i] += m.errCntStdy[i];
            errCntTotal[i] += m.errCntTotal[i];
            respSumStdy[i] += m.respSumStdy[i];
            respSumTotal[i] += m.respSumTotal[i];
            hiRespSumStdy[i] += m.hiRespSumStdy[i];
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
            // Add the sum squares before adding the count and response sum.
            // The values of count and sum have to be unchanged at this point.
            sumSquaresStdy[i] = Metrics.addSumSquare(
                    sumSquaresStdy[i], txCntStdy[i], respSumStdy[i],
                    m.sumSquaresStdy[i], m.txCntStdy[i], m.respSumStdy[i]);
            sumSquaresTotal[i] = Metrics.addSumSquare(
                    sumSquaresTotal[i], txCntTotal[i], respSumTotal[i],
                    m.sumSquaresTotal[i], m.txCntTotal[i], m.respSumTotal[i]);
            txCntStdy[i] += m.txCntStdy[i];
            txCntTotal[i] += m.txCntTotal[i];
            errCntStdy[i] += m.errCntStdy[i];
            errCntTotal[i] += m.errCntTotal[i];
            respSumStdy[i] += m.respSumStdy[i];
            respSumTotal[i] += m.respSumTotal[i];
            hiRespSumStdy[i] += m.hiRespSumStdy[i];
            for (int j = 0; j < m.respHist[i].length; j++)
                respHist[i][j] += m.respHist[i][j];
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
        b.append("\nhiRespSumStdy:").append(hiRespSumStdy[0]);
        for (int i = 1; i < txTypes; i++) {
            b.append('/').append(hiRespSumStdy[i]);
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

        Logger logger = Logger.getLogger(RuntimeMetrics.class.getName());
        double[] ckSD = null;
        Level crosscheck = Level.FINE;
        if (logger.isLoggable(crosscheck)) {
            ckSD = new double[txTypes];
            for (int i = 0; i < txTypes; i++)
                ckSD[i] = Double.NaN;

        }

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

        double[][] s = new double[LABELS.length][txTypes];

        // Initialize results to NaN;
        for (int i = 0; i < s.length; i++) {
            for (int j = 0; j < s[i].length; j++) {
                s[i][j] = Double.NaN;
            }
        }

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

            if (logger.isLoggable(Level.FINER)) {
                double p2 = precision * precision;
                logger.finer("sumSquaresStdy[" + i + "] = " +
                        sumSquaresStdy[i] / p2 + ", txCntStdy[" + i + "] = " +
                        txCntStdy[i] + ", respSumStdy[" + i + "] = " +
                        respSumStdy[i] / precision);
                logger.finer("sumSquaresTotal[" + i + "] = " +
                        sumSquaresTotal[i] / p2 + ", txCntTotal[" + i + "] = " +
                        txCntTotal[i] + ", respSumTotal[" + i + "] = " +
                        respSumTotal[i] / precision);
            }

            // Current standard deviation based on Chan's paper
            if (txCntTotal[i] > prev.txCntTotal[i]) {
                double variance = Metrics.subtractSumSquare(sumSquaresTotal[i],
                        txCntTotal[i], respSumTotal[i], prev.sumSquaresTotal[i],
                        prev.txCntTotal[i], prev.respSumTotal[i]);
                if (variance == 0d) {
                    s[C_SD][i] = 0d;
                } else if (!Double.isNaN(variance)) {
                    variance /= txCntTotal[i] - prev.txCntTotal[i];
                    s[C_SD][i] = Math.sqrt(variance) / precision;
                }
            }
            if (txCntStdy[i] > 0) {
                // Overall avg response time
                s[O_RESP][i] = respSumStdy[i] / (txCntStdy[i] * precision);
                // Overall standard deviation based on Chan's paper
                s[O_SD][i] = Math.sqrt(sumSquaresStdy[i] / txCntStdy[i]) /
                               precision;

                // Current 90th% response time (last n secs)
                int sumtx, cnt90, j;
                if (txCntStdy[i] > prev.txCntStdy[i]) {
                    sumtx = 0;
                    cnt90 = (int) ((txCntStdy[i] - prev.txCntStdy[i]) * .90d);
                    j = 0;
                    for (; j < respHist[i].length; j++) {
                        sumtx += respHist[i][j] - prev.respHist[i][j];
                        if (sumtx >= cnt90) {	/* 90% of tx. got */
                            break;
                        }
                    }
                    // We report the base of the next bucket.
                    ++j;
                    s[C_RESP90][i] = getBucketValue(j) / precision;
                }

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

                if (logger.isLoggable(crosscheck)) {
                    // Overall standard deviation check from histogram
                    double sumDev2 = 0d;
                    for (j = 0; j < respHist[i].length; j++) {
                        int frequency = respHist[i][j] - prev.respHist[i][j];
                        if (frequency == 0) {
                            continue;
                        }
                        long bucketRep = getBucketRepValue(j);
                        double dev;
                        if (bucketRep == Long.MIN_VALUE) {
                            dev = (hiRespSumStdy[i] - prev.hiRespSumStdy[i]) /
                                    frequency;
                        } else {
                            dev = bucketRep;
                        }
                        dev = dev / precision - s[C_RESP][i];
                        sumDev2 += dev * dev * frequency;
                    }
                    ckSD[i] = Math.sqrt(sumDev2 / nTx);
                }
            }
        }

        // Log the crosscheck if applicable.
        if (logger.isLoggable(crosscheck) && !Double.isNaN(ckSD[0])) {
            StringBuilder b = new StringBuilder();
            Formatter formatter = new Formatter(b);
            b.append("Crosscheck estimated CSD from histogram: ");
            formatter.format("%.03f", ckSD[0]);
            for (int j = 1; j < ckSD.length; j++) {
                formatter.format("/%.03f", ckSD[j]);
            }
            logger.log(crosscheck, b.toString());
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
            resp = coarseRespHistMax + coarseRespBucketSize;
        }
        return resp;
    }

    private long getBucketRepValue(int bucketId) {
        long resp;
        if (bucketId < Metrics.FINE_RESPBUCKETS) {
            resp = bucketId * fineRespBucketSize + (fineRespBucketSize/2);
        } else if (bucketId < Metrics.RESPBUCKETS) {
            resp = (bucketId - Metrics.FINE_RESPBUCKETS) *
                    coarseRespBucketSize + fineRespHistMax +
                    (coarseRespBucketSize/2);
        } else {
            resp = Long.MIN_VALUE;
        }
        return resp;
    }
}