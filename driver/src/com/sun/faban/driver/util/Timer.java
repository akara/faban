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
package com.sun.faban.driver.util;

import com.sun.faban.driver.FatalException;
import com.sun.faban.driver.util.timermeter.TimerMeter;

import java.io.Serializable;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Timer for all benchmark runs.
 */
public class Timer implements Serializable {

    /** The millisec epoch time of this benchmark. */
	long epochMillis;

    /** The nanosec epoch time of this benchmark. */
    transient long epochNanos; // This has no meaning on a different system.

    transient long diffms; // The epoch difference, millisec part
    transient int diffns; // The epoch difference, nanosec part

    private static transient Logger logger =
            Logger.getLogger(Timer.class.getName());
    private long compensation = 5000000l;  // Some pretty good starting numbers
    private double deviation = 5000000d; // for both fields.
    private Boolean debug = null;

    /**
     * Default Constructor which saves the current time
     * as epochMillis and epochNanos (the start of the benchmark).
     * The resolution is unknown. Note that this is only constructed on the
     * master so the initial values before adjustments pertain to the master
     * alone.
     */
	public Timer() {

        // This is just a fake setting of the epochNanos. The call
        // into System.nanoTime() ensures initialization of the nano timer
        // and prevents underflow from calibration.
        epochNanos = System.nanoTime();

        // Set the benchmark epoch 10ms ahead of the actual current time
        // also prevents underflow in case the nano timer is just initialized.
        // The value of the nano timer may be very close to Long.MIN_VALUE.
        epochMillis = System.currentTimeMillis() + 10l;
        epochNanos = calibrateNanos(epochMillis);

        logger.fine("Timer: baseTime ms: " + epochMillis +
                         ", ns: " + epochNanos);
	}

