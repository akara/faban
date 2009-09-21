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
package com.sun.faban.harness.engine;

import com.sun.faban.common.NameValuePair;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.common.RunId;
import com.sun.faban.harness.logging.XMLFormatter;
import com.sun.faban.harness.util.FileHelper;
import com.sun.faban.harness.webclient.RunRetriever;
import com.sun.faban.harness.webclient.RunUploader;

import com.sun.faban.harness.webclient.TagEngine;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;


/**
 * This class implements the RunDaemon thread. The runq object notifies this
 * thread whenever a new run is added to the runq. The RunDaemon thread picks
 * the next run for execution and checks the runType file. If it is a benchmark
 * run, it instantiates the GenericBenchmark object to execute the benchmark
 * in a separate thread and waits for it to complete. If it is a command
 * then it uses the runCommand object to execute the same.
 *
 * @author Ramesh Ramachandran
 *
 */
public class RunDaemon implements Runnable {

    Thread runDaemonThread = null;
    volatile boolean suspended = false;
    volatile boolean keepRunning = true;
    GenericBenchmark gb = null;
    RunQLock  runqLock = null;
    Run currRun = null;
    Logger logger;

    /**
     * Constructor.
     *
     * @param runqLock the monitor object used to syncronize on the runq
     *
     */
    public RunDaemon(RunQLock runqLock) {
        super();
        logger = Logger.getLogger(this.getClass().getName());
        this.runqLock = runqLock;
        if (Config.daemonMode == Config.DaemonModes.POLLER ||
                Config.daemonMode == Config.DaemonModes.LOCAL) {
            runDaemonThread = new Thread(this);
            runDaemonThread.start();
        }
    }

    /**
     * Obtains the name and age of the next run, in milliseconds
     * since submitted, if the age is more than minAge.
     * @param minAge The minimum run age to return.
     * @return The age of the next run, or null if there is no next run or the
     *         next run is younger than the given age
     */
    public NameValuePair<Long> nextRunAge(long minAge) {
        String runId = getNextRun();
        if (runId == null)
            return null;
        File runqDir = new File(Config.RUNQ_DIR, runId);
        long age = System.currentTimeMillis() - runqDir.lastModified();
        if (age <= minAge)
            return null;
        return new NameValuePair<Long>(runId, minAge);
    }

    /**
     * Obtains the name of the next run.
     * @return The id of the next run, or null if there is no next run
     */
    private String getNextRun() {
        // get the list of runs in the runq
        String[] list = new File(Config.RUNQ_DIR).list();

        // if there is no run in the runq then wait for 10 sec and
        // check again if it is suspended and there are any runs this time.
        if ((list == null) || (list.length == 0)) {
            return null;
        }

        Arrays.sort(list, new ComparatorImpl());
        return list[0];
    }

