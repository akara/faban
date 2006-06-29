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
 * $Id: GenericTool.java,v 1.2 2006/06/29 19:38:43 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.agent.*;
import com.sun.faban.harness.common.Config;

import java.io.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * GenericTool is an abstract class that all tools should extend.
 * It provides the basic functionality for most tools. 
 * The tools modelled in GenericTool are the common system tools
 * like vmstat, mpstat etc where the user arguments are directly
 * passed to the tool and the tool is stopped by simply killing
 * the corresponding process. 
 * When tools differ in functionality (for example statit), they
 * should override the abstract class methods, configure, start, stop
 * kill. Note that they can still use the abstract class methods
 * to transfer the stdout, stderr from the tool to the master machine
 * (not to mention cut and paste a large chunk of the remaining code).
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.tools.Tool
 */
public class GenericTool implements Tool {
    String cmd;
    CommandHandle tool;
    boolean toolStarted = false;
    String logfile, outfile;	// Name of stdout,stderr from tool
    String toolName;
    String path = null; // The path to the tool.
    int priority;
    int delay;
    int duration;
    CmdAgent cmdAgent;

    private GenericTool toolObj;

    Thread toolThread = null;

    Logger logger;

    // GenericTool implementation
    public GenericTool(){
        toolObj = this;
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
     * This is the method that should get the arguments to
     * call the tool with.
     *
     */
    public void configure(String tool, List argList, String path, String outDir,
                          String host, String masterhost, CmdAgent cmdAgent) {
        toolName = tool;
        this.cmdAgent = cmdAgent;

        if (path != null)
            this.path = path;

        // Get output logfile name
        outfile = outDir + toolName + ".log." + host;
        // Create a unique temporary out file in /tmp
        logfile = Config.TMP_DIR + toolName + ".out." + this.hashCode();

        buildCmd(argList);

        logger.fine(toolName + " Configured with cmd " + this.cmd);
    }

    /**
     * Builds the command from the path, tool name, and argument list.
     * @param argList The argument list
     */
    protected void buildCmd(List argList) {
        StringBuilder cmd = new StringBuilder();
        if(this.path != null)
            cmd.append(this.path);
        cmd.append(toolName);

        int len = argList.size();		// Get args to tool
        for (int i = 0; i < len; i++)
            cmd.append(" ").append((String)argList.get(i));

        this.cmd = cmd.toString();
    }


    /**
     * This method is required by the Tool API.
     * It is called to abort the currently running tool if any.
     */
    public void kill() {
        // For most tools, we try to collect the output no matter what.
        stop();
    }

    /**
     * This method is responsible for starting up the tool and stopping it
     * after the duration specified. It uses a thread to sleep for the
     * delay + duration and then calls the stop().
     * @param delay int delay (sec) after which start should be called
     * @param duration int duration for which the tool needs to be run
     */

    public boolean start(int delay, int duration) {
        this.duration = duration;

        if(this.start(delay)) {
            Thread stopThread = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(toolObj.delay*1000 + toolObj.duration*1000);
                    }
                    catch(Exception e) {
                        logger.severe("Cannot start Tool Stop thread " + e);
                        logger.log(Level.FINE,  "Exception", e);
                    }
                    toolObj.stop();
                }
            };
            stopThread.start();
            return true;
        }
        else
            return false;
    }

    /**
     * This method is responsible for starting up the tool utility.
     * @param delay int delay (sec) after which start should be called
     */

    public boolean start (int delay) {
        this.delay = delay;

        // start the tool in a new thread
        toolThread = new Thread(this);
        // Set this tool thread as a daemon thread.  So that JVM can exit when 
        // the only threads running are all daemon threads.        
        toolThread.setDaemon(true);
        toolThread.start();
        return(true);
    }

    /**
     * The Runnable.run() is used to really start the tool after the delay.
     */
    public void run() {

        try {
            // Sleep for the delay secs.
            Thread.sleep(delay*1000);
        } catch(InterruptedException e) {
            // the stop was called.
            return;
        }

        try {
            Command cmd = new Command(this.cmd);
            cmd.setSynchronous(false);
            cmd.setStreamHandling(Command.STDOUT, Command.CAPTURE);
            cmd.setOutputFile(Command.STDOUT, logfile);
            tool = cmdAgent.execute(cmd);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot start tool " + toolName, e);
            return;
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Tool start interrupted: " + toolName, e);
            return;
        }
        toolStarted = true;
        logger.fine(toolName + " Started with Cmd = " + this.cmd);
    }

    /**
     * This method is responsible for stopping the tool utility.
     */
    public void stop () {

        logger.fine("Stopping tool " + this.cmd);
        if (toolStarted)
            try {
                tool.destroy();
                tool.waitFor();
                // saveToolLogs(tool.getInputStream(), tool.getErrorStream());
                toolStarted = false;
                // xfer log file to master machine, log any errors
                xferLog();
            } catch (RemoteException e) {
                logger.log(Level.SEVERE, "RemoteException stopping tool: " +
                           toolName, e);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Interrupted waiting for stop: " +
                           toolName, e);
            }
        else
            logger.warning("Stop called without start");

        // If the Thread start was called
        if((toolThread != null) && (toolThread.isAlive()))
            toolThread.interrupt();

        logger.fine(toolName + " Stopped ");
    }


    protected void xferLog() {
        try {
            byte[] buf = tool.fetchOutput(Command.STDOUT);
            logger.finer("Transferring log from " + logfile + " to " + outfile);
            if(buf.length > 0) {
                // Use FileAgent on master machine to copy log
                String s = Config.FILE_AGENT;
                FileAgent fa = (FileAgent)CmdAgentImpl.getRegistry().getService(s);
                FileService outfp = fa.open(outfile, FileAgent.WRITE);
                outfp.writeBytes(buf, 0, buf.length);
                outfp.close();
            }
        } catch (FileServiceException fe) {
                logger.severe("Error in creating file " + outfile);
        } catch (IOException ie) {
                logger.severe("Error in reading file " + logfile);
        }
    }
}

