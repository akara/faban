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
 */package com.sun.faban.harness.agent;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;

import java.io.File;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This is the OracleAgent class to perform the OracleService remotely on
 * the server machine(s).
 *
 * @author Ramesh Ramachandran
 * @deprecated Replaced by the services/tools infrastructure
 */
@Deprecated public class OracleAgentImpl extends UnicastRemoteObject
        implements OracleAgent, Unreferenced {

    static String masterMachine = null;
    static String host;
    CmdAgent cmdAgent;
    FileAgent fa;
    private Properties configFiles = new Properties();
    // The first of the allConfigs sent by the caller is assumed to be the 
    // startup file for oracle. And this is assigned to the String "startupConf"
    // during configuration.
    String startupConf;
    String oracleHome;
    String oracleSid;
    String[] args;
    boolean listenerRunning = false;

    boolean started = false;

    Logger logger;

    private static final String ORACLE_SCRIPT = Config.TMP_DIR + "sql.sh";

    /**
     * Constructs an Oracle agent.
     * @throws RemoteException A communication error occurred.
     */
    public OracleAgentImpl() throws RemoteException {

        super();
        logger = Logger.getLogger(this.getClass().getName());
        host = CmdAgentImpl.getHost();
        masterMachine = CmdAgentImpl.getMaster();
    }

    /**
     *
     * To configure the OracleAgent at the start of a run. 
     *
     * @param run - The run object.
     * @param oracleHome - ORACLE_HOME
     * @param oracleSid - ORACLE_SID
     * @param allConfigs - Pathnames for all config files.
     *
     */
    public void configure (Run run, String oracleHome, String oracleSid,
                           String[] allConfigs) {

        this.oracleHome = oracleHome;
        this.oracleSid = oracleSid;

        try {
            cmdAgent = CmdAgentImpl.getHandle();
            fa = (FileAgent) CmdAgentImpl.getRegistry().getService(
                    Config.FILE_AGENT + "@" + host);

            startupConf = allConfigs[0];
            for (int i = 0; i < allConfigs.length; i++) {
                String fullPath = allConfigs[i];
                File fullPathFile = new File(fullPath);
                String fileName = fullPathFile.getName();
                configFiles.setProperty(fileName, fullPath);
            }
            logger.config("Configured");
        } catch (Exception e) {
            logger.severe("Failed to configure " + e);
            logger.log(Level.FINE, "Exception", e);
        }
    }


    /**
     * This method gets the configure parameters of the Oracle instance started
     * on this machine.
     * @param serverID - this variable is not used (can be null)
     * @return - list array of parameters
     */
    public List getConfig(String serverID) {
        List paramList = new ArrayList();
        return paramList;
    }


    /**
     * This method set the configure parameters of the Oracle instance on this
     * machine.
     * @param serverID - not used (can be null)
     * @param oracleParams - list of parameters
     */
    public void setConfig(String serverID, List oracleParams) {
        return;
    }

    /**
     * Start an Oracle instance.
     * @param serverID Instance name
     * @return Whether the server started successfully
     * @throws Exception An error occurred in the process
     */
    public boolean start(String serverID) throws Exception {

        String oracleStartCmd = null;
        boolean retVal;

        if (started) {
            stop(serverID);
            started = false;
        }

        // The command is a combination of user/passwd\ncommand
        oracleStartCmd = " / as sysdba \nstartup pfile=" + startupConf;

        retVal = this.execSQL(oracleStartCmd);
        if (retVal) {
            started = true;
            logger.info("Started the Oracle instance successfully");
        }
        else {
            logger.severe("Could not start the Oracle instance");
        }
        return retVal;
    }

    /**
     * Stop an Oracle instance.
     * @param serverID Instance name
     * @return Whether the server stopped successfully
     * @throws Exception An error occurred in the process
     */
    public boolean stop(String serverID) throws Exception {

        String oracleStopCmd = null;
        boolean retVal;

        logger.fine("OracleAgentImpl.stop: entering");

        oracleStopCmd = " / as sysdba\nshutdown";

        logger.fine("Stopping the instance of Oracle");

        retVal = this.execSQL(oracleStopCmd);

        if (retVal) {
            started = false;
            logger.info("Stopped the Oracle instance successfully");
        }
        else {
            logger.severe("Could not stop the Oracle instance");
        }
        return retVal;
    }

    /**
     * Executes an SQL statement.
     * @param sql The statement
     * @return Whether the SQL statement executed successfully
     * @throws Exception An error occurred in the process
     */
    public boolean execSQL(String sql) throws Exception {

        StringBuffer buff = new StringBuffer("#!/bin/csh -f\nsetenv ORACLE_SID ");
        buff.append(oracleSid);
        buff.append("\nsetenv ORACLE_HOME ");
        buff.append(oracleHome);
        buff.append("\nsetenv PATH $ORACLE_HOME/bin\nsetenv LD_LIBRARY_PATH $ORACLE_HOME/lib\n");
        buff.append("\nsetenv LD_LIBRARY_PATH_64 $ORACLE_HOME/lib/sparcv9\n");
        buff.append("$ORACLE_HOME/bin/sqlplus /nolog <<EOT\nconnect ");
        buff.append(sql);
        buff.append("\nexit\nEOT");

        String sqlCmd = buff.toString();

        try {
            if (!fa.writeWholeFile(ORACLE_SCRIPT, sqlCmd)) {
                logger.severe("Could not write to file " + ORACLE_SCRIPT);
                return false;
            }
        }
        catch (RemoteException re) {
        }
        try {
            Command c = new Command("/bin/chmod", "a+x", ORACLE_SCRIPT);
            CommandHandle h = cmdAgent.execute(c, null);
            int exitValue = h.exitValue();
            if (exitValue != 0) {
                logger.severe("Could not change mode for "+ ORACLE_SCRIPT +
                        ". Exit value for chmod is " + exitValue);
            }
        }
        catch (Exception e) {
            logger.severe("Could not change mode of " + ORACLE_SCRIPT  + " : " +
                    e.toString());
        }

        logger.fine("Executing command " + sqlCmd);

        Command c = new Command(ORACLE_SCRIPT);
        CommandHandle h = cmdAgent.execute(c, null);
        int exitValue = h.exitValue();
        boolean retVal;
        if (exitValue == 0) {
            logger.fine("Command executed successfully");
            retVal = true;
        } else {
            logger.warning("Could not execute command " + sqlCmd);
            retVal = false;
        }
        return retVal;
    }

    /**
     * Start the Oracle listener.
     * @return Whether the listener started successfully
     * @throws Exception An error occurred in the process
     */
    public boolean startListener() throws Exception {

        StringBuffer buff = new StringBuffer("#!/bin/csh -f\nsetenv ORACLE_SID ");
        buff.append(oracleSid);
        buff.append("\nsetenv ORACLE_HOME ");
        buff.append(oracleHome);
        buff.append("\nsetenv PATH $ORACLE_HOME/bin\nsetenv LD_LIBRARY_PATH $ORACLE_HOME/lib\n");
        buff.append("\nsetenv LD_LIBRARY_PATH_64 $ORACLE_HOME/lib/sparcv9\n");
        buff.append("$ORACLE_HOME/bin/lsnrctl start\n");

        String cmd = buff.toString();

        try {
            if (!fa.writeWholeFile(ORACLE_SCRIPT, cmd)) {
                logger.severe("Could not write to " + ORACLE_SCRIPT);
                return false;
            }
        }
        catch (RemoteException re) {
            logger.severe("Could not write to " + ORACLE_SCRIPT + " " + re);
            logger.log(Level.FINE, "RemoteException", re);
        }

        try {
            Command c = new Command("/bin/chmod", "a+x", ORACLE_SCRIPT);
            CommandHandle h = cmdAgent.execute(c, null);
            int exitValue = h.exitValue();
            if (exitValue != 0) {
                logger.severe("Could not change mode for "+ ORACLE_SCRIPT +
                        ". Exit value for chmod is " + exitValue);
            }
        }
        catch (Exception e) {
            logger.severe("Could not change mode of " + ORACLE_SCRIPT + " : " + e);
            logger.log(Level.FINE, "Exception", e);
        }

        logger.fine("Executing command " + cmd);

        Command c = new Command(ORACLE_SCRIPT);
        CommandHandle h = cmdAgent.execute(c, null);
        int exitValue = h.exitValue();
        boolean retVal;
        if (exitValue == 0) {
            logger.info("Listener started.");
            retVal = true;
        } else {
            logger.warning("Could not start the Listener");
            retVal = false;
        }
        return retVal;
    }

    /**
     * Start the Oracle listener.
     * @return Whether the listener started successfully
     * @throws Exception An error occurred in the process
     */
    public boolean stopListener() throws Exception {

        StringBuffer buff = new StringBuffer("#!/bin/csh -f\nsetenv ORACLE_SID ");
        buff.append(oracleSid);
        buff.append("\nsetenv ORACLE_HOME ");
        buff.append(oracleHome);
        buff.append("\nsetenv PATH $ORACLE_HOME/bin\nsetenv LD_LIBRARY_PATH $ORACLE_HOME/lib\n");
        buff.append("\nsetenv LD_LIBRARY_PATH_64 $ORACLE_HOME/lib/sparcv9\n");
        buff.append("$ORACLE_HOME/bin/lsnrctl stop\n");

        String cmd = buff.toString();

        try {
            if (!fa.writeWholeFile(ORACLE_SCRIPT, cmd)) {
                logger.severe("Could not write to file " + ORACLE_SCRIPT);
                return false;
            }
        }
        catch (RemoteException re) {
            logger.severe("Could not write to " + ORACLE_SCRIPT + " " + re);
            logger.log(Level.FINE, "Exception", re);
        }
        try {
            Command c = new Command("/bin/chmod", "a+x", ORACLE_SCRIPT);
            CommandHandle h = cmdAgent.execute(c, null);
            int exitValue = h.exitValue();
            if (exitValue != 0) {
                logger.severe("Could not change mode for "+ ORACLE_SCRIPT +
                        ". Exit value for chmod is " + exitValue);
            }
        }
        catch (Exception e) {
            logger.severe("Could not change mode of " + ORACLE_SCRIPT + " : " + e);
            logger.log(Level.FINE, "Exception", e);
        }

        logger.fine("Executing command " + cmd);

        Command c = new Command(ORACLE_SCRIPT);
        CommandHandle h = cmdAgent.execute(c, null);
        int exitValue = h.exitValue();
        boolean retVal;
        if (exitValue == 0) {
            logger.info("Listener stopped");
            retVal = true;
        } else {
            logger.warning("Could not stop the Listener");
            retVal = false;
        }
        return retVal;
    }


    /**
     * Checks the listener status.
     * @return Whether the listener is running
     * @throws Exception An error occurred in the process
     */
    public boolean checkListenerStatus() throws Exception {

        Command cmd = new Command(oracleHome + "/bin/lsnrctl",  "status");

        cmdAgent.execute(cmd, null);

        return true;

    }

    /**
     * Start collection of Oracle stats. This runs a script whose name is
     * specified by the ServerNames class variable ORACLESTARTSTATS which 
     * does the work for it.
     */
    public void startStats() {
    }


    /**
     * Stop collection of Oracle stats. This runs a script whose name is
     * specified by the ServerNames class variable ORACLESTOPSTATS which 
     * does the work for it.
     */
    public void stopStats() {
    }

    /**
     * Clear oracle logs. This runs a script whose name is
     * specified by the ServerNames class variable ORACLECLEARLOGS which 
     * does the work for it.
     */
    public void clearLogs() {
    }

    /**
     * Kill this oracle instance. This just calls the stop method.
     */
    public void kill() {

        logger.fine("Killed");
        return;
    }

    /**
     * When this instance is unreferenced the application must exit.
     * @see java.rmi.server.Unreferenced
     */
    public void unreferenced() {

        try {
            kill();
        }
        catch (Exception e) { }
    }
}
