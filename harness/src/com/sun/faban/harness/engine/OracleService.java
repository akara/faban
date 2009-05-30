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
 * $Id: OracleService.java,v 1.4 2009/05/30 04:48:49 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.agent.OracleAgent;
import com.sun.faban.harness.agent.OracleAgentImpl;
import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.ParamRepository;

import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class implements the service of configure/start/stop Oracle instances.
 * it is used by the benchmark to start the OracleAgent on the server machine 
 * and perform these operations throught OracleAgent.
 *
 * @author Ramesh Ramachandran
 * @deprecated
 */

@Deprecated final public class OracleService {

    private static OracleService oracle;

    private static final String ORACLE_AGENT = "OracleAgent";

    Logger logger;
    protected String masterMachine;

    String oracleHome;
    String oracleSid;
    private Run run;

    private CmdService cmdService;
    ParamRepository par = null;
    String host;
    //    private Properties allServers = new Properties();
    private String[] serverMachines = null;
    private HashMap allAgents = new HashMap();

    private OracleService() {
        logger = Logger.getLogger(this.getClass().getName());
    }

    public static OracleService getHandle () {
        if(oracle == null)
            oracle = new OracleService();

        return oracle;
    }

    public void setup(Run r, CmdService c, String oracleHome, String oracleSid, String[] serverMachines, String[] allConfigs) {

        run = r;
        cmdService = c;
        this.oracleHome = oracleHome;
        this.oracleSid = oracleSid;

        OracleAgent oracleAgent = null;

        this.serverMachines = serverMachines;

        try {
            // start OracleAgents on server machines
            for (int i = 0; i < serverMachines.length; i++) {

                logger.fine("Using CmdService to start " +  ORACLE_AGENT + " on " + serverMachines[i]);

                cmdService.startAgent(serverMachines[i], OracleAgentImpl.class, ORACLE_AGENT);

                Thread.sleep(5000);

                // connect to oracleAgent
                String s = ORACLE_AGENT + "@" + serverMachines[i];
                oracleAgent = (OracleAgent) cmdService.getRegistry().getService(s);
                if (oracleAgent == null) {
                    logger.severe("Failed to connect to " + s);
                } else {
                    logger.fine("Connected to " + s);
                    oracleAgent.configure(run, oracleHome, oracleSid, allConfigs);
                    logger.info("Configured " + s);
                    allAgents.put(serverMachines[i], oracleAgent);
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "OracleAgent failed.", e);
        }
    }


    /**
     * get server configuration parameters from the conf files
     * if null is passed then the operation is applied to all servers
     */
    public List getConfig(String serverMachine) {

        List l = null;

        OracleAgent oracleAgent = (OracleAgent) allAgents.get(serverMachine);
        if (oracleAgent == null) {
            logger.severe("Unable to find Oracle Agent for " + serverMachine);
        }
        try {
            l = oracleAgent.getConfig(serverMachine);
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get Oracle Config for " +
                    serverMachine + '.', e);

        }
        return l;
    }

    /**
     * set server configuration parameters: update the two conf files
     * if null is passed then the operation is applied to all servers
     */
    public void setConfig(String serverMachine, List oracleParams) {
        String[] machines = new String[1];
        if(serverMachine != null)
            machines[0] = serverMachine;
        else
            machines = serverMachines;

        for(int i = 0; i < machines.length; i++) {
            OracleAgent oracleAgent = (OracleAgent) allAgents.get(machines[i]);
            if (oracleAgent != null) {
                try {
                    oracleAgent.setConfig(machines[i], oracleParams);
                } catch (Exception e) {
                    logger.severe("Failed to set oracleParams of "+ machines[i] + " "  + e);
                    logger.log(Level.FINE, "Exception", e);
                }
            }
            else
                logger.severe("Failed to set oracleParams of "+ machines[i] + " Agent is null");
        }
    }


    /**
     * start/restart Server 
     * if null is passed then the operation is applied to all servers
     *
     */
    public boolean restartServer(String serverMachine) {


        // Need some mechanism to check if Oracle is running currently or not

        boolean ret = true;
        String[] machines = new String[1];
        if(serverMachine != null)
            machines[0] = serverMachine;
        else
            machines = serverMachines;

        for(int i = 0; i < machines.length; i++) {
            OracleAgent oracleAgent = (OracleAgent) allAgents.get(machines[i]);
            if (oracleAgent != null) {
                try {
                    oracleAgent.stop(machines[i]);
                    oracleAgent.start(machines[i]);
                } catch (Exception e) {
                    logger.severe("Failed to start on " + machines[i] + " " + e.toString());
                    logger.log(Level.FINE, "Exception", e);
                    ret = ret && false;
                }
                logger.info("OracleServer: restartServer - Successfully started Oracle on machine " + machines[i]);
            }
            else {
                logger.severe("Failed to restart Oracle on  "+ machines[i] + " Agent is null");
                ret = ret && false;
            }
        }
        return ret;
    }

