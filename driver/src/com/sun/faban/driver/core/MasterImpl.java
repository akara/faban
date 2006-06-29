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
 * $Id: MasterImpl.java,v 1.1 2006/06/29 18:51:33 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.core;

import com.sun.faban.common.Registry;
import com.sun.faban.common.RegistryLocator;
import com.sun.faban.driver.ConfigurationException;
import com.sun.faban.driver.FatalException;
import com.sun.faban.driver.RunControl;
import com.sun.faban.driver.util.PlotServer;
import com.sun.faban.driver.util.Timer;

import java.io.*;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.Formatter;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This is the main Master class for running a Faban driver.
 * The Master is instantiated on the <b>master machine</b> by the
 * user wishing to run a benchmark. It is responsible for co-ordinating
 * the work of all the Agents, setting up the benchmark test,
 * collecting the results etc.
 * NOTE: The registry and agents must have been brought up before
 * starting the driver. The driver will fail otherwise.
 *
 * @see        com.sun.faban.driver.core.Agent
 * @see        com.sun.faban.common.Registry
 *
 */
public class MasterImpl extends UnicastRemoteObject implements Master {

    // This field is a legal requirement and serves no other purpose.
    static final String COPYRIGHT =
            "Copyright \251 2006 Sun Microsystems, Inc., 4150 Network Circle, " +
            "Santa Clara, California 95054, U.S.A. All rights reserved.\n" +
            "U.S. Government Rights - Commercial software.  Government users " +
            "are subject to the Sun Microsystems, Inc. standard license " +
            "agreement and applicable provisions of the FAR and its " +
            "supplements.\n" +
            "Use is subject to license terms.\n" +
            "This distribution may include materials developed by third " +
            "parties.\n" +
            "Sun,  Sun Microsystems,  the Sun logo and  Java are trademarks " +
            "or registered trademarks of Sun Microsystems, Inc. in the U.S. " +
            "and other countries.\n" +
            "Apache is a trademark of The Apache Software Foundation, and is " +
            "used with permission.\n" +
            "This product is covered and controlled by U.S. Export Control " +
            "laws and may be subject to the export or import laws in other " +
            "countries.  Nuclear, missile, chemical biological weapons or " +
            "nuclear maritime end uses or end users, whether direct or " +
            "indirect, are strictly prohibited.  Export or reexport to " +
            "countries subject to U.S. embargo or to entities identified on " +
            "U.S. export exclusion lists, including, but not limited to, the " +
            "denied persons and specially designated nationals lists is " +
            "strictly prohibited.\n" +
            "\n" +
            "Copyright \251 2006 Sun Microsystems, Inc., 4150 Network Circle, " +
            "Santa Clara, California 95054, Etats-Unis. Tous droits " +
            "r\351serv\351s.\n" +
            "L'utilisation est soumise aux termes de la Licence.\n" +
            "Cette distribution peut comprendre des composants " +
            "d\351velopp\351s par des tierces parties.\n" +
            "Sun,  Sun Microsystems,  le logo Sun et  Java sont des marques " +
            "de fabrique ou des marques d\351pos\351es de " +
            "Sun Microsystems, Inc. aux Etats-Unis et dans d'autres pays.\n" +
            "Apache est une marque d’Apache Software Foundation, utilis\351e " +
            "avec leur permission.\n" +
            "Ce produit est soumis \340 la l\351gislation am\351ricaine " +
            "en mati\350re de contr\364le des exportations et peut \352tre " +
            "soumis \340 la r\350glementation en vigueur dans d'autres pays " +
            "dans le domaine des exportations et importations. Les " +
            "utilisations, ou utilisateurs finaux, pour des armes " +
            "nucl\351aires, des missiles, des armes biologiques et chimiques " +
            "ou du nucl\351aire maritime, directement ou indirectement, sont " +
            "strictement interdites. Les exportations ou r\351exportations " +
            "vers les pays sous embargo am\351ricain, ou vers des entit\351s " +
            "figurant sur les listes d'exclusion d'exportation " +
            "am\351ricaines, y compris, mais de mani\350re non exhaustive, " +
            "la liste de personnes qui font objet d'un ordre de ne pas " +
            "participer, d'une fa\347on directe ou indirecte, aux " +
            "exportations des produits ou des services qui sont r\351gis par " +
            "la l\351gislation am\351ricaine en mati\350re de contr\364le " +
            "des exportations et la liste de ressortissants sp\351cifiquement " +
            "d\351sign\351s, sont rigoureusement interdites.\n";

    private String className = getClass().getName();
    protected Logger logger = Logger.getLogger(className);

