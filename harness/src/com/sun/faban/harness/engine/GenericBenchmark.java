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

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.HostRoles;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.services.ServiceManager;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
    private ServiceManager serviceMgr = null;
    //private Benchmark bm = null;
    private static BenchmarkWrapper bmw = null;

    private static Logger logger =
            Logger.getLogger(GenericBenchmark.class.getName());

    // Flag to detect failed run
    private int runStatus = Run.FAILED;
    private int stdyState = 0;

    /**
     * Constructor.
     * @param r run
     */
    public GenericBenchmark(Run r) {
        this.run = r;
    }

    /**
     * Responsible for configuring, starting and stopping services and tools.
     * Creates the actual benchmark object and requests it to execute the run.
     */
    @SuppressWarnings("static-access")
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
        //bm = newInstance(benchDesc);
        bmw = BenchmarkWrapper.getInstance(benchDesc);

        if (bmw == null)
            return;

        startTime = System.currentTimeMillis();

        // Update the status of the run
        try {
            run.updateStatus(Run.STARTED);
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
                bmw.validate();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Benchmark validation failed.", t);
                return;
            }

            try {
                // Initialize CmdService
                logger.fine("Initializing Command Service");

                cmds = new CmdService();
                cmds.init();

                // We need to place a marker into the Benchmark's META-INF
                // directory so if it is shared, download won't happen.
                FileWriter runIdFile = new FileWriter(Config.BENCHMARK_DIR +
                                run.getBenchmarkName() + File.separator +
                                "META-INF" + File.separator + "RunID");
                runIdFile.write(run.getRunId());
                runIdFile.close();

                // Start CmdAgent on all ENABLED hosts using the JAVA HOME
                // Specified JVM options will be used by the Agent when it
                // starts java processes
                if (!cmds.setup(benchDesc.shortName, par)) {
                logger.severe("CmdService setup failed. Exiting");
                    return;
                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Start failed.", e);
                return;
            }

            // Extract host metadata and save it.
            HostRoles hr = new HostRoles(par);
            cmds.setHostRoles(hr);
            try {
                hr.write(run.getOutDir() + File.separator + "META-INF" +
                                        File.separator + "hosttypes");
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error writing hosttypes file!", e);
            }

            // Reading parameters needed for ToolService
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

            int len = -1;
            s = par.getParameter("fa:runControl/fa:steadyState");
            if (s != null) {
                s = s.trim();
                len = s.length();
            }
            if (len > 0) {
                try {
                    stdyState = Integer.parseInt(s);
                } catch (NumberFormatException e) {
                    logger.log(Level.SEVERE,
                            "Parameter steadyState is not a number.", e);
                    return;
                }
                if (stdyState <= 0) {
                    logger.severe(
                            "Parameter steadyState must be more than zero.");
                    return;
                }
            }


            // Now, process generic server parameters
            logger.fine("Processing Generic Parameters");
            try {
                server = new ServerConfig(run, par);
            } catch (ConfigurationException e) {
                logger.log(Level.SEVERE, e.getMessage(), e);
                return;
            }

            // Set # of cpus
            if (!server.set(cmds)) {
                logger.severe("'psradm' Failed! Aborting run.");
                return;
            }

            // Gather /etc/system, prtdiag, psrinfo, uname, ps , vxprint
            logger.info("Gathering system configuration");
            server.get();

            // Configure the benchmark before starting services
            try {
                bmw.configure();
                logger.fine("Configured benchmark " + benchDesc.name);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Run configuration failed!", t);
                return;
            }

            // Deal with the services.
            serviceMgr = new ServiceManager(par, run);
            logger.fine("Got Service Manager Instance");
            // transfer service configuration.
            serviceMgr.configure();
            logger.fine("Executed services configure method");
            // start services
            serviceMgr.startup();
            serviceMgr.getConfig();
            logger.fine("Executed services Startup method");

            // Initialize ToolService, call setup with cmds as parameter
            logger.fine("Initializing Tool Service");
            tools = ToolService.getHandle();
            logger.finer("Got Tool Service Handle");
            tools.init();
            logger.finer("Tool Service Inited");
            tools.setup(par, run.getOutDir(), serviceMgr);	// If Tools setup fails,
                                                // we ignore it
            logger.finer("Tool Service Set Up");

            // Calling PreRun to prepare the data, etc.
            try {
                logger.fine("Calling PreRun for " + benchDesc.name);
                bmw.preRun();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "PreRun failed!", t);
                return;
            }

            // Now run the benchmark
            try {
                bmw.start();
                logger.fine("Started benchmark " + benchDesc.name);
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Run start failed!", t);
                return;
            }

            // Start the tools
            try {
                if (stdyState > 0) {
                    tools.start(delay, stdyState);
                    logger.fine("Started tools with tools." +
                            "start(delay, stdyState)");
                }else{
                    tools.start(delay);
                    logger.fine("Started tools with tools.start(delay)");
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "ToolService not started.", e);
                // Keep running. Failing the tools should not fail the
                // benchmark.
            }

            // Wait and end the benchmark
            try {
                bmw.end();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "Run end failed!", t);
                return;
            }

            // s represents the string value of steady state.
            // We only call stop here for benchmarks that do not have
            // a firm length, i.e. no steady state.
            if (s == null || s.length() == 0) {
                logger.fine("Stop called for tools");
                tools.stop();
                logger.fine("Stopped tools");
            }

            // Even if the run got killed, we can arrive here.
            // So we need to check the killed flag first.
            if (runStatus != Run.KILLED) {
                tools.waitFor();
                runStatus = Run.COMPLETED;
            }

            serviceMgr.getLogs();

            try {
                // Postprocessing may need tools output. So the postRun
                // must be called after all tools and services are done.
                logger.fine("Calling PostRun for " + benchDesc.name);
                bmw.postRun();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "PostRun failed!", t);
                return;
            }

            // Benchmark complete, Capture system messages
            endTime = System.currentTimeMillis();
            logger.info("Benchmark Complete");

            server.report(startTime, endTime);

            logger.info("END TIME : " + new java.util.Date());

            return;
        } catch (Throwable t) {
            logger.log(Level.SEVERE,
                    "Unexpected Exception processing benchmark.", t);
        } finally { // Ensure we kill the processes in any case.
            if (serviceMgr != null)
                serviceMgr.shutdown();
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

    /**
     * Method : kill
     * This method is called externally to abort or terminate
     * the current run.
     */
    public void kill() {
        runStatus = Run.KILLED;
        _kill();
    }

    /**
     * This method is called internally to terminate the current run.
     */
    private void _kill() {

        try {
            run.updateStatus(runStatus);
        } catch (IOException e) {
            logger.log(Level.SEVERE,  "Failed to update run status.", e);
        }

        if(bmw != null) {
            logger.info("Killing benchmark");
            try {
                bmw.kill();
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

    /**
     * Responsible for doing the postprocessing.
     * @return true if successful
     */
    private boolean postProcess() {
        // Create the dir for storing Xanadu XMLs
        String outDir = run.getOutDir();

        String postDir = outDir + File.separator + Config.POST_DIR;
        if(!(new File(postDir)).mkdirs())
            return false;

		// Process the text using FenXi.
        /** Not required. We generate the graphs dynamically
		Command fenxi = new Command ("fenxi", "process", outDir, postDir,
                                                            run.getRunId());
        fenxi.setWorkingDirectory(Config.FABAN_HOME + "logs");

		try {
            CommandHandle handle = cmds.execute(fenxi, null);
            if (handle.exitValue() != 0)
                logger.severe("FenXi process command " + fenxi + " Failed");
        } catch(Exception e) {
            logger.log(Level.SEVERE, "FenXi process command " + fenxi +
                    " failed.", e);
            return false;
        }
         */
        return true;
    }
}
