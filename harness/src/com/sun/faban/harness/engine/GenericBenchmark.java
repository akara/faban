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
 * $Id: GenericBenchmark.java,v 1.27 2008/04/04 22:09:26 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.Benchmark;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.HostTypes;
import com.sun.faban.harness.common.Run;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GenericBenchmark.java
 * The Generic Benchmark object is created by the RunDaemon to execute
 * a run. It starts up the various Services (CmdService, Servers
 * and ToolService), reads the run's Parameters and processes
 * the generic parameters.
 * It then performs generic system logging of /etc/system, prtdiag
 * psrinfo, etc.
 * Finally, it creates the actual benchmark object and requests it
 * to execute the run.
 * When the run completes, it captures more info for system reports
 * and exits.
 * @author Ramesh Ramachandran
 */
public class GenericBenchmark {
    private Run run;
    private CmdService cmds = null;
    private ToolService tools = null;
    private Benchmark bm = null;

    private static Logger logger =
            Logger.getLogger(GenericBenchmark.class.getName());

    public static final int COMPLETED = 0;
    public static final int FAILED = 1;
    public static final int KILLED = 2;
    
    public static final String[] COMPLETIONMESSAGE =
            { "COMPLETED", "FAILED", "KILLED" };

    // Flag to detect failed run
    private int runStatus = FAILED;

    public GenericBenchmark(Run r) {
        this.run = r;
    }

    public void start() {
        ParamRepository par = null;
        ServerConfig server;

        // Read the benchmark description.
        BenchmarkDescription benchDesc = run.getBenchDesc();

        long startTime;	// benchmark start/end time
        long endTime;

        // Create benchmark object.
        logger.fine("Instantiating benchmark class " +
                benchDesc.benchmarkClass);
        bm = newInstance(benchDesc);
        if (bm == null)
            return;

        startTime = System.currentTimeMillis();

        // Update the status of the run
        try {
            run.updateStatus("STARTED");
        } catch (IOException e) {
            logger.log(Level.SEVERE,  "Failed to update run status.", e);
            return;
        }

        // Read in user parameters
        logger.info("START TIME : " + new java.util.Date());

        logger.fine("Reading in user parameters");
        try {
            try {
                par = new ParamRepository(run.getParamFile(), true);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to open ParamRepository "
                        + run.getParamFile() + '.', e);
                return;		// can't proceed with benchmark
            }

            // Create the facade for the benchmark to access.
            RunFacade.newInstance(run, par);

            try {
                bm.validate();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Benchmark validation failed.", t);
                return;
            }

            try {
                // Initialize CmdService
                logger.fine("Initializing Command Service");

                cmds = CmdService.getHandle();
                cmds.init();

                // We need to place a marker into the Benchmark's META-INF
                // directory so if it is shared, download won't happen.
                FileWriter runIdFile = new FileWriter(Config.BENCHMARK_DIR +
                                run.getBenchmarkName() + File.separator +
                                "META-INF" + File.separator + "RunID");
                runIdFile.write(run.getRunId());
                runIdFile.close();



                String javaHome =
                        par.getParameter("fh:jvmConfig/fh:javaHome").trim();
                if(!(new File(javaHome)).isDirectory()) {
                    logger.severe("Cannot set JAVA_HOME. " + javaHome +
                            " is not set. Exiting");
                    return;
                }

                String jvmOpts =
                        par.getParameter("fh:jvmConfig/fh:jvmOptions").trim();
                if((jvmOpts == null) || (jvmOpts.trim().length() == 0))
                    jvmOpts = "";

                // Start CmdAgent on all ENABLED hosts using the JAVA HOME
                // Specified JVM options will be used by the Agent when it
                // starts java processes
                ArrayList enabledHosts = new ArrayList();
                List hosts = par.getTokenizedParameters(
                                                    "fa:hostConfig/fa:host");
                List enabled = par.getParameters("fa:hostConfig/fh:enabled");
                if(hosts.size() != enabled.size()) {
                    logger.severe("Number of hosts, " + hosts.size() +
                            ", does not match enabled, " +
                            enabled.size() + ".");
                    return;
                }
                else {
                    for(int i = 0; i < hosts.size(); i++) {
                        if(Boolean.valueOf((String) enabled.get(i)).
                                booleanValue()) {
                            enabledHosts.add(hosts.get(i));
                        }
                    }
                    String[][] hostArray = (String[][])
                            enabledHosts.toArray(new String[1][1]);
                    if (!cmds.setup(benchDesc.shortName,
                            hostArray, javaHome, jvmOpts)) {
                        logger.severe("CmdService setup failed. Exiting");
                        return;
                    }
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Start failed.", e);
                return;
            }

            // Extract host metadata and save it.
            HostTypes ht = new HostTypes(par);
            cmds.hostTypes = ht;
            try {
                ht.write(run.getOutDir() + File.separator + "META-INF" +
                                        File.separator + "hosttypes");
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error writing hosttypes file!", e);
            }

            // Reading parameters used by ToolService
            String s = par.getParameter("fa:runControl/fa:rampUp");
            if (s != null)
                s = s.trim();
            if (s == null || s.length() == 0) {
                logger.severe("Configuration runControl/rampUp not set.");
                return;
            }
            int delay = 0;
            try {
                delay = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE,
                        "Parameter rampUp is not a number.", e);
                return;
            }
            if (delay < 0) {
                logger.severe("Parameter rampUp is negative");
                return;
            }

            s = par.getParameter("fa:runControl/fa:steadyState");
            if (s != null)
                s = s.trim();
            if (s == null || s.length() == 0) {
                logger.severe("Configuration runControl/steadyState not set.");
                return;
            }

            int stdyState = 0;
            try {
                stdyState = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE,
                        "Parameter steadyState is not a number.", e);
                return;
            }
            if (stdyState <= 0) {
                logger.severe("Parameter steadyState must be more than zero.");
                return;
            }

