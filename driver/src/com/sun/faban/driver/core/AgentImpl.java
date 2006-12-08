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
 * $Id: AgentImpl.java,v 1.4 2006/12/08 05:15:54 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.core;

import com.sun.faban.common.RegistryLocator;
import com.sun.faban.driver.util.Timer;

import java.io.Serializable;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
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
 * @see com.sun.faban.driver.core.Agent
 * @see com.sun.faban.driver.core.AgentThread
 */
public class AgentImpl extends UnicastRemoteObject
        implements Agent, Unreferenced, Runnable {

    static AgentImpl agentImpl;
    Master master;
    RunInfo runInfo;
    int driverType;
    Timer timer;
    AgentThread[] agentThreads;
    String agentType;
    Metrics results[] = null;
    int numThreads;
    private Logger logger;
    private String agentName;
    private String agentId;
    private String displayName;
    private String className;
    CountDownLatch threadStartLatch;
    CountDownLatch timeSetLatch;
    CountDownLatch preRunLatch;
    CountDownLatch postRunLatch;
    private boolean runAborted = false;

    /**
     * Constructor
     * Create properties object from file
     */
    AgentImpl(String driverName, String agentId, String master)
            throws Exception {
        this (driverName, agentId);

        RegistryLocator.getRegistry(master).
                        register(agentType, agentName, this);

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
     * Configures each agents with the props passed
     * The threads are created at this point
     * @param master the remote interface to the Master
     * @param runInfo run information passed by Master
     * @param timer BenchmarkDefinition Start time
     */
    public void configure(Master master, RunInfo runInfo, int driverType,
                          Timer timer) throws RemoteException {

        runAborted = false;
        this.master = master;
        this.runInfo = runInfo;
        this.driverType = driverType;
        this.timer = timer;
        threadStartLatch = new CountDownLatch(runInfo.agentInfo.threads);
        timeSetLatch = new CountDownLatch(1);
        if (runInfo.agentInfo.startThreadNumber == 0) { // first agent
            if (runInfo.driverConfig.preRun != null)
                preRunLatch = new CountDownLatch(1);
            if (runInfo.driverConfig.postRun != null)
                postRunLatch = new CountDownLatch(1);
        }
        try {
            runInfo.postDeserialize();
        } catch (ClassNotFoundException e) {
            logger.severe(e.getMessage());
            logger.throwing(className, "configure", e);
            master.abortRun();
        }

        runInfo.agentInfo.agentType = agentType;
        results = null;		// so that we don't use old results
        calibrateTime();

        // Agents will Start the threads in parallel if
        // parallelThreadStart is set.
        if (runInfo.parallelAgentThreadStart) {
            // start thread and return
            new Thread(this).start();
            if (runInfo.agentInfo.startThreadNumber == 0 &&
                    runInfo.driverConfig.preRun != null)
                try {
                    preRunLatch.await();
                } catch (InterruptedException e) {
                    // Do nothing.
                }
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
        long t1 = 0;
        long tm = 0;
        int diff = 0;
        int retries = 0;

        // Just in case we run into GC situations, the times can be nasty.
        // In that case we retry up to 5 times.
        for (; retries < 5; retries++) {
            t1 = System.currentTimeMillis();
            tm = master.currentTimeMillis();
            long t2 = System.currentTimeMillis();

            // We really don't know when between t1 and t2 tm occurred.
            // but if we assume it to be short enough, we can savely
            // use the avg with errors in the 5 millisec range.
            diff  = (int) (t2 - t1);
            if (diff <= 10)
                break;
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
        }

        // After 5 retries, if the simple call to Master.currentTimeMillis
        // is still too time consuming, we abort the whole thing.
        if (retries >= 5) {
                logger.log(Level.SEVERE, "Roundtrip to master took " + diff +
                        " secs. Too high latency!");
                master.abortRun();
        }

        // Doing a simple avg on millis can overwhelm the long system.
        // So we subtract first.
        int offset = diff / 2 + ((int) (t1 - tm));
        timer.adjustBaseTime(offset);
    }

    public void run() {
        // Create the required number of threads
        numThreads = runInfo.agentInfo.threads;
        agentThreads = new AgentThread[numThreads];
        try {
            int baseTime = timer.getTime();
            int count = 0;
            for (; count < numThreads && !runAborted; count++) {
                int globalThreadId = runInfo.agentInfo.startThreadNumber +
                        count;
                agentThreads[count] = AgentThread.getInstance(agentType,
                        agentId, globalThreadId,
                        runInfo.driverConfig.driverClass, timer, this);
                agentThreads[count].start();

                // We ensure we catch up with the configured thread starting
                // rate. If we fall short, we sleep less until we caught up.
                int sleepTime = runInfo.msBetweenThreadStart * (count + 1) +
                        baseTime - timer.getTime();

                // In case we fall short, we sleep only 1/3 the interval
                if (sleepTime <= 0)
                    sleepTime = runInfo.msBetweenThreadStart / 3;

                // If the configured time is low, we can still end up
                // with 0 on an integer operation, so we just check again.
                if (sleepTime > 0)
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ie) {
                    }
                if (globalThreadId == 0 &&  // Make sure preRun is done
                        runInfo.driverConfig.preRun != null)
                    preRunLatch.await();
            }
            if (runAborted)
                logger.warning(displayName + ": Run aborted before starting " +
                        numThreads + " driver threads.\n" + count +
                        " threads were started.");
            else
                logger.info(displayName + ": Successfully started " +
                        numThreads + " driver threads.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            try {
                master.abortRun();
            } catch (RemoteException e1) {
            }
        }
    }

    /**
     * Contacts the master to abort the run. Don't kill until
     * master calls kill.
     */
    public synchronized void abortRun() {
        if (runAborted) // If it is already aborted, we do not need to call
            return;     // the master again. Once per agent is enough.
        runAborted = true;
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
        if (!runAborted)
            try {
                threadStartLatch.await();
            } catch (InterruptedException e) {
            }
    }

    /**
     * Sets the actual run start time.
     * @param time The relative time of the benchmark start
     */
    public void setStartTime(int time) {
        runInfo.benchStartTime = time;
        runInfo.start = time + timer.getOffsetTime();        
        timeSetLatch.countDown();

        // After we know the start time, we calibrate
        // the timer during the rampup.
        timer.calibrate(displayName,
                runInfo.benchStartTime + runInfo.rampUp * 1000);
    }

    /**
     * This method kills off the current run.
     * It terminates all threads.
     */
    public synchronized void kill() {
        runAborted = true;
        logger.warning(displayName + ": Killing benchmark run");
        for (int i = 0; i < numThreads; i++)
            if (agentThreads[i] != null && agentThreads[i].isAlive())
                try {
                    agentThreads[i].stopExecution();
                } catch (Throwable t) {
                    logger.log(Level.SEVERE, agentThreads[i].name +
                            ": Error killing thread.", t);
                }
        // cleanup
        results = null;
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
        for (int i = numThreads - 1; i > 0; i--)
            if (agentThreads[i] != null && agentThreads[i].isAlive())
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

        if (runInfo.agentInfo.startThreadNumber == 0 &&
                runInfo.driverConfig.postRun != null &&
                agentThreads[0] != null) {
            if (agentThreads[0].getThreadState() == AgentThread.RUNNING) {
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
            postRunLatch.countDown();
            agentThreads[0].waitThreadState(AgentThread.ENDED);
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
        if (terminationCount > 0)
            logger.log(Level.WARNING, displayName + ": " + terminationCount +
                           " threads forcefully terminated.", t);
    }

    /**
     * Report stats from a run
     * Each thread's result is obtained by calling that thread's getResult()
     * All these results are then aggregated by calling one of the
     * thread's getAggregateResult method.
     */
    public Serializable getResults() {
        Metrics[] results = new Metrics[numThreads];
        for (int i = 0; i < numThreads; i++) {
            results[i] = agentThreads[i].getResult();
        }
        Metrics agentStats = (Metrics) results[0].clone();
        for (int index = 1; index < results.length; index++)
            agentStats.add(results[index]);
        return(agentStats);
    }

    /**
     * Waits for all the threads to terminate.
     */
    public void join() {
        for (int i = agentThreads.length - 1; i > 0; i--)
            while(agentThreads[i] != null && agentThreads[i].isAlive())
                try {
                    agentThreads[i].join();
                } catch (InterruptedException e) {
                }
        if (runInfo.agentInfo.startThreadNumber == 0 &&
                runInfo.driverConfig.postRun != null &&
                agentThreads[0] != null) {// first agent, preRun
            postRunLatch.countDown();
            try {
                agentThreads[0].join();
            } catch (InterruptedException e) {
            }            
        } else if (agentThreads[0] != null && agentThreads[0].isAlive()) {
            try {
                agentThreads[0].join();
            } catch (InterruptedException e) {
            }
        }
        master = null;
    }

    /**
     * When this instance is unreferenced the application must exit.
     */
    public void unreferenced() {
        logger.warning(displayName + ": unreferenced() called!");
        // Seems like there is a bug in JDK1.5 and unreferenced is called
        // sporadically. So it is better we do not really kill it.
        /*
        kill();
        join();
        */
    }

    /**
     * AgentImpl's main method
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

    /*
    static class ContextFactory implements TimeRecorderFactory {
        public TimeRecorder getTimeRecorder() {
            return DriverContext.getContext();
        }
    }
    */
}
