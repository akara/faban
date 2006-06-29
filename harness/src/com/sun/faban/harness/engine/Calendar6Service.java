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
 * $Id: Calendar6Service.java,v 1.2 2006/06/29 19:38:41 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.agent.Calendar6Agent;
import com.sun.faban.harness.agent.Calendar6AgentImpl;
import com.sun.faban.harness.common.Run;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class implements the service of configure/start/stop Messaging6 instances.
 * it is used by the benchmark to start the Messaging6Agent on the server machine and
 * perform these operations remotely using this Service.
 *
 * @author Ramesh Ramachandran
 */
final public class Calendar6Service {

    private static Calendar6Service service;

    private static final String AGENT = "Calendar6Agent";

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
    private Calendar6Service() {
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
      *
      * Get the reference to the singleton object.
      *
      */
    public static Calendar6Service getHandle () {
        if(service == null)
            service = new Calendar6Service();

        return service;
    }

    /**
      * The setup method is called to set up a benchmark run. It starts Calendar6Agent
      * instances on specified machines
      *
      * @param r - the run object.
      * @param serverMachines - array specifying the machines for each
      *                 instance.
      * @param serverHomes - String arrays of  installation dir corresponding
      *                 to each serverMachine
      * @param dataHomes - data/configuration directory
      */
    public void setup(Run r, String[] serverMachines, String[] serverHomes, String[] dataHomes) {
        run = r;
        cmdService = CmdService.getHandle();
        Calendar6Agent agent = null;

        try {
            for (int i = 0; i < serverMachines.length; i++) {
                agent = (Calendar6Agent) allAgents.get(serverMachines[i]);

                if (agent == null) {

                    logger.info("Using CmdService to start Calendar6 Agent on " + serverMachines[i]);

                    cmdService.startAgent(serverMachines[i], Calendar6AgentImpl.class, AGENT);

                    // wait for the Agent to start
                    Thread.sleep(5000);

                    // connect to agent
                    String s = AGENT + "@" + serverMachines[i];
                    agent = (Calendar6Agent) cmdService.getRegistry().getService(s);

                    logger.fine("Connected to " + s);
                    myServers.add(serverMachines[i]);
                    allAgents.put(serverMachines[i], agent);
                }
                agent.setup(run, serverHomes[i], dataHomes[i]);
            }
            logger.fine("Setup complete");
        }
        catch (Exception e) {
            logger.log(Level.WARNING, "Setup failed.", e);
        }
    }

    /**
      * set server configuration parameters: update the conf files
      */
    public void setConfig(String serverMachine, Properties params) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
        
        for(int i = 0; i < servers.length; i++) {
            Calendar6Agent agent = (Calendar6Agent) allAgents.get(servers[i]);
            try {
                agent.setConfig(params);
                logger.config("Config updated for server " + servers[i]);
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Failed to set config for " +
                        servers[i] + '.', e);
            }
        }
    }

    /**
      * start/restart Server
      * boolean force if true the instances will be restarted even if there is 
      * no change in the config from the last start
      */
    public boolean restartServer(String serverMachine, boolean force) {

        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        logger.info("Restarting calendar server/s. Please wait ... ");
        
        for(int i = 0; i < servers.length; i++) {
            Calendar6Agent agent = (Calendar6Agent) allAgents.get(servers[i]);
            try {
                agent.start(force);
                logger.fine("Restarted server on " + servers[i]);
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Failed to start Calendar6 on " +
                        servers[i] + '.', e);
           }
        }
        return true;
    }

    /**
      * stop Server
      */
    public boolean stopServer(String serverMachine) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        for(int i = 0; i < servers.length; i++) {
            Calendar6Agent agent = (Calendar6Agent) allAgents.get(servers[i]);
            try {
                agent.stop();
                logger.info("Stopped Calendar6 on " + servers[i]);
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Failed to stop Calendar6 on " +
                        servers[i] + ".", e);
            }
        }
        return true;
    }


    /**
      * clear log files
      */
    public void clearLogs(String serverMachine) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        for(int i = 0; i < servers.length; i++) {
            Calendar6Agent agent = (Calendar6Agent) allAgents.get(servers[i]);
            try {
                agent.clearLogs();
                logger.fine("Logs cleared for " + servers[i]);
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "clearLogs failed on " + servers[i] +
                        '.', e);
            }
        }
    }

    /**
      * tarnsfer log files
      */
    public void xferLogs(String serverMachine) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        for(int i = 0; i < servers.length; i++) {
            Calendar6Agent agent = (Calendar6Agent) allAgents.get(servers[i]);
            try {
                agent.xferLogs();
                logger.fine("XferLog Completed for " + servers[i]);
            }
            catch (Exception e)
            {
                logger.log(Level.WARNING, "Failed to tranfer log of " +
                        servers[i] + '.', e);
            }
        }
    }


    public String[] getConfigProperty(String serverMachine, String configFile,
                                       String propName) {
           String [] servers = null;

           if(serverMachine != null) {
               servers = new String[1];
               servers[0] = serverMachine;
           }
           else { // ALL Servers in the setup.
               servers = (String[]) myServers.toArray(new String[1]);
           }
           String[] results = new String[servers.length];

           for(int i = 0; i < servers.length; i++) {
               Calendar6Agent agent = (Calendar6Agent) allAgents.get(servers[i]);
               try {
                   results[i] = agent.getConfigProperty(configFile, propName);
               }
               catch(Exception e) {
                   logger.log(Level.SEVERE, "Failed to get config property for " +
                           servers[i] + '.', e);
               }
           }
           return results;
       }


    /**
      *
      * To kill all Calendar6Agents
      * by these agents
      *
      */
    public void kill () {
        for (Iterator iter = allAgents.values().iterator(); iter.hasNext();) {
            Calendar6Agent agent = (Calendar6Agent) iter.next();

            try {
                agent.kill();
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Kill Failed.", e);
            }
        }
        myServers.clear();
        allAgents.clear();
        logger.info("Killed all Calendar6 Agents");
    }
}
