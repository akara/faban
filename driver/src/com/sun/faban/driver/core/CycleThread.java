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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: CycleThread.java,v 1.1 2006/06/29 18:51:33 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.core;

import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;


/**
 * A driver thread that controls the run by ramp up, steady state,
 * and ramp down cycles.
 *
 * @author Akara Sucharitakul
 */
public class CycleThread extends AgentThread {

    /**
     * Allocates and initializes the timing structures which is specific
     * to the pseudo-thread dimensions.
     */
    void initTimes() {
        delayTime = new int[1];
        startTime = new int[1];
        endTime = new int[1];

        // This is the start and end time of the previous operation used to
        // calculate the start of the next operation. We set it to the current
        // time for the first operation to have a reference point. In fact,
        // any reference point is OK.
        startTime[0] = timer.getTime();
        endTime[0] = startTime[0];
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
        driverContext = new DriverContext(this, timer);

        try {
            driver = driverClass.newInstance();
        } catch (Exception e) {
            logger.log(Level.SEVERE, name +
                    ": Error initializing driver object.", e);
            agent.abortRun();
            return;
        }

        if (runInfo.simultaneousStart)
            waitStartTime();

        // Calculate cycle counts
        endRampUp = runInfo.rampUp;
        endStdyState = endRampUp + runInfo.stdyState;
        endRampDown = endStdyState + runInfo.rampDown;

        selector = new Mix.Selector[1];
        selector[0] = driverConfig.mix[0].selector(random);

        logger.fine(name + ": Start of run.");

        // Loop until cycles are up
        driverLoop:
        while (!stopped) {

            // Select the operation
            currentOperation = selector[0].select();
            BenchmarkDefinition.Operation op =
                    driverConfig.operations[currentOperation];

            driverContext.setInvokeTime(getInvokeTime(op, mixId));

            // Invoke the operation
            try {
                op.m.invoke(driver);
                validateTimeCompletion(op);
                checkRamp();
                metrics.recordTx();
                metrics.recordDelayTime();
            } catch (InvocationTargetException e) {
                // An invocation target exception is caused by another
                // exception thrown by the operation directly.
                Throwable cause = e.getCause();
                checkFatal(cause, op);
                checkRamp();
                metrics.recordError();
                logError(cause, op);

                // We have to fix up the invoke/respond times to have valid
                // values and not -1.

                // In case of exception, invokeTime or even respondTime may
                // still be -1.
                DriverContext.TimingInfo timingInfo = driverContext.timingInfo;
                // If it never waited, we'll see whether we can just use the
                // previous start and end times.
                if (timingInfo.invokeTime == -1) {
                    int currentTime = timer.getTime();
                    if (currentTime < timingInfo.intendedInvokeTime) {
                        timingInfo.invokeTime = startTime[mixId];
                        timingInfo.respondTime = endTime[mixId];
                    } else {
                        // Too late, we'll need to use the real time
                        // for both invoke and respond time.
                        timingInfo.invokeTime = timer.getTime();
                        timingInfo.respondTime = timingInfo.invokeTime;
                        // The delay time is invalid,
                        // we cannot record in this case.
                    }
                } else if (timingInfo.respondTime == -1) {
                    timingInfo.respondTime = timingInfo.invokeTime;
                    metrics.recordDelayTime();
                } else {
                    metrics.recordDelayTime();
                }
            } catch (IllegalAccessException e) {
                logger.log(Level.SEVERE, name + "." + op.m.getName() + ": "
                        + e.getMessage(), e);
                agent.abortRun();
                return;
            }

            startTime[mixId] = driverContext.timingInfo.invokeTime;
            endTime[mixId] = driverContext.timingInfo.respondTime;

            if (cycleCount > endRampDown)
                break driverLoop;
        }
        logger.fine(name + ": End of run.");
    }

    /**
     * Tests whether the last operation is in steady state or not. This is
     * called through the context from the driver from within the operation
     * so we need to be careful not to change run control parameters. This
     * method only reads the stats.
     * @return True if the last operation is in steady state, false otherwise.
     */
    boolean isSteadyState() {
        if (!startTimeSet)
            return false;
        // Copy out cycle count so we do not alternate it here.
        // Remember, this method is controlled by the user-implemented
        // driver through the context.
        int cycleCount = this.cycleCount;
        ++cycleCount;
        if (cycleCount > endRampUp && cycleCount <= endStdyState)
            return true;
        else
            return false;
    }

    /**
     * Checks whether the last operation is in the ramp-up or ramp-down or
     * not. Updates the inRamp parameter accordingly.
     */
    void checkRamp() {
        // Note: in cycle runs without simultaneous start, the startTimeSet
        // flag is only set once the start time has reached. Unlike time runs
        // without simultaneous starts where the startTimeSet is set to true
        // once the start time is actually set.
        if (!runInfo.simultaneousStart && !startTimeSet &&
                agent.timeSetLatch.getCount() == 0) {
            int invoke = driverContext.timingInfo.invokeTime;
            if (invoke >= runInfo.benchStartTime) {
                ++cycleCount; // Count this tx which started after bench start.
                startTimeSet = true;
                inRamp = true;
            } else if (invoke == -1 &&
                    timer.getTime() >= runInfo.benchStartTime) {
                // We need to set the start time as time has come.
                startTimeSet = true;
                inRamp = true;
            }
        } else if (startTimeSet) { // Cycle where start time set not counted.
            ++cycleCount;
            if (cycleCount > endRampUp && cycleCount <= endStdyState)
                inRamp = false;
            else
                inRamp = true;
        }
    }
}