    BenchmarkDefinition benchDef;

    /** Remote references for each agentImpl of each driver type. */
    protected Agent[][] agentRefs;
    // Note: first dimension is the driver type

    /** Threads required per agentImpl for each driver. */
    protected int[] agentThreads;

    /** Remaining threads to be distributed to the first agents. */
    protected int[] remainderThreads;

    /** The RunInfo structure */
    protected RunInfo runInfo;

    protected Timer timer; // The time recorder.
    protected static String homeDir = System.getProperty("user.home");
    protected static String fs = System.getProperty("file.separator");

    private boolean runAborted = false;

    protected java.util.Timer scheduler;

    /**
     * Creates and exports a new Master
     *
     * @throws java.rmi.RemoteException if failed to export object
     */
    protected MasterImpl() throws RemoteException {
        super();
    }

    /**
     * Runs the benchmark from begin to end.
     * @throws Exception Any error that had occurred during the run.
     */
    public void runBenchmark() throws Exception {

        // Read the benchmark definition from the defining class
        benchDef = BenchmarkDefinition.read(RunInfo.getDefiningClassName());

        // Get the runInfo
        runInfo = RunInfo.read(benchDef);

        // When run form the harness, the outputdir may be the runId.
        // In that case the faban.outputdir.unique property must be set to true.
        boolean uniqueDir = false;
        String uniqueDirString = System.getProperty("faban.outputdir.unique");
        if (uniqueDirString != null)
            uniqueDir = RunInfo.ConfigurationReader.
                    relaxedParseBoolean(uniqueDirString);

        if (uniqueDir) {
            // Ensure separator is not at end.
            if (runInfo.resultsDir.endsWith(fs))
                runInfo.resultsDir = runInfo.resultsDir.substring(0,
                        runInfo.resultsDir.length() - fs.length());

            // Then take the innermost directory name.
            int idx = runInfo.resultsDir.lastIndexOf(fs);
            ++idx;
            runInfo.runId = runInfo.resultsDir.substring(idx);
        } else {
            // Gets the ID for this run from the sequence file.
            try {
                runInfo.runId = getRunID(true);
            } catch (Exception e) {
                logger.severe("Cannot read the run id");
                logger.throwing(className, "<init>", e);
                throw e;
            }
        }
        logger.info("RunID for this run is : " + runInfo.runId);

        String runOutputDir = runInfo.resultsDir;
        if (!uniqueDir)
            runOutputDir = runInfo.resultsDir + fs + runInfo.runId;

        // make a new directory for the run.
        File runDirFile = null;
        runDirFile = new File(runOutputDir);
        if ( !runDirFile.exists())
            if ( !runDirFile.mkdir())
                throw new IOException("Could not create the new " +
                        "Run Directory: " + runOutputDir);

        logger.info("Output directory for this run is : " + runOutputDir);
        runInfo.resultsDir = runOutputDir;

        configureLogger (runOutputDir);

        timer = new Timer();

        agentRefs = new Agent[benchDef.drivers.length][];
        agentThreads = new int[benchDef.drivers.length];
        remainderThreads = new int[benchDef.drivers.length];
        preRun();

        scheduler = new java.util.Timer("Scheduler", false);
        try {
            int agentCnt = configure();
            if (agentCnt > 0) {
                for (int i = 0; i < benchDef.drivers.length && !runAborted; i++)
                    configureAgents(i);
                logger.config("Detected " + agentCnt + " Remote Agents.");
            } else {
                configureLocal();
            }
        } catch (ConnectException e) {
            configureLocal();
        } catch (NotBoundException e) {
            configureLocal();
        } catch (RemoteException e) {
            Throwable t = e.getCause();
            Throwable tt;
            while ((tt = t.getCause()) != null)
                t = tt;
            logger.log(Level.WARNING,
                    "Error acccessing registry or agent!", t);
            configureLocal();
        }
        executeRun();
        postRun();
    }

    /**
     * Hook for subclass to define what needs to be done pre-run.
     * This implementation is a noop.
     * @exception Exception Signalling errors in the preRun
     */
    protected void preRun() throws Exception {
    }

    /**
     * Hook for subclass to define what needs to be done post-run.
     * This implementation is a noop.
     * @exception Exception Signalling errors in the postRun
     */
    protected void postRun() throws Exception {
    }