    /**
     * stop Server
     * if null is passed then the operation is applied to all servers
     */
    public boolean stopServer(String serverMachine) {


        // Need some mechanism to check if Oracle is running currently or not
        //	String serverMachine = allServers.getProperty(serverRoot);

        /*
        try {
        String s = ServerConfig.ORACLEAGENT + "@" + serverMachine;
        OracleAgent oracleAgent = (OracleAgent) con.getService(s);

        logger.severe("Connected to " + s);
        }
        catch (RemoteException re) {
        try {
        logger.severe("OracleService: stopServer - Could not get the reference to the OracleAgent " + serverMachine + " " + e.toString());
        }
        catch (IOException ie) { }
        }
        */
        boolean ret = true;
        String[] machines = new String[1];
        if(serverMachine != null)
            machines[0] = serverMachine;
        else
            machines = serverMachines;

        for(int i = 0; i < machines.length; i++) {
            OracleAgent oracleAgent = (OracleAgent) allAgents.get(machines[i]);
            if (oracleAgent != null) {
                try {
                    oracleAgent.stop(machines[i]);
                } catch (Exception e) {
                    logger.severe("Failed to stop on " + machines[i] + " " + e);
                    logger.log(Level.FINE, "Exception", e);
                    ret = ret && false;
                }
                logger.fine("Successfully shutdown Oracle on machine " + machines[i]);
            }
            else {
                logger.severe("Failed to stop Oracle on  "+ machines[i] + " Agent is null");
                ret = ret && false;
            }
        }
        return ret;
    }

    /**
     * restart listener
     * if null is passed then the operation is applied to all servers
    */
    public boolean restartListener(String serverMachine) {
        return stopListener(serverMachine) && startListener(serverMachine);
    }

    /**
     * start listener
     * if null is passed then the operation is applied to all servers
    */
    public boolean startListener(String serverMachine) {

        boolean ret = true;
        String[] machines = new String[1];
        if(serverMachine != null)
            machines[0] = serverMachine;
        else
            machines = serverMachines;

        for(int i = 0; i < machines.length; i++) {
            OracleAgent oracleAgent = (OracleAgent) allAgents.get(machines[i]);
            if (oracleAgent != null) {
                try {
                    ret  = ret && oracleAgent.startListener();
                } catch (Exception e) {
                    ret = ret && false;
                    logger.severe("Failed to start on machine " + serverMachine + " " + e);
                    logger.log(Level.FINE, "Exception", e);
                }
            }
            else {
                ret = ret && false;
                logger.severe("Failed to stop Oracle on  "+ machines[i] + " Agent is null");
            }
        }
        return ret;
    }


    /**
     * stop listener
     * if null is passed then the operation is applied to all servers
    */
    public boolean stopListener(String serverMachine) {

        boolean ret = true;
        String[] machines = new String[1];
        if(serverMachine != null)
            machines[0] = serverMachine;
        else
            machines = serverMachines;

        for(int i = 0; i < machines.length; i++) {
            OracleAgent oracleAgent = (OracleAgent) allAgents.get(machines[i]);
            if (oracleAgent != null) {
                try {
                    ret  = ret && oracleAgent.stopListener();
                } catch (Exception e) {
                    ret = ret && false;
                    logger.severe("Failed to stop listner on " + machines[i] + " " + e);
                    logger.log(Level.FINE, "Exception", e);
                }
            }
            else {
                ret = ret && false;
                logger.severe("Failed to stop Oracle on  "+ machines[i] + " Agent is null");
            }
        }
        return ret;
    }


    /**
     * start statisticas collection
     */
    public void startStats(String serverMachine) {

        OracleAgent oracleAgent = (OracleAgent) allAgents.get(serverMachine);
        try {
            oracleAgent.startStats();
        } catch (Exception e) {
            logger.severe("Failed to clear Oracle Stats for " + serverMachine + " " + e);
            logger.log(Level.FINE, "Exception", e);
        }

    }

    /**
     * stop statisticas collection
     */
    public void stopStats(String serverMachine) {

        OracleAgent oracleAgent = (OracleAgent) allAgents.get(serverMachine);
        try {
            oracleAgent.stopStats();
        } catch (Exception e) {
            logger.severe("Failed to collect Oracle Stats for " + serverMachine + " " + e);
            logger.log(Level.FINE, "Exception", e);
        }

    }

    /**
     * clear log files
     */
    public void clearLogs(String serverMachine) {

        OracleAgent oracleAgent = (OracleAgent) allAgents.get(serverMachine);
        try {
            oracleAgent.clearLogs();            
        } catch (Exception e) {
            logger.severe("Failed to clear Oracle Logs for " + serverMachine + " " + e);
            logger.log(Level.FINE, "Exception", e);
        }
    }

    public void kill () {
        if((serverMachines != null) &&(serverMachines.length > 0)) {
            for (int i = 0; i < serverMachines.length; i++) {
                OracleAgent oracleAgent = (OracleAgent) allAgents.get(serverMachines[i]);
                try {
                    if(oracleAgent != null) {
                        oracleAgent.kill();
                        logger.fine("killed OracleAgent on " + serverMachines[i] );
                    }
                    else {
                        logger.severe("Unable to kill OracleAgent on " + serverMachines[i] + " OracleAgent is null");
                    }
                } catch (Exception ex) {
                }
            }
        }
        allAgents.clear();
    }

    /**
     * execute an SQL command
     * @param command is a combination of user/passwd\ncommand to execute
     * @param priority in which to run command (not used)
     */
    public void cmdServer(String command, int priority) {

        for (int i = 0; i < serverMachines.length; i++) {

            OracleAgent oracleAgent = (OracleAgent) allAgents.get(serverMachines[i]);
            try {
                if(!oracleAgent.execSQL(command))
                    logger.severe("Failed to exec SQL " + command + " on " + serverMachines[i]);
            } catch (Exception ex) {
                try {
                    logger.severe("Failed to exec SQL " + command + " on " + serverMachines[i] + " " + ex);
                    logger.log(Level.FINE, "Exception", ex);
                }catch (Exception e) {}
            }
        }
    }
}
