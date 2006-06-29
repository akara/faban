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
 * $Id: WebServer6AgentImpl.java,v 1.2 2006/06/29 19:38:40 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import com.sun.faban.harness.common.Run;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.FileHelper;

import java.io.*;
import java.rmi.RemoteException;
import java.rmi.server.Unreferenced;
import java.rmi.server.UnicastRemoteObject;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 *
 *
 * @author Ramesh Ramachandran
 * TODO handle multiple instances
 */
public class WebServer6AgentImpl extends UnicastRemoteObject implements WebServer6Agent, Unreferenced {

    private String serverHome;
    private String[] myInstances;
    private Logger logger;
    private String outDir;
    private CmdAgent cmdAgent;

    public WebServer6AgentImpl() throws RemoteException {
        logger = Logger.getLogger(this.getClass().getName());
    }

  /**
   * This method configures the instance.
   */
   public void setup(Run run, String server, String[] instances) throws RemoteException, Exception {
      cmdAgent = CmdAgentImpl.getHandle();
      serverHome = server;
      myInstances = instances;
      outDir = run.getOutDir();
      logger.info("Setup Completed");
   }

    /**
     * This method set the configure parameters
     */
    public void setConfig(Properties params, String[] instances) throws RemoteException, IOException {
         //TODO implement reconfig routines
        logger.config("SetConfig Completed");
    }
  
    /**
     * start the server.
     * boolean force if true the instances will be restarted even if there is 
     * no change in the config from the last start
     */
    public void start(boolean force, String[] instances) throws RemoteException, IOException {
        //TODO use the force flag to determine if a restart is needed

        // We first stop and clear the logs
        this.stop(instances);
        this.clearLogs(instances);

        String[] instanceHomes;

        if(instances != null) {
            instanceHomes = new String[instances.length];
            for(int i = 0; i < instances.length; i++)
                instanceHomes[i] = serverHome + File.separator + "https-" + instances[i] + File.separator;
        }
        else {
            instanceHomes = new String[myInstances.length];
            for(int i = 0; i < myInstances.length; i++)
                instanceHomes[i] = serverHome + File.separator + "https-" + myInstances[i] + File.separator;
        }

        for(int i = 0; i < instanceHomes.length; i++) {
            String cmd = "cd " + instanceHomes[i] + "; ./start";

            try {
                boolean retVal = false;

                // Run the command in the foreground and wait for the start
                retVal = cmdAgent.start(cmd, Config.DEFAULT_PRIORITY);

                if (retVal) {
                    retVal = false;
                    // Read the log file to make sure the server has started.
                    String msg = "successful server startup";
                    String log = instanceHomes[i] + "logs" + File.separator + "errors";

                    FileInputStream is = new FileInputStream(log);
                    BufferedReader bufR = new BufferedReader(new InputStreamReader(is));

                    // Just to make sure we don't wait for ever.
                    // We try to read the msg 120 times before we give up
                    // Sleep 1 sec between each try. So wait for about 2 min
                    int attempts = 120;
                    while(attempts > 0) {
                        // make sure we don't block
                        if(bufR.ready()) {
                            String s = bufR.readLine();
                            if((s !=  null) && (s.indexOf(msg) != -1)) {
                                retVal = true;
                                break;
                            }
                        }
                        else {
                            // Sleep for some time
                            try {
                                Thread.sleep(1000);
                                attempts --;
                            }
                            catch(Exception e){ break; }
                        }
                    }
                    bufR.close();
                    if(retVal) {
                        logger.fine("Completed instance startup command successfully");
                    }
                    else {
                        logger.warning("Could not find expcted message in server log");
                    }
                }
                else {
                    logger.warning("Could not complete start command");
                }
            }
            catch (Exception ie) {
               logger.severe("Failed  with " + ie.toString());
               logger.log(Level.FINE, "Exception", ie);
            }
        }
    }
  
