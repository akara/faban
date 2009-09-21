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
package com.sun.faban.harness.tools;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.FileTransfer;
import com.sun.faban.harness.agent.*;
import com.sun.faban.harness.common.Config;

import java.io.*;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
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
 * @deprecated The GenericTool has been replaced by the CommandLineTool.
 */
@Deprecated public class GenericTool implements Tool {

    static final int NOT_STARTED = 0;
    static final int STARTED = 1;
    static final int STOPPED = 2;

    List<String> toolCmd;
    CommandHandle tool;
    int toolStatus = NOT_STARTED;
    CountDownLatch latch;
    boolean countedDown = false;
    String logfile, outfile;	// Name of stdout,stderr from tool
    String toolName;
    String path = null; // The path to the tool.
    CmdAgentImpl cmdAgent;

    /** The timer used for scheduling the start of the tool. */
    protected Timer timer;

    Logger logger;

    /**
     * Constructs the GenericTool.
     */
    public GenericTool(){
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
     * This is the method that should get the arguments to
     * call the tool with.
     * @param tool The tool to start
     * @param argList The tool arguments
     * @param path The path, if any, to find the tool
     * @param outDir The output directory
     * @param host The host to run the tool
     * @param masterhost The Faban master
     * @param cmdAgent The command agent for running commands
     * @param latch Latch set when tool is done
     */
    public void configure(String tool, List<String> argList, String path,
                          String outDir, String host, String masterhost,
                          CmdAgentImpl cmdAgent, CountDownLatch latch) {
        toolName = tool;
        this.cmdAgent = cmdAgent;
        timer = cmdAgent.getTimer();
        this.latch = latch;

        if (path != null)
            this.path = path;

        // Get output logfile name
        outfile = outDir + toolName + ".log." + host;
        // Create a unique temporary out file in tmp dir
        logfile = Config.TMP_DIR + toolName + ".out." + this.hashCode();

        buildCmd(argList);

        logger.fine(toolName + " Configured with toolCmd " + this.toolCmd);

        // configureMethod.invoke();
    }

    /**
     * Builds the command from the path, tool name, and argument list.
     * @param argList The argument list
     */
    protected void buildCmd(List<String> argList) {
        this.toolCmd = new ArrayList<String>();
        StringBuilder cmd = new StringBuilder();
        if(this.path != null)
            cmd.append(this.path);
        cmd.append(toolName);
        this.toolCmd.add(cmd.toString());
        this.toolCmd.addAll(argList);
    }


    /**
     * This method is required by the Tool API.
     * It is called to abort the currently running tool if any.
     */
    public void kill() {
        // For most tools, we try to collect the output no matter what.
        stop(false);
        finish();
    }

    /**
     * This method is responsible for starting up the tool and stopping it
     * after the duration specified. It uses a thread to sleep for the
     * delay + duration and then calls the stop().
     * @param delay int delay (sec) after which start should be called
     * @param duration int duration for which the tool needs to be run
     * @return true if the tool is scheduled successfully
     */

    public boolean start(int delay, int duration) {

        if(this.start(delay)) {
            TimerTask stopTask = new TimerTask() {
                public void run() {
                    stop();
                }
            };
            timer.schedule(stopTask, (delay + duration) * 1000);
            return true;
        } else {
            return false;
        }
    }

    /**
     * This method is responsible for starting up the tool utility.
     * @param delay int delay (sec) after which start should be called
     * @return true if the tool is scheduled successfully
     */
    public boolean start(int delay) {
        TimerTask startTask = new TimerTask() {
            public void run() {
                start(); // startMethod.invoke();
            }
        };
        timer.schedule(startTask, delay * 1000);
        return true;
    }

    /**
     * Starts the tool.
     */
    protected void start() {
        try {
            Command cmd = new Command(toolCmd);
            cmd.setSynchronous(false);
            cmd.setStreamHandling(Command.STDOUT, Command.CAPTURE);
            cmd.setOutputFile(Command.STDOUT, logfile);
            tool = cmdAgent.execute(cmd, null);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Cannot start tool " + toolName, e);
            finish();
            return;
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Tool start interrupted: " + toolName, e);
            finish();
            return;
        }
        toolStatus = STARTED;
        logger.fine(toolName + " Started with Cmd = " + toolCmd);
    }

    /**
     * This method is responsible for stopping the tool utility.
     */
    public void stop() {
        stop(true);
        finish();
    }

    /**
     * This method is responsible for stopping the tool utility.
     * @param warn Whether to warn if the tool already ended.
     *
     */
    protected void stop(boolean warn) {

        logger.fine("Stopping tool " + this.toolCmd);
        if (toolStatus == STARTED)
            try {
                tool.destroy();
                tool.waitFor(10000);
                // saveToolLogs(tool.getInputStream(), tool.getErrorStream());
                toolStatus = STOPPED;
                // xfer log file to master machine, log any errors
                xferLog();
                logger.fine(toolName + " Stopped ");
            } catch (RemoteException e) {
                logger.log(Level.SEVERE, "RemoteException stopping tool: " +
                           toolName, e);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Interrupted waiting for stop: " +
                           toolName, e);
            }
        else if (warn && toolStatus == NOT_STARTED)
            logger.warning("Tool not started but stop called for " + this.toolCmd);
    }

    /**
     * Transfers the tool output back to the master.
     */
    protected void xferLog() {
        try {
            FileTransfer transfer = tool.fetchOutput(Command.STDOUT, outfile);
            logger.finer("Transferring log from " + logfile + " to " + outfile);
            if(transfer != null) {
                // Use FileAgent on master machine to copy log
                String s = Config.FILE_AGENT;
                FileAgent fa = (FileAgent)CmdAgentImpl.getRegistry().getService(s);
                if (fa.push(transfer) != transfer.getSize())
                    logger.log(Level.SEVERE, "Invalid transfer size");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error transferring " + logfile, e);
        }
    }

    /**
     * Finish up the tool.
     */
    protected void finish() {
        if (!countedDown)
            latch.countDown();
        countedDown = true;
    }
}

