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
 * $Id: DefaultFabanBenchmark.java,v 1.6 2006/10/20 22:39:08 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness;

import static com.sun.faban.harness.RunContext.*;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The default benchmark class for use with benchmarks implemented with the
 * Faban Driver Framework. This class is designed to be extended if additional
 * features are desired. Do not use the DefaultFabanBenchmark if the actual
 * driver is not implemented using the Faban Driver Framework. Implement the
 * Benchmark interface directly in such cases.
 *
 * @author Akara Sucharitakul
 */
public class DefaultFabanBenchmark implements Benchmark {

    private Logger logger = Logger.getLogger(getClass().getName());;
    protected ParamRepository params;
    protected List agents;
    protected String ident;
    protected String[] master;
    protected CommandHandle masterHandle;

    /**
     * Allows benchmark to validate the configuration file. Note that no
     * execution facility is available during validation.
     *
     * @throws Exception if any error occurred.
     * @see RunContext#exec(com.sun.faban.common.Command)
     */
    public void validate() throws Exception {
        params = getParamRepository();

        // Update the output directory to the one assigned by the harness.
        try {
            params.setParameter("runConfig/outputDir", getOutDir());
            params.save();
        } catch(Exception e) {
            logger.severe("Exception updating " + getParamFile() + " : " + e);
            logger.log(Level.FINE, "Exception", e);
            throw e;
        }
    }

    /**
     * This method is called to configure the specific benchmark run
     * Tasks done in this method include reading user parameters,
     * logging them and initializing various local variables.
     *
     * @throws Exception if any error occurred.
     */
    public void configure() throws Exception {
        // No configuration needed.
    }

    /**
     * This method is responsible for starting the benchmark run
     */
    public void start() throws Exception {

        // First, list the drivers in the config file.
        agents = params.getAttributeValues(
                "runConfig/driverConfig", "name");

        // Second, obtain the systems to run the drivers.
        String[] agentHosts = params.getTokenizedValue("runConfig/hostConfig/host");

        // Then, spread the drivers to the systems.
        // Start Agents. Other JVM Options like security policy and logging properties are added by CmdAgent when
        // starting the java command.
        String[] host = new String[1];
        Command c = null;

        int hostIdx = 0;
        for (int i = 0; i < agents.size(); i++) {
            String agentName = (String) agents.get(i);
            int numAgents = Integer.parseInt(params.getParameter(
                    "runConfig/driverConfig[@name=\"" + agentName +
                    "\"]/agents").trim());
            for (int j = 0; j < numAgents; j++) {
                host[0] = agentHosts[hostIdx];
                Command agent = new Command("com.sun.faban.driver.core.AgentImpl " + agentName + " " +
                      j + " " + getMaster());
                agent.setSynchronous(false);
                java(host, agent);
                ++hostIdx;
                if (hostIdx >= agentHosts.length)
                    hostIdx = 0;
                //Wait for the Agents to register
                try {
                    Thread.sleep(500);
                } catch(InterruptedException e) {
                    logger.severe("Exception Sleeping : " + e);
                    logger.log(Level.FINE, "Exception", e);
                }
            }
        }

        //Wait for all the Agents to register
        try {
            Thread.sleep(5000);
        } catch(InterruptedException e) {
            logger.severe("Exception Sleeping : " + e);
            logger.log(Level.FINE, "Exception", e);
        }

        // Start the master
        c = new Command("-Dbenchmark.config=" + getParamFile() +
                " -Dfaban.outputdir.unique=true com.sun.faban.driver.core.MasterImpl");
        c.setSynchronous(false);

        masterHandle = java(c);

        // Wait until the master gets to rampup before we give back control.
        // This will ensure the tools start at correct times.
        java(new Command("com.sun.faban.driver.core.MasterState RAMPUP"));
        logger.info("Rampup started!");
    }

    /**
     * This method is responsible for waiting for all commands started and
     * run all postprocessing needed.
     *
     * @throws Exception if any error occurred.
     */
    public void end() throws Exception {

        // Wait for the master to complete the run.
        masterHandle.waitFor();
        if (masterHandle.exitValue() != 0)
            throw new Exception("Driver failed to complete benchmark run");
    }

    /**
     * This method aborts the current benchmark run and is
     * called when a user asks for a run to be killed
     *
     * @throws Exception if any error occurred.
     */
    public void kill() throws Exception {
        // We don't need to kill off anything here. All processes managed
        // by the run context are automatically terminated.
    }
}