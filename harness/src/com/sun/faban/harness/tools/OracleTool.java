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
import com.sun.faban.harness.agent.CmdAgentImpl;
import com.sun.faban.harness.agent.FileAgent;
import com.sun.faban.harness.common.Config;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * OracleTool is an abstract class for Oracle tools to extend.
 * It provides the basic functionality typical Oracle tools.
 *
 * @author Akara Sucharitakul
 * @see Tool
 * @deprecated
 */
@Deprecated public abstract class OracleTool implements Tool {

    static final int NOT_STARTED = 0;
    static final int STARTED = 1;
    static final int STOPPED = 2;

    String cmd;
    Command sqlplus;
    CommandHandle tool;
    int toolStatus = NOT_STARTED;
    String logfile, outfile;	// Name of stdout,stderr from tool
    String toolName;
    String path = null; // The path to the tool.
    int priority;
    CmdAgentImpl cmdAgent;
    String snapId;
    Timer timer;
    CountDownLatch latch;
    boolean countedDown = false;

    Thread toolThread = null;

    static Logger logger = Logger.getLogger(OracleTool.class.getName());

    /**
     * Configures the Oracle tool.
     * @param tool name of the tool (Executable)
     * @param argList list containing arguments to tool
     * @param path The path to run the tool
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

        outfile = getOutputFile(outDir, host);
        logfile = getLogFile();

        if (argList.size() < 2)
            throw new IllegalArgumentException(
                    "ORACLE_HOME and/or ORACLE_SID not passed as argument");

        String oracleHome = argList.get(0);
        String oracleSID = argList.get(1);

        if (oracleHome.endsWith(File.separator))
            oracleHome = oracleHome.substring(0, oracleHome.length() -
                                              File.separator.length());

        // Prepare the environment
        String[] env = new String[4];
        env[0] = "ORACLE_HOME=" + oracleHome;
        env[1] = "ORACLE_SID=" + oracleSID;
        env[2] = "LD_LIBRARY_PATH=" + oracleHome + File.separator + "lib";
        env[3] = "LD_LIBRARY_PATH_64=" + oracleHome + File.separator + "lib";

        // Prepare the input
        String stdin = "/ as sysdba\n" +
                getSnapCommand() + "\n" +
                "exit\n";

        // Prepare the command
        String cmd = "sqlplus";
        if (oracleHome != null)
            cmd = oracleHome + File.separator + "bin" + File.separator +
                    "sqlplus";
        sqlplus = new Command(cmd);
        sqlplus.setEnvironment(env);
        sqlplus.setInput(stdin.getBytes());

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
     * Creates a sqlplus command to create tool snapshot.
     * @return The sqlplus command string
     */
    protected abstract String getSnapCommand();


    /**
     * The parseSnapId method parses the sqlplus stdout and finds the
     * snapshot just taken.
     * @param output The stdout from sqlplus where a snapshot has been taken
     * @return The snapshot id pertaining to the snapshot just taken.
     */
    protected String parseSnapId(byte[] output) {
        String snapId = null;
        String o = new String(output);
        StringTokenizer t = new StringTokenizer(o, "\n");

        // Find SNAPID
        while (t.hasMoreTokens()) {
            String hdr = t.nextToken().trim();
            if ("SNAPID".equals(hdr))
                break;
        }

        // Line with separator '------------'
        if (t.hasMoreTokens())
            t.nextToken();

        // The line with the actual id.
        if (t.hasMoreTokens())
            snapId = t.nextToken().trim();

        return snapId;
    }

    /**
     * Creates a sqlplus command to create a tool report.
     * @param snapId The id of the first snap
     * @param snapId1 The id of the second snap
     * @param outputFile The output file
     * @return The sqlplus command string
     */
    protected abstract String getReportCommand(String snapId, String snapId1,
                                               String outputFile);

    /**
     * This method is required by the Tool API.
     * It is called to abort the currently running tool if any.
     */
    public void kill() {
        // If feasible, we'll try to collect the end snapshot and transfer
        // back the output. So we still have some stats even for a bad run.
        stop(false);
        finish();
    }


    /**
     * This method is responsible for starting up the Oracle tool and
     * stopping it after the duration specified.
     * @param delay int delay (sec) after which start should be called
     * @param duration int duration for which the tool needs to be run
     * @return 	\true if tool started successfully
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
     * This method is responsible for starting up the Oracle tool.
     * @param delay int delay (sec) after which start should be called
     * @return 	true if tool started successfully
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
     * The Runnable.run() is used to really start the tool after the delay.
     */
    protected void start() {
        try {
            tool = cmdAgent.execute(sqlplus, null);
            snapId = parseSnapId(tool.fetchOutput(Command.STDOUT));
            logger.finer("snapId: " + snapId);
            toolStatus = STARTED;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error executing sqlplus", e);
            finish();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted executing sqlplus", e);
            finish();
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
                tool = cmdAgent.execute(sqlplus, null);
                String snapId1 = parseSnapId(tool.fetchOutput(Command.STDOUT));
                logger.finer("snapId1: " + snapId1);
                // Prepare the input
                String stdin = "/ as sysdba\n" +
                       getReportCommand(snapId, snapId1, logfile) + "\n" +
                       "exit\n";
                sqlplus.setInput(stdin.getBytes());
                sqlplus.setLogLevel(Command.STDOUT, Level.FINER);
                tool = cmdAgent.execute(sqlplus, null);
                toolStatus = STOPPED;
                // xfer log file to master machine, log any errors
                xferLog();
                logger.fine(toolName + " Stopped ");
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error executing sqlplus", e);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Interrupted executing sqlplus", e);
            }
        else if (warn && toolStatus == NOT_STARTED)
            logger.warning("Tool not started but stop called for " + toolName);

        // If the Thread start was called
        if((toolThread != null) && (toolThread.isAlive()))
            toolThread.interrupt();
    }

    /**
     * Transfers the log files to the output file in the results directory.
     */
    protected void xferLog() {
        xferFile(logfile, outfile);
    }

    /**
     * Transfers the tool output file back to the master.
     * @param srcFile The source file of the tool output
     * @param destFile The transfer destination
     */
    protected static void xferFile(String srcFile, String destFile) {
        if (!new File(srcFile).exists()) {
            logger.log(Level.SEVERE,
                    "Transfer file " + srcFile + " not found.");
            return;
        }
        try {
            FileTransfer transfer = new FileTransfer(srcFile, destFile);
            logger.finer("Transferring log from " + srcFile + " to " + destFile);
            // Use FileAgent on master machine to copy log
            String s = Config.FILE_AGENT;
            FileAgent fa = (FileAgent)CmdAgentImpl.getRegistry().getService(s);
            if (fa.push(transfer) != transfer.getSize())
                logger.log(Level.SEVERE, "Invalid transfer size");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error transferring " + srcFile, e);
        }
    }

    /**
     * Finishes up the Oracle tool.
     */
    protected void finish() {
        if (!countedDown)
            latch.countDown();
        countedDown = true;
    }
}

