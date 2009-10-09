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

import com.sun.faban.driver.BenchmarkOperation;
import com.sun.faban.driver.FatalException;
import com.sun.faban.driver.Timing;
import com.sun.faban.driver.ExpectedException;
import com.sun.faban.driver.util.Random;
import com.sun.faban.driver.util.Timer;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.TimeUnit;
import java.io.InterruptedIOException;


/**
 * Abstract superclass for all driver threads. It provides a factory for
 * instantiating the right implementation depending on the driver specification.
 * Subclasses execute the provided driver, keeps track of response times,
 * think times, transaction counts, etc.
 *
 * @author Akara Sucharitakul
 */
public abstract class AgentThread extends Thread {

	/**
	 * The various run states for an {@link AgentThread}.
	 * 
	 * @author ncampbell
	 */
	public static enum RunState {
		/* !!!
		 * Please note the order is significant for this enumeration.  A
		 * Enum#compareTo is used which is based on the order of these RunStates.
		 * !!!
		 */

		/**
		 * The {@link AgentThread} thread has not started.
		 */
	    NOT_STARTED,
	    /**
	     * The {@link AgentThread} has initialized.
	     */
	    INITIALIZING,
	    /**
	     * The {@link AgentThread} is executing {@link BenchmarkOperation}s to warm
	     * the SUT.
	     */
	    PRE_RUN,
	    /**
	     * The {@link AgentThread} is running in steady state.
	     */
	    RUNNING,
	    /**
	     * The {@link AgentThread} is doing a post run.
	     */
	    POST_RUN,
	    /**
	     *  The {@link AgentThread} has completed.
	     */
	    ENDED
	}

    String type;
    String name;
    int id;
    int currentOperation = -1; // Global index into the current operation.
    int[] previousOperation; // Index into the previous operation.
    int mixId = 0; // 0 for foreground and 1 for background
    Mix.Selector[] selector; // The selector array, size 1 if no bg, 2 if bg

    DriverContext driverContext;
    Metrics metrics;
    Random random = new Random();
    Timer timer;
    AgentImpl agent;
    RunInfo.DriverConfig driverConfig;
    Class<?> driverClass;
    Object driver;
    boolean inRamp = true; // indicator for rampup or rampdown, initially true
    long[] delayTime;  // recently calculated cycle times
    long[] startTime; // start times for previous tx
    long[] endTime; // end time for the recent tx ended

    private RunState threadState = RunState.NOT_STARTED;

    Logger logger;
    String className;
    long endRampUp = Long.MAX_VALUE;
    long endStdyState = Long.MAX_VALUE;
    long endRampDown = Long.MAX_VALUE;
    int cycleCount = 0; // The cycles executed so far

    /** Run configuration from the Master. */
    RunInfo runInfo;

    boolean startTimeSet = false;

    boolean stopped = false;

    /** Constant value for specifying that the time is not set. */
    public static final long TIME_NOT_SET = Long.MIN_VALUE;

    /**
     * Factory method for instantiating the right type of AgentThread.
     * @param type The type of this agent
     * @param agentId The display id of this agent
     * @param id The id of this agent
     * @param driverClass The driver class
     * @param timer The timer object reference
     * @param agent The agent calling this thread
     * @return An instance of the AgentThread subclass.
     */
    public static AgentThread getInstance(String type, String agentId, int id,
                                Class<?> driverClass, Timer timer,
                                AgentImpl agent) {
        RunInfo.DriverConfig driverConfig = RunInfo.getInstance().driverConfig;
        AgentThread agentThread = null;
        switch (driverConfig.runControl) {
           case TIME : if (driverConfig.mix[1] != null) {
			agentThread = new TimeThreadWithBackground();
		} else {
			agentThread = new TimeThread();
		}
                       break;
           case CYCLES : agentThread = new CycleThread();
        }

        agentThread.configure(type, agentId, id, driverClass, timer, agent);
        return agentThread;
    }

