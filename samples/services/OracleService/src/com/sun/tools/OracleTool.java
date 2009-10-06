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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.tools;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.Context;
import com.sun.faban.harness.Configure;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;

import com.sun.faban.harness.tools.ToolContext;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * OracleTool is an abstract class for Oracle tools to extend.
 * It provides the basic functionality typical Oracle tools.
 *
 * @author Akara Sucharitakul modified by Sheetal Patil
 */
public abstract class OracleTool {

    private static Logger logger =
            Logger.getLogger(OracleTool.class.getName());

    /** Injected tool context. */
    @Context public ToolContext ctx;
    Command cmd;
    CommandHandle processRef;
    ArrayList<String> toolCmd;
    String toolName;
    String snapId;

    /**
     * This is the method that should get the arguments to
     * call the tool with.
     *
     */
    @Configure public void configure() throws ConfigurationException {
        toolName = ctx.getToolName();        
        String oracleHome = ctx.getServiceProperty("serverHome");
        if(oracleHome == null || oracleHome.trim().length() <= 0) {
            throw new ConfigurationException("Oracle DB serverHome property is not provided");
        }
        String oracleSid = ctx.getServiceProperty("serverId");
        if(oracleSid == null || oracleSid.trim().length() <= 0){
            throw new ConfigurationException("Oracle DB serverId is not provided");
        }
        String oracleBin = oracleHome + "bin" + File.separator;
        
        // Prepare the environment
        String[] env = new String[4];
        env[0] = "ORACLE_HOME=" + oracleHome;
        env[1] = "ORACLE_SID=" + oracleSid;
        env[2] = "LD_LIBRARY_PATH=" + oracleHome + "lib";
        env[3] = "LD_LIBRARY_PATH_64=" + oracleHome + "lib";

        // Prepare the input
        String stdin = "/ as sysdba\n" +
                getSnapCommand() + "\n" +
                "exit\n";

        // Prepare the command
        toolCmd.add(oracleBin);
        toolCmd.add("sqlplus");
        cmd = new Command(toolCmd);
        cmd.setOutputFile(Command.STDOUT, ctx.getOutputFile());
        cmd.setEnvironment(env);
        cmd.setInput(stdin.getBytes());

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
     * Starts up a Oracle DB monitoring tool.
     * @throws IOException Error executing tool
     * @throws InterruptedException Monitoring tool interrupted
     */
    @Start public void start() throws IOException, InterruptedException {
        processRef = RunContext.exec(cmd);
        snapId = parseSnapId(processRef.fetchOutput(Command.STDOUT));
        logger.fine(toolName + " Started with Cmd = " + toolCmd);
    }

    /**
     * Stops the Oracle DB monitoring tool.
     * @throws IOException Error stopping tool
     */
    @Stop public void stop() throws IOException {
        try {
            processRef = RunContext.exec(cmd);
            String snapId1 = parseSnapId(processRef.fetchOutput(Command.STDOUT));
            logger.finer("snapId1: " + snapId1);
            // Prepare the input
            String stdin = "/ as sysdba\n" +
                       getReportCommand(snapId, snapId1, ctx.getOutputFile()) + "\n" +
                       "exit\n";
            cmd.setInput(stdin.getBytes());
            cmd.setLogLevel(Command.STDOUT, Level.FINER);
            logger.fine("Stopping tool " + this.toolCmd);
            processRef = RunContext.exec(cmd);
            processRef.destroy();
            processRef.waitFor(10000);
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, null, ex);
        } catch (RemoteException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
}