    /**
     * This method retrieves the ID for the current run, by looking
     * in the specappplatform.seq file in the user's home directory.
     * If the increment flag is set to true, the runId is incremented.
     * Also the file will be created if it does not exist. This shall
     * be done only maximum once in a run. Non-incrementing getRunID
     * may be called more than once from many processes.
     *
     * @param increment Whether the file shall be incremented or not
     */
    public String getRunID(boolean increment) throws IOException{
        int runID;

        String seqFileName = homeDir + fs +
                benchDef.name.toLowerCase() + ".seq";
        File seqFile = new File(seqFileName);
        if (seqFile.exists()) {
            FileReader bufIn = null;
            char[] buffer = new char[64];
            int length = 0;
            try {
                bufIn = new FileReader(seqFile);
            }
            catch (FileNotFoundException e) {
                logger.log(Level.SEVERE, "The sequence file '" + seqFile +
                        "' does not exist.", e);
                throw e;
            }
            try {
                length = bufIn.read(buffer);
                bufIn.close();
            }
            catch (IOException e) {
                logger.log(Level.SEVERE, "Could not read/close the sequence " +
                        "file " + seqFileName + '.', e);
                throw e;
            }
            // Strip off the newlines
            if (buffer[length - 1] == '\n')
                --length;
            if (buffer[length - 1] == '\r')
                --length;
            runID = Integer.parseInt(new String(buffer, 0, length));
        }
        else {
            if (increment) // Only create file in case we increment it
                try {
                    seqFile.createNewFile();
                }
                catch (IOException e) {
                    logger.log(Level.SEVERE, "Could not create the sequence " +
                            "file " + seqFileName + '.', e);
                    throw e;
                }
            runID = 1;
        }
        // Update the runid in the sequence file
        if (increment)
            try {
                FileWriter fileOut = new FileWriter(seqFileName);
                fileOut.write(Integer.toString(runID + 1));
                fileOut.close();
            }
            catch (IOException e) {
                logger.log(Level.SEVERE, "Could not write to the sequence file "
                        + seqFileName + '.', e);
                throw e;
            }
        return Integer.toString(runID);
    }

    private void configureLogger(String dir) {

        logger = Logger.getLogger("com.sun.faban.driver");
        FileHandler handler = null;
        try {
            handler = new FileHandler(dir + fs + "driver.log", true);
        } catch (IOException e) {
            logger.severe(e.getMessage());
            System.exit(1);
        }

        handler.setFormatter(new SimpleFormatter());
        handler.setLevel(Level.FINEST);
        logger.addHandler(handler);
        runInfo.logHandler = handler;
    }

    /**
     * Contacts the registry and gets references for all agents.
     * Then calculates the load distribution to each agentImpl.
     * @return The total number of agents configured
     */
    protected int configure() throws Exception {

        Registry registry = RegistryLocator.getRegistry();

        int totalAgentCnt = 0;

        // Get all agents for all drivers
        for (int i = 0; i < benchDef.drivers.length && !runAborted; i++) {
            // Only for drivers to run...
            if (runInfo.driverConfigs[i].numAgents != 0) {

                // Get all the agentImpl refs
                String agentName = benchDef.drivers[i].name + "Agent";
                Remote[] refs = registry.getServices(agentName);
                int agentCnt = 0;

                // Usually, the agents should have been started
                // according to the given number. But just in case
                // some of them did not get started or there might
                // be some others sneaking in...
                if (refs == null || (agentCnt = refs.length) == 0) {
                    // Hmmm, for some reason the agents didn't get started
                    if (runInfo.driverConfigs[i].numAgents > 0)
                        logger.warning("Cannot find " + agentName + "s. Not " +
                                "starting " + benchDef.drivers[i].name + '.');
                    runInfo.driverConfigs[i].numAgents = 0;
                    continue;
                }

                if (agentCnt != runInfo.driverConfigs[i].numAgents) {
                    if (runInfo.driverConfigs[i].numAgents > 0) {
                        logger.warning("Configured " + runInfo.driverConfigs[i].
                                numAgents + ' ' + benchDef.drivers[i].name +
                                "Agents but found " + agentCnt + '.');
                        if (agentCnt > runInfo.driverConfigs[i].numAgents)
                            logger.warning("Some unkown agents managed to " +
                                    "sneak in! We'll use'em!");
                        else
                            logger.warning("Some agents surely didn't get " +
                                   "started. We'll just use the ones we have.");
                    }

                    // Now we need to adjust the runInfo according to realty
                    runInfo.driverConfigs[i].numAgents = agentCnt;
                }

                // Now assign the agent refs to the global agent array.
                agentRefs[i] = new Agent[agentCnt];
                for (int j = 0; j < agentCnt; j++)
                    agentRefs[i][j] = (Agent) refs[j];

                // We need to calculate the thread counts
                if (runInfo.driverConfigs[i].numThreads == -1)
                    runInfo.driverConfigs[i].numThreads = runInfo.scale *
                            benchDef.drivers[i].threadPerScale;
                agentThreads[i] = runInfo.driverConfigs[i].numThreads /
                        runInfo.driverConfigs[i].numAgents;
                remainderThreads[i] = runInfo.driverConfigs[i].numThreads -
                        agentThreads[i] * runInfo.driverConfigs[i].numAgents;

                totalAgentCnt += agentCnt;
            }
        }
        return totalAgentCnt;
    }

