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
package com.sun.faban.harness.agent;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.engine.DeployImageClassLoader;
import com.sun.faban.harness.tools.CommandLineTool;
import com.sun.faban.harness.tools.MasterToolContext;
import com.sun.faban.harness.tools.ToolDescription;
import com.sun.faban.harness.tools.ToolWrapper;
import com.sun.faban.harness.util.XMLReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the ToolAgent interface.
 * It runs in a seperate VM and is started by the GenericBenchmark.
 * It configures and manages all tools on a particular machine
 * and performs the ToolService API functions for this machine.
 * Each tool is run as a seperate thread.
 *
 * @see com.sun.faban.harness.engine.ToolService
 * @see com.sun.faban.harness.engine.GenericBenchmark
 * @author Ramesh Ramachandran
 */
public class ToolAgentImpl extends UnicastRemoteObject implements ToolAgent, Unreferenced {
    String toolNames[];
    ToolWrapper tools [];	// handles to the Tool objects
    int numTools;
    static String masterMachine = null;	// name of master machine
    static String host;	// our current hostname
    Logger logger;
    CountDownLatch latch;

    /**
     * Constructor for Tool agent implementation.
     * @throws java.rmi.RemoteException Error constructing ToolAgentImpl
     */
    public ToolAgentImpl() throws RemoteException {
        super();
        logger = Logger.getLogger(this.getClass().getName());
        host = CmdAgentImpl.getHost();
        masterMachine = CmdAgentImpl.getMaster();
        logger.fine("Started processing tools");
    }

    /**
     * This method configures the tools that must be run on
     * this machine by calling the configure method on each of
     * the specified tools.
     * @param toolList - each element in the array is the
     * name of a tool and optional arguments, e.g "sar -u -c"
     * @param osToolSet list of os tools
     * @param outDir output directory of the run
     * @throws IOException
     *
     */
    public void configure(List<MasterToolContext> toolList,
                          Set<String> osToolSet, String outDir)
            throws IOException {
        List<MasterToolContext> toollist = new ArrayList<MasterToolContext>();
        if(toolList != null)
            toollist.addAll(toolList);

        LinkedHashMap<String, List<String>> toolSetsMap = parseOSToolSets();
        if (osToolSet != null) {
            for (String tool : osToolSet) {
                StringTokenizer tt = new StringTokenizer(tool);
                String toolId = tt.nextToken();
                Set<String> toolsetTools = new LinkedHashSet<String>();
                if (toolSetsMap.containsKey(toolId)) {
                    toolsetTools.addAll(toolSetsMap.get(toolId));
                    for (String t1 : toolsetTools) {
                        MasterToolContext tCtx = new MasterToolContext(
                                t1, null, null);
                        if (tCtx != null) {
                            toollist.add(tCtx);
                        }
                    }
                } else {
                    MasterToolContext tCtx = new MasterToolContext(
                            tool, null, null);
                    if (tCtx != null) {
                        toollist.add(tCtx);
                    }
                }
            }
        }
        numTools = toollist.size();
        toolNames = new String[numTools];
        tools = new ToolWrapper[numTools];

        logger.fine("Processing tools");
        latch = new CountDownLatch(toollist.size());
        
        for (int i=0; i<toollist.size(); i++) {
            MasterToolContext ctx = toollist.get(i);
            String toolId = ctx.getToolId();
            toolNames[i] = toolId;
            //List<String> args = Command.parseArgs(ctx.getToolParams());
           

            String path = null;
            int nameIdx = toolId.lastIndexOf(File.separator) + 1;
            if (nameIdx > 0) {
                path = toolId.substring(0, nameIdx - 1);
                toolId = toolId.substring(nameIdx);
            }

            if (path != null && path.length() == 0)
                path = null;

            String toolClass = null;
            ToolDescription toolDesc = ctx.getToolDescription();
            if (toolDesc != null)
                toolClass = toolDesc.getToolClass();

            // Now, create the tool object and call its configure method            
            if (toolClass != null) {
                try {
                    DeployImageClassLoader loader = DeployImageClassLoader.
                            getInstance(toolDesc.getLocationType(),
                            toolDesc.getLocation(), getClass().getClassLoader());
                    Class c = loader.loadClass(toolClass);
                    tools[i] = new ToolWrapper(c, ctx);
                    logger.fine("Trying to run tool " + c.getName());
                    tools[i].configure(toolNames[i], path, outDir, host, CmdAgentImpl.getHandle(), latch);
                } catch (ClassNotFoundException ce) {
                    logger.log(Level.WARNING, "Class " + toolClass + " not found");
                    latch.countDown();
                } catch (Exception ie) {
                    logger.log(Level.WARNING, "Error in creating tool object " +
                               toolClass, ie);
                    latch.countDown(); // Tool did not get started.
                }
            } else if (!"default".equals(ctx.getToolId()) || 
                       (ctx.getToolParams() != null &&
                        ctx.getToolParams().trim().length() > 0)) {
                try {
                    tools[i] = new ToolWrapper(CommandLineTool.class, ctx);
                    tools[i].configure(toolNames[i], path, outDir, host, CmdAgentImpl.getHandle(), latch);
                    logger.fine("Trying to run tool " + tools[i] + " using CommandLineTool.");
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Cannot start CommandLineTool!", ex);
                    latch.countDown();
                }
            } else {
                latch.countDown();
            }
        }
    }

