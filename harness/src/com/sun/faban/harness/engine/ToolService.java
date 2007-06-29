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
 * $Id: ToolService.java,v 1.5 2007/06/29 08:36:44 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.agent.ToolAgent;
import com.sun.faban.harness.agent.ToolAgentImpl;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.ParamRepository;

import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This file contains the class that implements the Tool service API.
 * The Tool Service object is created by the Engine at the start of a
 * run. The Tool Service will interpret the Tools specific parameters in
 * the ParamRepository and start up the ToolAgents on all the
 * machines on which the user requested that tools be run. It then
 * connects to the ToolAgents via RMI. In the API implementation,
 * it identifies the particular ToolAgent and passes the call along.
 * IMPORTANT: There should be a single ToolService object in the
 * entire framework or else multiple copies of the ToolAgent
 * application will be spawned on the target machines.
 * For this reason, this class is a Singleton.
 *
 * @author Ramesh Ramachandran
 */
final public class ToolService {
    private ToolAgent[] toolAgents;
    private String[] hostNames;
    private Logger logger;
    private CmdService cmds;
    private boolean runTools;

    private static ToolService toolService;

    private ToolService() {
        runTools = false;
    }

    /**
     * This method is the only way that an external object
     * can get a reference to the singleton ToolService
     * @return reference to the single ToolService
     */
    public static ToolService getHandle() {
        if(toolService == null)
            toolService = new ToolService();
        return toolService;
    }

    public void init() {
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
     * This method initializes the ToolAgent RMI server processes
     * on the specified set of machines
     * @return true if setup successful, else false
     *
     */
    public boolean setup(ParamRepository par, String outDir) {

        cmds = CmdService.getHandle();

        /* Get tool related parameters */

        List hostClasses = par.getTokenizedParameters("fa:hostConfig/fa:host");
        List allTools =  par.getParameters("fa:hostConfig/fh:tools");
        List enabled = par.getParameters("fa:hostConfig/fh:enabled");

        if(hostClasses.size() != enabled.size()) {
            logger.warning("Number of hosts does not match " +
                    "Number of enabled node");
            return false;
        }

        // HashMap containing exclusive list of tools to start on each machine.
        HashMap<String, HashSet<String>> hostMap =
                new HashMap<String, HashSet<String>>();

        // Temporary tool list for host class being processed.
        ArrayList<String> newTools = new ArrayList<String>();

        // First we flatten out the classes into host names and tools sets
        for (int i = 0; i < hostClasses.size(); i++) {
            // Ignore if the host class is not enabled.
            if (!Boolean.parseBoolean((String)enabled.get(i)))
                continue;

            String toolCmds = ((String) allTools.get(i)).trim();

            // Ignore class if no tools to start.
            if (toolCmds.length() == 0 || toolCmds.toUpperCase().equals("NONE"))
                continue;

            // Get the hosts list in the class.
            String[] hosts = (String[]) hostClasses.get(i);

            // Get the tools list for this host list.

            StringTokenizer st = new StringTokenizer(toolCmds, ";");
            while(st.hasMoreTokens()) {
                String tool = st.nextToken().trim();
                if (tool.length() > 0)
                    newTools.add(tool);
            }

            for (int j = 0; j < hosts.length; j++) {
                String host = hosts[j];
                // Now get the tools list for this host,
                // or allocate if non-existent
                HashSet<String> toolsSet = hostMap.get(host);
                if (toolsSet == null) {
                    toolsSet = new HashSet<String>(newTools);
                    hostMap.put(host, toolsSet);
                } else {
                    toolsSet.addAll(newTools);
                }
            }
            // Clear the set for the next host class.
            newTools.clear();
        }

        if (hostMap.size() == 0) {
            logger.fine("No Tool to start !!");
            return true;
        }

        // Put the host names into an array.
        Set<String> hostSet = hostMap.keySet();
        hostNames = new String[hostSet.size()];
        hostNames = hostSet.toArray(hostNames);

        // Start the tools.
        try {
            for (int i = 0; i < hostNames.length; i++) {
                logger.info("Setting up tools on machine " + hostNames[i]);
                cmds.startAgent(hostNames[i], ToolAgentImpl.class,
                        Config.TOOL_AGENT);

            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to setup tools.", e);
            return(false);
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ie) {
            kill();
            return(false);
        }

        toolAgents = new ToolAgent[hostNames.length];
        try {
            for (int i = 0; i < hostNames.length; i++) {
                String serviceName = Config.TOOL_AGENT + "@" + hostNames[i];
                logger.fine("Connecting to " + serviceName);
                toolAgents[i] = (ToolAgent) CmdService.getHandle().
                        getRegistry().getService(serviceName);
                if (toolAgents[i] == null) {
                    logger.warning("Could not connect to " + serviceName);
                    continue;
                }
                // Send toolslist
                logger.fine("Configuring ToolAgent at " + serviceName);

                HashSet<String> toolsSet = hostMap.get(hostNames[i]);
                String[] toolsArray = new String[toolsSet.size()];
                toolsArray = toolsSet.toArray(toolsArray);
                toolAgents[i].configure(toolsArray, outDir);
            }

        } catch (RemoteException re) {
            logger.log(Level.WARNING, "RemoteException in ToolAgent.", re);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception starting tools.", e);
            return(false);
        }

        runTools = true;
        return(true);
    }


    /**
     * Start all tools on all machines
     * @param delay after which tools should start
     */
    public void start(int delay) {

        if (runTools == false || toolAgents == null || toolAgents.length  == 0)
            return;

        for (int i = 0; i < toolAgents.length; i++) {
            try {
                if (toolAgents[i] != null)
                    toolAgents[i].start(delay);
            } catch (RemoteException r) {
                logger.log(Level.WARNING, "Error in Starting tools on " +
                        "machine " + hostNames[i] + ".", r);
            }
        }
    }

    /**
     * Start all tools on all machines
     * @param delay after which tools should start
     * @param duration after which tools must be stopped
     */
    public void start(int delay, int duration) {

        if (runTools == false || toolAgents == null || toolAgents.length == 0)
            return;

        for (int i = 0; i < toolAgents.length; i++) {
            try {
                if (toolAgents[i] != null)
                    toolAgents[i].start(delay, duration);
            } catch (RemoteException r) {
                logger.log(Level.WARNING, "Error in Starting tools on " +
                        "machine " + hostNames[i] + ".", r);
            }
        }
    }

    /**
     * Stop all tools on all machines
     *
     */
    public void stop() {
        if (runTools == false || toolAgents == null || toolAgents.length <= 0)
            return;

        for (int i = 0; i < toolAgents.length; i++) {
            try {
                if (toolAgents[i] != null)
                    toolAgents[i].stop();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in Stopping tools on " +
                        "machine " + hostNames[i] + ".", e);
            }
        }
    }

    /**
     * Kill all tools and ToolAgents
     * This method is called when a run must be aborted
     * or at the end of a benchmark run.
     */
    public void kill() {
        if (runTools == false || toolAgents == null || toolAgents.length <= 0)
            return;
        for (int i = 0; i < toolAgents.length; i++) {
            if (toolAgents[i] != null)
                try {
                    toolAgents[i].kill();
                } catch (Exception r) { // Ignore Errors
                }
        }
        for (int i = 0; i < toolAgents.length; i++) {
            if (toolAgents[i] != null)
                try {
                    // wait will clear tmp files
                    cmds.wait(hostNames[i], "ToolAgent" + i);
                } catch (Exception e) {
                }
        }
        toolAgents = null;
        hostNames = null;
    }
}