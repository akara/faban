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
 * $Id: TimeThread.java,v 1.3 2009/07/21 21:21:09 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.engine;

import com.sun.faban.driver.FatalException;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;


/**
 * A driver thread that controls the run by ramp up, steady state,
 * and ramp down times.
 *
 * @author Akara Sucharitakul
 */
public class TimeThread extends AgentThread {

    /**
     * Allocates and initializes the timing structures which is specific
     * to the pseudo-thread dimensions.
     */
	void initTimes() {
        delayTime = new long[1];
        startTime = new long[1];
        endTime = new long[1];
        previousOperation = new int[1];

        // This is the start and end time of the previous operation used to
        // calculate the start of the next operation. We set it to the current
        // time for the first operation to have a reference point. In fact,
        // any reference point is OK.
        startTime[0] = System.nanoTime();
        endTime[0] = startTime[0];
        previousOperation[0] = -1;
    }

    /**
     * Each thread executes in the doRun method until the benchmark time is up
     * The main loop chooses a tx. type according to the mix specified in
     * the parameter file and calls the appropriate transaction
     * method to do the job.
   	 * The stats for the entire run are stored in a Metrics object
   	 * which is returned to the Agent via the getResult() method.
     * @see Metrics
     */
	void doRun() {

        boolean active = true;
        driverContext = new DriverContext(this, timer);

        try {
            driver = driverClass.newInstance();
        } catch (Throwable t) {
            Throwable cause = t.getCause();
            while (cause != null) {
                t = cause;
                cause = t.getCause();
            }
            logger.log(Level.SEVERE, name +
                    ": Error initializing driver object.", t);
            agent.abortRun();
            return; // Terminate this thread immediately
        }

        // Call the preRun.
        preRun();

        // Notify the agent that we have started successfully.
        agent.threadStartLatch.countDown();

        selector = new Mix.Selector[1];
        selector[0] = driverConfig.mix[0].selector(random);

        if (runInfo.simultaneousStart) {
            waitStartTime();

            // Calculate time periods
            // Note that the time periods are in secs, need to convert
            endRampUp = agent.startTime + runInfo.rampUp * 1000000000l;
            endStdyState = endRampUp + runInfo.stdyState * 1000000000l;
            endRampDown = endStdyState + runInfo.rampDown * 1000000000l;
        }

        logger.fine(name + ": Start of run.");

        // Loop until time or cycles are up
        driverLoop:
        while (!stopped) {

            if (runInfo.variableLoad) {
                if (id >= agent.runningThreads) {
                    try {
                        logger.log(Level.FINE, "Current load level: (" + 
                                agent.runningThreads + ") Thread " + id + 
                                " sleeping.");
                        sleep(agent.timeToRunFor * 1000);
                        active = false;
                    } catch (InterruptedException e) {
                        logger.log(Level.FINE, e.getMessage(), e);
                    }
                } else {
                    active = true;
                }
            }

            if (active) {

                if (!runInfo.simultaneousStart && !startTimeSet &&
                        agent.timeSetLatch.getCount() == 0) {
                    startTimeSet = true;

                    // Calculate time periods
                    // Note that the time periods are in secs, need to convert
                    endRampUp = agent.startTime + runInfo.rampUp * 1000000000l;
                    endStdyState = endRampUp + runInfo.stdyState * 1000000000l;
                    endRampDown = endStdyState + runInfo.rampDown * 1000000000l;
                }

                // Save the previous operation
                previousOperation[mixId] = currentOperation;
                BenchmarkDefinition.Operation previousOp = null;
                if (previousOperation[mixId] >= 0) {
                    previousOp = driverConfig.operations[currentOperation];
                }

                // Select the operation
                currentOperation = selector[0].select();
                BenchmarkDefinition.Operation op =
                        driverConfig.operations[currentOperation];

                // The invoke time is based on the delay after the previous op.
                // so we need to use previous op for calculating and recording.
                long invokeTime = getInvokeTime(previousOp, mixId);

                // endRampDown is only valid if start time is set.
                // If the start time of next tx is beyond the end
                // of the ramp down, just stop right here.
                if (startTimeSet && invokeTime >= endRampDown) {
                    break driverLoop;
                }

                logger.finest(name + ": Invoking " + op.name + " at time " +
                        invokeTime + ". Ramp down ends at time " +
                        endRampDown + '.');

                driverContext.setInvokeTime(invokeTime);

                // Invoke the operation
                try {
                    if (id == 0)
                        logger.finest("Invoking " + op.name + " at " + System.nanoTime());
                    op.m.invoke(driver);
                    if (id == 0)
                        logger.finest("Returned from " + op.name + " (OK) at " + System.nanoTime());
                    validateTimeCompletion(op);
                    if (id == 0) {
                        DriverContext.TimingInfo t = driverContext.timingInfo;
                        logger.finest("Invoke: " + t.invokeTime + ", Respond: " + t.respondTime + ", Pause: " + t.pauseTime);
                    }
                    checkRamp();
                    metrics.recordTx();
                    metrics.recordDelayTime();
                } catch (InvocationTargetException e) {
                    if (id == 0)
                        logger.finest("Returned from " + op.name + " (Err) at " + System.nanoTime());
                    // An invocation target exception is caused by another
                    // exception thrown by the operation directly.
                    Throwable cause = e.getCause();
                    checkFatal(cause, op);

                    // We have to fix up the invoke/respond times to have valid
                    // values and not TIME_NOT_SET.

                    // In case of exception, invokeTime or even respondTime may
                    // still be TIME_NOT_SET.
                    DriverContext.TimingInfo timingInfo =
                            driverContext.timingInfo;
                    // If it never waited, we'll see whether we can just use the
                    // previous start and end times.
                    if (timingInfo.invokeTime == TIME_NOT_SET) {
                        long currentTime = System.nanoTime();
                        if (currentTime < timingInfo.intendedInvokeTime) {
                            // No time change, no need to checkRamp
                            metrics.recordError();
                            logError(cause, op);
                            continue driverLoop;
                        }
                        // Too late, we'll need to use the real time
                        // for both invoke and respond time.
                        timingInfo.invokeTime = System.nanoTime();
                        timingInfo.respondTime = timingInfo.invokeTime;
                        checkRamp();
                        metrics.recordError();
                        logError(cause, op);
                    // The delay time is invalid,
                    // we cannot record in this case.
                    } else if (timingInfo.respondTime == TIME_NOT_SET) {
                        timingInfo.respondTime = System.nanoTime();
                        checkRamp();
                        metrics.recordError();
                        logError(cause, op);
                        metrics.recordDelayTime();
                    } else { // All times are there
                        checkRamp();
                        metrics.recordError();
                        logError(cause, op);
                        metrics.recordDelayTime();
                    }
                } catch (IllegalAccessException e) {
                    logger.log(Level.SEVERE, name + "." + op.m.getName() +
                            ": " + e.getMessage(), e);
                    agent.abortRun();
                    return;
                }

                startTime[mixId] = driverContext.timingInfo.invokeTime;
                endTime[mixId] = driverContext.timingInfo.respondTime;

                if (startTimeSet && endTime[mixId] >= endRampDown) {
                    break driverLoop;
                }
            }
        }
        logger.fine(name + ": End of run.");
    }

