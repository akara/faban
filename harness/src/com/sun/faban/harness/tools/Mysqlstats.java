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
 * $Id: Mysqlstats.java,v 1.1 2008/06/16 18:30:44 shanti_s Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
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
import java.util.List;
import java.util.StringTokenizer;
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
 */
public class Mysqlstats implements Tool {

    static final int NOT_STARTED = 0;
    static final int STARTED = 1;
    static final int STOPPED = 2;
	Logger logger;

    String cmd;
    Command mysql;
    CommandHandle tool;
	int toolStatus = NOT_STARTED;
    String logfile, logfile1, logfile2, outfile; // Name of logfiles
    String toolName;
    String path = null; // The path to the tool.
    int priority;
    int delay;
    int duration;
    CmdAgent cmdAgent;

    Thread toolThread = null;
	private Mysqlstats toolObj;

    public Mysqlstats() {
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

        outfile = getOutputFile(outDir, host);
        logfile = getLogFile();
		// We need two intermediate files for the begin/end snapshots
		logfile1 = logfile + "_1";
		logfile2 = logfile + "_2";

        if (argList.size() < 3)
            throw new IllegalArgumentException(
                    "MYSQL_HOME, USER, PASSWD not passed as argument");

        String mysqlHome = (String) argList.get(0);
        String mysqlUser = (String) argList.get(1);
        String mysqlPass = (String) argList.get(2);

        if (mysqlHome.endsWith(File.separator))
            mysqlHome = mysqlHome.substring(0, mysqlHome.length() -
                                              File.separator.length());

        // Prepare the input
        String stdin = "show global status\n" + "quit\n";
        // Prepare the command
        String cmd = "mysql";
        if (mysqlHome != null)
            cmd = mysqlHome + File.separator + "bin" + File.separator + cmd;
		if (mysqlUser != null)
			cmd = cmd + " -u" + mysqlUser;
        if (mysqlPass != null)
			cmd = cmd + " -p" + mysqlPass;
		cmd = cmd + " -B -e \"show global status;\"";
        logger.log(Level.FINE, "Setting up mysql command: " + cmd);
        mysql = new Command(cmd);
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
                        logger.log(Level.SEVERE,
                                "Cannot start Tool Stop thread.", e);
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
		    mysql.setOutputFile(Command.STDOUT, logfile1);
            tool = cmdAgent.execute(mysql);
            logger.log(Level.FINE, "Calling mysql show status at start");
            toolStatus = STARTED;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error executing mysql show status", e);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted executing mysql show status", e);
        }
    }

    /**
     * This method is responsible for stopping the tool
     */
    public void stop() {
        stop(true);
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
                tool = cmdAgent.execute(mysql);
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

        // If the Thread start was called
        if((toolThread != null) && (toolThread.isAlive()))
            toolThread.interrupt();
    }

    /**
     * Get final report by diffing the two logfiles
     * @param log1 The first logfile
     * @param log2 The second logfile
     * @param outputFile The output file
     * @return void
     */
    protected void getReport(String log1, String log2,
                                               String outFile) {
		String c = "mysql_diff_status.sh " + log1 + " " + log2 + " " + outFile;
        logger.log(Level.FINE, "Calling " + c);
	    Command diffCommand = new Command(c);
	    try {
	        cmdAgent.execute(diffCommand);
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
}

