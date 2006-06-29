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
 * $Id: Calendar6AgentImpl.java,v 1.2 2006/06/29 19:38:40 akara Exp $
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
public class Calendar6AgentImpl extends UnicastRemoteObject implements Calendar6Agent, Unreferenced {

    private String binDir;
    private String logDir;
    private Logger logger;
    private String outDir;
    private CmdAgent cmdAgent;
    private String configDir;

    public Calendar6AgentImpl() throws RemoteException {
        logger = Logger.getLogger(this.getClass().getName());
    }

  /**
   * This method configures the instance.
   * the install dir is not being used
   */
   public void setup(Run run, String installDir, String dataDir) throws RemoteException, Exception {
      cmdAgent = CmdAgentImpl.getHandle();
      configDir = installDir + File.separator + "cal" + File.separator  + "config" + File.separator;
      binDir =  File.separator + "etc" + File.separator + "init.d" + File.separator;
      logDir = dataDir + File.separator + "logs" + File.separator;
      outDir = run.getOutDir();
      logger.info("Setup Completed");
   }

    /**
     * This method set the configure parameters
     */
    public void setConfig(Properties params) throws RemoteException, IOException {
         //TODO implement reconfig routines
        logger.info("SetConfig Completed");
    }
  
    /**
     * start the server.
     * boolean force if true the instances will be restarted even if there is 
     * no change in the config from the last start
     */
    public void start(boolean force) throws RemoteException, IOException {
        //TODO use the force flag to determine if a restart is needed

        // We first stop and clear the logs
        this.stop();
        this.clearLogs();

        String cmd = "cd " + binDir + "; ./sunwics5 start";

        try {
            boolean retVal = false;

            // Run the command in the foreground
            retVal = cmdAgent.start(cmd, Config.DEFAULT_PRIORITY);

            if (retVal) {
                retVal = false;
                // Read the log file to make sure the server has started.
                String msg = "Calendar service(s) were started";

                FileInputStream is = new FileInputStream(logDir + "start.log");
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
                    logger.info("Completed instance startup command successfully");
                }
                else {
                    logger.warning("Could not find expcted message in server log");
                }
            }
            else {
                logger.severe("Could not complete start command");
            }
        }
        catch (Exception ie) {
           logger.severe("Failed  with " + ie.toString());
           logger.log(Level.FINE, "Exception", ie);
        }
    }
  
    /**
     * stop Server. 
     */
    public void stop() throws RemoteException, IOException {
        String cmd = "cd " + binDir + "; ./sunwics5 stop";

        try {
            boolean retVal = false;

            // Run the command in the foreground
            retVal = cmdAgent.start(cmd, Config.DEFAULT_PRIORITY);

            if (retVal) {
                retVal = false;
                // Read the log file to make sure the server has stopped.
                String msg = "Calendar service(s) were stopped";

                FileInputStream is = new FileInputStream(logDir + "stop.log");
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
                    logger.info("Completed instance stop command successfully");
                }
                else {
                    logger.warning("Could not find expcted message in server log");
                }
            }
            else {
                logger.severe("Could not complete stop command");
            }
        }
        catch (Exception ie) {
           logger.severe("Failed  with " + ie.toString());
           logger.log(Level.FINE, "Exception", ie);
        }
    }

     /**
     * Gets a property from a given config file
     * @param configFile The config file name
     * @param propName The property key name
     * @return The property value
     * @throws IOException If there is an error accessing the config file
     */
    public String getConfigProperty(String configFile, String propName)
            throws IOException {
        String configFilePath = configDir + File.separator + configFile;
        Properties p = new Properties();
        FileInputStream cfgStream = new FileInputStream(configFilePath);
        p.load(cfgStream);
        cfgStream.close();
        return p.getProperty(propName);
    }


    /**
     * transfer Server logs. 
     */
    public void xferLogs() throws RemoteException {
        String logFile = logDir + "http.log";
        String outFile = outDir + "http.log.sjscalsrv6." + CmdAgentImpl.getHost();
        FileHelper.xferFile(logFile, outFile, false);
    }
    
    /**
     * clear log files. 
     */
    public void clearLogs() throws RemoteException  {
        (new File(logDir + "start.log")).delete();
        (new File(logDir + "stop.log")).delete();
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
