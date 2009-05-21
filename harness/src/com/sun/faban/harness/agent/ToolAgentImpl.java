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
 * $Id: ToolAgentImpl.java,v 1.5 2009/05/21 10:13:28 sheetalpatil Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;
import com.sun.faban.common.Command;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.engine.DeployImageClassLoader;
import com.sun.faban.harness.services.ServiceContext;
import com.sun.faban.harness.services.ServiceDescription;
import com.sun.faban.harness.tools.CommandLineTool;
import com.sun.faban.harness.tools.Tool;

import com.sun.faban.harness.tools.MasterToolContext;
import com.sun.faban.harness.tools.ToolDescription;
import com.sun.faban.harness.tools.ToolWrapper;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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

    public ToolAgentImpl() throws RemoteException {
        super();
        logger = Logger.getLogger(this.getClass().getName());
        host = CmdAgentImpl.getHost();
        masterMachine = CmdAgentImpl.getMaster();
        logger.info("Started");
    }

    /**
     * This method configures the tools that must be run on
     * this machine by calling the configure method on each of
     * the specified tools.
     * @param toollist - each element in the array is the
     * name of a tool and optional arguments, e.g "sar -u -c"
     */
    public void configure(List<MasterToolContext> toollist, String outDir) 
            throws RemoteException, IOException {
        
        numTools = toollist.size();
        toolNames = new String[numTools];
        tools = new ToolWrapper[numTools];

        logger.info("Processing tools");
        latch = new CountDownLatch(toollist.size());
        
        downloadTools(toollist);
        
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
                toolClass = toolDesc.toolClass;

            // Now, create the tool object and call its configure method            
            if(toolClass != null){                
                try {
                    ServiceDescription serviceDesc = ctx.getToolDescription().service;
                    DeployImageClassLoader loader = DeployImageClassLoader.
                            getInstance(serviceDesc.locationType,
                            serviceDesc.location, getClass().getClassLoader());
                    Class c = loader.loadClass(toolClass);
                    tools[i] = new ToolWrapper(c, ctx);

                    logger.fine("Trying to run tool " + c.getName());
                    tools[i].configure(toolNames[i], path, outDir, host, CmdAgentImpl.getHandle(), latch);
                } catch (ClassNotFoundException ce) {
                    logger.log(Level.WARNING, "Class " + toolClass + " not found");
                } catch (Exception ie) {
                    logger.log(Level.WARNING, "Error in creating tool object " +
                               tools[i], ie);
                    latch.countDown(); // Tool did not get started.
                }
            }else{
                try {
                    tools[i] = new ToolWrapper(CommandLineTool.class, ctx);
                    tools[i].configure(toolNames[i], path, outDir, host, CmdAgentImpl.getHandle(), latch);
                    logger.fine("Trying to run tool " + tools[i] + " using CommandLineTool.");
                } catch (Exception ex) {
                    logger.log(Level.WARNING, "Cannot start CommandLineTool!", ex);
                }
            }
        }
    }

    /**
     * This method is responsible for starting all tools
     * @param 	delay - time to delay before starting
     * @return true if all tools started successfully, else false
     */
    public boolean start(int delay) throws RemoteException {
        int i;
        boolean ret = true;
        for (i = 0; i < tools.length; i++) {
            if ( ! tools[i].start(delay)) {
                ret = false;
                try {
                    logger.severe("Could not start tool " +
                            toolNames[i]);
                } catch (Exception e) { }
            }
        }
        return(ret);
    }


    /**
     * This method is responsible for starting all tools
     * @param 	delay - time to delay before starting
     * @param duration after which tools must be stopped
     * @return true if all tools started successfully, else false
     */
    public boolean start(int delay, int duration) throws RemoteException {
        int i;
        boolean ret = true;
        for (i = 0; i < tools.length; i++) {
            if ( ! tools[i].start(delay, duration)) {
                ret = false;
                logger.severe("Could not start tool " + toolNames[i]);
            }
        }
        return(ret);
    }

    /**
     * Start only specified tools 
     */
    public boolean start(int delay, String[] t)
            throws RemoteException {
        int i, j;
        boolean ret = true;
        for (j = 0; j < t.length; j++) {
            for (i = 0; i < tools.length; i++) {
                if (toolNames[i].equals(t[j])) {
                    if ( ! tools[i].start(delay)) {
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
     * Start only specified tools
     */
    public boolean start(int delay, String[] t, int duration)
            throws RemoteException {
        int i, j;
        boolean ret = true;
        for (j = 0; j < t.length; j++) {
            for (i = 0; i < tools.length; i++) {
                if (toolNames[i].equals(t[j])) {
                    if ( ! tools[i].start(delay, duration)) {
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
     * This method is responsible for stopping the tools
     */
    public void stop() {
        int i;
        for (i = 0; i < tools.length; i++) {
            try {
                tools[i].stop();
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "ToolAgent: toolName = " +
                        toolNames[i] + " cannot stop", e);
            }
        }
    }

    public void stop(String t[]) {
        int i, j;
        for (j = 0; j < t.length; j++) {
            for (i = 0; i < tools.length; i++) {
                if (toolNames[i].equals(t[j])) {
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

    public void waitFor() {
        try {
            latch.await();
        } catch (InterruptedException e) {
            logger.log(Level.WARNING,
                    "Interrupted waiting for tools to finish.", e);
        }
    }

    public void kill() {
        logger.info("Killing tools");
        for (int i = 0; i < tools.length; i++)
            try {
                tools[i].kill();
            } catch (Exception e) {
                logger.log(Level.WARNING, "ToolAgent: toolName = " +
                        toolNames[i] + " cannot kill", e);
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

    private void downloadTools(List<MasterToolContext> toollist) throws IOException {
        LinkedHashSet<ServiceContext> downloads = new LinkedHashSet<ServiceContext>();
        for (int i=0; i < toollist.size(); i++) {
            MasterToolContext ctx = toollist.get(i);
            ServiceContext sc = ctx.getToolServiceContext();
            if(sc != null){
               downloads.add(sc);
            }
        }

        for(ServiceContext ctx : downloads){
            if ("services".equals(ctx.desc.locationType))
                new Download().loadService(ctx.desc.location,
                                            AgentBootstrap.downloadURL);
        }
    }
}