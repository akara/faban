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
 * $Id: OracleAgentImpl.java,v 1.1 2006/06/29 18:51:41 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */package com.sun.faban.harness.agent;

import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.common.Run;

import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This is the OracleAgent class to perform the OracleService remotely on
 * the server machine(s)
 *
 * @author Ramesh Ramachandran
 */
public class OracleAgentImpl extends UnicastRemoteObject
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
     *
     * Constructor
     *
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
    public void configure (Run run, String oracleHome, String oracleSid, String[] allConfigs) throws RemoteException, Exception {

        this.oracleHome = oracleHome;
        this.oracleSid = oracleSid;

        try {
            cmdAgent = CmdAgentImpl.getHandle();
            fa = (FileAgent) CmdAgentImpl.getRegistry().getService(Config.FILE_AGENT + "@" + host);

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
     * on this machine
     * @param serverID - this variable is not used (can be null)
     * @return - list array of parameters
     */
    public List getConfig(String serverID) throws RemoteException, IOException {
        List paramList = new ArrayList();

        return paramList;

    }


    /**
     * This method set the configure parameters of the Oracle instance on this
     * machine.
     * @param serverID - not used (can be null)
     * @param oracleParams - list of parameters
     */

    public void setConfig(String serverID, List oracleParams) 
            throws RemoteException, IOException {

        return;
    }

    /**
     *
     * To start an instance of Oracle.
     *
     * @param serverID - not used.
     *
     * @return boolean - true if successful, false if not
     *
     */
    public boolean start(String serverID) throws RemoteException, Exception {

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
     * To stop an Oracle instance (shutdown)
     *
     * @param serverID - not used.
     *
     * @return boolean - true if successful, false if not.
     *
     */
    public boolean stop(String serverID) throws RemoteException, Exception {

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
     * To exec SQL commands
     *
     * @param sql - its a combination of user/passwd\ncommand
     *
     * @return boolean - true if successful, false if not.
     *
     */
    public boolean execSQL(String sql) throws RemoteException, Exception {

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
            cmdAgent.start("/bin/chmod a+x " + ORACLE_SCRIPT, Config.DEFAULT_PRIORITY);
        }
        catch (Exception e) {
            logger.severe("Could not change mode of " + ORACLE_SCRIPT  + " : " + e.toString());
        }

        logger.fine("Executing command " + sqlCmd);

        boolean retVal = cmdAgent.start(ORACLE_SCRIPT, Config.DEFAULT_PRIORITY);

        if (retVal) {
            logger.fine("Command executed successfully");
        }
        else {
            logger.warning("Could not execute command " + sqlCmd);
        }
        return retVal;
    }

    /**
     *
     * Starts the listener on the machine running Oracle.
     *
     * @return boolean - true if successful false if not.
     *
     */
    public boolean startListener() throws RemoteException, Exception {

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
            cmdAgent.start("/bin/chmod a+x " + ORACLE_SCRIPT, Config.DEFAULT_PRIORITY);
        }
        catch (Exception e) {
            logger.severe("Could not change mode of " + ORACLE_SCRIPT + " : " + e);
            logger.log(Level.FINE, "Exception", e);
        }

        logger.fine("Executing command " + cmd);

        boolean retVal = cmdAgent.start(ORACLE_SCRIPT, Config.DEFAULT_PRIORITY);
        if (retVal) {
            logger.info("Listener started.");
        }
        else {
            logger.warning("Could not start the Listener");
        }
        return retVal;
    }

    /**
     *
     * Stops the listener on the machine running Oracle.
     *
     * @return boolean - true if successful false if not.
     *
     */
    public boolean stopListener() throws RemoteException, Exception {

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
            cmdAgent.start("/bin/chmod a+x " + ORACLE_SCRIPT, Config.DEFAULT_PRIORITY);
        }
        catch (Exception e) {
            logger.severe("Could not change mode of " + ORACLE_SCRIPT + " : " + e);
            logger.log(Level.FINE, "Exception", e);
        }

        logger.fine("Executing command " + cmd);

        boolean retVal = cmdAgent.start(ORACLE_SCRIPT, Config.DEFAULT_PRIORITY);
        listenerRunning = false;
        if (retVal) {
            logger.info("Listener stopped");
        }
        else {
            logger.warning("Could not stop the Listener");
        }
        return retVal;
    }


    /**
     *
     * Check status of the listener on the machine running Oracle.
     *
     * @return boolean - true if it is running false if not.
     *
     */
    public boolean checkListenerStatus() throws RemoteException, Exception {

        String cmd = oracleHome + "/bin/lsnrctl status";

        cmdAgent.start(cmd, Config.DEFAULT_PRIORITY);

        return true;

    }

    /**
     * 
     * start collection of Oracle stats. This runs a script whose name is
     * specified by the ServerNames class variable ORACLESTARTSTATS which 
     * does the work for it.
     * 
     */
    public void startStats() throws RemoteException, Exception {
    }


    /**
     * 
     * stop collection of Oracle stats. This runs a script whose name is
     * specified by the ServerNames class variable ORACLESTOPSTATS which 
     * does the work for it.
     * 
     */ 
    public void stopStats() throws RemoteException, Exception {
    }

    /**
     * 
     * Clear oracle logs. This runs a script whose name is
     * specified by the ServerNames class variable ORACLECLEARLOGS which 
     * does the work for it.
     * 
     */ 	
    public void clearLogs() throws RemoteException, Exception {
    }

    /**
     *
     * Kill this oracle instance. This just calls the stop method.
     *
     */
    public void kill() throws RemoteException, Exception {

        logger.fine("Killed");
        return;
    }

    /**
     * When this instance is unreferenced the application must exit.
     *
     * @see         java.rmi.server.Unreferenced
     *
     */
    public void unreferenced() {

        try {
            kill();
        }
        catch (Exception e) { }
    }
}
