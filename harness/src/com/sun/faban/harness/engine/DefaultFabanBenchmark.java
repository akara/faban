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
 * $Id: DefaultFabanBenchmark.java,v 1.1 2006/06/29 18:51:42 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Run;

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

    private Logger logger;
    protected Run run;
    protected ParamRepository params;
    protected List agents;

    /**
     * This method is called to configure the specific benchmark run
     * Tasks done in this method include reading user parameters,
     * logging them and initializing various local variables.
     *
     * @param r   Run object that identifies this run
     * @param par ParamRepository for this run
     * @return true if configuration was successful,
     *         false otherwise (abort run)
     */
    public boolean configure(Run r, ParamRepository par) throws Exception {
        logger = Logger.getLogger(getClass().getName());
        params = par;
        run = r;

        // Update the output directory to the one assigned by the harness.
        try {
            params.setParameter("runConfig/outputDir", run.getOutDir());
            params.save();
            return true;
        } catch(Exception e) {
            logger.severe("Exception updating " + run.getParamFile() + " : " + e);
            logger.log(Level.FINE, "Exception", e);
            return false;
        }
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
        String cmd = null;

        int hostIdx = 0;
        for (int i = 0; i < agents.size(); i++) {
            String agentName = (String) agents.get(i);
            int numAgents = Integer.parseInt(params.getParameter(
                    "runConfig/driverConfig[@name=\"" + agentName +
                    "\"]/agents").trim());
            for (int j = 0; j < numAgents; j++) {
                host[0] = agentHosts[hostIdx];
                cmd = "com.sun.faban.driver.core.AgentImpl " + agentName + " " +
                      j + " " + CmdService.getHandle().getMaster();
                CmdService.getHandle().startJavaCmd(host, cmd,
                                                    agentName + '.' + j, null);
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

        // Start the driver
        cmd = "-Dbenchmark.config=" + run.getParamFile() +
                " -Dfaban.outputdir.unique=true com.sun.faban.driver.core.MasterImpl";

        String ident = "Master";
        String[ ] master = { CmdService.getHandle().getMaster() };

        CmdService.getHandle().startJavaCmd(master, cmd, ident, null);
        // Start the monitoring tools using ToolService.
        int delay = Integer.parseInt(params.getParameter("runControl/rampUp").trim());
        int stdyState = Integer.parseInt(params.getParameter("runControl/steadyState").trim());
        ToolService.getHandle().start(delay, stdyState);

        // Wait for the master to complete the run.
        if(!CmdService.getHandle().wait(master[0], ident)) {
            throw new Exception("Driver failed to complete benchmark run");
        }
    }

    /**
     * This method aborts the current benchmark run and is
     * called when a user asks for a run to be killed
     */
    public void kill() {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}