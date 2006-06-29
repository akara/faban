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
 * $Id: SJSAS8Service.java,v 1.1 2006/06/29 18:51:42 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.agent.SJSAS8Agent;
import com.sun.faban.harness.agent.SJSAS8AgentImpl;
import com.sun.faban.harness.common.Run;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class implements the service of configure/start/stop BEA SJSAS8 instances.
 * it is used by the benchmark to start the SJSAS8Agent on the server machine and
 * perform these operations remotely using this Agent.
 *
 * @author Ramesh Ramachandran
 */
final public class SJSAS8Service implements J2eeService {

    private static SJSAS8Service sunService;

    private static final String SJSAS8_AGENT = "SJSAS8Agent";

    private Run run;
    private CmdService cmdService;
    private ArrayList myServers = new ArrayList();
    private HashMap allAgents = new HashMap();
    private Logger logger;

    /**
     *
     * Private Constructor for a singleton object.
     *
     */
    private SJSAS8Service() {
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
      *
      * Get the reference to the singleton object.
      *
      */
    public static SJSAS8Service getHandle () {
        if(sunService == null)
            sunService = new SJSAS8Service();

        return sunService;
    }

    /**
      * The setup method is called to set up a benchmark run. It starts SJSAS8Agent
      * instances on specified machines
      *
      * @param r - the run object.
      * @param serverMachines - array specifying the machines for each
      *                 SJSAS8 instance.
      * @param instanceArray  - String arrays of Instance names corresponding
      *                 to each serverMachine
      */
    public void setup(Run r, String[] serverMachines, String[] serverHomes,
                      List instanceArray, List logsArray) {
        run = r;
        cmdService = CmdService.getHandle();
        SJSAS8Agent sunAgent = null;

        try {
            for (int i = 0; i < serverMachines.length; i++) {
                sunAgent = (SJSAS8Agent) allAgents.get(serverMachines[i]);

                if (sunAgent == null) {

                    logger.fine("Using CmdService to start SJSAS8 Agent on " + serverMachines[i]);

                    cmdService.startAgent(serverMachines[i], SJSAS8AgentImpl.class, SJSAS8_AGENT);

                    // wait for the Agent to start
                    Thread.sleep(5000);

                    // connect to sunAgent
                    String s = SJSAS8_AGENT + "@" + serverMachines[i];
                    sunAgent = (SJSAS8Agent) cmdService.getRegistry().getService(s);

                    logger.fine("Connected to " + s);
                    myServers.add(serverMachines[i]);
                    allAgents.put(serverMachines[i], sunAgent);
                }
                sunAgent.setup(run, serverHomes[i], 
                               (String[])instanceArray.get(i),
                               (String[])logsArray.get(i));
            }
            logger.info("Setup complete");
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Setup failed.", e);
        }
    }

    /**
      * set server configuration parameters: update the conf files
      */
    public void setConfig(String serverMachine, String[] instanceHomes, Properties params) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
        
        for(int i = 0; i < servers.length; i++) {
            SJSAS8Agent sunAgent = (SJSAS8Agent) allAgents.get(servers[i]);
            try {
                if(instanceHomes != null) 
                    sunAgent.setConfig(instanceHomes, params);
                else // For all instances of this Server 
                    sunAgent.setConfig(null, params);

                logger.info("Config updated for server " + servers[i]);
            }
            catch(Exception e) {
                logger.log(Level.SEVERE, "Failed to set config for " +
                        servers[i] + '.', e);
            }
        }
    }

    /**
      * start/restart Server
      * boolean force if true the instances will be restarted even if there is 
      * no change in the config from the last start
      */
    public boolean restartServer(String serverMachine, String[] instanceHomes, boolean force) {

        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        logger.info("Restarting App server/s. Please wait ... ");
        
        for(int i = 0; i < servers.length; i++) {
            SJSAS8Agent sunAgent = (SJSAS8Agent) allAgents.get(servers[i]);
            try {
                if(instanceHomes != null) 
                    sunAgent.start(instanceHomes, force);
                else // For all instances of this Server 
                    sunAgent.start(null, force);
                logger.fine("Restarted server on " + servers[i]);
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to start SJSAS8 on " +
                        servers[i] + '.', e);
           }
        }
        return true;
    }

    /**
      * stop Server
      */
    public boolean stopServer(String serverMachine, String[] instanceHomes) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        for(int i = 0; i < servers.length; i++) {
            SJSAS8Agent sunAgent = (SJSAS8Agent) allAgents.get(servers[i]);
            try {
                if(instanceHomes != null) 
                        sunAgent.stop(instanceHomes);
                else // For all instances of this Server 
                    sunAgent.stop(null);
                logger.info("Stopped SJSAS8 on " + servers[i]);
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to stop SJSAS8 on " +
                        servers[i] + '.', e);
            }
        }
        return true;
    }


    /**
      * clear log files
      */
    public void clearLogs(String serverMachine, String[] instanceHomes) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        for(int i = 0; i < servers.length; i++) {
            SJSAS8Agent sunAgent = (SJSAS8Agent) allAgents.get(servers[i]);
            try {
                if(instanceHomes != null) 
                    sunAgent.clearLogs(instanceHomes);
                else // For all instances of this Server 
                    sunAgent.clearLogs(null);
                logger.fine("Logs cleared for " + servers[i]);
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "Failed on " + servers[i] + '.', e);
                logger.log(Level.FINE, "Exception", e);
            }
        }
    }

    /**
      * tarnsfer log files
      */
    public void xferLogs(String serverMachine, String[] instanceHomes) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        for(int i = 0; i < servers.length; i++) {
            SJSAS8Agent sunAgent = (SJSAS8Agent) allAgents.get(servers[i]);
            try {
                if(instanceHomes != null) 
                    sunAgent.xferLogs(instanceHomes);
                else // For all instances of this Server 
                    sunAgent.xferLogs(null);
                logger.fine("XferLog Completed for " + servers[i]);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to tranfer log of " +
                        servers[i] + '.', e);
            }
        }
    }
    
    /**
      *
      * To kill all SJSAS8Agents 
      * by these agents
      *
      */
    public void kill () {
        for (Iterator iter = allAgents.values().iterator(); iter.hasNext();) {
            SJSAS8Agent sunAgent = (SJSAS8Agent) iter.next();

            try {
                sunAgent.kill();
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "kill failed.", ex);
            }
        }
        myServers.clear();
        allAgents.clear();
        logger.info("Killed all SJSAS8 Agents");
    }
}