            // Initialize ToolService, call setup with cmds as parameter
            logger.fine("Initializing Tool Service");
            tools = ToolService.getHandle();
            logger.finer("Got Tool Service Handle");
            tools.init();
            logger.finer("Tool Service Inited");
            tools.setup(par, run.getOutDir());	// If Tools setup fails,
                                                // we ignore it
            logger.finer("Tool Service Set Up");

            // Now, process generic server parameters
            logger.fine("Processing Generic Parameters");
            server = new ServerConfig(run, par);

            // Set # of cpus
            if (!server.set(cmds)) {
                logger.severe("'psradm' Failed! Aborting run.");
                return;
            }

            // Gather /etc/system, prtdiag, psrinfo, uname, ps , vxprint
            logger.info("Gathering system configuration");
            server.get();

            // Log parameters that were changed since last run.


            // Configure benchmark
            try {
                bm.configure();
                logger.fine("Configured benchmark " + benchDesc.name);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Run configuration failed!", t);
                return;
            }

            // Now run the benchmark
            try {
                bm.start();
                logger.fine("Started benchmark " + benchDesc.name);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Run start failed!", t);
                return;
            }

            // Start the tools
            try {
                ToolService.getHandle().start(delay, stdyState);
            } catch (Exception e) {
                logger.log(Level.WARNING, "ToolService not started.", e);
                // Keep running. Failing the tools should not fail the
                // benchmark.
            }

            // Wait and end the benchmark
            try {
                bm.end();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Run end failed!", t);
                return;
            }

            // Benchmark complete, Capture system messages
            endTime = System.currentTimeMillis();
            logger.info("Benchmark Complete");

            server.report(startTime, endTime);

            logger.info("END TIME : " + new java.util.Date());

            // Even if the run got killed, we can arrive here.
            // So we need to check the killed flag first.
            if (runStatus != KILLED) {
                runStatus = COMPLETED;
            }
            return;

        } finally { // Ensure we kill the processes in any case.
            postProcess();
            _kill();
            // We need to place a marker into the Benchmark's META-INF directory
            // so if it is shared, download won't happen.
            File runIdFile = new File(Config.BENCHMARK_DIR +
                                run.getBenchmarkName() + File.separator +
                                "META-INF" + File.separator + "RunID");
            if (runIdFile.exists())
                runIdFile.delete();

            RunFacade.clearInstance();
        }
    }

    private Benchmark newInstance(BenchmarkDescription desc) {
        Benchmark benchmark = null;
        BenchmarkClassLoader loader = BenchmarkClassLoader.getInstance(
                desc.shortName, this.getClass().getClassLoader());
        if (loader != null)
            try {
                benchmark = (Benchmark) Class.forName(desc.benchmarkClass,
                        true, loader).asSubclass(Benchmark.class).newInstance();
            } catch (ClassNotFoundException e) {
                logger.warning("Cannot find class " + desc.benchmarkClass +
                        " within the Faban Harness.");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error instantiating " +
                        desc.benchmarkClass + '.', e);
            }

        // In some cases, the benchmark class is in the Faban package itself.
        if (benchmark == null) {
            logger.info("Trying reading class " + desc.benchmarkClass +
                        " from Faban Harness.");
            try {
                benchmark = (Benchmark) Class.forName(desc.benchmarkClass).
                            asSubclass(Benchmark.class).newInstance();
            } catch (ClassNotFoundException e) {
                logger.severe("Cannot find class " + desc.benchmarkClass +
                              " within the Faban Harness.");
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error instantiating " +
                           desc.benchmarkClass + '.', e);
            }
        }
        return benchmark;
    }

    /**
     * Method : kill
     * This method is called externally to abort or terminate 
     * the current run.
     */
    public void kill() {
        runStatus = KILLED;
        _kill();
    }

    /**
     * This method is called internally to terminate the current run.
     */ 
    private void _kill() {

        try {
            run.updateStatus(COMPLETIONMESSAGE[runStatus]);
        } catch (IOException e) {
            logger.log(Level.SEVERE,  "Failed to update run status.", e);
        }

        if(bm != null) {
            logger.info("Killing benchmark");
            try {
                bm.kill();
            } catch (Throwable t) {
                logger.log(Level.WARNING, "Exceptions killing benchmark.", t);
            }
        }

        if (tools != null) {
            logger.fine("Calling tools.kill");
            tools.kill();
        }
        if (cmds != null) {
            logger.fine("Calling cmds.kill");
            cmds.kill();
        }
    }

    private boolean postProcess() {
        // Create the dir for storing Xanadu XMLs
        String outDir = run.getOutDir();

        String fenxiDir = outDir + File.separator + Config.XML_STATS_DIR;
        if(!(new File(fenxiDir)).mkdirs())
            return false;

		// Process the text using FenXi.
		Command fenxi = new Command ("fenxi", "process", outDir, outDir,
                                                            run.getRunId());
        fenxi.setWorkingDirectory(Config.FABAN_HOME + "logs");

		try {
            CommandHandle handle = cmds.execute(fenxi);
            if (handle.exitValue() != 0)
                logger.severe("FenXi process command " + fenxi + " Failed");
        } catch(Exception e) {
            logger.log(Level.SEVERE, "FenXi process command " + fenxi +
                    " failed.", e);
            return false;
        }
        return true;
    }
}
