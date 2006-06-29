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
 * $Id: WebServer6Service.java,v 1.1 2006/06/29 18:51:42 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.agent.WebServer6Agent;
import com.sun.faban.harness.agent.WebServer6AgentImpl;
import com.sun.faban.harness.common.Run;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class implements the service of configure/start/stop WebServer6 instances.
 * it is used by the benchmark to start the WebServer6Agent on the server machine and
 * perform these operations remotely using this Service.
 *
 * @author Ramesh Ramachandran
 */
final public class WebServer6Service {

    private static WebServer6Service service;

    private static final String AGENT = "WebServer6Agent";

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
    private WebServer6Service() {
        logger = Logger.getLogger(this.getClass().getName());
        logger.fine(this.getClass().getName() + " Created");
    }

    /**
      *
      * Get the reference to the singleton object.
      *
      */
    public static WebServer6Service getHandle () {
        if(service == null)
            service = new WebServer6Service();

        return service;
    }

    /**
      * The setup method is called to set up a benchmark run. It starts WebServer6Agent
      * instances on specified machines
      *
      * @param r - the run object.
      * @param serverMachines - array specifying the machines for each
      *                 instance.
      * @param serverHomes - String arrays of  installation dir corresponding
      *                 to each serverMachine
      */
    public void setup(Run r, String[] serverMachines, String[] serverHomes, List instanceArray) {
        run = r;
        cmdService = CmdService.getHandle();
        WebServer6Agent agent = null;

        try {
            for (int i = 0; i < serverMachines.length; i++) {
                agent = (WebServer6Agent) allAgents.get(serverMachines[i]);

                if (agent == null) {

                    logger.fine("Using CmdService to start WebServer6 Agent on " + serverMachines[i]);

                    cmdService.startAgent(serverMachines[i], WebServer6AgentImpl.class, AGENT);

                    // wait for the Agent to start
                    Thread.sleep(5000);

                    // connect to agent
                    String s = AGENT + "@" + serverMachines[i];
                    agent = (WebServer6Agent) cmdService.getRegistry().getService(s);

                    logger.fine("Connected to " + s);
                    myServers.add(serverMachines[i]);
                    allAgents.put(serverMachines[i], agent);
                }
                agent.setup(run, serverHomes[i], (String[])instanceArray.get(i));
            }
            logger.info("WebServer6Agent setup complete.");
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "WebServer6Agent Setup failed.", e);
        }
    }

    /**
      * set server configuration parameters: update the conf files
      */
    public void setConfig(String serverMachine, String[] instances, Properties params) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
        
        for(int i = 0; i < servers.length; i++) {
            WebServer6Agent agent = (WebServer6Agent) allAgents.get(servers[i]);
            try {
                agent.setConfig(params, instances);
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
    public boolean restartServer(String serverMachine, String[] instances, boolean force) {

        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        logger.info("Restarting App server/s. Please wait ... ");
        
        for(int i = 0; i < servers.length; i++) {
            WebServer6Agent agent = (WebServer6Agent) allAgents.get(servers[i]);
            try {
                agent.start(force, instances);
                logger.fine("Restarted server on " + servers[i]);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to start WebServer6 on " +
                        servers[i] + '.', e);
           }
        }
        return true;
    }

    /**
      * stop Server
      */
    public boolean stopServer(String serverMachine, String[] instances) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        for(int i = 0; i < servers.length; i++) {
            WebServer6Agent agent = (WebServer6Agent) allAgents.get(servers[i]);
            try {
                agent.stop(instances);
                logger.info("Stopped WebServer6 on " + servers[i]);
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to stop WebServer6 on " +
                        servers[i] + '.', e);
            }
        }
        return true;
    }


    /**
      * clear log files
      */
    public void clearLogs(String serverMachine, String[] instances) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        for(int i = 0; i < servers.length; i++) {
            WebServer6Agent agent = (WebServer6Agent) allAgents.get(servers[i]);
            try {
                agent.clearLogs(instances);
                logger.fine("Logs cleared for " + servers[i]);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed on " + servers[i] + ".", e);
            }
        }
    }

    /**
      * tarnsfer log files
      */
    public void xferLogs(String serverMachine, String[] instances) {
        String [] servers = new String[1];
        
        if(serverMachine != null) {
            servers[0] = serverMachine;
        }
        else { // ALL Servers in the setup.
            servers = (String[]) myServers.toArray(new String[1]);
        }        
                
        for(int i = 0; i < servers.length; i++) {
            WebServer6Agent agent = (WebServer6Agent) allAgents.get(servers[i]);
            try {
                agent.xferLogs(instances);
                logger.fine("XferLog Completed for " + servers[i]);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Failed to tranfer log of " +
                        servers[i] + '.', e);
                logger.log(Level.FINE, "Exception", e);
            }
        }
    }
    
    /**
      *
      * To kill all WebServer6Agents
      * by these agents
      *
      */
    public void kill () {
        for (Iterator iter = allAgents.values().iterator(); iter.hasNext();) {
            WebServer6Agent agent = (WebServer6Agent) iter.next();

            try {
                agent.kill();
            }
            catch (Exception e) {
                logger.log(Level.SEVERE, "kill Failed.", e);
            }
        }
        myServers.clear();
        allAgents.clear();
        logger.info("Killed all WebServer6 Agents");
    }
}
