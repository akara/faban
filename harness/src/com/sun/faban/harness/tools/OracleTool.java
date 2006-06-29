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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: OracleTool.java,v 1.1 2006/06/29 18:51:43 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.tools;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.agent.*;
import com.sun.faban.harness.common.Config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * OracleTool is an abstract class for Oracle tools to extend.
 * It provides the basic functionality typical Oracle tools.
 *
 * @author Akara Sucharitakul
 * @see Tool
 */
public abstract class OracleTool implements Tool {
    String cmd;
    Command sqlplus;
    CommandHandle tool;
    boolean toolStarted = false;
    String logfile, outfile;	// Name of stdout,stderr from tool
    String toolName;
    String path = null; // The path to the tool.
    int priority;
    int delay;
    int duration;
    CmdAgent cmdAgent;
    String snapId;

    private OracleTool toolObj;

    Thread toolThread = null;

    Logger logger;

    // GenericTool implementation
    public OracleTool(){
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

        if (argList.size() < 2)
            throw new IllegalArgumentException(
                    "ORACLE_HOME and/or ORACLE_SID not passed as argument");

        String oracleHome = (String) argList.get(0);
        String oracleSID = (String) argList.get(1);

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
     * Parses the snapshot output for the snap id.
     * @param output The output from sqlplus doing a snapshot
     * @return The snapshot id string
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
            tool = cmdAgent.execute(sqlplus);
            snapId = parseSnapId(tool.fetchOutput(Command.STDOUT));
            logger.finer("snapId: " + snapId);
            toolStarted = true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error executing sqlplus", e);
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Interrupted executing sqlplus", e);
        }
    }

    /**
     * The parseSnapId method parses the sqlplus stdout and finds the
     * snapshot just taken.
     * @param output The stdout from sqlplus where a snapshot has been taken
     * @return The snapshot id pertaining to the snapshot just taken.
     */


    /**
     * This method is responsible for stopping the tool utility.
     */
    public void stop () {

        if (toolStarted)
            try {
                tool = cmdAgent.execute(sqlplus);
                String snapId1 = parseSnapId(tool.fetchOutput(Command.STDOUT));
                logger.finer("snapId1: " + snapId1);
                // Prepare the input
                String stdin = "/ as sysdba\n" +
                       getReportCommand(snapId, snapId1, logfile) + "\n" +
                       "exit\n";
                sqlplus.setInput(stdin.getBytes());
                sqlplus.setLogLevel(Command.STDOUT, Level.FINER);
                tool = cmdAgent.execute(sqlplus);
                toolStarted = false;
                // xfer log file to master machine, log any errors
                xferLog();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error executing sqlplus", e);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Interrupted executing sqlplus", e);
            }
        else
            logger.warning("Stop called without start");

        // If the Thread start was called
        if((toolThread != null) && (toolThread.isAlive()))
            toolThread.interrupt();

        logger.fine(toolName + " Stopped ");
    }

    /**
     * Transfers the log files to the output file in the results directory.
     */
    protected void xferLog() {
        FileChannel channel = null;
        try {
            channel = (new FileInputStream(logfile)).getChannel();
            long channelSize = channel.size();
            if (channelSize >= Integer.MAX_VALUE)
                throw new IOException("Cannot handle file size >= 2GB");
            ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY,
                    0, channelSize);
            byte[] content = new byte[(int) channelSize];
            buffer.get(content);
            logger.finer("Transferring log from " + logfile + " to " + outfile);
            if(content.length > 0) {
                // Use FileAgent on master machine to copy log
                String s = Config.FILE_AGENT;
                FileAgent fa = (FileAgent)CmdAgentImpl.getRegistry().getService(s);
                FileService outfp = fa.open(outfile, FileAgent.WRITE);
                outfp.writeBytes(content, 0, content.length);
                outfp.close();
            }
        } catch (FileServiceException fe) {
                logger.severe("Error in creating file " + outfile);
        } catch (IOException ie) {
                logger.severe("Error in reading file " + logfile);
        } finally {
            if (channel != null)
                try {
                    channel.close();
                } catch (IOException e) {
                    logger.log(Level.SEVERE, "Error closing file channel for " +
                               logfile);
                }
        }
    }
}

