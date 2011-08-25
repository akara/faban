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

import com.sun.faban.common.Registry;
import com.sun.faban.common.RegistryLocator;
import com.sun.faban.driver.ConfigurationException;
import com.sun.faban.driver.FatalException;
import com.sun.faban.driver.RunControl;
import com.sun.faban.driver.util.PairwiseAggregator;
import com.sun.faban.driver.util.Timer;

import java.io.*;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
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
 * @see        com.sun.faban.driver.engine.Agent
 * @see        com.sun.faban.common.Registry
 *
 */
public class MasterImpl extends UnicastRemoteObject implements Master {

	private static final long serialVersionUID = 1L;

	// This field is a legal requirement and serves no other purpose.
    static final String COPYRIGHT =
            "Copyright \251 2006-2009 Sun Microsystems, Inc., 4150 Network " +
            "Circle, Santa Clara, California 95054, U.S.A. All rights " +
            "reserved.\n" +
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
            "Copyright \251 2006-2009 Sun Microsystems, Inc., 4150 Network " +
            "Circle, Santa Clara, California 95054, Etats-Unis. Tous droits " +
            "r\351serv\351s.\n" +
            "L'utilisation est soumise aux termes de la Licence.\n" +
            "Cette distribution peut comprendre des composants " +
            "d\351velopp\351s par des tierces parties.\n" +
            "Sun,  Sun Microsystems,  le logo Sun et  Java sont des marques " +
            "de fabrique ou des marques d\351pos\351es de " +
            "Sun Microsystems, Inc. aux Etats-Unis et dans d'autres pays.\n" +
            "Apache est une marque d\264Apache Software Foundation, utilis\351e " +
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

    private Logger logger = Logger.getLogger(className);

    BenchmarkDefinition benchDef;

    /** Remote references for each agentImpl of each driver type. */
    protected Agent[][] agentRefs;
    // Note: first dimension is the driver type

    /** Threads required per agentImpl for each driver. */
    protected int[] agentThreads;

    /** Remaining threads to be distributed to the first agents. */
    protected int[] remainderThreads;

    /** The RunInfo structure. */
    protected RunInfo runInfo;

    /** The time recorder. */
    protected Timer timer;

    /** Convenience accessor to the file separator. */
    protected static String fs = System.getProperty("file.separator");

    private boolean runAborted = false;

    /** The lock object for the state. */
    protected final Object stateLock = new Object();

    /** The current state of the master. */
    protected MasterState state = MasterState.CONFIGURING;

    /** The scheduler used in the master. */
    protected java.util.Timer scheduler;

    StatsWriter statsWriter;