    protected void configureLocal() throws Exception {
        int driverToRun = -1;
        if (runInfo.driverConfigs.length > 1) {
            for (int i = 0; i < runInfo.driverConfigs.length; i++)
                if (runInfo.driverConfigs[i].numAgents == 1) {
                    if (driverToRun == -1) {
                        driverToRun = i;
                    } else {
                        String msg = "Can only configure 1 agentImpl for " +
                                "local runs.\nDetected " + benchDef.
                                drivers[driverToRun].name + " and " +
                                benchDef.drivers[i].name + " set to run.";
                        throw new ConfigurationException(msg);
                    }
                } else if (runInfo.driverConfigs[i].numAgents > 1) {
                    String msg = "Can only configure 1 agentImpl for local " +
                            "runs.\n" + benchDef.drivers[i].name + " is set to " +
                            runInfo.driverConfigs[i].numAgents + " agents.";
                    throw new ConfigurationException(msg);
                }
        } else {
            driverToRun = 0;
            runInfo.driverConfigs[0].numAgents = 1;
        }

        if (driverToRun < 0)
            throw new ConfigurationException("No driver configured to run.");

        logger.config("Starting single, in-process " +
                      benchDef.drivers[driverToRun].name + "Agent.");

        // We need to calculate the thread counts
        if (runInfo.driverConfigs[driverToRun].numThreads == -1)
            runInfo.driverConfigs[driverToRun].numThreads = runInfo.
                    scale * benchDef.drivers[driverToRun].threadPerScale;
        agentThreads[driverToRun] =
                runInfo.driverConfigs[driverToRun].numThreads;

        RunInfo.AgentInfo agentInfo = new RunInfo.AgentInfo();
        runInfo.agentInfo = agentInfo;

        runInfo.driverConfig = runInfo.driverConfigs[driverToRun];
        agentRefs[driverToRun] = new Agent[1];
        agentRefs[driverToRun][0] =
                new AgentImpl(runInfo.driverConfig.name, "0");

        runInfo.agentInfo.agentNumber = 0;

        agentInfo.threads = this.agentThreads[driverToRun];
        agentInfo.agentScale = runInfo.scale;
        ((Agent) agentRefs[driverToRun][0]).configure(this, runInfo,
                driverToRun, timer);
    }

    /**
     * configureAgents()
     * Get a list of all the registered agents and parseProperties them
     */
    private void configureAgents(int driverType) throws Exception {

        int agentCnt = runInfo.driverConfigs[driverType].numAgents;
        if (agentCnt > 0) {
            RunInfo.AgentInfo agentInfo = new RunInfo.AgentInfo();
            runInfo.agentInfo = agentInfo;
            logger.config("num" + benchDef.drivers[driverType].name +
                        "Agents = " + agentCnt);

            agentInfo.threads = agentThreads[driverType];
            agentInfo.agentScale = (double) runInfo.scale/agentCnt;
            Remote[] refs = agentRefs[driverType];
            logger.info("Configuring " + refs.length + ' ' +
                        benchDef.drivers[driverType].name + "Agents...");

            runInfo.driverConfig = runInfo.driverConfigs[driverType];
            int agentId = 0;

            // If there are remaining threads left, distribute each to
            // the first agents. Ditto for scale
            if (remainderThreads[driverType] > 0) {
                agentInfo.threads = agentThreads[driverType] + 1;
                agentInfo.agentScale = (double) runInfo.scale *
                        runInfo.driverConfigs[driverType].numThreads /
                        agentInfo.threads;

                for (; agentId < remainderThreads[driverType]; agentId++) {
                    runInfo.agentInfo.agentNumber = agentId;
                    ((Agent)refs[agentId]).configure(this, runInfo,
                                                     driverType, timer);
                    runInfo.agentInfo.startThreadNumber += agentInfo.threads;
                }
            }

            // Now deal with the non-remainders...
            agentInfo.threads = agentThreads[driverType];
            agentInfo.agentScale = (double) runInfo.scale *
                    runInfo.driverConfigs[driverType].numThreads /
                    agentInfo.threads;

            for (; agentId < refs.length && !runAborted; agentId++) {
                runInfo.agentInfo.agentNumber = agentId;
                ((Agent)refs[agentId]).configure(this, runInfo, driverType, timer);
                runInfo.agentInfo.startThreadNumber += agentInfo.threads;
            }
        }
        runInfo.driverConfig = null;
        runInfo.agentInfo = null; // reset it so we don't use it anywhere else
    }

