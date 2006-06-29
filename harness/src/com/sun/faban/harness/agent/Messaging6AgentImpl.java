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
 * $Id: Messaging6AgentImpl.java,v 1.1 2006/06/29 18:51:41 akara Exp $
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
public class Messaging6AgentImpl extends UnicastRemoteObject implements Messaging6Agent, Unreferenced {

    private String installDir;
    private String binDir;
    private String logDir;
    private Logger logger;
    private String outDir;
    private CmdAgent cmdAgent;

    public Messaging6AgentImpl() throws RemoteException {
        logger = Logger.getLogger(this.getClass().getName());
    }

  /**
   * This method configures the instance.
   */
   public void setup(Run run, String installDir, String dataDir) throws RemoteException, Exception {
      cmdAgent = CmdAgentImpl.getHandle();
      this.installDir = installDir;
      binDir = installDir + File.separator + "sbin";
      logDir = dataDir + File.separator + "log" + File.separator;
      outDir = run.getOutDir();
      logger.info("Setup Completed");
   }

    /**
     * This method set the configure parameters
     */
    public void setConfig(Properties params) throws RemoteException, IOException {
         //TODO implement reconfig routines
        logger.config("SetConfig Completed");
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
        String configFilePath = binDir + File.separator + ".." + File.separator
                + "config" + File.separator + configFile;
        Properties p = new Properties();
        FileInputStream cfgStream = new FileInputStream(configFilePath);
        p.load(cfgStream);
        cfgStream.close();
        return p.getProperty(propName);
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
        // this.clearMailMsgs();

        String cmd = "cd " + binDir + "; ./start-msg";

        try {
            boolean retVal = false;

            // Run the command in the foreground
            retVal = cmdAgent.start(cmd, Config.DEFAULT_PRIORITY);

            if (retVal) {
                retVal = false;
                // Read the log file to make sure the server has started.
                String msg = "starting up";

                FileInputStream is = new FileInputStream(logDir + "default");
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
                    logger.warning("Could not find expected message in server log");
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
        // tests indicated that Msg server got to up before the mails are cleared
        this.clearMailMsgs();
    }
  
    /**
     * stop Server. 
     */
    public void stop() throws RemoteException, IOException {
        String cmd = "cd " + binDir + "; ./stop-msg";

        try {
            boolean retVal = false;

            // Run the command in the foreground
            retVal = cmdAgent.start(cmd, Config.DEFAULT_PRIORITY);

            if (retVal) {
                retVal = false;
                // Read the log file to make sure the server has started.
                String msg = "shutting down";

                FileInputStream is = new FileInputStream(logDir + "default");
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
                logger.severe("Could not complete stop command");
            }
        }
        catch (Exception ie) {
           logger.severe("Failed  with " + ie.toString());
           logger.log(Level.FINE, "Exception", ie);
        }
    }

    /**
     * transfer Server logs. 
     */
    public void xferLogs() throws RemoteException {
        String logFile = logDir + "default";
        String outFile = outDir + "dafult.sjsms6." + CmdAgentImpl.getHost();
        FileHelper.xferFile(logFile, outFile, false);
    }
    
    /**
     * clear log files. 
     */
    public void clearLogs() throws RemoteException  {
        (new File(logDir + "default")).delete();
    }

     /**
      * clear the /var/spool/mqueue dir.
      */
    public void clearMailMsgs() throws RemoteException  {
       logger.fine("Clearing the mail (MTA) queue");

         try {
             if (!cmdAgent.start("delete_mailq.sh " + installDir,
                     Config.DEFAULT_PRIORITY));
         } catch (Exception e) {
             logger.log(Level.WARNING,
                     "Exception clearing the mail queue (MTA).", e);
         }
/*
        String cmd1 = new String("/bin/rm -rf /var/spool/mqueue");
        String cmd2 = new String("mkdir /var/spool/mqueue");
        String cmd3 = new String("chmod 750 /var/spool/mqueue");
        String cmd4 = new String("chgrp bin /var/spool/mqueue");

        try {
          boolean retValue = cmdAgent.start(cmd1, Config.DEFAULT_PRIORITY);
          if (retValue != true)
              logger.info("Deleting /var/spool/mqueue exited with exit value - " + retValue);
          retValue = cmdAgent.start(cmd2, Config.DEFAULT_PRIORITY);
          if (retValue != true)
              logger.info("Creating /var/spool/mqueue exited with exit value - " + retValue);
          retValue = cmdAgent.start(cmd3, Config.DEFAULT_PRIORITY);
          if (retValue != true)
              logger.info("chmod of /var/spool/mqueue exited with exit value - " + retValue);
          retValue = cmdAgent.start(cmd4, Config.DEFAULT_PRIORITY);
          if (retValue != true)
              logger.info("chgrp of /var/spool/mqueue exited with exit value - " + retValue);

        } catch (Exception e) {
          logger.log(Level.SEVERE, "Exception in cleaning mail spool directory ", e);
        }
*/
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