    /**
     * Checks whether the last operation is in the ramp-up or ramp-down or
     * not. Updates the inRamp parameter accordingly.
     */
	void checkRamp() {
        inRamp = !isSteadyState(driverContext.timingInfo.invokeTime,
                                driverContext.timingInfo.respondTime);
    }

    /**
     * Tests whether the last operation is in steady state or not. This is
     * called by the driver from within the operation so we need to be careful
     * not to change run control parameters. This method only reads the stats.
     * @return True if the last operation is in steady state, false otherwise.
     */
	boolean isSteadyState() {
        if (driverContext.timingInfo.respondTime == TIME_NOT_SET) {
			throw new FatalException("isTxSteadyState called before response " +
                    "time capture. Cannot determine tx in steady state or " +
                    "not. This is a bug in the driver code.");
		}

        return isSteadyState(driverContext.timingInfo.invokeTime,
                                driverContext.timingInfo.respondTime);
    }

    /**
     * Tests whether the time between start and end is in steady state or not.
     * For non time-based steady state, this will depend on the current cycle
     * count. Otherwise time is used.
     *
     * @param start The start of a time span
     * @param end   The end of a time span
     * @return true if this time span is in steady state, false otherwise.
     */
	boolean isSteadyState(long start, long end) {
        return startTimeSet && start >= endRampUp && end < endStdyState;
    }
}
