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
 * $Id: ToolService.java,v 1.10 2009/05/28 21:03:24 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.ParamRepository;
import com.sun.faban.harness.agent.ToolAgent;
import com.sun.faban.harness.agent.ToolAgentImpl;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.services.ServiceManager;
import com.sun.faban.harness.tools.MasterToolContext;
import com.sun.faban.harness.util.XMLReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
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
    private LinkedHashMap<String, List<String>> toolSetsMap =
            new LinkedHashMap<String, List<String>>();

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
     * @param par The parameter repository
     * @param outDir The run output directory, relative to Config.OUT_DIR
     * @return true if setup successful, else false
     *
     */
    public boolean setup(ParamRepository par, String outDir,
                         ServiceManager serviceMgr) {

        cmds = CmdService.getHandle();

        /* Get tool related parameters */

        List<String[]> hostClasses = null;
        List<String> allTools =  null;
        try {
            hostClasses = par.getEnabledHosts();
            allTools = par.getParameters("fa:hostConfig/fh:tools");
        } catch (ConfigurationException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            return false;
        }

        // HashMap containing exclusive list of tools to start on each machine.
        HashMap<String, List<MasterToolContext>> hostMap =
                new HashMap<String, List<MasterToolContext>>();

        List<MasterToolContext> tools = serviceMgr.getTools();
        List<MasterToolContext> hostToolList = null;
        for (MasterToolContext tool : tools) {
            String[] hosts = tool.getToolServiceContext().getHosts();
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
        ArrayList<MasterToolContext> newTools = new ArrayList<MasterToolContext>();
        ArrayList<String> toolset = new ArrayList<String>();
        // First we flatten out the classes into host names and tools sets
        for (int i = 0; i < hostClasses.size(); i++) {

            // Get the hosts list in the class.
            String[] hosts = hostClasses.get(i);
            if (hosts.length == 0)
                continue; // This class is disabled.

            String toolCmds = allTools.get(i).trim();

            // Ignore class if no tools to start.
            if (toolCmds.toUpperCase().equals("NONE")){
                continue;
            }

            // Get the tools list for this host list.
            if(toolCmds.length() != 0){
                StringTokenizer st = new StringTokenizer(toolCmds, ";");
                while (st.hasMoreTokens()) {
                    toolset.add(st.nextToken().trim());
                }
            }else if ("".equals(toolCmds) && toolCmds.length() == 0){
                String key = "default";
                ArrayList<String> toolset_tools = new ArrayList<String>();
                if(toolSetsMap.containsKey(key)){
                    toolset_tools.addAll(toolSetsMap.get(key));
                    for (String tool : toolset_tools) {
                        MasterToolContext tCtx =  new MasterToolContext(
                                        tool, null, null);

                        if (tCtx != null) {
                            newTools.add(tCtx);
                        }
                    }
                }
            }
            if (toolset != null) {
                for (String tool : toolset) {
                    StringTokenizer tt = new StringTokenizer(tool);
                    String toolId = tt.nextToken();
                    String toolKey = toolId;
                    ArrayList<String> toolset_tools = new ArrayList<String>();
                    if (toolSetsMap.containsKey(toolKey)) {
                        toolset_tools.addAll(toolSetsMap.get(toolKey));
                        for (String t1 : toolset_tools) {
                            MasterToolContext tCtx = new MasterToolContext(
                                    t1, null, null);
                            if (tCtx != null) {
                                newTools.add(tCtx);
                            }
                        }
                    } else {
                        MasterToolContext tCtx = new MasterToolContext(
                                tool, null, null);
                        if (tCtx != null) {
                            newTools.add(tCtx);
                        }
                    }
                }
            }
            for (int j = 0; j < hosts.length; j++) {
                String host = hosts[j];
                // Now get the tools list for this host,
                // or allocate if non-existent
                hostToolList = hostMap.get(host);
                if (hostToolList == null) {
                    hostToolList = new ArrayList<MasterToolContext>();
                    hostMap.put(host, hostToolList);
                }
                hostToolList.addAll(newTools);
                
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

                List<MasterToolContext> toolList = hostMap.get(hostNames[i]);
                if(toolList != null)
                    toolAgents[i].configure(toolList, outDir);
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

    private void parseOSToolSets() {
        File toolsetsXml = new File(
                            Config.CONFIG_DIR + Config.OS_DIR + "toolsets.xml");
        try {
            if(toolsetsXml.exists()){
                XMLReader reader = new XMLReader(Config.CONFIG_DIR + Config.OS_DIR + "toolsets.xml");
                Element root = null;
                if (reader != null) {
                    root = reader.getRootNode();
                    // First, parse the services.
                    NodeList toolsetsNodes = reader.getNodes("toolset", root);
                    for (int i = 0; i < toolsetsNodes.getLength(); i++) {
                        Node toolsetsNode = toolsetsNodes.item(i);
                        if (toolsetsNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Element tse = (Element) toolsetsNode;
                        ArrayList<String> toolsCmds = new ArrayList<String>();
                        String name = reader.getValue("name", tse);
                        String base = reader.getValue("base", tse);
                        List<String> toolIncludes = reader.getValues("includes", tse);
                        List<String> toolExcludes = reader.getValues("excludes", tse);
                        if(!"".equals(base)){
                            toolsCmds.addAll(toolSetsMap.get(base));
                        }
                        if(toolIncludes != null){
                            for (String tool : toolIncludes){
                                StringTokenizer st = new StringTokenizer(tool, ";");
                                while(st.hasMoreTokens())
                                    toolsCmds.add(st.nextToken().trim());
                            }
                        }
                        if(toolExcludes != null){
                            ArrayList<String> td = new ArrayList<String>();
                            for (String tool : toolExcludes){
                                StringTokenizer st = new StringTokenizer(tool, ";");
                                while(st.hasMoreTokens())
                                    td.add(st.nextToken().trim());
                            }
                            toolsCmds.removeAll(td);
                        }
                        if (!"".equals(name) &&
                                (toolIncludes != null || base != null)) {
                            String key = name;
                            if (toolSetsMap.containsKey(key)) {
                                logger.log(Level.WARNING,
                                        "Ignoring duplicate toolset");
                            } else {
                                toolSetsMap.put(key, toolsCmds);
                            }
                        }
                    }
                }
            }
        } catch  (Exception e) {
            logger.log(Level.WARNING, "Error reading toolsets.xml ", e);
        }
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

    /**
     * Wait for all tools.
=     */
    public void waitFor() {
        if (runTools == false || toolAgents == null || toolAgents.length <= 0)
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