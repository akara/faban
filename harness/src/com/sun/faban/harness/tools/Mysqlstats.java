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
 * Copyright 2008-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.FileTransfer;
import com.sun.faban.harness.agent.CmdAgent;
import com.sun.faban.harness.agent.CmdAgentImpl;
import com.sun.faban.harness.agent.FileAgent;
import com.sun.faban.harness.common.Config;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * MysqlStats is a class for mysql stats collection.
 * It simply collects "show global status" at the start and end of a
 * run and diffs them.
 * The tool needs to be specified as follows: 
 * mysqlstats <MYSQL_HOME> <mysql_user> <mysql_password>
 * where MYSQL_HOME is the home directory of mysql,
 * mysql_user is the username to connect to mysql,
 * mysql_password is the password for username to connect to mysql.
 * E.g: mysqlstats /opt/coolstack/mysql_32bit web20 web20
 * @author Shanti Subramanyam
 * @see GenericTool
 * @deprecated
 */
@Deprecated public class Mysqlstats implements Tool {

    static final int NOT_STARTED = 0;
    static final int STARTED = 1;
    static final int STOPPED = 2;
	Logger logger;

    String cmd;
    Command mysql;
    CommandHandle tool;
	int toolStatus = NOT_STARTED;
    CountDownLatch latch;
    boolean countedDown = false;
    String logfile, logfile1, logfile2, outfile; // Name of logfiles
    String toolName;
    String path = null; // The path to the tool.
    int priority;
    int delay;
    int duration;
    CmdAgent cmdAgent;

    /** The timer to be used by this tool. */
    protected Timer timer;

    // Thread toolThread = null;
	// private Mysqlstats toolObj;

    /**
     * Constructs the Mysqlstats.
     */
    public Mysqlstats() {
		logger = Logger.getLogger(this.getClass().getName());
	}

    /**
     * Configures Mysqlstats.
     * @param tool name of the tool (Executable)
     * @param argList list containing arguments to tool
     * @param path The path to run mysqlstats
     * @param outDir The output directory
     * @param host name of machine the tool is running on
     * @param masterhost name of master machine
     * @param cmdAgent agent The command agent used for executing tools
     * @param latch The latch the tool uses to identify it's completion.
     */
    public void configure(String tool, List<String> argList, String path,
                          String outDir, String host, String masterhost,
                          CmdAgentImpl cmdAgent, CountDownLatch latch) {
        toolName = tool;
        this.cmdAgent = cmdAgent;
        timer = cmdAgent.getTimer();
        this.latch = latch;

        outfile = getOutputFile(outDir, host);
        logfile = getLogFile();
		// We need two intermediate files for the begin/end snapshots
		logfile1 = logfile + "_1";
		logfile2 = logfile + "_2";

        if (argList.size() < 3)
            throw new IllegalArgumentException(
                    "MYSQL_HOME, USER, PASSWD not passed as argument");

        String mysqlHome = argList.get(0);
        String mysqlUser = argList.get(1);
        String mysqlPass = argList.get(2);

        if (mysqlHome.endsWith(File.separator))
            mysqlHome = mysqlHome.substring(0, mysqlHome.length() -
                                              File.separator.length());

        // Prepare the input
        // String stdin = "show global status\n" + "quit\n";
        // Prepare the command
        ArrayList<String> c = new ArrayList<String>();
        String cmd = "mysql";
        if (mysqlHome != null)
            cmd = mysqlHome + File.separator + "bin" + File.separator + cmd;
        c.add(cmd);
		if (mysqlUser != null)
            c.add("-u" + mysqlUser);
        if (mysqlPass != null)
            c.add("-p" + mysqlPass);
        c.add("-B");
        c.add("-e");
        c.add("show global status;");
        logger.log(Level.FINE, "Setting up mysql command: " + c);
        mysql = new Command(c);
		/***
        mysql.setEnvironment(env);
        mysql.setInput(stdin.getBytes());
        ***/
    }

    /**
     * Obtains the temporary log file name.
     * @return The log file name
     */
    protected String getLogFile() {
        return Config.TMP_DIR + toolName + ".out." + this.hashCode();

        // TODO: Take out hashcode, replace with other value.
    }

    /**
     * Obtains the final output file name.
     * @param outDir The output or run directory
     * @param host The host name running the tool
     * @return The output file name
     */
    protected String getOutputFile(String outDir, String host) {
        return outDir + toolName + ".log." + host;
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
     * @return true if mysqlstats scheduled successfully
     */
    public boolean start(int delay, int duration) {
        this.duration = duration;

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
     * @return true if mysqlstats scheduled successfully
     */
    public boolean start(int delay) {
        TimerTask startTask = new TimerTask() {
            public void run() {
                start();
            }
        };
        timer.schedule(startTask, delay * 1000);
        return true;
    }

    /**
     * Starts the mysqlstat tool.
     */
    protected void start() {
        try {            
		    mysql.setOutputFile(Command.STDOUT, logfile1);
            tool = cmdAgent.execute(mysql, null);
            logger.log(Level.FINE, "Calling mysql show status at start");
            toolStatus = STARTED;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error executing mysql show status", e);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted executing mysql show status", e);
        }
    }

    /**
     * This method is responsible for stopping the tool.
     */
    public void stop() {
        stop(true);
        finish();
    }


    /**
     * This method is responsible for stopping the tool utility.
     * @param warn Whether to warn if tool is not running.
     */
    protected void stop(boolean warn) {

        if (toolStatus == STARTED)
            try {
		        mysql.setOutputFile(Command.STDOUT, logfile2);
                logger.log(Level.FINE, "Calling mysql show status at end of run");
                tool = cmdAgent.execute(mysql, null);
                toolStatus = STOPPED;

                // Get the report from both snapshots
                getReport(logfile1, logfile2, logfile);
                // xfer log file to master machine, log any errors
                xferLog();
                logger.fine(toolName + " Stopped ");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error executing mysql show status", e);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Interrupted executing mysql show status", e);
            }
        else if (warn && toolStatus == NOT_STARTED)
            logger.warning("Tool not started but stop called for " + toolName);
    }

    /**
     * Get final report by diffing the two logfiles.
     * @param log1 The first logfile
     * @param log2 The second logfile
     * @param outFile The output file
     */
    protected void getReport(String log1, String log2,
                                               String outFile) {
		String c = "mysql_diff_status.sh " + log1 + " " + log2 + " " + outFile;
        logger.log(Level.FINE, "Calling " + c);
	    Command diffCommand = new Command(c);
	    try {
	        cmdAgent.execute(diffCommand, null);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error executing mysql_diff_status.sh", e);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted executing mysql_diff_status.sh", e);
        }
    }

    /**
     * Transfers the log files to the output file in the results directory.
     */
    protected void xferLog() {
        if (!new File(logfile).exists()) {
            logger.log(Level.SEVERE,
                    "Transfer file " + logfile + " not found.");
            return;
        }
        try {
            FileTransfer transfer = new FileTransfer(logfile, outfile);
            logger.finer("Transferring log from " + logfile + " to " + outfile);
            // Use FileAgent on master machine to copy log
            String s = Config.FILE_AGENT;
            FileAgent fa = (FileAgent)CmdAgentImpl.getRegistry().getService(s);
            if (fa.push(transfer) != transfer.getSize())
                logger.log(Level.SEVERE, "Invalid transfer size");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error transferring " + logfile, e);
        }
    }

    /**
     * Finishes up mysqlstats.
     */
    protected void finish() {
        if (!countedDown)
            latch.countDown();
        countedDown = true;
    }
}