    /**
     * stop Server. 
     */
    public void stop(String[] instances) throws RemoteException, IOException {

        String[] instanceHomes;

        if(instances != null) {
            instanceHomes = new String[instances.length];
            for(int i = 0; i < instances.length; i++)
                instanceHomes[i] = serverHome + File.separator + "https-" + instances[i] + File.separator;
        }
        else {
            instanceHomes = new String[myInstances.length];
            for(int i = 0; i < myInstances.length; i++)
                instanceHomes[i] = serverHome + File.separator + "https-" + myInstances[i] + File.separator;
        }

        for(int i = 0; i < instanceHomes.length; i++) {

            String cmd = "cd " + instanceHomes[i] + "; ./stop";

            try {
                boolean retVal = false;

                // Run the command in the foreground
                retVal = cmdAgent.start(cmd, Config.DEFAULT_PRIORITY);

                if (retVal) {
                    retVal = false;
                    // Read the log file to make sure the server has started.
                    String msg = "Web server shutdown in progress";
                    String log = instanceHomes[i] + "logs" + File.separator + "errors";

                    FileInputStream is = new FileInputStream(log);
                    BufferedReader bufR = new BufferedReader(new InputStreamReader(is));

                    // Just to make sure we don't wait for ever.
                    // We try to read the msg 120 times before we give up
                    // Sleep 1 sec between each try. So wait for about 2 min
                    int attempts = 120;
                    while(attempts > 0) {
                        // make sure we don't block
                        if(bufR.ready()) {
                            String s = bufR.readLine();
                            if((s !=  null) && (s.indexOf(msg) != -1)) {
                                retVal = true;
                                break;
                            }
                        }
                        else {
                            // Sleep for some time
                            try {
                                Thread.sleep(1000);
                                attempts --;
                            }
                            catch(Exception e){ break; }
                        }
                    }
                    bufR.close();

                    if(retVal) {
                        logger.fine("Completed instance stop command successfully");
                    }
                    else {
                        logger.warning("Could not find expected message in server log");
                    }
                }
                else {
                    logger.warning("Could not complete stop command");
                }
            }
            catch (Exception ie) {
               logger.severe("Failed  with " + ie.toString());
               logger.log(Level.FINE, "Exception", ie);
            }
        }
    }

    /**
     * transfer Server logs. 
     */
    public void xferLogs(String[] instances) throws RemoteException {
        String[] instanceHomes;

        if(instances != null) {
            instanceHomes = new String[instances.length];
            for(int i = 0; i < instances.length; i++)
                instanceHomes[i] = serverHome + File.separator + "https-" + instances[i] + File.separator;
        }
        else {
            instanceHomes = new String[myInstances.length];
            for(int i = 0; i < myInstances.length; i++)
                instanceHomes[i] = serverHome + File.separator + "https-" + myInstances[i] + File.separator;
            instances = myInstances;
        }

        for(int i = 0; i < instanceHomes.length; i++) {
            String logFile = instanceHomes[i] + "logs" + File.separator + "errors";
            String outFile = outDir + "errors.sjswbsvr6." + instances[i] + "." + CmdAgentImpl.getHost();
            FileHelper.xferFile(logFile, outFile, false);
        }
    }
    
    /**
     * clear log files. 
     */
    public void clearLogs(String[] instances) throws RemoteException  {
        String[] instanceHomes;

        if(instances != null) {
            instanceHomes = new String[instances.length];
            for(int i = 0; i < instances.length; i++)
                instanceHomes[i] = serverHome + File.separator + "https-" + instances[i] + File.separator;
        }
        else {
            instanceHomes = new String[myInstances.length];
            for(int i = 0; i < myInstances.length; i++)
                instanceHomes[i] = serverHome + File.separator + "https-" + myInstances[i] + File.separator;
        }

        for(int i = 0; i < instanceHomes.length; i++) {
            String logFile = instanceHomes[i] + "logs" + File.separator + "errors";
            (new File(logFile)).delete();
        }
    }
        
    /**
     * kill server agent started 
     */        
    public void kill() throws RemoteException {
        // Cleanup if needed
        logger.fine("Kill called");
    }

    public void unreferenced() {
        try {
            this.kill();
        } catch(RemoteException e) { }

    }
}
