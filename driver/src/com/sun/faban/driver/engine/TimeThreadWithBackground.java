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

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;


/**
 /**
  * A driver thread that controls the run by ramp up, steady state,
  * and ramp down times, and allows for a pseudo-background thread used
 * to simulate actions regularly taken by a fat client like timed operations.
 * 
  * @author Akara Sucharitakul
  */
public class TimeThreadWithBackground extends TimeThread {

    int[] mixOperation = new int[2]; // Per-mix index

    /**
     *  Allocates and initializes the timing structures which is specific
     *  to the pseudo-thread dimensions.
     */
    @Override
	void initTimes() {
        delayTime = new long[2];
        startTime = new long[2];
        endTime = new long[2];
        previousOperation = new int[2];

        // This is the start and end time of the previous operation used to
        // calculate the start of the next operation. We set it to the current
        // time for the first operation to have a reference point. In fact,
        // any reference point is OK.
        startTime[0] = System.nanoTime();
        endTime[0] = startTime[0];
        endTime[1] = startTime[1] = startTime[0];
        previousOperation[0] = -1;
        previousOperation[1] = -1;
        mixOperation[0] = -1;
        mixOperation[1] = -1;
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
    @Override
	void doRun() {

        driverContext = new DriverContext(this, timer);

        try {
            driver = driverClass.newInstance();
        } catch (Exception e) {
            logger.log(Level.SEVERE, name +
                    ": Error initializing driver object.", e);
            agent.abortRun();
            return; // Terminate this thread immediately
        }

        // Call the preRun.
        preRun();

        // Notify the agent that we have started successfully.
        agent.threadStartLatch.countDown();

        mixId = -1; // Since we run both fg and bg,
                   // initially set it to -1 (not set)
        
        selector = new Mix.Selector[2];
        selector[0] = driverConfig.mix[0].selector(random);
        selector[1] = driverConfig.mix[1].selector(random);

        if (runInfo.simultaneousStart) {
            waitStartTime();

            // Calculate time periods
            // Note that the time periods are in secs, need to convert
            endRampUp = agent.startTime + runInfo.rampUp * 1000000000l;
            endStdyState = endRampUp + runInfo.stdyState * 1000000000l;
            endRampDown = endStdyState + runInfo.rampDown * 1000000000l;
        }

        logger.fine(name + ": Start of run.");
        
        // Next fg and bg operation
        BenchmarkDefinition.Operation[] op = 
                new BenchmarkDefinition.Operation[2];
        // Next fg and bg time
        long[] invokeTime = new long[2];

        // Loop until time or cycles are up
        driverLoop:
        while (!stopped) {

            if (runInfo.variableLoad) {
                if (id >= agent.runningThreads) {
                    logger.log(Level.FINE, "Current load level: (" +
                            agent.runningThreads + ") Thread " + id +
                            " sleeping.");
                    timer.wakeupAt(agent.loadSwitchTime);
                    // Reset ops and don't record first cycle
                    mixOperation[0] = -1;
                    previousOperation[0] = -1;
                    mixOperation[1] = -1;
                    previousOperation[1] = -1;
                    continue;
                }
            }

            if (!runInfo.simultaneousStart && !startTimeSet &&
                    agent.timeSetLatch.getCount() == 0) {
                startTimeSet = true;

                // Calculate time periods
                // Note that the time periods are in secs, need to convert
                endRampUp = agent.startTime + runInfo.rampUp * 1000000000l;
                endStdyState = endRampUp + runInfo.stdyState * 1000000000l;
                endRampDown = endStdyState + runInfo.rampDown * 1000000000l;
            }

            // Select the operations and invoke times
            if (mixId != 1) {
                previousOperation[0] = mixOperation[0];
                BenchmarkDefinition.Operation previousOp = null;
                if (previousOperation[0] >= 0) {
                    previousOp = driverConfig.operations[currentOperation];
                }

                mixOperation[0] = selector[0].select();
                op[0] = driverConfig.mix[0].operations[mixOperation[0]];
                invokeTime[0] = getInvokeTime(previousOp, 0);
            }

            if (mixId != 0) {
                previousOperation[1] = mixOperation[1];
                BenchmarkDefinition.Operation previousOp = null;
                if (previousOperation[1] >= 0) {
                    previousOp = driverConfig.operations[currentOperation];
                }

                mixOperation[1] = selector[1].select();
                op[1] = driverConfig.mix[1].operations[mixOperation[1]];
                invokeTime[1] = getInvokeTime(previousOp, 1);
            }

            // Now get the new mixId, note that foreground has preference
            // whenever the invoke times are the same.
            if (invokeTime[1] < invokeTime[0]) {
                mixId = 1;
            } else {
                mixId = 0;
            }

            currentOperation = driverConfig.getOperationIdx(
                    mixId, mixOperation[mixId]);

            // endRampDown is only valid if start time is set.
            // If the start time of next tx is beyond the end
            // of the ramp down, just stop right here.
            if (startTimeSet && invokeTime[mixId] >= endRampDown) {
                break driverLoop;
            }

            driverContext.setInvokeTime(invokeTime[mixId]);

            // Invoke the operation
            try {
                op[mixId].m.invoke(driver);
                validateTimeCompletion(op[mixId]);
                checkRamp();
                metrics.recordTx();
                metrics.recordDelayTime();
            } catch (InvocationTargetException e) {
                // An invocation target exception is caused by another
                // exception thrown by the operation directly.
                Throwable cause = e.getCause();
                checkFatal(cause, op[mixId]);

                // We have to fix up the invoke/respond times to have valid
                // values and not TIME_NOT_SET.

                // In case of exception, invokeTime or even respondTime may
                // still be TIME_NOT_SET.
                DriverContext.TimingInfo timingInfo =
                        driverContext.timingInfo;

                // The lastRespondTime may be set, though. if so, propagate
                // it back to respondTime.
                if (timingInfo.respondTime == TIME_NOT_SET &&
                        timingInfo.lastRespondTime != TIME_NOT_SET) {
                    logger.fine("Potential open request in operation " +
                            op[mixId].m.getName() + ".");
                    timingInfo.respondTime = timingInfo.lastRespondTime;
                }
                // If it never waited, we'll see whether we can just use
                // the previous start and end times.
                if (timingInfo.invokeTime == TIME_NOT_SET) {
                    long currentTime = System.nanoTime();
                    if (currentTime < timingInfo.intendedInvokeTime) {
                        // No time change, no need to checkRamp
                        metrics.recordError();
                        logError(cause, op[mixId]);
                        continue driverLoop;
                    }
                    // Too late, we'll need to use the real time
                    // for both invoke and respond time.
                    timingInfo.invokeTime = System.nanoTime();
                    timingInfo.respondTime = timingInfo.invokeTime;
                    checkRamp();
                    metrics.recordError();
                    logError(cause, op[mixId]);
                    // The delay time is invalid,
                    // we cannot record in this case.
                } else if (timingInfo.respondTime == TIME_NOT_SET) {
                    timingInfo.respondTime = System.nanoTime();
                    checkRamp();
                    metrics.recordError();
                    logError(cause, op[mixId]);
                    metrics.recordDelayTime();
                } else { // All times are there
                    checkRamp();
                    metrics.recordError();
                    logError(cause, op[mixId]);
                    metrics.recordDelayTime();
                }
            } catch (IllegalAccessException e) {
                logger.log(Level.SEVERE, name + "." +
                        op[mixId].m.getName() + ": " + e.getMessage(), e);
                agent.abortRun();
                return;
            }

            startTime[mixId] = driverContext.timingInfo.invokeTime;
            endTime[mixId] = driverContext.timingInfo.respondTime;

            if (startTimeSet && endTime[mixId] >= endRampDown) {
                break driverLoop;
            }
        }
        logger.fine(name + ": End of run.");
    }
}