    /**
     * Calibrates the difference of the nanosec timer from the millisec
     * timer using 100 iterations. This is probably the most accurate
     * way to establish the relationship.
     * @param baseMillis the base millisec to find the base nanosec for
     * @return The base nanosec corresponding to the base millisec.
     */
    private long calibrateNanos(long baseMillis) {

        if (isDebug())
            return estimateNanos(baseMillis);

        int iterations = 100;
        int limit = 1000;  // Limit the number of total loops...
        long[] diffms = new long[iterations];
        int[] diffns = new int[iterations];
        int[] clockStep = new int[iterations];
        int msStep = Integer.MAX_VALUE;
        System.gc(); // We don't want gc during calibration. So we try to gc
                     // now, on best effort.

        // Run the calibration loop and collect data.
        for (int i = 0, tooLong = 0; i < iterations;) {
            if (tooLong > limit) // prevent infinite loops.
                throw new FatalException("Cannot establish clock offset " +
                                         "(ms->ns), retries " + i + "," +
                                         tooLong);

            long nanos;
            long millis;
            long millisBefore = System.currentTimeMillis();
            do {
               nanos = System.nanoTime();
               millis = System.currentTimeMillis();
            } while (millis == millisBefore);

            // Now we're on the edge of a new ms value
            // Find the ms clock step for this system
            // by iterating down the min step value.
            clockStep[i] = (int) (millis - millisBefore);
            if (clockStep[i] < msStep)
                msStep = clockStep[i];

            // If we discover any step bigger than the best recorded step,
            // ignore the iteration.
            if (msStep != Integer.MAX_VALUE && clockStep[i] > msStep) {
                ++tooLong;
                continue;
            }

            diffms[i] = millis - nanos / 1000000l;
            diffns[i] = (int) (nanos % 1000000l);
            logger.finer("iter: " + i + ", millis: " + millis +
                    ", nanos: " + nanos + ", diffms: " + diffms[i] +
                    ", diffns: " + diffns[i] + ", stepms: " + clockStep[i]);
            tooLong = 0; // Reset retry counters on success.
            ++i;
        }
        logger.fine("System.currentTimeMillis() granularity is " + msStep +
                    "ms");

        // There might still be some records left at the beginning before
        // that have the step > minstep. This happens before the minstep
        // is established. We must not use these records. Count them and
        // report. If more than 25% are bad, don't continue.
        int badRecords = 0;
        for (int i = 0; i < clockStep.length; i++) {
            if (clockStep[i] > msStep) {
                logger.finer("Rejected bad record " + i + 
                             "Edge mis-detection. Clock step of " +
                             clockStep[i] + "ms higher than granularity.");
                ++badRecords;
            }
        }
        if (badRecords > iterations / 4) {
            throw new FatalException("Cannot establish clock offset " +
                    "(ms->ns), edge mis-detections beyond threshold - " +
                    badRecords + " out of " + iterations +
                    ". Perhaps system is too busy.");
        } else {
            logger.fine("Rejected " + badRecords + " bad records.");
        }
        int goodRecords = iterations - badRecords;

        // Find the granularity of the nanosec results.
        int granularity = 6;
        for (int i = 0; i < diffns.length; i++) {
            int ns = diffns[i];
            int mod = 10;
            int g = 0;
            for (; g < 6; g++) {
                if (ns % mod != 0)
                    break;
                mod *= 10;
            }
            if (g < granularity)
                granularity = g;
            if (granularity == 0)
                break;
        }
        logger.fine("Nanosec timer granularity: 1e" + granularity);

        // Find the max ms difference.
        long maxDiffMs = Long.MIN_VALUE;
        for (int i = 0; i < diffms.length; i++) {
            if (clockStep[i] > msStep) // ignore bad records
                continue;
            if (diffms[i] > maxDiffMs)
                maxDiffMs = diffms[i];
        }

        // Adjust the ms difference to be the same, the rest goes into ns.
        for (int i = 0; i < diffms.length; i++) {
            if (clockStep[i] > msStep) // again, ignore bad records
                continue;
            if (diffms[i] < maxDiffMs) {
                diffns[i] += (maxDiffMs - diffms[i]) * 1000000l;
                diffms[i] = maxDiffMs;
            }
        }

        // Find the avg diffns
        double avgDiffNs = 0d;
        for (int i = 0; i < diffns.length; i++) {
            if (clockStep[i] == msStep) // again, ignore bad records
                avgDiffNs += diffns[i];
        }
        avgDiffNs /= goodRecords;

        // Find the standard deviation
        double sdevDiffNs = 0d;
        for (int i = 0; i < diffns.length; i++) {
            if (clockStep[i] == msStep) { // again, use only good records
                double dev = diffns[i] - avgDiffNs;
                dev *= dev;
                sdevDiffNs += dev;
            }
        }
        sdevDiffNs = Math.sqrt(sdevDiffNs / goodRecords);
        logger.fine("Sdev nsec: " + sdevDiffNs);

        // Now, eliminate the outliers beyond 2x sdev.
        // Based on the empirical rule, about 95% of the values are within
        // 2 standard deviations (assuming a normal distribution of
        // timing discrepancy). So what's beyond 2x sdev can really
        // be counted as outliers.
        int count = 0;
        double avgDiffNs2 = 0;
        for (int i = 0; i < diffns.length; i++) {
            if (clockStep[i] > msStep) // again, ignore bad records
                continue;
            double dev = Math.abs(diffns[i] - avgDiffNs);
            if (dev <= sdevDiffNs * 2d) { // Outliers are beyond 2x sdev.
                ++count;
                avgDiffNs2 += diffns[i];
            } else {
                logger.fine("Excluded outlier record " + i + ". " +
                            "Nanosec diff beyond 2*sdev or about 95th% " +
                            "according to empirical rule");
            }
        }
        avgDiffNs2 /= count;

        // Warn if we have a lot of outliers, allow 10% on each side.
        if (count < (int) 0.8d * goodRecords)
            logger.warning("Too many outliers in calibration. " +
                    (goodRecords - count) + " of " + goodRecords);

        // Round the average to the granularity
        int grainSize = 1;
        for (int i = 0; i < granularity; i++) {
            grainSize *= 10;
        }
        int avgDiffNsI = (int) Math.round(avgDiffNs2 / grainSize);
        avgDiffNsI *= grainSize;

        // Re-adjust the ms so the ns does not exceed 1,000,000.
        maxDiffMs -= avgDiffNsI / 1000000;
        avgDiffNsI %= 1000000;

        // Then assign the diffs.
        this.diffms = maxDiffMs;
        this.diffns = avgDiffNsI;

        // Based on our local differences between the nanos and millis clock
        // clock, we calculate the base nanose based on the given base millis.
        return (baseMillis - this.diffms) * 1000000l + this.diffns;
    }

