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
 * $Id: Timer.java,v 1.3 2008/05/14 07:06:03 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.util;

import com.sun.faban.driver.FatalException;

import java.io.Serializable;
import java.util.Random;
import java.util.logging.Logger;

/**
 * This class has the functions to get timestamps
 */
public class Timer implements Serializable {
    /** The millisec epoch time of this benchmark */
	long epochMillis;

    /** The nanosec epoch time of this benchmark */
    transient long epochNanos; // This has no meaning on a different system.

    transient long diffms; // The epoch difference, millisec part
    transient int diffns; // The epoch difference, nanosec part

    private transient Logger logger;
    private long compensation = 5000000l;  // Some pretty good starting numbers
    private double deviation = 5000000d; // for both fields.

    /**
     * Default Constructor which saves the current time
     * as epochMillis and epochNanos (the start of the benchmark).
     * The resolution is unknown. Note that this is only constructed on the
     * master so the initial values before adjustments pertain to the master
     * alone.
     */
	public Timer() {
        // This is the closest we could possibly get, to put the two epochs
        // at the same time. The accuracy is different, but both epochs will
        // be in the same millisec.
        long epochMillis0 = 0l;
        int retries = 100;
        do {
            epochMillis0 = System.currentTimeMillis();
            epochNanos = System.nanoTime();
		    epochMillis = System.currentTimeMillis();
            if (retries-- <= 0)
                throw new FatalException("Cannot establish epoch times, " +
                                         "system may be too busy.");
        } while (epochMillis0 != epochMillis);

        diffms = epochMillis - epochNanos / 1000000l;
        diffns = (int) (epochNanos % 1000000l);

        getLogger().fine("Timer: baseTime ms: " + epochMillis +
                         ", ns: " + epochNanos);
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

    /**
     * Runs a timer sleep time calibration for a certain amount of time.
     * @param id The agent identifier - used for logging purposes.
     * @param endTime The time to end the calibration, referencing this JVMs
     *        nanosec timer.
     */
    public void calibrate(String id, long endTime) {
        // Convert relative time to abs time.
        endTime = toAbsTime(endTime);
        // Only calibrate if we have at least 5 secs to do so.
        // Otherwise it does not make sense at all.
        if (endTime - System.nanoTime() > 5000000000l) {
            Calibrator calibrator = new Calibrator(id, endTime);
            calibrator.start();
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

        long refMillis0 = 0l;
        long refMillis = 0l;
        long refNanos = Long.MIN_VALUE;

        // First, establish the local relationship between
        // the nanos and millis clocks.
        int retries = 100;
        do {
            refMillis0 = System.currentTimeMillis();
            refNanos = System.nanoTime();
		    refMillis = System.currentTimeMillis();
            if (retries-- <= 0)
                throw new FatalException("Cannot establish ref times, " +
                                         "system may be too busy.");
        } while (refMillis0 != refMillis);

        diffms = refMillis - refNanos / 1000000l;
        diffns = (int) (refNanos % 1000000l);

        // Then, we use the provided offset to calculate the millis
        // reprsenting the same timestamp on a remote system.
        epochMillis += offset;

        // And based on our local differences between the nanos and millis
        // clock, we set the epochNanos reopresenting the same instance in time.
        epochNanos = (epochMillis - diffms) * 1000000l + diffns;
    }

    /**
     * The Calibrator thread is used to calibrate the deviation of the sleep
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
     * The Calibrator must be run by each agent during the rampup time.
     * Assuming the rampup is adequately long, it should capture a pretty good
     * average deviation which will include the effects of several garbage
     * collections and use this difference to compensate the sleep time.
     *
     * @author Akara Sucharitakul
     */
    class Calibrator extends Thread {

        private String id;
        private long endTime;

        /**
         * Constructs the calibrator.
         * @param endTime The time to end the calibration, based on this timer
         */
        Calibrator(String id, long endTime) {
            this.id = id;
            this.endTime = endTime;
            setName("Calibrator");
        }

        /**
         * Runs the calibrator thread.
         */
        public void run() {
            long timeAfter = Long.MIN_VALUE;
            int maxSleep = -1; // Initial value
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
                    maxSleep = intendedSleep;
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
                    long actualSleep = timeAfter - timeBefore;
                    long deviation = actualSleep - intendedSleep * 1000000l;
                    devSum += deviation;
                    ++count;
                    if (actualSleep > maxSleep)
                        maxSleep = (int) (actualSleep / 1000000l);
                    // Keep converging the deviation to the final value
                    // until this thread exits, just before steady state.
                    if (count % 50 == 0) // Sets every 50 experiments, ~1 sec.
                        setDeviation((double) devSum/count);
                }
            }

            // Check whether we're in debug mode. Debug mode will make
            // the Faban driver less timing sensitive.
            boolean debug = false;
            String debugSwitch = System.getProperty("faban.debug");
            if (debugSwitch != null)
                debug = Boolean.parseBoolean(debugSwitch);

            // Test for qualifying final compensation...
            if (!debug && compensation > 100000000l) {
                getLogger().severe(id + ": System needed time compensation " +
                        "of " + compensation / 1000000l + ".\nValues over " +
                        "100ms are unacceptable for a driver. \nPlease use a " +
                        "faster system or tune the driver JVM/GC.\nExiting...");
                System.exit(1);
            }
            getLogger().fine(id + ": Calibration succeeded. Sleep time " +
                    "deviation: " + getDeviation() + " ns, compensation: " +
                    compensation + " ns.");
        }
    }
}