    /**
     * Tell the agents to start the run execution
     * Note that the Agent's run method call is non-blocking
     * i.e the Master does not wait for an Agent. Instead, we
     * wait for the total length of the run, after we signal
     * all the agents to start.
     */
    private void executeRun() throws Exception {
        StatsWriter sw = null;

        // Now wait for all threads to start if it is parallel.
        if (runInfo.parallelAgentThreadStart)
            waitForThreadStart();

        // Leave plenty of time to notify all agents of th start time.
        setStartTime(estimateCommsTime() + timer.getTime());

        int sleepTime = runInfo.benchStartTime - timer.getTime();
        if (sleepTime <= 0) {
            String msg = "Threads are not done initializing by start time.\n" +
                    "Possibly too high latency between agents.";
            logger.severe(msg);
            throw new ConfigurationException(msg);
        }

        if (runAborted) { // If aborted during thread start, we discontinue.
            try {
                Thread.sleep(10000); // But wait for some cleanup before we do
            } catch (InterruptedException e) {
            }
            throw new FatalException("Run Aborted.");
        }

        logger.info("Started all threads; run commences in " + sleepTime +
                " ms");
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {
        }
        // At this time each agent will start the run automatically.

        // Start thread to dump stats for charting
        sw = new StatsWriter();

        TimerTask killAtEnd = null;
        final AtomicBoolean joining = new AtomicBoolean(false);

        // In case of time control, we can wait and log more stats. We also
        // know when the run should terminate and can force a kill. But in
        // case of cycle control, we can only wait.
        if (benchDef.runControl == RunControl.TIME) {
            try {
                Thread.sleep(runInfo.rampUp * 1000);
            } catch (InterruptedException ie) {}
            logger.info("Ramp up completed");
            try {
                Thread.sleep(runInfo.stdyState * 1000);
            } catch (InterruptedException ie) {}
            logger.info("Steady state completed");
            try {
                Thread.sleep(runInfo.rampDown * 1000);
            } catch (InterruptedException ie) {}
            logger.info("Ramp down completed");

            // Schedule a forced termination 2 minutes from here where we start
            // the wait.
            killAtEnd = new TimerTask() {

                        Thread mainThread = Thread.currentThread();

                        public void run() {
                            for (int i = 0; i < agentRefs.length; i++)
                                if (agentRefs[i] != null)
                                    for (int j = 0; j < agentRefs[i].length;
                                         j++)
                                        try {
                                            agentRefs[i][j].terminate();
                                        } catch (RemoteException e) {
                                            logger.log(Level.SEVERE,
                                                    "Error checking thread " +
                                                    "starts.", e);
                                        }
                            if (joining.get())
                                mainThread.interrupt();
                        }
                    };
            scheduler.schedule(killAtEnd, 120000);
        }

        // Now wait for all threads under all agents to terminate.
        joining.set(true);

        joinLoop:
        for (int driverType = 0; driverType < runInfo.driverConfigs.length;
             driverType++) {
            if (runInfo.driverConfigs[driverType].numAgents > 0) {
                Remote refs[] = agentRefs[driverType];
                for (int i = 0; i < refs.length; i++)
                    try {
                        ((Agent) refs[i]).join();
                    } catch (RemoteException e) {
                        logger.warning("Master: RemoteException got " + e);
                        logger.throwing(className, "executeRun", e);

                        // If the RemoteException is caused by an interrupt,
                        // we break the loop. This is because killAtEnd is in
                        // effect.
                        if (Thread.interrupted())
                            break joinLoop;
                    }
            }
        }
        joining.set(false);

        // It would be good if we do not have to execute killAtEnd. By now
        // if it's still there in the scheduler it means all the joins work
        // flawlessly.
        if (killAtEnd != null)
            killAtEnd.cancel();

        /* Gather stats and print report */
        Metrics[] results = new Metrics[runInfo.driverConfigs.length];
        for (int driverType = 0; driverType < results.length; driverType++)
            results[driverType] = getDriverMetrics(driverType);

        generateReports(results);

        // Tell StatsWriter to quit
        sw.quit();
    }