    /**
     * Estimates the difference of the nanosec timer from the millisec
     * timer. This method is not as timing sensitive as calibrateNanos
     * and is routed from calibrateNanos in debug mode when the driver
     * is run in an IDE or debugger. It is by far not as accurate as
     * calibrateNanos.
     * @param baseMillis the base millisec to find the base nanosec for
     * @return The base nanosec corresponding to the base millisec.
     */
    private long estimateNanos(long baseMillis) {
        long ns1 = System.nanoTime();
        long ms = System.currentTimeMillis();
        long ns2 = System.nanoTime();
        long avgNs = (ns2 - ns1) / 2 + ns1;
        this.diffms = ms - avgNs / 1000000;
        this.diffns = (int) (avgNs % 1000000);
        return (baseMillis - this.diffms) * 1000000l + this.diffns;
    }

    private Logger getLogger() {
        if (logger == null)
            logger = Logger.getLogger(getClass().getName());
        return logger;
    }

    /**
     * Converts the millisec relative time to absolute nanosecs.
     * @param relTimeMillis The millisec relative time
     * @return The corresponding nanosec time
     */
    public long toAbsNanos(int relTimeMillis) {
        return (relTimeMillis + epochMillis - diffms) * 1000000l + diffns;
    }

    /**
     * Converts the nanosecond time relative to the run's epoch to absolute
     * millisec comparable to System.currentTimeMillis().
     * @param relTimeNanos The relative time in nanosecs
     * @return The absolute time in millisecs
     */
    public long toAbsMillis(long relTimeNanos) {
        return (relTimeNanos + epochNanos) / 1000000l + diffms;
    }

    /**
     * Converts the millisec time relative to the run's epoch to absolute
     * millisec comparable to System.currentTimeMillis().
     * @param relTimeMillis The relative time in nanosecs
     * @return the absolute time in millisecs
     */
    public long toAbsMillis(int relTimeMillis) {
        return relTimeMillis + epochMillis;
    }

    /**
     * Obtains the current time relative to the base time, in
     * milliseconds. This is mainly used to determine the current state of
     * of the run. The base time is synchronized between the Faban
     * master and all driver agents.
     * @return The nanoseconds from the base time.
     */
    public int getTime() {
        long c = System.currentTimeMillis();
        return (int) (c - epochMillis);
    }

    /**
     * Obtains the time relative to the base time, given a nanoTime
     * with an unknown datum. The base time is synchronized between the Faban
     * master and all driver agents.
     * @param nanoTime The nanotime obtained from System.nanoTime()
     * @return The nanosecond time relative to our base time.
     */
    public long toRelTime(long nanoTime) {
        return nanoTime - epochNanos;
    }

    /**
     * Obtains the nano time comparable to System.nanoTime() from a given
     * nanotime relative to the base time.
     * @param relNanos The relative nanosecond time
     * @return The nanoseconds comparable to System.nanoTime.
     */
    public long toAbsTime(long relNanos) {
        return relNanos + epochNanos;
    }

    /**
     * Sets the actual measured sleep time deviation. This is called from
     * the calibrator. Since the deviation is rarely read and certainly not
     * read concurrently we do not need to protect it. The compensation
     * will automatically be set as a round-up of deviation.
     * @param deviation The deviation, in nanosecs.
     */
    private void setDeviation(double deviation) {
        this.deviation = deviation;
        int compensation = (int) (deviation / 1000000); // A low cost way to
        ++compensation;                                 // round up.
        // Note: There's a 1:10^6 chance we round up a full millis.
        // But that's better than not rounding up at all.

        // Make a single atomic int assignment in order to avoid race conditions
        this.compensation = compensation * 1000000l;
    }

    /**
     * Reads the compensation value.
     * @return The compensation
     */
    public long getCompensation() {
        return compensation;
    }

    /**
     * Reads the deviation value.
     * @return The deviation
     */
    public double getDeviation() {
        return deviation;
    }