    /**
     * Configures this AgentThread.
     *
     * @param type The type of this agent
     * @param agentId The display id of this agent
     * @param id The id of this agent
     * @param driverClass The driver class
     * @param timer The timer object reference
     * @param agent The agent calling this thread
     */
    private void configure(String type, String agentId, int id,
                                Class<?> driverClass, Timer timer,
                                AgentImpl agent) {
        this.type = type;
        this.id = id;
        this.driverClass = driverClass;
        this.timer = timer;
        this.runInfo = RunInfo.getInstance();
        this.agent = agent;
        random = new Random(System.nanoTime() + hashCode());
        className = getClass().getName();
        driverConfig = runInfo.driverConfig;
        name = type + '[' + agentId + "]." + id;
        setName(name);
        logger = Logger.getLogger(className + '.' + id);
        metrics = new Metrics(this);
        initTimes();
    }

    /**
     *  Allocates and initializes the timing structures which is specific
     *  to the pseudo-thread dimensions.
     */
    abstract void initTimes();

    /**
     * Entry point for starting the thread. Subclasses do not override this
     * method but override doRun instead. Run implicitly calls doRun.
     */
    @Override
	public final void run() {
        try {
            setThreadState(RunState.INITIALIZING);
            doRun();
        } catch (FatalException e) {
            // A fatal exception thrown by the driver is already caught
            // in the run methods and logged there.
            // A fatal exception otherwise thrown by the run
            // methods signals termination of this thread.
            if (!e.wasLogged())  {
                Throwable t = e.getCause();
                if (t != null) {
					logger.log(Level.SEVERE, name + ": " + t.getMessage(), t);
				} else {
					logger.log(Level.SEVERE, name + ": " + e.getMessage(), e);
				}
                e.setLogged();
                agent.abortRun();
            }
        } catch (Throwable t) {
            logger.log(Level.SEVERE, name + ": " + t.getMessage(), t);
            agent.abortRun();
        } finally {
            postRun();
        }
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
    abstract void doRun();

    /**
     * Checks for a fatal exception. This is called from the invocation loop.
     * @param e The throwable
     * @param op The operation
     */
    void checkFatal(Throwable e, BenchmarkDefinition.Operation op) {
        checkFatal(e, op.m);
    }

    /**
     * Checks for a fatal exception. This is called from the pre/post.
     * @param e The throwable
     * @param m The method
     */
    void checkFatal(Throwable e, Method m) {
        if (e instanceof FatalException) {
            FatalException fatal = (FatalException) e;
            e = fatal.getCause();
            if (e != null) {
				logger.log(Level.SEVERE, name + '.' + m.getName() +
                        ": " + e.getMessage(), e);
			} else {
				logger.log(Level.SEVERE, name + '.' + m.getName() +
                        ": " + fatal.getMessage(), fatal);
			}
            fatal.setLogged();
            agent.abortRun();
            throw fatal; // Also don't continue with current thread.
        }
    }

    /**
     * Logs the normal errors occuring in operations. The operation is marked
     * as failed.
     * @param e  The throwable received
     * @param op The operation being executed.
     */
    void logError(Throwable e, BenchmarkDefinition.Operation op) {
        String message = e.getMessage();
        if (message == null) { // Find the message in the highest level exception.
            Throwable t = e.getCause();
            while (message == null && t != null) {
                message = t.getMessage();
                t = t.getCause();
            }
        }
        if (message == null) // If still null, artificially create message.
            message = "Exception in operation.";
        message = name + "." + op.m.getName() + ": " + message;

        if (inRamp) {
			message += "\nNote: Error not counted in result." +
                    "\nEither transaction start or end time is not " +
                    "within steady state.";
		}
        Level level;
        if (e instanceof ExpectedException) {
			level = Level.FINER;
		} else {
			level = Level.WARNING;
		}
        logger.log(level, message, e);
    }

    private synchronized void setThreadState(RunState state) {
        threadState = state;
        notifyAll();
    }

    private synchronized boolean compareAndSetThreadState(RunState orig, RunState state) {
        boolean set = threadState == orig;
        if (set) {
            threadState = state;
            notifyAll();
        }
        return set;
    }


    /**
     * Obtains the state of the current thread.
     * @return The state of the current thread.
     */
    public synchronized RunState getThreadState() {
        return threadState;
    }

    /**
     * Waits for a given state of the thread to arrive.
     * @param state The state to wait for.
     */
    public synchronized void waitThreadState(RunState state) {
        while (threadState.compareTo(state) < 0) {
            try {
                wait(10000);
            } catch (InterruptedException e) {
            	logger.log(Level.FINE, e.getMessage(), e);
            }
        }
    }

    /**
     * Executes the method market with @OnceBefore in thread 0.
     */
    void preRun() {
        // Thread 0 needs to do the preRun
        if (id == 0 && driverConfig.preRun != null) {
            setThreadState(RunState.PRE_RUN);
            logger.fine(name + ": Invoking preRun @OnceBefore");
            try {
                invokePrePost(driverConfig.preRun.m);
            } catch (InterruptedIOException e) {
                // Should not happen unless run is cancelled. And if so,
                // we don't really care to redo this.
            } finally {
                agent.preRunLatch.countDown();
            }
            try {
                while (!stopped) {
                    if (agent.startLatch.await(500, TimeUnit.MILLISECONDS))
                        break;
                }
            } catch (InterruptedException e) {
                logger.log(Level.WARNING, name +
                        ": Start latch await interrupted!");
            }
            agent.startLatch = null;
            logger.finest(name + ": Thread 0 got startLatch, now executing.");
        }
        setThreadState(RunState.RUNNING);
    }

    /**
     * Executes the method marked with @OnceAfter in thread 0.
     */
    void postRun() {
        if (id == 0 && driverConfig.postRun != null &&
                compareAndSetThreadState(RunState.RUNNING, RunState.POST_RUN)) {
            // Tell the world we're finished.
            agent.finishLatch.countDown();
            logger.finest(name +
                    ": Thread 0 finished, awaiting postRunLatch");

            // Then wait for a signal (everybody else finished) to run postrun.
            while (agent.postRunLatch.getCount() > 0l) {
				try {
                    while (!stopped) {
                        if (agent.postRunLatch.await(500,
                                TimeUnit.MILLISECONDS))
                            break;
                    }
                } catch (InterruptedException e) {
                	logger.log(Level.FINE, e.getMessage(), e);
                }
			}

            if (stopped)
                return;
            // We need to make sure this method is re-run if I/O is interrupted.
            // This may happen if terminate gets called while thread is
            // switching to POST_RUN state.
            boolean interrupted = false;
            logger.fine(name + ": Invoking postRun @OnceAfter");
            do {
                try {
                    invokePrePost(driverConfig.postRun.m);
                } catch (InterruptedIOException e) {
                    interrupted = true;
                }
            } while (interrupted);
            logger.finest(name + ": Thread 0 finished postRun.");
        }
        setThreadState(RunState.ENDED);
    }

    private void invokePrePost(Method m) throws InterruptedIOException {
        try {
            m.invoke(driver);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause == null) {
				cause = e;
			}
            checkFatal(cause, m);
            logger.log(Level.WARNING, name + "." + m.getName() + ": " +
                    cause.getMessage(), cause);
            if (cause instanceof InterruptedIOException) {
				throw (InterruptedIOException) cause;
			}
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, name + "." + m.getName() + ": " +
                    e.getMessage(), e);
        }

    }

    /**
     * Obtains the invoke time of the next operation. 
     * 
     * @param op The operation
     * @param mixId The mix
     * @return The targeted invoke time.
     */
    long getInvokeTime(BenchmarkDefinition.Operation op, int mixId) {
        Cycle cycle;
        if (op == null) {
			// No op, this is the initial cycle, use initialDelay
            cycle = runInfo.driverConfig.initialDelay[mixId];
		} else {
			// Set the start time based on the operation selected
            cycle = op.cycle;
		}

        long invokeTime = TIME_NOT_SET;
        delayTime[mixId] = cycle.getDelay(random);

        switch (cycle.cycleType) {
            case CYCLETIME :
                invokeTime = startTime[mixId] + delayTime[mixId];
                break;
            case THINKTIME :
                invokeTime = endTime[mixId] + delayTime[mixId];
                break;
        }
        return invokeTime;
    }

    /**
     * This method blocks until the start time is set by the master.
     * Called by AgentThread implementations.
     */
    void waitStartTime() {
        try {
            agent.timeSetLatch.await();
            startTimeSet = true;
            long delay = agent.startTime - System.nanoTime();
            if (delay <= 0) {
                logger.severe(name + ": Start time is set " + (-delay) +
                        " nanosecs too late. Please file a bug.");
                agent.abortRun();
            } else {
                timer.wakeupAt(agent.startTime);
            }
        } catch (InterruptedException e) { // Run is killed.
            throw new FatalException(e);
        }
    }

    /**
     * Validates whether the times for a successful operation are properly
     * captured. Called by AgentThread implementations.
     * @param op The operation.
     */
    void validateTimeCompletion(BenchmarkDefinition.Operation op) {
        DriverContext.TimingInfo timingInfo = driverContext.timingInfo;

        // If there is no error, we still check for the
        // unusual case of time not recorded and issue the
        // proper message.
        if (timingInfo.invokeTime == TIME_NOT_SET) {
            String msg = null;
            if (driverConfig.operations[
                    currentOperation].timing == Timing.AUTO) {
                msg = name + '.' + op.m.getName() +
                        ": Transport not called! " +
                        "Please ensure transport instantiation " +
                        "before making any remote calls!";
            } else {
                msg = name + '.' + op.m.getName() +
                        ": Cannot determine time! " +
                        "DriverContext.recordTime() not called " +
                        "before critical section in operation.";

            }
            logger.severe(msg);
            agent.abortRun();
            throw new FatalException(msg);
        } else if (timingInfo.respondTime == TIME_NOT_SET &&
                   timingInfo.lastRespondTime == TIME_NOT_SET) {
            String msg = null;
            if (driverConfig.operations[
                    currentOperation].timing == Timing.AUTO) {
                msg = name + '.' + op.m.getName() +
                        ": Transport incomplete! " +
                        "Please ensure transport exception is " +
                        "thrown from operation.";
            } else {
                msg = name + '.' + op.m.getName() +
                        ": Cannot determine end time! " +
                        "DriverContext.recordTime() not called " +
                        "after critical section in operation.";

            }
            logger.severe(msg);
            agent.abortRun();
            throw new FatalException(msg);
        } else if (timingInfo.respondTime == TIME_NOT_SET) {
            timingInfo.respondTime = timingInfo.lastRespondTime;
            logger.fine("Potential open request in operation " +
                    op.m.getName() + ".");
        }
    }

    /**
     * Checks whether the last operation is in the ramp-up or ramp-down or
     * not. Updates the inRamp parameter accordingly.  This is only supposed
     * to be called from a subclass.
     */
    abstract void checkRamp();

    /**
     * Tests whether the last operation is in steady state or not. This is
     * called through the context from the driver from within the operation
     * so we need to be careful not to change run control parameters. This
     * method only reads the stats.
     * @return True if the last operation is in steady state, false otherwise.
     */
    abstract boolean isSteadyState();

    /**
     * Tests whether the time between start and end is in steady state or not.
     * For non time-based steady state, this will depend on the current cycle
     * count. Otherwise time is used.
     * @param start The start of a time span
     * @param end The end of a time span
     * @return true if this time span is in steady state, false otherwise.
     */
    abstract boolean isSteadyState(long start, long end);

    /**
     * Return results of this thread.
     * @return Final stats
     */
    public Metrics getResult() {
        metrics.wrap();
        return metrics;
    }

    /**
     * Triggers stopping and exiting of this thread.
     */
    public void stopExecution() {
        stopped = true;
        interrupt();
    }
}