    /**
     * Fetches the next run from the run queue and places it into the output to
     * be executed.
     * @param name The name of the run to fetch
     * @return The run object for the next run
     * @throws RunEntryException The next run entry is incomplete
     * @throws IOException There is an error reading the entry
     * @throws ClassNotFoundException Could not find the benchmark class
     * for the run.
     */
    public Run fetchNextRun(String name) throws RunEntryException, IOException,
            ClassNotFoundException {

        // get the lock for the runq.
        runqLock.grabLock();

        // get the list of runs in the runq
        String runId = getNextRun();

        // name == null in non-poller mode. Don't check name in such cases.
        if (runId == null || (name != null && !runId.equals(name))) {
            runqLock.releaseLock();
            return null;
        }

        // Get the next run, create an output directory for the run and
        // copy the parameter repository file to it.

        // Check to see if the dir has anything in it.
        // $$$$$$$$$$$$$$$$$$$$$$$$$ WARNING $$$$$$$$$$$$$$$$$$$$$$$$$
        // If the user creates an empty runq dir then it will endup with an infinite loop
        // Need to enhance this to avoid this problem
        File runqDir = new File(Config.RUNQ_DIR + runId);
        if(runqDir.list().length < 1) {
            runqLock.releaseLock();
            logger.warning(runId + " is empty. Waiting !!");
            return null;
        }

        RunId runIdObj = new RunId(runId);

        BenchmarkDescription benchDesc =
                BenchmarkDescription.getDescription(runIdObj.getBenchName());
        String runDir = Config.RUNQ_DIR + runId;
        String outDir = Config.OUT_DIR + runId;

        // Create output directory
        File outDirFile = new File(outDir);
        outDirFile.mkdir();

        // Create the metadata directory
        File metaInfFile = new File(outDirFile, "META-INF");
        metaInfFile.mkdir();
        String metaInf = metaInfFile.getAbsolutePath() + File.separator;

        String sourceParamFile =
                runDir + File.separator + benchDesc.configFileName;
        String destParamFile =
                outDir + File.separator + benchDesc.configFileName;

        // Copy whole META-INF dir.
        File srcMetaInf = new File(runDir, "META-INF");
        if (srcMetaInf.isDirectory())
            for (String metaFile : srcMetaInf.list()) {
                FileHelper.copyFile(srcMetaInf.getAbsolutePath() +
                        File.separator + metaFile, metaInf + metaFile, false);
            }

        if (Config.SECURITY_ENABLED) {
            File submitter = new File(outDir + File.separator + "META-INF" +
                                      File.separator + "submitter");
            if (!submitter.isFile()) {
                logger.warning("Unidentified submitter. Removing run " +
                                runId + '.');
                FileHelper.recursiveDelete(new File(Config.RUNQ_DIR), runId);
                runqLock.releaseLock();
                throw new RunEntryException("Unidentified submitter on run " +
                                            runId + '.');
            }
        }


        if (!FileHelper.copyFile(sourceParamFile, destParamFile, false)) {
            logger.warning("Error copying Parameter Repository. " +
                           "Removing run " + runId + '.');
            FileHelper.recursiveDelete(new File(Config.RUNQ_DIR), runId);
            runqLock.releaseLock();
            throw new RunEntryException("Error run param file on run " +
                                        runId + '.');
        }

        FileHelper.recursiveDelete(new File(Config.RUNQ_DIR), runId);
        runqLock.releaseLock();
        uploadTags(runId);

        return new Run(runIdObj.getRunSeq(), benchDesc);
    }

    private void uploadTags(String runId) throws IOException, ClassNotFoundException {
            File file = new File(Config.OUT_DIR + runId + "/META-INF/tags");
            String tags = FileHelper.readContentFromFile(file);
            TagEngine te = TagEngine.getInstance();
            String[] tagsArray;
            if (tags != null && !"".equals(tags)) {
                StringTokenizer tok = new StringTokenizer(tags," ");
                tagsArray = new String[tok.countTokens()];
                int count = tok.countTokens();
                int i=0;
                while(i < count){
                    String nextT = tok.nextToken().trim();
                    tagsArray[i] = nextT;
                    i++;
                }
                te.add(runId, tagsArray);
            }
            te.save();
        }