    private Metrics getDriverMetrics(int driverType) {
        Remote refs[];
        Metrics result = null;
        try {
            if (runInfo.driverConfigs[driverType].numAgents > 0) {
                Metrics[] results = new Metrics[
                        runInfo.driverConfigs[driverType].numAgents];
                refs = agentRefs[driverType];
                logger.info("Gathering " +
                        benchDef.drivers[driverType].name + "Stats ...");
                for (int i = 0; i < refs.length; i++)
                    results[i] = (Metrics) (((Agent) refs[i]).getResults());

                for (int i = 0; i < results.length; i++)
                    if (results[i] != null)
                        if (result == null)
                            result = results[i];
                        else
                            result.add(results[i]);

                // Once we have the metrics, we have to set it's start time
                // Since this is set after all threads have started, it will
                // be 0 in all the metrices we receive.
                if (result != null)
                    result.startTime =  runInfo.start;
            }
        } catch (RemoteException re) {
            logger.log(Level.WARNING, "Master: RemoteException got " + re, re);
        }
        return result;
    }

    /**
     * Generates the summary and detail report.
     * @param results Array of Metrics objects, one per driver type
     */
    public void generateReports(Metrics[] results) throws IOException {
        String runOutputDir = runInfo.resultsDir + fs;
        FileWriter summary = new FileWriter(runOutputDir + "summary.xml");
        FileWriter detail = new FileWriter(runOutputDir + "detail.xml");

        // As all stats from each agentImpl are of the same type, we can
        // create a new instance from any instance.
        logger.info("Printing Summary report...");
        summary.append(createSummaryReport(results));
        summary.close();

        logger.info("Summary finished. Now printing detail ...");
        detail.append(createDetailReport(results));
        detail.close();

        logger.info("Detail finished. Results written to " +
                runInfo.resultsDir + '.');
    }

    /**
     * Aggregates results of incompatible stats and prints the benchmark
     * summary report header.
     * @param results The per-driver metrics
     * @return The report as a char sequence
     */
    private CharSequence createSummaryReport(Metrics[] results) {
        long startTime = Long.MAX_VALUE;
        long endTime = 0l;
        double metric = 0d;
        boolean passed = true;

        StringBuffer buffer = new StringBuffer(8192);
        StringBuffer hdrBuffer = new StringBuffer(1024);

        for (int i = 0; i < results.length; i++) {
            if (results[i] == null) {
                logger.warning("Unable to obtain " + benchDef.drivers[i].name +
                        " results, ignoring...");
                continue;
            }
            if (results[i].startTime < startTime)
                startTime = results[i].startTime;
            long end;
            if ((end = results[i].startTime + results[i].endTime) > endTime)
                endTime = end;
            if (!results[i].printSummary(buffer, benchDef))
                passed = false;
            metric += results[i].metric;
        }
        String xslPath = System.getProperty("faban.xsl.path", "../../xslt/");
        if (!xslPath.endsWith("/"))
            xslPath += '/';
        hdrBuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        hdrBuffer.append("<?xml-stylesheet type=\"text/xsl\" href=\"").
                append(xslPath).append("summary_report.xsl\"?>\n");
        hdrBuffer.append("<benchResults>\n");
        hdrBuffer.append("    <benchSummary name=\"").append(benchDef.name).
                append("\" version=\"").append(benchDef.version).
                append("\">\n");
        hdrBuffer.append("        <runId>").append(runInfo.runId).
                append("</runId>\n");
        hdrBuffer.append("        <startTime>").append(new Date(startTime)).
                append("</startTime>\n");
        hdrBuffer.append("        <endTime>").append(new Date(endTime)).
                append("</endTime>\n");
        Formatter formatter = new Formatter(hdrBuffer);
        formatter.format("        <metric unit=\"%s\">%.03f</metric>\n",
                benchDef.metric, metric);
        hdrBuffer.append("        <passed>").append(passed).
                append("</passed>\n");
        hdrBuffer.append("    </benchSummary>\n");

        buffer.insert(0, hdrBuffer);
        buffer.append("</benchResults>\n");
        return buffer;
    }

    /**
     * Aggregates detail results into a single buffer.
     * @param results The per-driver metrics
     * @return The report as a char sequence
     */
    private CharSequence createDetailReport(Metrics[] results) {
        StringBuffer buffer = new StringBuffer(8192);
        buffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        buffer.append("<stat_doc name=\"").append(benchDef.name).
                append(" Detailed Results\">\n");
        buffer.append("<meta name=\"RunId\" value=\"").append(runInfo.runId).
                append("\"/>\n");

        for (Metrics result : results)
            if (result != null)
                result.printDetail(buffer);

        buffer.append("</stat_doc>\n");
        return buffer;
    }

