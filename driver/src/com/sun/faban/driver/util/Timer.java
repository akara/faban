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
 * $Id: Timer.java,v 1.2 2006/06/29 19:38:39 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.util;

import java.io.Serializable;
import java.util.Random;
import java.util.logging.Logger;

/**
 * This class has the functions to get timestamps
 */
public class Timer implements Serializable {
	long baseTime;
    private transient Logger logger;
    private int compensation = 5;  // Some pretty good starting numbers
    private double deviation = 5d; // for both fields.

    /**
     * Default Constructor which saves the current time
     * as baseTime (the start of the benchmark). The resolution
     * is unknown.
     */
	public Timer() {
		baseTime = System.currentTimeMillis();
        getLogger().fine("Timer: baseTime in getOffsetTime = " + baseTime);
	}

    private Logger getLogger() {
        if (logger == null)
            logger = Logger.getLogger(getClass().getName());
        return logger;
    }

    /**
        * this method is used to print the benchmark report
        * @return baseTime the start of the benchmark time
        */
	public long getOffsetTime() {
		return baseTime;
	}


	/**
	 * This  method returns the current time relative to baseTime.
	 * This way, we don't need to keep track of large numbers and
	 * worry about long variables
         * @return int duration the difference between start and current time
	 */
	public int getTime() {
		long c = System.currentTimeMillis();
		return (int) (c - baseTime);
	}

    /**
     * Sets the actual measured sleep time deviation. This is called from
     * the calibrator. Since the deviation is rarely read and certainly not
     * read concurrently we do not need to protect it. The compensation
     * will automatically be set as a round-up of deviation.
     * @param deviation The deviation.
     */
    private void setDeviation(double deviation) {
        this.deviation = deviation;
        int compensation = (int) deviation;     // A low cost way to
        if (deviation - compensation > 0d)      // round up the deviation.
            ++compensation;
        // Make a single atomic int assignment in order to avoid race conditions
        this.compensation = compensation;
    }

    /**
     * Reads the compensation value.
     * @return The compensation
     */
    public int getCompensation() {
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
    public void sleep(int wakeupTime) {
        int currentTime;
        if ((currentTime = getTime()) < wakeupTime - compensation)
            try {
                Thread.sleep(wakeupTime - currentTime - compensation);
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
     * @param endTime The time to end the calibration, referencing this timer.
     */
    public void calibrate(String id, int endTime) {
        // Only calibrate if we have at least 5 secs to do so.
        // Otherwise it does not make sense at all.
        if (endTime - getTime() > 5000) {
            Calibrator calibrator = new Calibrator(id, endTime);
            calibrator.start();
        }
    }

    /**
     * Adjusts the base time based on the clock differences of this JVM to
     * the master's JVM.
     * @param offset
     */
    public void adjustBaseTime(int offset) {
        baseTime += offset;
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
        private int endTime;

        /**
         * Constructs the calibrator.
         * @param endTime The time to end the calibration, based on this timer
         */
        Calibrator(String id, int endTime) {
            this.id = id;
            this.endTime = endTime;
            setName("Calibrator");
        }

        /**
         * Runs the calibrator thread.
         */
        public void run() {
            int timeAfter = Integer.MIN_VALUE;
            int maxSleep = -1; // Initial value
            Random random = new Random();

            int count = 0;
            int devSum = 0;
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
                int timeBefore = Integer.MAX_VALUE;
                try {
                    timeBefore = getTime();
                    Thread.sleep(intendedSleep);
                    timeAfter = getTime();
                } catch (InterruptedException e) {
                }
                if (timeAfter > timeBefore) { // If not interrupted.
                    int actualSleep = timeAfter - timeBefore;
                    int deviation = actualSleep - intendedSleep;
                    devSum += deviation;
                    ++count;
                    if (actualSleep > maxSleep)
                        maxSleep = actualSleep;
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
            if (!debug && compensation > 100) {
                getLogger().severe(id + ": System needed time compensation " +
                        "of " + compensation + ".\nValues over 100ms are " +
                        "unacceptable for a driver. \nPlease use a faster " +
                        "system or tune the driver JVM/GC.\nExiting...");
                System.exit(1);
            }
            getLogger().fine(id + ": Calibration succeeded. Sleep time " +
                    "deviation: " + getDeviation() + " compensation: " +
                    compensation + " ms.");
        }
    }
}