    /**
     * Creates and exports a new Master.
     *
     * @throws RemoteException if failed to export object
     */
    protected MasterImpl() throws RemoteException {
        super();

        try {
            RegistryLocator.getRegistry().reregister("Master", this);
        } catch (ConnectException e) {
            // A ConnectException should be interpreted as no registry.
        } catch (NotBoundException e) {
            // Here too, do nothing. If we run in single process mode,
            // the registry is just not there.
        }
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
        if (uniqueDirString != null) {
			uniqueDir = RunInfo.ConfigurationReader.
                    relaxedParseBoolean(uniqueDirString);
		}

        if (uniqueDir) {
            // Ensure separator is not at end.
            if (runInfo.resultsDir.endsWith(fs)) {
				runInfo.resultsDir = runInfo.resultsDir.substring(0,
                        runInfo.resultsDir.length() - fs.length());
			}

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
        if (!uniqueDir) {
			runOutputDir = runInfo.resultsDir + fs + runInfo.runId;
		}

        // make a new directory for the run.
        File runDirFile = new File(runOutputDir);
        if ( !runDirFile.exists()) {
			if ( !runDirFile.mkdirs()) {
				throw new IOException("Could not create the new " +
                        "Run Directory: " + runOutputDir);
			}
		}

        logger.info("Output directory for this run is : " + runOutputDir);
        runInfo.resultsDir = runOutputDir;

        configureLogger (runOutputDir);

        timer = new Timer();

        agentRefs = new Agent[benchDef.drivers.length][];
        agentThreads = new int[benchDef.drivers.length];
        remainderThreads = new int[benchDef.drivers.length];

        scheduler = new java.util.Timer("Scheduler", false);
        try {
            int agentCnt = configure();
            if (agentCnt > 0) {
                for (int i = 0; i < benchDef.drivers.length && !runAborted; i++) {
					configureAgents(i);
				}
                for (int i = 0; i < benchDef.drivers.length && !runAborted; i++) {
					startThreads(i);
				}
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
            while ((tt = t.getCause()) != null) {
				t = tt;
			}
            logger.log(Level.WARNING,
                    "Error acccessing registry or agent!", t);
            configureLocal();
        }
        changeState(MasterState.STARTING);
        executeRun();
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
     * @return runId
     * @throws IOException Problem acecessing or creating the run id file
     */
    public String getRunID(boolean increment) throws IOException{
        int runID = -1;

        String seqDir = System.getProperty("faban.sequence.path");
        if (seqDir == null) {
			seqDir = System.getProperty("user.home");
		}

        String seqFileName = System.getProperty("faban.sequence.file");
        if (seqFileName == null)
            seqFileName = benchDef.name.toLowerCase() + ".seq";
        seqFileName = seqDir + fs + seqFileName;
        File seqFile = new File(seqFileName);
        if (seqFile.exists()) {
            FileReader bufIn;
            char[] buffer = new char[64];
            int length;
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
            if (length > 0) {
                // Strip off the newlines
                if (buffer[length - 1] == '\n') {
					--length;
				}
                if (buffer[length - 1] == '\r') {
					--length;
				}
                runID = Integer.parseInt(new String(buffer, 0, length));
            }
        }
        if (runID == -1) {
            if (increment) {
				try {
                    seqFile.createNewFile();
                }
                catch (IOException e) {
                    logger.log(Level.SEVERE, "Could not create the sequence " +
                            "file " + seqFileName + '.', e);
                    throw e;
                }
			}
            runID = 1;
        }
        // Update the runid in the sequence file
        if (increment) {
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
     * @throws Exception Any error that could happen configuring the master
     */
    protected int configure() throws Exception {

        Registry registry = RegistryLocator.getRegistry();

        int totalAgentCnt = 0;

        // Get all agents for all drivers
        for (int i = 0; i < benchDef.drivers.length && !runAborted; i++) {
            // Only for drivers to run...
            if (runInfo.driverConfigs[i].numAgents > 0) {

                // Get all the agentImpl refs
                String agentName = benchDef.drivers[i].name + "Agent";
                Remote[] refs = registry.getServices(agentName);
                int agentCnt;

                // Usually, the agents should have been started
                // according to the given number. But just in case
                // some of them did not get started or there might
                // be some others sneaking in...
                if (refs == null || (agentCnt = refs.length) == 0) {
                    // Hmmm, for some reason the agents didn't get started
                    if (runInfo.driverConfigs[i].numAgents > 0) {
                        logger.warning("Cannot find " + agentName + "s. Not " +
                                "starting " + benchDef.drivers[i].name + '.');
					}
                    runInfo.driverConfigs[i].numAgents = 0;
                    continue;
                }

                // We have to ensure the agents are sorted by their id. Using
                // an array sort will cause multiple duplicate calls into the
                // agent.getId() so we're better off using a map here.
                TreeMap<Integer, Agent> sortMap = new TreeMap<Integer, Agent>();
                for (int j = 0; j < agentCnt; j++) {
                    Agent agent = (Agent) refs[j];
                    int agentId = agent.getId();
                    if (sortMap.put(agentId, agent) != null) {
                        logger.warning("Duplicate agent id " + agentId +
                                        " for " + agentName +
                                        ". Ignoring an agent.");
                    }
                }

                // Re-adjust the agent count to eliminate duplicates.
                agentCnt = sortMap.size();

                if (agentCnt != runInfo.driverConfigs[i].numAgents) {
                    logger.warning("Configured " + runInfo.driverConfigs[i].
                            numAgents + ' ' + benchDef.drivers[i].name +
                            "Agents but found " + agentCnt + '.');
                    if (agentCnt > runInfo.driverConfigs[i].numAgents) {
                        logger.warning("Some unkown agents managed to " +
                                "sneak in! We'll use'em!");
                    } else {
                        logger.warning("Some agents surely didn't get " +
                                "started. We'll just use the ones we have.");
                    }

                    // Now we need to adjust the runInfo according to reality
                    runInfo.driverConfigs[i].numAgents = agentCnt;
                }

                // We need to calculate the thread counts
                if (runInfo.driverConfigs[i].numThreads == -1) {
					runInfo.driverConfigs[i].numThreads = Math.round(
                            runInfo.scale * benchDef.drivers[i].threadPerScale);
				}

                // Adjust the agent count to not exceed the thread count.
                if (runInfo.driverConfigs[i].numAgents >
                        runInfo.driverConfigs[i].numThreads) {
                    logger.warning("Reducing agents from " +
                            runInfo.driverConfigs[i].numAgents + " to " +
                            runInfo.driverConfigs[i].numThreads +
                            " due to too low scale.");
                    agentCnt = runInfo.driverConfigs[i].numThreads;
                    runInfo.driverConfigs[i].numAgents = agentCnt;
                }

                // Now assign the agent refs to the agentRefs array,
                // just for the agents being used.
                agentRefs[i] = new Agent[agentCnt];

                int j = 0;
                for (Agent agent : sortMap.values()) {
                    if (j >= agentCnt)
                        break;
                    agentRefs[i][j++] = agent;
                }

                // Finally, calculate the agent and overflow threads.
                agentThreads[i] = runInfo.driverConfigs[i].numThreads /
                        runInfo.driverConfigs[i].numAgents;
                remainderThreads[i] = runInfo.driverConfigs[i].numThreads -
                        agentThreads[i] * runInfo.driverConfigs[i].numAgents;

                totalAgentCnt += agentCnt;
            }
        }

        // Check that no threads in an active driver should be 0 or less.
        ArrayList<Integer> minList = new ArrayList<Integer>();
        for (int i = 0; i < benchDef.drivers.length; i++) {
            if (runInfo.driverConfigs[i].numAgents > 0 &&
                    runInfo.driverConfigs[i].numThreads < 1) {
                minList.add(Math.round(1f / benchDef.drivers[i].threadPerScale));
            }
        }

        int maxMinScale = Integer.MIN_VALUE;
        // Scan for max of minList and report it as a min scale.
        if (minList.size() > 0) {
            for (int minScale : minList)
                if (minScale > maxMinScale)
                    maxMinScale = minScale;
            throw new ConfigurationException("Scale must be at least " +
                    maxMinScale + ".");
        }

        return totalAgentCnt;
    }

    /**
     * Configures a local, in-process agent.
     * @throws Exception If anything goes wrong during the configuration
     */
    protected void configureLocal() throws Exception {
        int driverToRun = -1;
        if (runInfo.driverConfigs.length > 1) {
            for (int i = 0; i < runInfo.driverConfigs.length; i++) {
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
			}
        } else {
            driverToRun = 0;
            runInfo.driverConfigs[0].numAgents = 1;
        }

        if (driverToRun < 0) {
			throw new ConfigurationException("No driver configured to run.");
		}

        logger.config("Starting single, in-process " +
                      benchDef.drivers[driverToRun].name + "Agent.");

        // We need to calculate the thread counts
        if (runInfo.driverConfigs[driverToRun].numThreads == -1) {
			runInfo.driverConfigs[driverToRun].numThreads = Math.round(runInfo.
                    scale * benchDef.drivers[driverToRun].threadPerScale);
		}
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
        agentRefs[driverToRun][0].configure(this, runInfo,
                driverToRun, timer);
        agentRefs[driverToRun][0].startThreads();
    }

    /**
     * Configures all agents for a driver type.
     * @param driverType The driver type id to configure
     * @throws Exception If anything goes wrong in the process
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
            Agent[] refs = agentRefs[driverType];
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
                    refs[agentId].configure(this, runInfo, driverType, timer);
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
                refs[agentId].configure(this, runInfo, driverType, timer);
                runInfo.agentInfo.startThreadNumber += agentInfo.threads;
            }
        }
        runInfo.driverConfig = null;
        runInfo.agentInfo = null; // reset it so we don't use it anywhere else
    }

    /**
     * Starts all the threads for a driver type.
     * @param driverType The type id of the driver
     * @throws Exception An error occurred starting the driver threads
     */
    private void startThreads(int driverType) throws Exception {
        int agentCnt = runInfo.driverConfigs[driverType].numAgents;
        if (agentCnt > 0) {
            Agent[] refs = agentRefs[driverType];
            for (Agent ref : refs)
                ref.startThreads();
        }
    }

    /**
     * Tell the agents to start the run execution
     * Note that the Agent's run method call is non-blocking
     * i.e the Master does not wait for an Agent. Instead, we
     * wait for the total length of the run, after we signal
     * all the agents to start.
     * @throws Exception Anything that could go wrong during the run
     */
    private void executeRun() throws Exception {

        // Now wait for all threads to start if it is parallel.
        if (runInfo.parallelAgentThreadStart) {
			waitForThreadStart();
		}
        
        // Start thread to dump stats for charting
        if (runInfo.runtimeStatsEnabled)
            statsWriter = new StatsWriter();

        // Leave plenty of time to notify all agents of the start time.
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
            	logger.log(Level.FINE, e.getMessage(), e);
            }
            throw new FatalException("Run Aborted.");
        }

        logger.info("Started all threads; run commences in " + sleepTime +
                " ms");
        try {
            Thread.sleep(sleepTime);
        } catch (InterruptedException ie) {
        	logger.log(Level.FINE, ie.getMessage(), ie);
        }
        // At this time each agent will start the run automatically.

        TimerTask killAtEnd = null;
        final AtomicBoolean joining = new AtomicBoolean(false);

        // In case of time control, we can wait and log more stats. We also
        // know when the run should terminate and can force a kill. But in
        // case of cycle control, we can only wait.
        if (benchDef.runControl == RunControl.TIME) {
            changeState(MasterState.RAMPUP);
            try {
                Thread.sleep(runInfo.rampUp * 1000);
            } catch (InterruptedException ie) {
            	logger.log(Level.FINE, ie.getMessage(), ie);
            }
            changeState(MasterState.STEADYSTATE);
            logger.info("Ramp up completed");
            try {
                Thread.sleep(runInfo.stdyState * 1000);
            } catch (InterruptedException ie) {
            	logger.log(Level.FINE, ie.getMessage(), ie);
            }
            changeState(MasterState.RAMPDOWN);
            logger.info("Steady state completed");
            try {
                Thread.sleep(runInfo.rampDown * 1000);
            } catch (InterruptedException ie) {
            	logger.log(Level.FINE, ie.getMessage(), ie);
            }

            // Schedule a forced termination 2 minutes from here where we start
            // the wait.
            killAtEnd = new TimerTask() {

                        Thread mainThread = Thread.currentThread();

						public void run() {
                            // Terminate all agents except agent 0 for all
                            // driver types first.
                            for (int i = 0; i < agentRefs.length; i++) {
								if (agentRefs[i] != null) {
									// Ensure we terminate the first agent last
                                    for (int j = agentRefs[i].length - 1;
                                         j > 0; j--) {
										try {
                                            agentRefs[i][j].terminate();
                                        } catch (RemoteException e) {
                                            logger.log(Level.SEVERE,
                                                    "Error checking thread " +
                                                    "termination.", e);
                                        }
									}
								}
							}
                            // Then terminate agent 0.
                            for (int i = 0; i < agentRefs.length; i++) {
                                if (agentRefs[i] != null &&
                                        agentRefs[i][0] != null)
                                    try {
                                        agentRefs[i][0].terminate();
                                    } catch (RemoteException e) {
                                        logger.log(Level.SEVERE,
                                                "Error checking thread " +
                                                "termination.", e);
                                    }
                            }

                            // Then, call postRun on all agent 0s.
                            for (int i = 0; i < agentRefs.length; i++) {
                                if (agentRefs[i] != null &&
                                        agentRefs[i][0] != null)
                                    try {
                                        agentRefs[i][0].postRun();
                                    } catch (RemoteException e) {
                                        logger.log(Level.SEVERE,
                                                "Error calling postRun", e);
                                    }
                            }

                            if (joining.get()) {
								mainThread.interrupt();
							}
                        }
                    };
            scheduler.schedule(killAtEnd, 120000);
        } else { // For cycle mode, we never know the end of rampup, steady.
            changeState(MasterState.STEADYSTATE);
        }

        // Now wait for all threads under all agents to terminate.
        joining.set(true);

        // Join all other agents.
        joinLoop:
        for (int driverType = 0; driverType < runInfo.driverConfigs.length;
             driverType++) {
            if (runInfo.driverConfigs[driverType].numAgents > 0) {
                Agent refs[] = agentRefs[driverType];
                // Make sure we join the first agent last
                for (int i = refs.length - 1; i >= 0; i--) {
					try {
                        refs[i].join();
                    } catch (RemoteException e) {
                        logger.log(Level.WARNING,
                                "Master: RemoteException got " + e, e);

                        // If the RemoteException is caused by an interrupt,
                        // we break the loop. This is because killAtEnd is in
                        // effect.
                        if (Thread.interrupted()) {
							break joinLoop;
						}
                    }
				}
            }
        }

        // Join agent 0.
        for (int driverType = 0; driverType < runInfo.driverConfigs.length;
             driverType++) {
            if (runInfo.driverConfigs[driverType].numAgents > 0) {
                Agent refs[] = agentRefs[driverType];
                if (refs != null && refs[0] != null) {
					try {
                        refs[0].join();
                    } catch (RemoteException e) {
                        logger.log(Level.WARNING,
                                "Master: RemoteException got " + e, e);

                        // If the RemoteException is caused by an interrupt,
                        // we break the loop. This is because killAtEnd is in
                        // effect.
                        if (Thread.interrupted()) {
							break;
						}
                    }
                }
            }
        }
        logger.info("Ramp down completed");

        joining.set(false);

        // It would be good if we do not have to execute killAtEnd. By now
        // if it's still there in the scheduler it means all the joins work
        // flawlessly.
        if (killAtEnd != null) {
			killAtEnd.cancel();
		}

        // Call postRun
        for (int driverType = 0; driverType < runInfo.driverConfigs.length;
             driverType++) {
            if (runInfo.driverConfigs[driverType].numAgents > 0) {
                Agent refs[] = agentRefs[driverType];
                if (refs != null && refs[0] != null) {
					try {
                        refs[0].postRun();
                    } catch (RemoteException e) {
                        logger.log(Level.WARNING,
                                "Master: RemoteException got " + e, e);

                        // If the RemoteException is caused by an interrupt,
                        // we break the loop. This is because killAtEnd is in
                        // effect.
                        if (Thread.interrupted()) {
							break;
						}
                    }
                }
            }
        }

        /* Gather stats and print report */
        changeState(MasterState.RESULTS);
        int driverTypes = runInfo.driverConfigs.length;
        ArrayList<Map<String, Metrics>> resultsList =
                new ArrayList<Map<String, Metrics>>(driverTypes);

        // Note: the index into the list is the actual driver type
        for (int driverType = 0; driverType < driverTypes; driverType++) {
			resultsList.add(getDriverMetrics(driverType));
		}

        generateReports(resultsList);

        // Tell StatsWriter to quit
        if (statsWriter != null)
            statsWriter.quit();
    }

    private class MetricsProvider
            implements PairwiseAggregator.Provider<Metrics> {

        ArrayList<Metrics> metrices;

        private MetricsProvider() {
            metrices = new ArrayList<Metrics>();
        }

        private MetricsProvider(int count) {
            metrices = new ArrayList<Metrics>(count);
        }

        public void add(Metrics m) {
            metrices.add(m);
        }

        public Metrics getMutableMetrics(int idx) {
            return (Metrics) metrices.get(idx).clone();
        }

        public void add(Metrics instance, int idx) {
            instance.add(metrices.get(idx));
        }

        public Class getComponentClass() {
            return Metrics.class;
        }

        public void recycle(Metrics r) {
        }
    }


    private Map<String, Metrics> getDriverMetrics(int driverType) {

        LinkedHashMap<String, MetricsProvider> hostProviders =
                                   new LinkedHashMap<String, MetricsProvider>();
        LinkedHashMap<String, Metrics> hostMetrics =
                                   new LinkedHashMap<String, Metrics>();
        try {
            if (runInfo.driverConfigs[driverType].numAgents > 0) {
                Agent[] agents = agentRefs[driverType];
                logger.info("Gathering " +
                        benchDef.drivers[driverType].name + "Stats ...");

                // Add the results on a per-host basis and grand summary
                MetricsProvider grandSumProvider = new MetricsProvider(
                        runInfo.driverConfigs[driverType].numAgents);

                for (Agent agent : agents) {
                    Metrics r = agent.getResults();
                    if (r == null)
                        continue;
                    MetricsProvider hostResult = hostProviders.get(r.host);
                    if (hostResult == null) {
                        hostResult = new MetricsProvider();
                        hostProviders.put(r.host, hostResult);
                    } 
                    hostResult.add(r);
                    grandSumProvider.add(r);

                    // Once we have the metrics, we have to set it's start time
                    // Since this is set after all threads have started, it will
                    // be 0 in all the metrices we receive.
                    r.startTime = runInfo.start;

                }

                Metrics result = null;

                // Aggregate the per driver host metrics, calculate results.
                for (MetricsProvider r : hostProviders.values()) {
                    PairwiseAggregator<Metrics> aggregator = new
                            PairwiseAggregator<Metrics>(r.metrices.size(), r);
                    result = aggregator.collectStats();
                    hostMetrics.put(result.host, result);
                }

                // Aggregate the final metrics, calculate results.
                if (grandSumProvider.metrices.size() > 0) {
                    PairwiseAggregator<Metrics> aggregator =
                            new PairwiseAggregator<Metrics>(
                                    grandSumProvider.metrices.size(),
                                    grandSumProvider);

                    result = aggregator.collectStats();

                    // And finally set it for the final result, too.
					result.startTime =  runInfo.start;
                    // Set it in the map, under the name __MASTER__
                    // This is an invalid host name so it will never conflict.
                    hostMetrics.put("__MASTER__", result);
				}
            }
        } catch (RemoteException re) {
            logger.log(Level.WARNING, "Master: RemoteException got " + re, re);
        }
        return hostMetrics;
    }

    private Metrics[] getHostMetrics(List<Map<String, Metrics>> results,
                                     String host) {
        Metrics[] thResults = new Metrics[results.size()];
        for (int i = 0; i < thResults.length; i++) {
            Map<String, Metrics> typeResults = results.get(i);
            thResults[i] = typeResults.get(host);
        }
        return thResults;
    }

    /**
     * Generates the summary and detail report.
     * @param results List of Host-Metrics maps, one per driver type
     * @throws IOException 
     */
    public void generateReports(List<Map<String, Metrics>> results)
            throws IOException {

        // Set of driver hosts.
        LinkedHashSet<String> hostSet = new LinkedHashSet<String>();
        for (Map<String, Metrics> hostResults : results) {
            hostSet.addAll(hostResults.keySet());
        }
        hostSet.remove("__MASTER__");

        // Only print the per-host results if there is more than one driver host
        if (hostSet.size() > 1) {
            for (String host : hostSet) {
                CharSequence summaryContent = createSummaryReport(
                                        getHostMetrics(results, host), host);
                if (summaryContent != null) {
                    String runOutputDir = runInfo.resultsDir + fs;
                    FileWriter summary = new FileWriter(runOutputDir + 
                            "summary.xml." + host);
                    FileWriter detail = new FileWriter(runOutputDir + 
                            "detail.xan." + host);

                    // As all stats from each agentImpl are of the same type,
                    // we can create a new instance from any instance.
                    logger.info("Printing Summary report for " + host + " ...");
                    summary.append(summaryContent);
                    summary.close();

                    logger.info("Summary finished. Now printing detail for " + 
                            host + " ...");
                    detail.append(createDetailReport(
                                        getHostMetrics(results, host), host));
                    detail.close();

                    logger.info("Detail for " + host + " finished.");
                }
            }
        }

        CharSequence summaryContent = createSummaryReport(
                                getHostMetrics(results, "__MASTER__"), null);
        if (summaryContent != null) {
            String runOutputDir = runInfo.resultsDir + fs;
            FileWriter summary = new FileWriter(runOutputDir + "summary.xml");
            FileWriter detail = new FileWriter(runOutputDir + "detail.xan");

            // As all stats from each agentImpl are of the same type, we can
            // create a new instance from any instance.
            logger.info("Printing Summary report ...");
            summary.append(summaryContent);
            summary.close();

            logger.info("Summary finished. Now printing detail ...");
            detail.append(createDetailReport(
                                getHostMetrics(results, "__MASTER__"), null));
            detail.close();

            logger.info("Detail finished. Results written to " +
                    runInfo.resultsDir + '.');
        }
    }

    /**
     * Aggregates results of incompatible stats and prints the benchmark
     * summary report header.
     * @param results The per-driver metrics
     * @param host The host name for which to create the summary report, or null
     * @return The report as a char sequence
     */
    @SuppressWarnings("boxing")
	private CharSequence createSummaryReport(Metrics[] results, String host) {
        long startTime = Long.MAX_VALUE;
        long endTime = 0l;
        double metric = 0d;
        boolean passed = true;

        StringBuilder buffer = new StringBuilder(8192);

        for (int i = 0; i < results.length; i++) {
            if (results[i] == null) {
                logger.warning("Unable to obtain " + benchDef.drivers[i].name +
                        " results, ignoring...");
                continue;
            }
            if (results[i].startTime < startTime) {
				startTime = results[i].startTime;
			}
            long end;
            if ((end = results[i].startTime + results[i].endTime) > endTime) {
				endTime = end;
			}
            if (!results[i].printSummary(buffer, benchDef)) {
				passed = false;
			}
            metric += results[i].metric;
        }

        // If we did not get any results for any reason, there's no need to
        // proceed. In that case startTime will still be Long.MAX_VALUE and
        // end time will still be 0.
        if (startTime == Long.MAX_VALUE || endTime == 0l) {
            logger.severe("Unable to obtain any results");
            buffer = null;
        } else {
            StringBuilder hdrBuffer = new StringBuilder(1024);
            String xslPath =
                    System.getProperty("faban.xsl.path", "../../xslt/");
            if (!xslPath.endsWith("/")) {
                xslPath += '/';
            }
            hdrBuffer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            hdrBuffer.append("<?xml-stylesheet type=\"text/xsl\" href=\"").
                    append(xslPath).append("summary_report.xsl\"?>\n");
            hdrBuffer.append("<benchResults>\n");
            hdrBuffer.append("    <benchSummary name=\"").append(benchDef.name).
                    append("\" version=\"").append(benchDef.version);
            if (host != null)
                hdrBuffer.append("\" host=\"").append(host);
            hdrBuffer.append("\">\n");
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
        }
        return buffer;
    }



    /**
     * Aggregates detail results into a single buffer.
     * @param results The per-driver metrics
     * @param host The host name for which to create the detail report, or null
     * @return The report as a char sequence
     */
    private CharSequence createDetailReport(Metrics[] results, String host) {
        StringBuilder buffer = new StringBuilder(8192);
        buffer.append("Title: ").append(benchDef.name);
        if (host == null)
            buffer.append(" Detailed Results");
        else
            buffer.append(" Partial Detailed Results for Driver ").append(host);
        buffer.append("\n\n\nSection: Benchmark Information\n");
        buffer.append("Name     Value\n");
        buffer.append("-----    -------------\n");
        buffer.append("RunId    ").append(runInfo.runId);
        if (host != null) {
            buffer.append("\nPartial  true");
            buffer.append("\nHost     ").append(host);
        }
        buffer.append("\n\n\n");
        for (Metrics result : results)
            if (result != null)
                result.printDetail(buffer);

        return buffer;
    }

    public void updateMetrics(RuntimeMetrics m) {
        if (statsWriter == null) {
            logger.severe("Runtime stats disabled, yet agent is trying to " +
                          "update runtime metrics. Please log this as a bug.");
            return;
        }
        try {
            statsWriter.queue.put(m);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING,
                    "Interrupted queueing runtime stats.", e);
        }
    }

    private class RuntimeMetricsProvider
            implements PairwiseAggregator.Provider<RuntimeMetrics> {

        public ArrayList<RuntimeMetrics> metrices =
                new ArrayList<RuntimeMetrics>();

        public void add(RuntimeMetrics m) {
            metrices.add(m);
        }

        public RuntimeMetrics getMutableMetrics(int idx) {
            return metrices.get(idx);
        }

        public void add(RuntimeMetrics instance, int idx) {
            instance.add(metrices.get(idx));
        }


        public Class getComponentClass() {
            return RuntimeMetrics.class;
        }

        public void recycle(RuntimeMetrics r) {
        }

        public int getSequence() {
            if (metrices.size() == 0)
                return -1;
            else
                return metrices.get(0).sequence;
        }

        public void reset() {
            metrices.clear();
        }
    }


    private class StatsWriter extends Thread {

        boolean terminated = false;
        LinkedBlockingQueue<RuntimeMetrics> queue =
                new LinkedBlockingQueue<RuntimeMetrics>();

        private StatsWriter() {
            setName("StatsWriter");
            setDaemon(true);
            start();
        }

        @Override
        public void run() {
            int[] metricsCount = new int[agentRefs.length];
            RuntimeMetrics[] previous = new RuntimeMetrics[agentRefs.length];
            RuntimeMetrics[] current = new RuntimeMetrics[agentRefs.length];
            RuntimeMetricsProvider[] providers =
                    new RuntimeMetricsProvider[agentRefs.length];
            ArrayList<PairwiseAggregator<RuntimeMetrics>> aggregators =
                    new ArrayList<PairwiseAggregator<RuntimeMetrics>>(
                    agentRefs.length);
            for (int i = 0; i < agentRefs.length; i++) {
                providers[i] = new RuntimeMetricsProvider();
                aggregators.add(new PairwiseAggregator<RuntimeMetrics>(
                        runInfo.driverConfigs[i].numAgents, providers[i]));
            }
            while (!terminated) {
                try {
                    RuntimeMetrics m = queue.poll(
                            runInfo.runtimeStatsInterval + 1, TimeUnit.SECONDS);
                    if (m == null) {
                        continue;
                    }
                    int type = m.driverType;
                    int sequence = providers[type].getSequence();
                    if (sequence < 0) { // Empty.
                        providers[type].add(m);
                        if (++metricsCount[type] >=
                                runInfo.driverConfigs[type].numAgents) {
                            current[type] = 
                                    aggregators.get(type).collectStats();
                            dumpStats(type, previous, current);
                            providers[type].reset();
                            previous[type] = current[type];
                            current[type] = null;
                            metricsCount[type] = 0;
                        }
                    } else {
                        if (sequence == m.sequence) {
                            providers[type].add(m);
                            if (++metricsCount[type] >=
                                    runInfo.driverConfigs[type].numAgents) {
                                current[type] =
                                        aggregators.get(type).collectStats();
                                dumpStats(type, previous, current);
                                providers[type].reset();
                                previous[type] = current[type];
                                current[type] = null;
                                metricsCount[type] = 0;
                            }
                        } else if (sequence < m.sequence) {
                            logger.warning("Missing " + (runInfo.driverConfigs[
                                    type].numAgents - metricsCount[type]) +
                                    " runtime stats from " + benchDef.drivers[
                                    type].name + ". Ignoring.");
                            providers[type].add(m);
                            metricsCount[type] = 1;
                        } else {
                            logger.warning("Received out-of-sequence runtime " +
                                    "stats. Current: " +
                                    current[type].sequence + ", received: " +
                                    m.sequence + ". Ignoring.");
                        }
                    }
                } catch (InterruptedException e) {
                    logger.log(Level.FINER, "Interrupted waiting for runtime " +
                            "metrics. Stats writer terminating!", e);
                }
            }
        }

        void dumpStats(int type, RuntimeMetrics[] previous,
                                 RuntimeMetrics[] current) {
            // Purchase\Manage\Browse (TxCnt=200\200\400) 90% Resp=0.5\0.6\0.6
            // ^MMfg (TxCnt=200) 90% Resp=2.50
            if (previous[type] == null)
                return;

            double[][] s = current[type].getResults(runInfo, previous[type]);
            StringBuilder b = new StringBuilder();
            Formatter formatter = new Formatter(b);

            formatter.format("%.02f", current[type].timestamp / 1000d);
            b.append("s - ").append(benchDef.drivers[type].name).append(": ");
            b.append(benchDef.drivers[type].operations[0].name);
            for (int j = 1; j < benchDef.drivers[type].operations.length; j++) {
                b.append('/');
                b.append(benchDef.drivers[type].operations[j].name);
            }

            for (int i = 0; i < s.length; i++) {
                b.append(' ').append(RuntimeMetrics.LABELS[i]).append('=');
                if (Double.isNaN(s[i][0]))
                    b.append('-');
                else
                    formatter.format("%.03f", s[i][0]);
                for (int j = 1; j < s[i].length; j++) {
                    b.append('/');
                    if (Double.isNaN(s[i][j]))
                        b.append('-');
                    else
                        formatter.format("%.03f", s[i][j]);
                }
            }
            
            logger.info(b.toString());
        }

        void quit() {
            terminated = true;
            interrupt();
        }
    }

    /**
     * Obtain the master's time for time adjustment.
     *
     * @return The current time on the master
     */
    public long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    /**
     * Introduces a state change in the master.
     * @param newState The new state
     */
    protected void changeState(MasterState newState) {
        synchronized (stateLock) {
            state = newState;
            stateLock.notifyAll();
        }
    }


    /**
     * Obtains the current state of the master.
     * @return The current state of the master.
     */
    public MasterState getCurrentState() {
        /*
         * State is an int and changed by int assignment which is atomic.
         * So we will get one or the other state, but not something in between.
         * synchronization is not necessary.
         */
        return state;
    }

    /**
     * Wait for a certain state on the master.
     * @param state The state to wait for
     */
    public void waitForState(MasterState state) {
        synchronized (stateLock) {
            while (state.compareTo(this.state) > 0) {
				try {
                    stateLock.wait();
                } catch (InterruptedException e) {
                    logger.log(Level.FINE, "Interrupted waiting for state!",
                               e);
                }
			}
        }
    }

    /**
     * Over-estimate the time it takes to ping all agents.
     * @return A time in ms more than enough to ping all agents
     */
    int estimateCommsTime() {
        int agentCount = 0;
        for (Agent[] agentRef : agentRefs) {
			if (agentRef != null) {
				agentCount += agentRef.length;
			}
		}

        // Given 100ms per agent (more than enough), Calculate the total time.
        int time = 100 * agentCount;

        // Minimum of 2 secs.
        if (time < 3000) {
			time = 3000;
		}

        return time;
    }

    /**
     * Waits for all threads in all agents to start.
     */
    public void waitForThreadStart() {
        if (agentRefs != null) {
			for (int i = 0; i < agentRefs.length && !runAborted; i++) {
				if (agentRefs[i] != null) {
					for (int j = 0; j < agentRefs[i].length; j++) {
						try {
                            agentRefs[i][j].waitForThreadStart();
                        } catch (RemoteException e) {
                            logger.log(Level.SEVERE,
                                    "Error checking thread starts.", e);
                        }
					}
				}
			}
		}
    }

    /**
     * Sets the benchmark start time after all threads are started.
     * @param relTime The ms time from the run epoch
     */
    public void setStartTime(int relTime) {
        runInfo.benchStartTime = relTime;
        runInfo.start = timer.toAbsMillis(relTime);
        if (agentRefs != null) {
			for (int i = 0; i < agentRefs.length && !runAborted; i++) {
				if (agentRefs[i] != null) {
					for (int j = 0; j < agentRefs[i].length; j++) {
						try {
                            agentRefs[i][j].setStartTime(relTime);
                        } catch (RemoteException e) {
                            logger.log(Level.SEVERE,
                                    "Error checking thread starts.", e);
                        }
					}
				}
			}
		}
    }

    /**
     * Notifies the master to terminate the run immediately.
     * This usually happens if there is a fatal error in the run.
     */
    public synchronized void abortRun() {

        if (runAborted) { // We only need to schedule the killAll once.
			return;
		}

        runAborted = true;
        changeState(MasterState.ABORTED);

        // Note: This is a remote call. We cannot terminate the
        // run right here. We need to schedule the termintation
        // asynchronously.
        TimerTask killAll = new TimerTask() {

			public void run() {
                if (agentRefs != null) {
                    for (int i = 0; i < agentRefs.length; i++) {
						if (agentRefs[i] != null) {
							for (int j = 0; j < agentRefs[i].length; j++) {
								try {
                                    agentRefs[i][j].kill();
                                } catch (RemoteException e) {
                                    logger.log(Level.SEVERE,
                                            "Error calling kill on agent.", e);
                                }
							}
						}
					}
                    for (int i = 0; i < agentRefs.length; i++) {
						if (agentRefs[i] != null) {
							for (int j = agentRefs[i].length - 1; j > 0; j--) {
								try {
                                    agentRefs[i][j].join();
                                } catch (RemoteException e) {
                                    logger.log(Level.SEVERE,
                                            "Error calling join on agent.", e);
                                }
							}
						}
					}
                    for (int i = 0; i < agentRefs.length; i++) {
						if (agentRefs[i] != null) {
							if (agentRefs[i] != null &&
                                    agentRefs[i][0] != null) {
								try {
                                    agentRefs[i][0].join();
                                } catch (RemoteException e) {
                                    logger.log(Level.SEVERE,
                                            "Error calling join on agent.", e);
                                }
							}
						}
					}
                    for (int i = 0; i < agentRefs.length; i++) {
						if (agentRefs[i] != null) {
							if (agentRefs[i][0] != null) {
								try {
                                    agentRefs[i][0].postRun();
                                } catch (RemoteException e) {
                                    logger.log(Level.SEVERE,
                                            "Error calling postRun on agent.",
                                            e);
                                }
							}
						}
					}
                }
                logger.severe("Run aborted. Master terminating!");
                System.exit(1);
            }
        };
        scheduler.schedule(killAll, 1000);
    }


    /**
     * The main method to start the master. No arguments
     * are required. The -noexit argument will cause the master
     * to wait. The only actual expectation is the benchmark.properties
     * property pointing to the properties file.
     * @param args The command line arguments are ignored.
     */
    public static void main(String[] args) {

        // Check whether -noexit is set.
        boolean normalExit = true;
        for (String arg : args) {
            if ("-noexit".equals(arg)) {
                normalExit = false;
                break;
            }
        }
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
            if (normalExit) {
				System.exit(0);
			}
        } catch (Throwable t) {
            m.logger.log(Level.SEVERE, "Master terminated with errors.", t);
            System.exit(1);
        }
    }
}