    private class StatsWriter extends Thread {

        private int driverTypes = benchDef.drivers.length;

        PlotServer[] plotServers = new PlotServer[driverTypes];
        Metrics[][] currentResults = new Metrics[driverTypes][];
        List[] dumpTargets = new List[driverTypes];
        Remote[] refs;
        boolean started = false;
        boolean endFlag = false;
        long dumpInterval;
        int dumpSecs;


        private int prevTxCnt[] = new int[driverTypes];
        private double avgTps[] = new double[driverTypes];
        private int elapsed = 0;



        public StatsWriter() {
            if (!runInfo.runtimeStatsEnabled)
                return;

            for (int driverId = 0; driverId < driverTypes; driverId ++) {
                if (runInfo.driverConfigs[driverId].numAgents <= 0)
                    continue;
                String dumpResource =
                        runInfo.driverConfigs[driverId].runtimeStatsTarget;
                if (dumpResource == null) // Ignore the driver without resource
                    continue;
                if (!started) {
                    logger.info("Starting StatsWriter ...");
                    started = true;
                }

                try {
                    plotServers[driverId] = new PlotServer(2, dumpResource);
                } catch (IOException e) {
                    logger.severe(e.getMessage());
                    logger.throwing(className, "StatsWriter.<init>", e);
                }
            }

            dumpInterval = runInfo.runtimeStatsInterval * 1000;
            // Make millis so we do not
            // have to re-calculate.

            start();
        }

        public void run() {

            long baseTime = System.currentTimeMillis();

            // Loop, sleeping for dumpInterval and then dump stats
            while (! endFlag) {
                baseTime += dumpInterval;
                for (;;)

                        /* This algorithm may not be very accurate
                        * but accurate enough. The more important
                        * thing is it adjusts for cumulative
                        * errors/delays caused by other ops,
                        * network, and environment.
                        */

                    try {
                        // Adjust for time spent in other ops.
                        long sleepTime = baseTime -
                                System.currentTimeMillis();

                        // Only sleep the remaining time.
                        if (sleepTime > 0)
                            Thread.sleep(sleepTime);

                        // Break loop when sleep complete
                        // or no time left to sleep.
                        break;
                    } catch (InterruptedException ie) {
                        // If interrupted, just loop
                        // back and sleep the remaining time.
                    }

                try {
                    // The time range for the next output is between
                    // elapsed and newElapsed.
                    int newElapsed = elapsed + dumpSecs;
                    for (int driverId = 0; driverId < driverTypes; driverId++) {

                        // runtimeStatsTarget for that driver will be null if
                        // a. The numAgents for the driver is 0 or less
                        // b. The runtimeStatsTarget property for the driver is null
                        if (plotServers[driverId] == null)
                            continue;
                        currentResults[driverId] = new Metrics[
                                runInfo.driverConfigs[driverId].numAgents];
                        for (int i = 0; i < agentRefs[driverId].length; i++)
                            currentResults[driverId][i] = (Metrics)
                                    (((Agent)agentRefs[driverId][i]).
                                    getResults());
                        dumpStats(driverId, currentResults[driverId]);
                    }
                    elapsed = newElapsed;
                } catch (RemoteException re) {
                    logger.warning("Master: RemoteException got " + re);
                    logger.throwing(className, "run", re);
                }
            }
        }

        void quit() {
            if (started) {
                logger.info("Quitting StatsWriter...");
                endFlag = true;
            }
        }

        /**
         * This method is called by the Master every time it wants to dump
         * the thruput data out to files
         * @param driverId The driver id to dump the stats
         * @param agentStats The stats to dump
         */
        void dumpStats(int driverId, Metrics[] agentStats) {
            double[] plotData = new double[2];
            // plotData[0] is the ops/sec
            // plotData[1] is the avg ops/sec

            int txCnt = 0;
            int rampUp = runInfo.rampUp/1000; // The rampup in secs

            // Get the aggregate tx
            for (int i = 0; i < agentStats.length; i++)
                for (int j = 0; j < agentStats[i].txCntStdy.length; j++)
                    txCnt += agentStats[i].txCntStdy[j];

            // The immediate tps;
            plotData[0] = (double) (txCnt - prevTxCnt[driverId]) / dumpSecs;

            // Calculate the new average tps
            if (elapsed > rampUp)
                plotData[1] = (double)txCnt / (elapsed - rampUp);

            try {
                plotServers[driverId].plot(plotData);
            } catch (IOException e) {
                logger.throwing(className, "dumpStats", e);
                // The plot server implicitly manages connections and
                // removes dropped connections. We don't have to worry here.
                // Just log what happened is enough.
            }

            // Adjust the values to reflect new values.
            prevTxCnt[driverId] = txCnt;
            avgTps[driverId] = plotData[1];
        }
    }