    /**
     * The run method for the RunDaemonThread. It loops indefinitely and blocks
     * when there are no runs in the runq. It continues when notified of a new
     * run by the RunQ object.
     *
     */
    public void run() {

        logger.info("RunDaemon Thread Started");
        // THE loop
        while (keepRunning) {
            try {
                // Wait if  the runDaemonThread is temporarily suspended.
                synchronized (runDaemonThread) {
                    if(suspended) {
                        try {
                            logger.info("RunDaemon Thread suspended");
                            runDaemonThread.wait();
                        }
                        catch (InterruptedException ie) {
                            logger.severe("RunDaemon Thread interrupted.");
                        }
                        // Go back and check if still suspended or got killed.
                        continue;
                    }
                }

                Run run = null;
                String runId = null;

                // Poll other hosts in poller mode. Otherwise skip this block.
                if (Config.daemonMode == Config.DaemonModes.POLLER) {
                    NameValuePair<Long> nextLocal = nextRunAge(Long.MIN_VALUE);
                    long runAge = -1;
                    if (nextLocal != null) {
                        runId = nextLocal.name;
                        runAge = nextLocal.value;
                    }
                    File tmpRunDir = null;
                    while ((tmpRunDir = RunRetriever.pollRun(runAge)) != null)
                        try {
                            run = fetchRemoteRun(tmpRunDir);
                            if (run == null)
                                logger.warning("Fetched null remote run");
                            break;

                        } catch (RunEntryException e) {
                            continue; // If we got a bad run, try polling again
                        }
                    if (run == null && nextLocal == null) {
                        // No local run or remote run...
                        runqLock.waitForSignal(10000);
                        continue;
                    }
                }

                boolean remoteRun = true;
                if (run == null)
                    try {
                        try {
                            // runId null if not poller.
                            run = fetchNextRun(runId);
                        } catch (IOException ex) {
                            Logger.getLogger(RunDaemon.class.getName()).log(
                                    Level.SEVERE,
                                    "IOException fetching remote run.", ex);
                        } catch (ClassNotFoundException ex) {
                            Logger.getLogger(RunDaemon.class.getName()).log(
                                    Level.SEVERE, "ClassNotFoundException " +
                                    "fetching remote run.", ex);
                        }
                        remoteRun = false;
                    } catch (RunEntryException e) {
                        // If there is a run entry issue, just skip to the next
                        // run immediately.
                        continue;
                    }
                if (run == null) {
                    runqLock.waitForSignal(10000);
                    continue;
                }

                String benchName = run.getBenchmarkName();
                String runDir = run.getOutDir();

                // Redirect the log to runOutDir/log.xml
                String logFile = runDir + File.separator + Config.LOG_FILE;
                redirectLog(logFile, null);

                logger.info("Starting " + benchName + " run using " + runDir);

                // instantiate, start running the benchmark
                currRun = run;
                gb = new GenericBenchmark(currRun);
                gb.start();

                // We could have done the uploads in GenericBenchmark.
                // But we fetched the remote run here, so we should return it
                // here, too!
                if (remoteRun)
                    try {
                        RunUploader.uploadIfOrigin(run.getRunId());
                    } catch (IOException e) {
                        logger.log(Level.WARNING, "Run upload failed!", e);
                    }

                logger.info(benchName + " Completed/Terminated");
                gb = null;

                // Redirect the log back to faban.log.xml
                // and limit the log file size to 100K.
                redirectLog(Config.DEFAULT_LOG_FILE, "102400");
            } catch (Throwable t) { // We won't let this loop exit.
                logger.log(Level.SEVERE, "Uncaught throwable in benchmark run.",
                        t);
            }
        }
        logger.fine("RunDaemon Thread is Exiting");
    }

    /**
     * Fetches a remote run downloaded into the given run directory.
     * @param tmpRunDir The temporary directory the run was downloaded into
     * @return The run object for this run.
     * @throws RunEntryException The run in the given dir cannot be run.
     */
    private Run fetchRemoteRun(File tmpRunDir) throws RunEntryException {

        runqLock.grabLock();

        // 1. get run id and identify run directory
        // tmpRunDir is in the form of host.bench.id
        RunId runId0 = new RunId(tmpRunDir.getName());

        // We ignore the remote run id at this time.
        String benchName = runId0.getBenchName();
        RunQ.RunSequence sequence = new RunQ.RunSequence();
        String runSeq = sequence.get();
        String runId = benchName + '.' + runSeq;

        File runDir = new File(Config.OUT_DIR, runId);

        // 2. copy directory
        if (runDir.exists() || !FileHelper.recursiveCopy(tmpRunDir, runDir)) {
            logger.warning("Error copying remote run. " +
                           "Removing run " + runId + '.');
            FileHelper.recursiveDelete(new File(Config.RUNQ_DIR), runId);
            runqLock.releaseLock();
            throw new RunEntryException("Error copy param file on run " +
                                        runId + '.');
        }
        try {
            sequence.next();
        } catch (IOException e) {
            sequence.cancel();
            logger.warning("Error updating run id.");
            throw new RunEntryException("Error updating run id");
        }        
        runqLock.releaseLock();

        FileHelper.recursiveDelete(tmpRunDir);

        BenchmarkDescription benchDesc = BenchmarkDescription.
                                            getDescription(benchName);
        if (benchDesc == null) {
            RunEntryException e = new RunEntryException(
                    "Received run for benchmark " + benchName +
                    "from remote, benchmark not deployed. " +
                    "Please deploy before continue");
            logger.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        }
        return new Run(runSeq, benchDesc);
    }

    /**
     * Obtains the run id of the current run.
     * @return The run id of the current run,
     *         or null if there is no ccurrent run
     */
    public String getCurrentRunId() {
        if (gb!= null)
            return currRun.getRunId();
        return null;
    }

