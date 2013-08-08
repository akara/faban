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

import com.sun.faban.common.RegistryLocator;
import com.sun.faban.common.Utilities;
import com.sun.faban.driver.util.PairwiseAggregator;
import com.sun.faban.driver.util.Timer;

import java.io.File;
import java.net.InetAddress;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * AgentImpl executes the driver threads of any given subtype.
 * It receives commands from the Master.
 * The AgentImpl is responsible for spawning and managing
 * threads, synchronizing between the threads and aggregating
 * the stats from all threads.
 *
 * @author Shanti Subramanyam
 * @see com.sun.faban.driver.engine.Agent
 * @see com.sun.faban.driver.engine.AgentThread
 */
public class AgentImpl extends UnicastRemoteObject
        implements Agent, Unreferenced, Runnable {

	private static final long serialVersionUID = 1L;
	static AgentImpl agentImpl;
    Master master;
    RunInfo runInfo;
    int driverType;
    Timer timer;
    AgentThread[] agentThreads;
    String agentType;
    int numThreads;
    String driverBase;
    String host;
    private Logger logger;
    private String agentName;
    private String agentId;
    private String displayName;
    private String className;
    long startTime = Long.MIN_VALUE;
    CountDownLatch threadStartLatch;
    CountDownLatch timeSetLatch;
    CountDownLatch preRunLatch;
    CountDownLatch startLatch;
    CountDownLatch finishLatch;
    CountDownLatch postRunLatch;
    private boolean runAborted = false;
    StatsCollector statsCollector;

    // Time to wake up and switch the number of active threads.
    volatile long loadSwitchTime = 1l;
    // Running threads at given load level.
    // All threads should run at start.
    volatile int runningThreads = Integer.MAX_VALUE;

    VariableLoadHandlerThread threadController;
    private long earliestStartTime = Long.MIN_VALUE;


    /**
     * Constructs the AgentImpl object.
     * @param driverName The type name of the driver to execute
     * @param agentId The id of this agent, unique among agents of same driver
     * @param master The master system
     * @throws Exception Any issue that may occur as part of the construction
     */
    AgentImpl(String driverName, String agentId, String master)
            throws Exception {
        this (driverName, agentId);

        host = InetAddress.getLocalHost().getHostName();

        // Sometimes we get the host name with the whole domain baggage.
        // The host name is widely used in result files, tools, etc. We
        // do not want that baggage. So we make sure to crop it off.
        // i.e. brazilian.sfbay.Sun.COM should just show as brazilian.
        int dotIdx = host.indexOf('.');
        if (dotIdx > 0)
            host = host.substring(0, dotIdx);

        RegistryLocator.getRegistry(master).
                reregister(agentType, agentName, this);

        logger.fine(displayName + " started ...");
    }


    AgentImpl(String driverName, String agentId) throws RemoteException {
        className = getClass().getName();
        logger = Logger.getLogger(className);
        agentType = driverName + "Agent";
        agentName = driverName + '.' + agentId;
        displayName = agentType + '[' + agentId + ']';
        this.agentId = agentId;
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    public void uncaughtException(Thread t, Throwable e) {
                        logger.log(Level.SEVERE, t.getName() + ": " +
                                e.getMessage(), e);
                    }
                }
        );
    }

    /**
     * Configures each agents with the properties passed.
     * @param master the remote interface to the Master
     * @param runInfo run information passed by Master
     * @param driverType 
     * @param timer BenchmarkDefinition Start time
     * @throws RemoteException 
     */
    public void configure(Master master, RunInfo runInfo, int driverType,
                          Timer timer) throws RemoteException {

        runAborted = false;
        this.master = master;
        this.runInfo = runInfo;
        this.driverType = driverType;
        this.timer = timer;

        // driverBase most accurate if faban.driver.base is provided.
        driverBase = System.getProperty("faban.driver.base");
        if (driverBase == null) {
            File driverJar = Utilities.getJarFile(
                                    runInfo.driverConfig.driverClass);
            if (driverJar != null)
                // Else find with jarpath/..
                driverBase = driverJar.getParentFile().getParent();
        }

        threadStartLatch = new CountDownLatch(runInfo.agentInfo.threads);
        timeSetLatch = new CountDownLatch(1);
        if (runInfo.agentInfo.startThreadNumber == 0) { // first agent
            if (runInfo.driverConfig.preRun != null) {
				preRunLatch = new CountDownLatch(1);
                startLatch = new CountDownLatch(1);
			}
            if (runInfo.driverConfig.postRun != null) {
                finishLatch = new CountDownLatch(1);
				postRunLatch = new CountDownLatch(1);
			}
        }
        try {
            runInfo.postDeserialize();
        } catch (ClassNotFoundException e) {
            logger.severe(e.getMessage());
            logger.throwing(className, "configure", e);
            master.abortRun();
        }

        runInfo.agentInfo.agentType = agentType;
        doPreRun();
    }

    /**
     * Start all the driver threads.
     */
    public void startThreads() {
        // Agents will Start the threads in parallel if
        // parallelThreadStart is set.
        if (runInfo.parallelAgentThreadStart) {
            // start thread and return
            new Thread(this).start();
        } else {
            // block until done and return
            this.run();
        }
    }

    /**
     * Calibrates the time with the master.
     * @throws RemoteException A network error occurred.
     */
    private void calibrateTime() throws RemoteException {
        int tries = 25;
        int minLatency = Integer.MAX_VALUE;
        int offset = 0;

        // Just in case we run into GC situations, the times can be nasty.
        // So we try 25 times to obtain the best.
        for (int i = 0; i < tries; i++) {

            // We really don't know when between t1 and t2 tm occurred.
            // but if we assume it to be short enough, we can savely
            // use the avg.
            long t1 = System.currentTimeMillis();
            long tm = master.currentTimeMillis();
            long t2 = System.currentTimeMillis();

            int latency  = (int) (t2 - t1);

            // Capture the best latency.
            if (latency < minLatency) {
                minLatency = latency;
                offset = (int) (t1 - tm);
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            	logger.fine("Sleep Interrupted: " + e.getMessage());
            }
        }

        if (minLatency > 10) {
            logger.log(Level.SEVERE, "This run may be invalid! Minimum " +
                    "achieved roundtrip time to master is " + minLatency +
                    " ms. Latencies beyond 10ms are too high and will impact " +
                    "the accuracy of the run startup and steady state times " +
                    "across driver instances.");
        }

        timer.adjustBaseTime(offset + minLatency / 2);
    }

    private void doPreRun() {
        numThreads = runInfo.agentInfo.threads;
        agentThreads = new AgentThread[numThreads];
        try {
            if (runInfo.agentInfo.startThreadNumber == 0 &&
                    runInfo.driverConfig.preRun != null) {
                agentThreads[0] = AgentThread.getInstance(agentType, agentId,
                        0, runInfo.driverConfig.driverClass, timer,
                        this);
                agentThreads[0].start();
                preRunLatch.await();
                preRunLatch = null;

                earliestStartTime = System.nanoTime() +
                        runInfo.msBetweenThreadStart * 1000000L;
            }
            if (runAborted) {
                logger.warning(displayName +
                        ": Run aborted starting initial driver threads.");
            }
        } catch (Exception e) {
            logger.warning(displayName +
                    ": Run aborted starting initial driver threads.");
            logger.log(Level.SEVERE, e.getMessage(), e);
            try {
                master.abortRun();
            } catch (RemoteException e1) {
                logger.log(Level.FINE, e1.getMessage(), e);
            }
        }
    }

    /**
     * Starts the driver threads for this agent, possibly in it's own thread.
     * @see java.lang.Runnable#run()
     */
    public void run() {

        timer.idleTimerCheck(displayName);

        // Create the required number of threads
        long nsBetweenThreadStart = runInfo.msBetweenThreadStart * 1000000L;
        try {
            // We use System.nanoTime() here directly
            // instead of timer.getTime().
            long baseTime = System.nanoTime();
            int count = 0;

            // First thread already started if preRun is there
            if (runInfo.agentInfo.startThreadNumber == 0 &&
                    runInfo.driverConfig.preRun != null) {
                // First thread already started, just let it run.
                startLatch.countDown();
                if (System.nanoTime() < earliestStartTime)
                    timer.wakeupAt(earliestStartTime);
                ++count;
            }

            calibrateTime();

            for (; count < numThreads && !runAborted; count++) {
                int globalThreadId = runInfo.agentInfo.startThreadNumber +
                        count;
                agentThreads[count] = AgentThread.getInstance(agentType,
                        agentId, globalThreadId,
                        runInfo.driverConfig.driverClass, timer, this);
                agentThreads[count].start();

                // We ensure we catch up with the configured thread starting
                // rate. If we fall short, we sleep less until we caught up.
                long wakeupTime = nsBetweenThreadStart * (count + 1) +
                        baseTime;
                long sleepTime = wakeupTime - System.nanoTime();

                // In case we fall short, we sleep only 1/3 the interval
                if (sleepTime <= 0) {
					sleepTime = nsBetweenThreadStart / 3;
                    wakeupTime = System.nanoTime() + sleepTime;
				}

                timer.wakeupAt(wakeupTime);
            }
            if (runAborted) {
				logger.warning(displayName + ": Run aborted before starting " +
                        numThreads + " driver threads.\n" + count +
                        " threads were started.");
			} else {
				logger.info(displayName + ": Successfully started " +
                        numThreads + " driver threads.");
			}
            if (runInfo.variableLoad) {
                runInfo.variableLoadHandler = new VariableLoadHandler(
                        runInfo.driverConfig.variableLoadFile);
                threadController = new VariableLoadHandlerThread(this);
                threadController.start();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            try {
                master.abortRun();
            } catch (RemoteException e1) {
            	logger.log(Level.FINE, e1.getMessage(), e);
            }
        }
    }

    /**
     * Obtains the id of this agent.
     * @return The id of this agent.
     */
    public int getId() {
        return Integer.parseInt(agentId);
    }

    /**
     * Contacts the master to abort the run. Don't kill until
     * master calls kill.
     */
    public synchronized void abortRun() {
        if (runAborted) {
			return;     // the master again. Once per agent is enough.
		}
        runAborted = true;
        if (startLatch != null) 
            try {
                startLatch.countDown();
            } catch (Exception e) {
                // Just make sure we don't get stuck here.
            }

        if (postRunLatch != null)
            try {
                postRunLatch.countDown();
            } catch (Exception e) {
                // Just make sure we don't get stuck here.
            }
        if (preRunLatch != null)
            try {
                preRunLatch.countDown();
            } catch (Exception e) {
                // Just make sure we don't get stuck here.
            }
        
        try {
            master.abortRun();
        } catch (RemoteException e) {
            logger.log(Level.SEVERE, "Error contacting master to abort run.",
                    e);
        }
    }

    /**
     * Wait until all threads are started.
     */
    public void waitForThreadStart() {
        if (!runAborted) {
			try {
                threadStartLatch.await();
            } catch (InterruptedException e) {
            	logger.log(Level.FINE, e.getMessage(), e);
            }
		}
    }

    /**
     * Sets the actual run start time.
     * @param time The relative millisec time of the benchmark start
     */
    public void setStartTime(int time) {
        runInfo.benchStartTime = time;
        startTime = timer.toAbsNanos(time);
        runInfo.start = timer.toAbsMillis(time);
        timeSetLatch.countDown();
        if (runInfo.runtimeStatsEnabled) {
            statsCollector = new StatsCollector();
        }

        // After we know the start time, we calibrate
        // the timer during the ramp up.
        timer.calibrate(displayName, startTime, 
                        startTime + runInfo.rampUp * 1000000000l);
    }
    
    /**
     * This method kills off the current run.
     * It terminates all threads.
     */
    public synchronized void kill() {
        runAborted = true;
        logger.warning(displayName + ": Killing benchmark run");
        for (int i = 0; i < numThreads; i++) {
			if (agentThreads[i] != null && agentThreads[i].isAlive()) {
				try {
                    agentThreads[i].stopExecution();
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, agentThreads[i].name +
                            ": Error killing thread.", t);
                }
			}
		}
        // cleanup
        if (statsCollector != null)
            statsCollector.cancel();
    }

    /**
     * Terminates all leftover threads remaining at the end of the run.
     * Logs the stack trace for all these threads but does not actually
     * wait for the threads to terminate (join). Terminate is called
     * while join is hanging on some thread that refuses to terminate.
     */
    public synchronized void terminate() {
        boolean terminationLogged = false;
        int terminationCount = 0;
        Throwable t = null;
        for (int i = numThreads - 1; i > 0; i--) {
			if (agentThreads[i] != null && agentThreads[i].isAlive()) {
				try {
                    if (!terminationLogged) { // Log this only once.
                        logger.warning(displayName +
                                ": Forcefully terminating benchmark run");
                        terminationLogged = true;
                    }
                    t = new Throwable(
                            "Stack of non-terminating thread.");
                    t.setStackTrace(agentThreads[i].getStackTrace());
                    logger.log(Level.FINE, agentThreads[i].name +
                            ": Thread not Terminated. " +
                            "Dumping stack and force termination.", t);
                    ++terminationCount;
                    agentThreads[i].stopExecution();
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, agentThreads[i].name +
                            ": Error killing thread.", e);
                }
			}
		}

        if (runInfo.agentInfo.startThreadNumber == 0 &&
                runInfo.driverConfig.postRun != null &&
                agentThreads[0] != null) {
            if (agentThreads[0].getThreadState() ==
                    AgentThread.RunState.RUNNING) {
                try {
                    if (!terminationLogged) { // Log this only once.
                        logger.warning(displayName +
                                ": Forcefully terminating benchmark run");
                        terminationLogged = true;
                    }
                    t = new Throwable(
                            "Stack of non-terminating thread.");
                    t.setStackTrace(agentThreads[0].getStackTrace());
                    logger.log(Level.FINE, agentThreads[0].name +
                            ": Thread not Terminated. " +
                            "Dumping stack and force termination.", t);
                    ++terminationCount;
                    agentThreads[0].stopExecution();
                } catch (Throwable e) {
                    logger.log(Level.SEVERE, agentThreads[0].name +
                            ": Error killing thread.", e);
                }
            }
            try {
                finishLatch.await();
            } catch (InterruptedException e) {
                logger.warning(
                        "Interrupted waiting for thread 0 to finish run. " +
                        "PostRun may not get executed.");
            }
        } else if (agentThreads[0] != null && agentThreads[0].isAlive()) {
            try { // Just terminate it like any other thread.
                if (!terminationLogged) { // Log this only once.
                    logger.warning(displayName +
                            ": Forcefully terminating benchmark run");
                    terminationLogged = true;
                }
                t = new Throwable(
                        "Stack of non-terminating thread.");
                t.setStackTrace(agentThreads[0].getStackTrace());
                logger.log(Level.FINE, agentThreads[0].name +
                        ": Thread not Terminated. " +
                        "Dumping stack and force termination.", t);
                ++terminationCount;
                agentThreads[0].stopExecution();
            } catch (Throwable e) {
                logger.log(Level.SEVERE, agentThreads[0].name +
                        ": Error killing thread.", e);
            }
        }
        if (terminationCount > 0) {
			logger.log(Level.WARNING, displayName + ": " + terminationCount +
                           " threads forcefully terminated.", t);
		}
        if (statsCollector != null)
            statsCollector.cancel();
    }

    private class MetricsProvider implements
            PairwiseAggregator.Provider<Metrics> {

        public Metrics getMutableMetrics(int idx) {
            return (Metrics) agentThreads[idx].getResult().clone();
        }

        public void add(Metrics instance, int idx) {
            instance.add(agentThreads[idx].getResult());
        }

        public Class<Metrics> getComponentClass() {
            return Metrics.class;
        }

        public void recycle(Metrics metrics) {
        }
    }

    /**
     * Report stats from a run
     * Each thread's result is obtained by calling that thread's getResult()
     * All these results are then aggregated by calling one of the
     * thread's getAggregateResult method.
     * @return results
     */
    public Metrics getResults() {
        PairwiseAggregator<Metrics> aggregator = new
                PairwiseAggregator<Metrics>(numThreads, new MetricsProvider());
        return aggregator.collectStats();
    }

    /**
     * Waits for all the threads to terminate.
     */
    public void join() {
        for (int i = agentThreads.length - 1; i > 0; i--) {
			while(agentThreads[i] != null && agentThreads[i].isAlive()) {
				try {
                    agentThreads[i].join();
                } catch (InterruptedException e) {
                	logger.log(Level.FINE, e.getMessage(), e);
                }
			}
		}
        if (runInfo.agentInfo.startThreadNumber == 0 &&
                runInfo.driverConfig.postRun != null &&
                agentThreads[0] != null) {// first agent, postRun
            try {
                finishLatch.await();
            } catch (InterruptedException e) {
            	logger.log(Level.FINE, e.getMessage(), e);
            }            
        } else if (agentThreads[0] != null && agentThreads[0].isAlive()) {
            try {
                agentThreads[0].join();
            } catch (InterruptedException e) {
            	logger.log(Level.FINE, e.getMessage(), e);
            }
        }
        if (statsCollector != null)
            statsCollector.cancel();
    }

    /**
     * Invokes the post run method on thread 0 of each driver agent 0, if
     * postRun is configured.
     */
    public void postRun() {
        if (runInfo.agentInfo.startThreadNumber == 0 &&
                runInfo.driverConfig.postRun != null &&
                agentThreads[0] != null) {// first agent, postRun
            logger.finest(agentType + "Releasing postRun latch.");
            postRunLatch.countDown();
            try {
                agentThreads[0].join();
                logger.finest(agentType + " Thread 0 completed postRun");
            } catch (InterruptedException e) {
                logger.warning(agentType +
                        " Interrupted waiting for thread 0 to finish postRun.");
            }
        }
        master = null;
    }

    /**
     * When this instance is unreferenced the application must exit.
     */
    public void unreferenced() {
        logger.warning(displayName + ": unreferenced() called!");
        if (statsCollector != null)
            statsCollector.cancel();
        // Seems like there is a bug in JDK1.5 and unreferenced is called
        // sporadically. So it is better we do not really kill it.
        /*
        kill();
        join();
        */
    }

    /**
     * AgentImpl's main method.
     * @param argv the arguments passed to the java command
     */
    public static void main(String [] argv) {

        //		LocateRegistry.createRegistry();
        System.setSecurityManager (new RMISecurityManager());
        if (argv.length != 3) {
            System.err.println("Usage: AgentImpl" +
                    " <driverName> <agentId> <masterMachine>");
            System.exit(-1);
        }
        String driverName = argv[0];
        String agentId = argv[1];
        String master = argv[2];

        // This needs to be set by the driver code instead
        // URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory());

        try {
            agentImpl = new AgentImpl(driverName, agentId, master);
        } catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private class RuntimeMetricsProvider
            implements PairwiseAggregator.Provider<RuntimeMetrics> {

        public ArrayList<RuntimeMetrics> pool = new ArrayList<RuntimeMetrics>();

        public RuntimeMetrics getMutableMetrics(int idx) {

            RuntimeMetrics rtm;
            int size = pool.size();
            if (size > 0) {
                rtm = pool.remove(size - 1);
            } else {
                rtm = new RuntimeMetrics();
            }
            rtm.copy(agentThreads[idx].metrics);
            return rtm;
        }

        public void add(RuntimeMetrics instance, int idx) {
            instance.add(agentThreads[idx].metrics);
        }

        public Class getComponentClass() {
            return RuntimeMetrics.class;
        }

        public void recycle(RuntimeMetrics r) {
            pool.add(r);
        }
    }


    private class StatsCollector extends Thread {

        long interval = runInfo.runtimeStatsInterval * 1000000000l;
        PairwiseAggregator<RuntimeMetrics> aggregator =
                new PairwiseAggregator<RuntimeMetrics>(
                agentThreads.length, new RuntimeMetricsProvider());
        boolean terminated = false;

        StatsCollector() {
            setName("StatsCollector");
            setDaemon(true);
            start();
        }

        @Override
        public void run() {
            int sequence = 0;
            long duration = (runInfo.rampUp + runInfo.stdyState +
                                runInfo.rampDown) * 1000000000l;
            RuntimeMetrics rtm = null;
            while (!terminated) {
                long wakeupTime = startTime + sequence * interval;
                if (wakeupTime > startTime + duration)
                    break;
                try {
                    timer.wakeupAt(wakeupTime);
                    rtm = aggregator.collectStats();
                    if (rtm == null)
                        logger.warning("Null RuntimeStats");
                    try {
                        rtm.timestamp = (int) ((System.nanoTime() - startTime) /
                                1000000l);
                        rtm.sequence = sequence;
                        master.updateMetrics(rtm);
                    } catch (RemoteException e) {
                        logger.log(Level.SEVERE, "Communication error " +
                                "sending runtime metrics to master", e);
                    }
                    ++sequence;
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }

        void cancel() {
            terminated = true;
            interrupt();
        }
    }
}