    /**
     * Obtain the master's time for time adjustment.
     *
     * @return The current time on the master
     * @throws java.rmi.RemoteException A network error occurred
     */
    public long currentTimeMillis() throws RemoteException {
        return System.currentTimeMillis();
    }

    /**
     * Over-estimate the time it takes to ping all agents.
     * @return A time in ms more than enough to ping all agents
     */
    int estimateCommsTime() {
        int agentCount = 0;
        for (Agent[] agentRef : agentRefs)
            if (agentRef != null)
                agentCount += agentRef.length;

        // Given 100ms per agent (more than enough), Calculate the total time.
        int time = 100 * agentCount;

        // Minimum of 2 secs.
        if (time < 2000)
            time = 2000;

        return time;
    }

    /**
     * Waits for all threads in all agents to start.
     */
    public void waitForThreadStart() {
        if (agentRefs != null)
            for (int i = 0; i < agentRefs.length && !runAborted; i++)
                if (agentRefs[i] != null)
                    for (int j = 0; j < agentRefs[i].length; j++)
                        try {
                            agentRefs[i][j].waitForThreadStart();
                        } catch (RemoteException e) {
                            logger.log(Level.SEVERE,
                                    "Error checking thread starts.", e);
                        }
    }

    /**
     * Sets the benchmark start time after all threads are started.
     * @param relTime The time relative to the timer
     */
    public void setStartTime(int relTime) {
        runInfo.benchStartTime = relTime;
        runInfo.start = relTime + timer.getOffsetTime();
        if (agentRefs != null)
            for (int i = 0; i < agentRefs.length && !runAborted; i++)
                if (agentRefs[i] != null)
                    for (int j = 0; j < agentRefs[i].length; j++)
                        try {
                            agentRefs[i][j].setStartTime(relTime);
                        } catch (RemoteException e) {
                            logger.log(Level.SEVERE,
                                    "Error checking thread starts.", e);
                        }
    }

    /**
     * Notifies the master to terminate the run immediately.
     * This usually happens if there is a fatal error in the run.
     *
     * @throws java.rmi.RemoteException A network error occurred.
     */
    public synchronized void abortRun() throws RemoteException {

        if (runAborted) // We only need to schedule the killAll once.
            return;

        runAborted = true;

        // Note: This is a remote call. We cannot terminate the
        // run right here. We need to schedule the termintation
        // asynchronously.
        TimerTask killAll = new TimerTask() {
            public void run() {
                if (agentRefs != null) {
                    for (int i = 0; i < agentRefs.length; i++)
                        if (agentRefs[i] != null)
                            for (int j = 0; j < agentRefs[i].length; j++)
                                try {
                                    agentRefs[i][j].kill();
                                } catch (RemoteException e) {
                                    logger.log(Level.SEVERE,
                                            "Error calling kill on agent.", e);
                                }
                    for (int i = 0; i < agentRefs.length; i++)
                        if (agentRefs[i] != null)
                            for (int j = 0; j < agentRefs[i].length; j++)
                                try {
                                    agentRefs[i][j].join();
                                } catch (RemoteException e) {
                                    logger.log(Level.SEVERE,
                                            "Error calling join on agent.", e);
                                }
                }
                System.exit(1);
            }
        };
        scheduler.schedule(killAll, 1000);
    }


    /**
     * The main method to start the master. No arguments
     * are expected or passed. The only expectation is
     * the benchmark.properties property pointing to the
     * properties file.
     * @param args The command line arguments are ignored.
     */
    public static void main(String[] args) {
        MasterImpl m = null;
        try {
            m = new MasterImpl();
        } catch  (RemoteException e) {
            // We have no master so we have no logger, create a new one
            // for logging this message.
            Logger logger = Logger.getLogger(Master.class.getName());
            logger.log(Level.SEVERE, "Cannot initialize remote object, " +
                    "stubs may not be generated properly.", e);
            System.exit(1);
        }
        try {
            m.runBenchmark();
            System.exit(0);
        } catch (Throwable t) {
            m.logger.log(Level.SEVERE, "Master terminated with errors.", t);
            System.exit(1);
        }
    }
}