    /**
     * Obtains the short name of the current benchmark run.
     * @return The benchmark's short name
     */
    public String getCurrentRunBenchmark() {
        if (gb != null)
            return currRun.getBenchmarkName();
        return null;
    }

    /**
     * To abort the currently executing benchmark run.
     * @param runId The name of the run
     * @param user The user killing the run
     * @return The run name being killed
     */
    public String killCurrentRun(String runId, String user) {
        if (runId.equals(currRun.getRunId()) && gb != null) {
            gb.kill();
            logger.info("Audit: Run " + runId + " killed by " + user);
            return runId;
        }
        return null;
    }

    private void killCurrentRun() {
        if (gb != null) {
            gb.kill();
            logger.fine("RunDaemon Killed Current run");
        }
    }

    /**
     * Exits the RunDaemon.
     */
    public void exit() {
        logger.info("RunDaemon Exit called");
        keepRunning = false;
        killCurrentRun();
        resumeRunDaemonThread();
    }

    /**
     * Obtains RunDaemon thread status.
     * @return status of RunDaemon thread
     */
    public String getRunDaemonThreadStatus() {
        if (runDaemonThread != null) {
            synchronized (runDaemonThread) {
                if(suspended)
                    return "Suspended";
                else
                    return "Alive";
            }
        }
        else {
            return "Not Running";
        }
    }

    /**
     * Called by RunQ's stopRunDaemon method.
     * @return Whether or not the suspend succeeded
     */
    public boolean suspendRunDaemonThread() {
        if (runDaemonThread != null && !suspended) {
            synchronized (runDaemonThread) {
                logger.info("RunDaemon Suspended");
                suspended = true;
                return true;
            }
        }
        return false;
    }

    /**
     * Called by RunQ's resumeRunDaemon method.
     * @return Whether or not the resume succeeded
     */
    public boolean resumeRunDaemonThread() {

        if (runDaemonThread == null)
            return false;

        if (!runDaemonThread.isAlive()) {
            runDaemonThread.start();
            logger.info("RunDaemon Resumed");
            return true;
        }
        if (suspended) {
            synchronized (runDaemonThread) {
                suspended = false;
                runDaemonThread.notify();
            }
            logger.info("RunDaemon Resumed");
            return true;
        }
        return false;
    }

    /**
     * Redirect the log to file named log.xml inside the
     * current run output directory.
     * @param logFile the output directory for the run
     * @param limit the log file size limit
     */
    private void redirectLog(String logFile, String limit) {
        StringBuilder sb = new StringBuilder();
        // sb.append("\nhandlers = java.util.logging.FileHandler\n");
        // sb.append("java.util.logging.FileHandler.pattern = ");
        // sb.append(logFile + "\n");
        sb.append("java.util.logging.FileHandler.append = true\n");
        // If a limit is passed add it to the porps.
        if(limit != null)
            sb.append("java.util.logging.FileHandler.limit = " + limit + "\n");

        sb.append("java.util.logging.FileHandler.formatter = " +
                "com.sun.faban.harness.logging.XMLFormatter\n");
        try {
            // Check the props for any levels and set them.
            Properties logProps = new Properties();
            FileInputStream is = new FileInputStream(Config.CONFIG_DIR +
                    "logging.properties");
            logProps.load(is);
            for (Enumeration en = logProps.propertyNames();
                 en.hasMoreElements();) {
                String key = (String) en.nextElement();
                if (key.endsWith(".level")) {
                    sb.append(key + " = " + logProps.getProperty(key) + '\n');
                }
            }
            is.close();
            LogManager.getLogManager().readConfiguration(
                    new ByteArrayInputStream(sb.toString().getBytes()));

            FileHandler fileHandler = new FileHandler(logFile);
            fileHandler.setFormatter(new XMLFormatter());
            Logger rootLogger = Logger.getLogger("");
            rootLogger.addHandler(fileHandler);

            // Set system property so that SocketHandler can write the logs from remote machines
            System.setProperty("faban.log.file", logFile);
        } catch(IOException e) {
            System.err.println("Exception setting log properties.");
            e.printStackTrace();
            logger.log(Level.WARNING, "Exception setting log properties.", e);
        }
    }

    private class ComparatorImpl implements Comparator {

        public int compare(Object o1, Object o2) {
            RunId r1 = new RunId((String) o1);
            RunId r2 = new RunId((String) o2);
            return r1.compareSeq(r2);
        }
    }
}