    /**
     * Causes this thread to sleep until the wakeup time as referenced
     * by this timer. Timer.sleep is not a minimum sleep time as in
     * Thread.sleep, but rather a calibrated and compensated sleep
     * time which gives the best statistical opportunity to wake up
     * at the required time. The actual wakeup can be slightly before
     * or slightly after the wakeup time but the average discrepancy
     * should be close to zero.
     * @param wakeupTime The time this thread is supposed to wakeup.
     */
    public void wakeupAt(long wakeupTime) {
        long currentTime;
        if ((currentTime = System.nanoTime()) < wakeupTime - compensation)
            try {
                long sleepTime = wakeupTime - currentTime - compensation;
                Thread.sleep(sleepTime / 1000000l, (int) (sleepTime % 1000000l));
            } catch (InterruptedException e) {
                throw new RuntimeException(
                        "Sleep interrupted. Run terminating.");
                // If we get an interrupt, the run is killed/terminated.
                // Just stop sleeping.
            }
    }

    public void idleTimerCheck(String id) {
        // Currently, this is only informational. In the future,
        // we could use the results to adjust the timer.
        logger.info(id + ": Performing idle timer check");
        logger.info(id + ": Idle timer characteristics:\n" + new TimerMeter(
                1000000, 10, 1000, 2, 100, 2).printTimerCharacterization());
    }

    /**
     * Runs a timer sleep time calibration for a certain amount of time.
     * @param id The agent identifier - used for logging purposes.
     * @param startRamp The start of ramp up
     * @param endRamp The end of ramp up
     */
    public void calibrate(String id, long startRamp, long endRamp) {

        // Start the meter.
        BusyTimerMeter meter = new BusyTimerMeter(id, startRamp, endRamp);
        meter.start();

        // Only calibrate if we have at least 5 secs to do so.
        // Otherwise it does not make sense at all.
        if (endRamp - System.nanoTime() > 5000000000l) {
            SleepCalibrator calibrator = new SleepCalibrator(id, endRamp);
            calibrator.start();
        } else {
            logger.warning(id + ": Rampup too short. " +
                    "Could not run calibration. Please increase rampup " +
                    "by at least 5 seconds");
        }       
    }

    /**
     * Adjusts the base time based on the clock differences of this JVM to
     * the master's JVM. This is done at the millisecond accuracy. Since
     * remote calls are usually not much faster than a millisec, it does
     * not make much sense to be too ideological about accuracy here.
     * @param offset The millisec offset between systems
     */
    public void adjustBaseTime(int offset) {

        // This is just a fake setting of the epochNanos. The call
        // into System.nanoTime() ensures initialization of the nano timer
        // and prevents underflow from calibration. This is just in case
        // the first call of System.nanoTime gives a value very close to
        // Long.MIN_VALUE.
        epochNanos = System.nanoTime();

        // We use the provided offset to calculate the millis
        // representing the same timestamp on a remote system.
        epochMillis += offset;

        // And then calibrate the nanos based on the new millis.
        epochNanos = calibrateNanos(epochMillis);
    }

    /**
     * Check whether we're in debug mode. Debug mode will make
     * the Faban driver less timing sensitive. Debug mode is set
     * by passing a system property -Dfaban.debug=true to the JVM
     * at startup.
     * @return true if debug is turned on.
     */
    boolean isDebug() {
        if (debug == null) {
            String debugSwitch = System.getProperty("faban.debug");
            if (debugSwitch != null)
                debug = new Boolean(debugSwitch);
            else
                debug = Boolean.FALSE;
        }
        return debug.booleanValue();
    }

    /**
     * The busy timer meter waits till the right time in the middle of ramp
     * up when the driver threads are busy and calls and reports the timer
     * characteristics at that time.
     */
    class BusyTimerMeter extends Thread {

        private String id;
        private long startRamp;
        private long endRamp;

        /**
         * Constructs the busy timer meter.
         * @param id The id of this agent
         * @param startRamp The start of ramp up, in absolute ns
         * @param endRamp The end of ramp up, in absolute ns
         */
        BusyTimerMeter(String id, long startRamp, long endRamp) {
            this.id = id;
            this.startRamp = startRamp;
            this.endRamp = endRamp;
        }

