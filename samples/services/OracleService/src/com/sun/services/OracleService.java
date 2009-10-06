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
package com.sun.services;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.services.ServiceContext;
import com.sun.faban.harness.Context;

import com.sun.faban.harness.Configure;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;
import java.io.File;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class implements the service of configure/start/stop Oracle instances.
 * it is used by the benchmark to start the OracleAgent on the server machine 
 * and perform these operations throught OracleAgent.
 *
 * @author Sheetal Patil based on work done by Ramesh Ramachandran
 */

public class OracleService {

    /** Injected service context. */
    @Context public ServiceContext ctx;
    Logger logger = Logger.getLogger(OracleService.class.getName());
    String oracleHome,  myServers[], oracleSid;
    ArrayList<String> listners = new ArrayList<String>();
    String oracleStartCmd, oracleStopCmd, startupConf, oracleBin;
    String[] env;
    CommandHandle serverHandles[];
    CommandHandle listnerHandles[];

    /**
     * Configures this Oracle service.
     */
    @Configure public void configure() throws ConfigurationException {
        logger.fine("Configuring oracle service ");
        myServers = ctx.getUniqueHosts();
        if(myServers == null){
            throw new ConfigurationException("Oracle DB hostname is not provided");
        }
        oracleHome = ctx.getProperty("serverHome");
        if(oracleHome != null && oracleHome.trim().length() > 0) {
            if (!oracleHome.endsWith(File.separator))
            oracleHome = oracleHome + File.separator;
        }else{
            throw new ConfigurationException("Oracle DB serverHome is not provided");
        }
        oracleSid = ctx.getProperty("serverId");
        if(oracleSid == null || oracleSid.trim().length() <= 0){
            throw new ConfigurationException("Oracle DB serverId is not provided");
        }
        startupConf = ctx.getProperty("startupConf"); // What is this used for?
        if(startupConf == null || startupConf.trim().length() <= 0){
            throw new ConfigurationException("Oracle DB startupConf is not provided");
        }
        String includeListners = ctx.getProperty("includes");
        if(includeListners != null && includeListners.trim().length() > 0){
            StringTokenizer st = new StringTokenizer(includeListners,"; ,\n");
            while(st.hasMoreTokens()){
                listners.add(st.nextToken());
            }
        }else{
            throw new ConfigurationException("Oracle DB includes property is not provided");
        }
        oracleBin = oracleHome + "bin" + File.separator;
        oracleStartCmd = oracleBin + "sqlplus /nolog <<EOT\nconnect " +
                " / as sysdba \nstartup pfile=" + startupConf + "\nexit\nEOT";
        oracleStopCmd = oracleBin + "sqlplus /nolog <<EOT\nconnect " +
                " / as sysdba\nshutdown" + "\nexit\nEOT";
        serverHandles = new CommandHandle[myServers.length];
        logger.fine("OracleService Configure complete.");
        String[] setEnv = {"ORACLE_SID="+oracleSid, "ORACLE_HOME=" + oracleHome,
                    "PATH=" + oracleBin, "LD_LIBRARY_PATH=" + oracleHome +"lib",
           "LD_LIBRARY_PATH_64="+oracleHome+"lib" + File.separator + "sparcv9"};
        this.env = setEnv;

    }

    /**
     * Starts up the Oracle instances.
     */
    @Start public void startup() {
        for (int i = 0; i < myServers.length; i++) {
            logger.fine("Starting oracle on " + myServers[i]);
            Command startCmd = new Command(oracleStartCmd);
            startCmd.setEnvironment(env);
            logger.fine("Starting oracle with: " + oracleStartCmd);
            startCmd.setSynchronous(false); // to run in bg
            try {
                // Run the command in the background
                if ( !checkServerStarted(i)) {
                    serverHandles[i] = RunContext.exec(myServers[i], startCmd);
                    logger.fine("Completed Oracle server startup " +
                            "successfully on" + myServers[i]);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to start Oracle server.", e);
            }
            if(ctx.getProperty("includes").equalsIgnoreCase("true")) {
                logger.fine("Starting listner");
                Command listerCmd = new Command(oracleBin + "lsnrctl start");
                listerCmd.setSynchronous(false); // to run in bg
                try {
                    // Run the command in the background
                    if (!checkListnerStarted(myServers[i])) {
                        RunContext.exec(myServers[i], listerCmd);
                        logger.fine("Completed listner startup successfully on"
                                + myServers[i]);
                    }
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Failed to start listner.", e);
                }

            }
        }       
    }

    /**
     * Shuts down the Oracle instances.
     * @throws Exception Error shutting down the instances
     */
    @Stop public void shutdown() throws Exception {
        for (int i = 0; i < myServers.length; i++) {
            String myServer = myServers[i];
            if (checkServerStarted(i)) {
                if (checkListnerStarted(myServer)) {
                    stopListner(myServer);
                }
                stopServer(myServer);
            }
        }
    }

    private boolean checkServerStarted(int i) throws Exception {
        boolean started = false;
        if (serverHandles[i] != null) {
            started = true;
        }
        return started;
    }

    private boolean checkListnerStarted(String hostName) throws Exception {
        boolean started = false;
        CommandHandle ch = RunContext.exec(hostName, new Command(oracleBin + "lsnrctl status"));
        if (ch.fetchOutput(0) != null){
            started = true;
        }
        return started;
    }

    private void stopServer(String serverId){
        logger.fine("Stopping Oracle server on" + serverId);
        try {
            // First kill oracle
            Command stopCmd = new Command(oracleStopCmd);
            RunContext.exec(serverId, stopCmd);
            logger.fine("Oracle server stopped successfully on" + serverId);
        } catch (Exception ie) {
            logger.warning("Kill Oracle failed with " + ie.toString());
            logger.log(Level.FINE, "kill Oracle Exception", ie);
        }
    }

    private void stopListner(String serverId){
        logger.fine("Stopping listner on" + serverId);
        try {
            Command stopCmd = new Command(oracleBin + "lsnrctl stop");
            RunContext.exec(serverId, stopCmd);
            logger.fine("Listner stopped successfully on" + serverId);
        } catch (Exception ie) {
            logger.warning("Kill listner failed with " + ie.toString());
            logger.log(Level.FINE, "kill listner Exception", ie);
        }
    }

    
}
