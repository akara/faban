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

import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.agent.ToolAgent;
import com.sun.faban.harness.agent.ToolAgentImpl;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.services.ServiceManager;
import com.sun.faban.harness.tools.MasterToolContext;

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
    private boolean runTools;

    private static ToolService toolService;
   
    private ToolService() {
        runTools = false;
    }

    /**
     * This method is the only way that an external object
     * can get a reference to the singleton ToolService.
     * @return reference to the single ToolService
     */
    public static ToolService getHandle() {
        if(toolService == null)
            toolService = new ToolService();
        return toolService;
    }

    /**
     * Intializes logger.
     */
    public void init() {
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
     * This method initializes the ToolAgent RMI server processes
     * on the specified set of machines.
     * @param par The parameter repository
     * @param outDir The run output directory, relative to Config.OUT_DIR
     * @param serviceMgr The service manager instance
     * @return true if setup successful, else false
     *
     */
    public boolean setup(ParamRepository par, String outDir,
                         ServiceManager serviceMgr) {

        CmdService cmds = CmdService.getHandle();

        /* Get tool related parameters */

        List<ParamRepository.HostConfig> hostConfigs;
        try {
            hostConfigs = par.getHostConfigs();
        } catch (ConfigurationException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        }

        // HashMap containing exclusive list of tools to start on each machine.
        HashMap<String, List<MasterToolContext>> hostMap =
                new HashMap<String, List<MasterToolContext>>();

        List<MasterToolContext> tools = serviceMgr.getTools();
        List<MasterToolContext> hostToolList;
        for (MasterToolContext tool : tools) {
            String[] hosts = tool.getToolServiceContext().getUniqueHosts();
            for (String host : hosts) {
                hostToolList = hostMap.get(host);
                if (hostToolList == null) {
                    hostToolList = new ArrayList<MasterToolContext>();
                    hostMap.put(host, hostToolList);
                }
                hostToolList.add(tool);
            }
        }

        // Temporary tool list for host class being processed.
        HashMap<String, Set<String>> osHostMap =
                new HashMap<String, Set<String>>();
        
        // First we flatten out the classes into host names and tools sets
        for (ParamRepository.HostConfig hostConfig : hostConfigs) {
            Set<String> toolset = new LinkedHashSet<String>();
            // Get the hosts list in the class.
            String[] hosts = hostConfig.hosts;
            if (hosts.length == 0) {
                continue; // This class is disabled.
            }

            String toolCmds = hostConfig.tools;

            // Get the tools list for this host list.
            if (toolCmds == null) {
                toolset.add("default");
            } else if ("NONE".equals(toolCmds.toUpperCase())) {
                // Ignore class if no tools to start.
                continue;
            } else {
                StringTokenizer st = new StringTokenizer(toolCmds, ";");
                while (st.hasMoreTokens()) {
                    toolset.add(st.nextToken().trim());
                }
            }

            for (String host : hosts) {
                // Now get the tools list for this host,
                // or allocate if non-existent
                hostToolList = hostMap.get(host);
                if (hostToolList == null) {
                    hostToolList = new ArrayList<MasterToolContext>();
                    hostMap.put(host, hostToolList);
                }
                if (osHostMap.containsKey(host))
                    toolset.addAll(osHostMap.get(host));

                osHostMap.put(host, toolset);
            }
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
            for (String hostName : hostNames) {
                logger.info("Setting up tools on machine " + hostName);
                cmds.startAgent(hostName, ToolAgentImpl.class,
                        Config.TOOL_AGENT);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to setup tools.", e);
            return (false);
        }

        try {
            Thread.sleep(5000);
        } catch (InterruptedException ie) {
            kill();
            return (false);
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

                List<MasterToolContext> toolList = hostMap.get(hostNames[i]);
                Set<String> osToolSet = osHostMap.get(hostNames[i]);
                if ((toolList != null && toolList.size() > 0) ||
                    (osToolSet != null && osToolSet.size() > 0) ) {
                    toolAgents[i].configure(toolList, osToolSet, outDir);
                }
            }

        } catch (RemoteException re) {
            logger.log(Level.WARNING, "RemoteException in ToolAgent.", re);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Exception starting tools.", e);
            return (false);
        }

        runTools = true;
        return (true);
    }

    
    /**
     * Start all tools on all machines.
     * @param delay after which tools should start
     */
    public void start(int delay) {

        if (!runTools || toolAgents == null || toolAgents.length  == 0)
            return;

        for (int i = 0; i < toolAgents.length; i++) {
            try {
                if (toolAgents[i] != null)
                    toolAgents[i].start(delay);
            } catch (Exception r) {
                logger.log(Level.WARNING, "Error in Starting tools on " +
                        "machine " + hostNames[i] + ".", r);
            }
        }
    }

    /**
     * Start all tools on all machines.
     * @param delay after which tools should start
     * @param duration after which tools must be stopped
     */
    public void start(int delay, int duration) {

        if (!runTools || toolAgents == null || toolAgents.length == 0)
            return;

        for (int i = 0; i < toolAgents.length; i++) {
            try {
                if (toolAgents[i] != null)
                    toolAgents[i].start(delay, duration);
            } catch (Exception r) {
                logger.log(Level.WARNING, "Error in Starting tools on " +
                        "machine " + hostNames[i] + ".", r);
            }
        }
    }

    /**
     * Stop all tools on all machines.
     *
     */
    public void stop() {
        if (!runTools || toolAgents == null || toolAgents.length <= 0)
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
        for (int i = 0; i < toolAgents.length; i++) {
            try {
                if (toolAgents[i] != null)
                    logger.fine("Post-processing tools on " + hostNames[i]);
                    toolAgents[i].postprocess();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in post-processing tools on " +
                        "machine " + hostNames[i] + ".", e);
            }
        }
    }

    /**
     * Kill all tools and ToolAgents.
     * This method is called when a run must be aborted
     * or at the end of a benchmark run.
     */
    public void kill() {
        if (!runTools || toolAgents == null || toolAgents.length <= 0)
            return;
        for (ToolAgent toolAgent : toolAgents) {
            if (toolAgent != null)
                try {
                    toolAgent.kill();
                } catch (Exception r) { // Ignore Errors
                }
        }
        toolAgents = null;
        hostNames = null;
    }

    /**
     * Wait for all tools.
=     */
    public void waitFor() {
        if (!runTools || toolAgents == null || toolAgents.length <= 0)
            return;

        for (int i = 0; i < toolAgents.length; i++) {
            try {
                if (toolAgents[i] != null)
                    toolAgents[i].waitFor();
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error in Stopping tools on " +
                        "machine " + hostNames[i] + ".", e);
            }
        }
    }
}