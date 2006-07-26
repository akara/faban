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
 * $Id: GenericBenchmark.java,v 1.4 2006/07/26 06:58:10 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.BenchmarkDescription;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.agent.CmdAgent;
import com.sun.faban.harness.agent.CmdAgentImpl;
import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
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
    private Logger logger;
    private Benchmark bm = null;

    public static final int COMPLETED = 0;
    public static final int FAILED = 1;
    public static final int KILLED = 2;
    
    public static final String[] COMPLETIONMESSAGE =
            { "COMPLETED", "FAILED", "KILLED" };

    // Flag to detect failed run
    private int runStatus = FAILED;

    public GenericBenchmark(Run r) {
        logger = Logger.getLogger(this.getClass().getName());
        this.run = r;
    }

    public void start() {
        ParamRepository par = null;
        ServerConfig server;

        // Create benchmark object
        BenchmarkDescription benchDesc = run.getBenchDesc();

        long startTime;	// benchmark start/end time
        long endTime;

        startTime = System.currentTimeMillis();

        // Update the status of the run
        this.updateResultInfoFile("STARTED");

        // Read in user parameters
        logger.info("START TIME : " + new java.util.Date());

        logger.fine("Reading in user parameters");
        try {
            try {
                par = new ParamRepository(run.getParamFile());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to open ParamRepository "
                        + run.getParamFile() + '.', e);
                return;		// can't proceed with benchmark
            }

            try {
                // Initialize CmdService
                logger.fine("Initializing Command Service");

                cmds = CmdService.getHandle();
                cmds.init();

                String javaHome = par.getParameter("jvmConfig/javaHome");
                if(!(new File(javaHome)).isDirectory()) {
                    logger.severe("Cannot set JAVA_HOME. " + javaHome +
                            " is not set. Exiting");
                    return;
                }

                String jvmOpts = par.getParameter("jvmConfig/jvmOptions");
                if((jvmOpts == null) || (jvmOpts.trim().length() == 0))
                    jvmOpts = "";

                // Start CmdAgent on all ENABLED hosts using the JAVA HOME
                // Specified JVM options will be used by the Agent when it
                // starts java processes
                ArrayList enabledHosts = new ArrayList();
                List hosts = par.getTokenizedParameters("hostConfig/host");
                List enabled = par.getParameters("hostConfig/enabled");
                if(hosts.size() != enabled.size()) {
                    logger.severe("Number of &lt;host&gt; (" + hosts.size() +
                            ") does not match &lt;enabled&gt; (" +
                            enabled.size() + ")");
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

            logger.fine("Instantiating benchmark class " +
                    benchDesc.benchmarkClass);
            bm = newInstance(benchDesc);
            if (bm == null)
                return;

            // Configure benchmark
            try {
                bm.configure(run, par);
                logger.fine("configured benchmark " + benchDesc.name);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Run configuration failed!", e);
                return;
            }

            // Now run the benchmark
            try {
                bm.start();
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Run failed!", e);
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
            _kill();
            postProcess();
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

        updateResultInfoFile(COMPLETIONMESSAGE[runStatus]);

        if(bm != null) {
            logger.info("Killing benchmark");
            bm.kill();
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

    private void updateResultInfoFile(String status) {
        // Update the resultinfo file with Status
        try {
            File resultInfo = new File(run.getOutDir(), Config.RESULT_INFO);
            resultInfo.delete();
            resultInfo.createNewFile();
            BufferedWriter resBuf = new BufferedWriter(new FileWriter(resultInfo));
            resBuf.write(status);
            resBuf.flush();
            resBuf.close();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE,  "Failed to update Result Info File.", e);
        }
    }

    private boolean postProcess() {
        // Create the dir for storing Xanadu XMLs
        String outDir = run.getOutDir();

        String xanaduDir = outDir + File.separator + "xanadu";
        if(!(new File(xanaduDir)).mkdirs())
            return false;

        String xanaduCommand = "xanadu" + File.separator + "scripts" +
                               File.separator + "xanadu";

        // Text => xml
        Command xanadu = new Command(xanaduCommand + " import " + outDir + " " +
                xanaduDir + " " + run.getRunName());

        CmdAgent masterAgent = null;
        try {
            // Obtain the local CmdAgent
            masterAgent = (CmdAgent) CmdAgentImpl.getRegistry().
                    getService(Config.CMD_AGENT);
            CommandHandle handle = masterAgent.execute(xanadu);
            if (handle.exitValue() != 0)
                logger.severe("Xanadu Import command " + xanadu + " Failed");
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Xanadu Import command " + xanadu +
                    " failed.", e);
            return false;
        }

        // Move detail file to xanadu directory for postprocessing
        File detailFile = new File(outDir, "detail.xml");
        File detailDest = new File(outDir, "xanadu" + File.separator +
                                   "detail.xml");
        if (detailFile.exists())
            if (!detailFile.renameTo(detailDest))
                logger.warning("Cannot move detail file to Xanadu directory");

        // xml => html + graphs
        xanadu = new Command(xanaduCommand + " export " + xanaduDir + " " + outDir);
        try {            
            CommandHandle handle = masterAgent.execute(xanadu);
            if (handle.exitValue() != 0)
                logger.severe("Xanadu Export command " + xanadu + " Failed");
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Xanadu Export command " + xanadu +
                    " failed.", e);
            return false;
        }
        return true;
    }
}