    /**
     * This method is responsible for starting all tools.
     * @param 	delay - time to delay before starting
     * @return true if all tools started successfully, else false
     * @throws RemoteException
     */
    public boolean start(int delay) throws RemoteException {
        int i;
        boolean ret = true;
        for (i = 0; i < tools.length; i++) {
            if (tools[i] == null || ! tools[i].start(delay)) {
                ret = false;
                logger.severe("Could not start tool " + toolNames[i]);
            }
        }
        return(ret);
    }


    /**
     * This method is responsible for starting all tools.
     * @param 	delay - time to delay before starting
     * @param duration after which tools must be stopped
     * @return true if all tools started successfully, else false
     * @throws RemoteException
     */
    public boolean start(int delay, int duration) throws RemoteException {
        int i;
        boolean ret = true;
        for (i = 0; i < tools.length; i++) {
            if (tools[i] == null || ! tools[i].start(delay, duration)) {
                ret = false;
                logger.severe("Could not start tool " + toolNames[i]);
            }
        }
        return(ret);
    }

    /**
     * Start only specified tools.
     * @param 	delay - time to delay before starting
     * @param 	t - specific list of tools to start
     * @return  true if all tools are started successfully, false otherwise
     * @throws RemoteException A communication error occurred
     */
    public boolean start(int delay, String[] t)
            throws RemoteException {
        int i, j;
        boolean ret = true;
        for (j = 0; j < t.length; j++) {
            for (i = 0; i < tools.length; i++) {
                if (toolNames[i].equals(t[j])) {
                    if ( tools[i] == null || ! tools[i].start(delay)) {
                        ret = false;
                        logger.severe("Could not start tool " +
                                    toolNames[i]);
                    }
                    break;
                }
            }
        }
        return(ret);
    }

    /**
     * Start only specified tools for specific duration.
     * @param delay - time to delay before starting
     * @param t - specific list of tools to start
     * @param duration after which tools must be stopped
     * @return true if all tools are started successfully, false otherwise
     * @throws RemoteException A communication error occurred
     */
    public boolean start(int delay, String[] t, int duration)
            throws RemoteException {
        int i, j;
        boolean ret = true;
        for (j = 0; j < t.length; j++) {
            for (i = 0; i < tools.length; i++) {
                if (toolNames[i].equals(t[j])) {
                    if ( tools[i] == null || ! tools[i].start(delay, duration)) {
                        ret = false;
                        logger.severe("Could not start tool " +
                                toolNames[i]);
                    }
                    break;
                }
            }
        }
        return(ret);
    }

    /**
     * This method is responsible for stopping the tools.
     */
    public void stop() {
        for (int i = 0; i < tools.length; i++) {
            if (tools[i] != null){
                try {
                    tools[i].stop();
                }
                catch (Exception e) {
                    logger.log(Level.WARNING, "ToolAgent: toolName = " +
                            toolNames[i] + " cannot stop", e);
                }
            }
        }
    }

    /**
     * Stopping specific tools.
     * @param t The tools to stop.
     */ 
    public void stop(String t[]) {
        for (String tool : t) {
            for (int i = 0; i < tools.length; i++) {
                if (tools[i] != null && toolNames[i].equals(tool)) {
                    try {
                        tools[i].stop();
                    } catch (Exception ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                    break;
                }
            }
        }
    }

    /**
     * This method is responsible for post processing tools.
     */
    public void postprocess() {
        for (int i = 0; i < tools.length; i++) {
            if (tools[i] != null){
                try {
                    logger.finer("Postprocessing " + toolNames[i]);
                    tools[i].postprocess();
                }
                catch (Exception e) {
                    logger.log(Level.WARNING, "ToolAgent: toolName = " +
                            toolNames[i] + " cannot stop", e);
                }
            }
        }
    }

    /**
     *  Waits for all tools to finish up.
     */
    public void waitFor() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.log(Level.WARNING,
                    "Interrupted waiting for tools to finish.", e);
        }
    }

    /**
     *  Kills all the tools.
     */
    public void kill() {
        logger.info("Killing tools");
        for (int i = 0; i < tools.length; i++){
            if (tools[i] != null){
                try {
                    tools[i].kill();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "ToolAgent: toolName = " +
                            toolNames[i] + " cannot kill", e);
                }
            }
        }
            
    }

    /**
     * When this instance is unreferenced the application must exit.
     *
     * @see         java.rmi.server.Unreferenced
     *
     */
    public void unreferenced() {
        kill();
    } 

    /**
     * Obtains the OS toolsets.
     * @return LinkedHashMap
     */
    protected LinkedHashMap<String, List<String>> parseOSToolSets() {
        LinkedHashMap<String, List<String>> toolSetsMap =
            new LinkedHashMap<String, List<String>>();
        File toolsetsXml = new File(Config.CONFIG_DIR + Config.OS_DIR + "toolsets.xml");
        try {
            if(toolsetsXml.exists()){
                XMLReader reader = new XMLReader(Config.CONFIG_DIR + Config.OS_DIR + "toolsets.xml");
                Element root;
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
                            if (toolSetsMap.containsKey(name)) {
                                logger.log(Level.WARNING,
                                        "Ignoring duplicate toolset = " + name);
                            } else {
                                toolSetsMap.put(name, toolsCmds);
                            }
                        }
                    }
                }
            }
        } catch  (Exception e) {
            logger.log(Level.WARNING, "Error reading toolsets.xml ", e);
        }

        return toolSetsMap;
    }
}