        /**
         * Starts the busy timer meter thread.
         */
        @Override
        public void run() {
            // Start running in the middle of ramp up but not beyond
            // 10 seconds before the end of ramp up.
            long startTime = startRamp + (endRamp - startRamp)/2;
            long maxStartTime = endRamp - 10000000000l;
            if (startTime > maxStartTime)
                startTime = maxStartTime;
            if (startTime < startRamp)
                startTime = startRamp;

            wakeupAt(startTime);

            // Currently, this is only informational. In the future,
            // we could use the results to adjust the timer.
            logger.info(id + ": Performing busy timer check");
            logger.info(id + ": Busy timer characteristics:\n" + new TimerMeter(
                    1000000, 10, 1000, 2, 100, 2).printTimerCharacterization());

            // Now make sure we still have enough time to calibrate the sleep.
            long timeExceeded = System.nanoTime() - endRamp;

            if (timeExceeded > 0l) {
                logger.warning(id + ": Rampup too short. " +
                        "Could not run calibration. Please increase rampup " +
                        "by at least " +
                        (int) Math.ceil(timeExceeded / 500000000d) +" seconds");
                // comes from 2 * timeExceeded / nano_to_seconds.
            }
        }
    }

    /**
     * The SleepCalibrator thread is used to calibrate the deviation of the sleep
     * time for compensation purposes. We already know the semantics of sleep
     * points to the minimum sleep time. The actual time calculated from<ul>
     *      <li>System.currentTimeMillis()
     *      <li>Thread.sleep(sleepTime)
     *      <li>System.currentTimeMillis()
     *
     * </ul>is always higher than sleepTime. The difference varies largely
     * between systems and could dramatically skew the driver cycles. It is
     * however expected to be in the 10s of milliseconds or less than 10
     * milliseconds range.<p>
     *
     * The SleepCalibrator must be run by each agent during the rampup time.
     * Assuming the rampup is adequately long, it should capture a pretty good
     * average deviation which will include the effects of several garbage
     * collections and use this difference to compensate the sleep time.
     *
     * @author Akara Sucharitakul
     */
    class SleepCalibrator extends Thread {

        private String id;
        private long endTime;

        /**
         * Constructs the calibrator.
         * @param id An identifier for this calibrator
         * @param endTime The time to end the calibration, based on this timer
         */
        SleepCalibrator(String id, long endTime) {
            this.id = id;
            this.endTime = endTime;
            setName("SleepCalibrator");
        }

        /**
         * Runs the calibrator thread.
         */
        public void run() {
            long timeAfter = Long.MIN_VALUE;
            long maxSleep = -1; // Initial value
            Random random = new Random();

            int count = 0;
            long devSum = 0l;
            for (;;) {
                // We random the intended sleep between 10 and 30 ms
                // thus assuming the systems running the driver will
                // have a timer resolution of 10ms or better.
                // The avg sleep time is 20ms as a result.
                int intendedSleep = random.nextInt(21) + 10;
                if (maxSleep == -1) // Set maxSleep for first check.
                    // maxSleep is in ns
                    maxSleep = intendedSleep * 1000000l;
                if (timeAfter + maxSleep >= endTime) {
                    setDeviation((double) devSum/count); // Final one.
                    break;
                }
                long timeBefore = Long.MAX_VALUE;
                try {
                    timeBefore = System.nanoTime();
                    Thread.sleep(intendedSleep, 0);
                    timeAfter = System.nanoTime();
                } catch (InterruptedException e) {
                }
                if (timeAfter > timeBefore) { // If not interrupted.
                    // actualSleep is in ns, intendedSleep is in ms.
                    long actualSleep = timeAfter - timeBefore;
                    long deviation = actualSleep - intendedSleep * 1000000l;
                    devSum += deviation;
                    ++count;
                    // deviation is in ns here
                    if (actualSleep > maxSleep)
                        maxSleep = actualSleep;
                    // Keep converging the deviation to the final value
                    // until this thread exits, just before steady state.
                    if (count % 50 == 0) // Sets every 50 experiments, ~1 sec.
                        setDeviation(devSum/(double) count);
                }
            }

            // Test for qualifying final compensation...
            if (!isDebug() && compensation > 100000000l) {
                logger.severe(id + ": System needed time compensation " +
                        "of " + compensation / 1000000l + ".\nValues over " +
                        "100ms are unacceptable for a driver. \nPlease use a " +
                        "faster system or tune the driver JVM/GC.\nExiting...");
                System.exit(1);
            }
            logger.info(id + ": Calibration succeeded. Sleep time " +
                    "deviation: " + (getDeviation() / 1000000l) +
                    " ms, compensation: " + (compensation / 1000000l) + " ms.");
        }
    }
}
