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
 * Copyright 2008-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.services;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.services.ServiceContext;
import com.sun.faban.harness.Context;

import com.sun.faban.harness.RemoteCallable;
import com.sun.faban.harness.WildcardFileFilter;
import com.sun.faban.harness.services.ClearLogs;
import com.sun.faban.harness.Configure;
import com.sun.faban.harness.services.GetLogs;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class implements the service to start/stop lighttpd instances.
 * It also provides functionality to transfer the portion of the lighttpd
 * error.log for a run to the run output directory.
 * It can be used by any lighttpd benchmark to manage lighttpd servers and
 * perform these operations remotely using this Service.
 *
 * @author Shanti Subramanyam modified by Sheetal Patil
 */

public class LighttpdService {

    /** Injected context. */
    @Context public ServiceContext ctx;
    private Logger logger = Logger.getLogger(LighttpdService.class.getName());
    private String[] myServers;
    private static String lightyCmd,  errlogFile,  acclogFile, confFile, 
            pidFile, sessionDir;
    private CommandHandle[] ch = null;

    /**
     * Configures the LighttpdService.
     */
    @Configure public void configure() throws ConfigurationException {
        myServers = ctx.getUniqueHosts();
        if(myServers == null){
            throw new ConfigurationException("Lighttpd hostname is not provided");
        }
        lightyCmd = ctx.getProperty("cmdPath");
        if(lightyCmd != null && lightyCmd.trim().length() > 0) {
            lightyCmd = lightyCmd + " ";
        }else{
            throw new ConfigurationException("Lighttpd cmdPath is not provided");
        }
        String logsDir = ctx.getProperty("logsDir");
        if(logsDir != null && logsDir.trim().length() > 0) {
            if (!logsDir.endsWith(File.separator))
                logsDir = logsDir + File.separator;
        }else{
            throw new ConfigurationException("Lighttpd logsDir is not provided");
        }

        String confDir = ctx.getProperty("confDir");
        if(confDir != null && confDir.trim().length() > 0) {
            if (!confDir.endsWith(File.separator))
            confDir = confDir + File.separator;
        }else{
            throw new ConfigurationException("Lighttpd confDir is not provided");
        }

        String pidDir = ctx.getProperty("pidDir");
        if(pidDir != null && pidDir.trim().length() > 0) {
            if (!pidDir.endsWith(File.separator))
                pidDir = pidDir + File.separator;
        }else{
            throw new ConfigurationException("Lighttpd pidDir is not provided");
        }

        sessionDir = ctx.getProperty("sessionDir");
        if(sessionDir != null && sessionDir.trim().length() > 0) {
            if (sessionDir.endsWith(File.separator)) {
                sessionDir = sessionDir.substring(0,
                        sessionDir.length() - File.separator.length());
            }
        }else{
            logger.warning("Lighttpd sessionDir is not provided");
        }
        
        errlogFile = logsDir + "error_log";
        acclogFile = logsDir + "access_log";
        confFile = confDir + File.separator + "lighttpd.conf";
        pidFile = pidDir + File.separator + "lighttpd.pid";
        logger.fine("LighttpdService Configure completed.");
    }

    /**
     * Starts up the Lighttpd servers.
     */
    @Start public void startup() {
        logger.fine("Starting command = "  + lightyCmd);
        Command startCmd = new Command(lightyCmd, "-f", confFile);
        startCmd.setLogLevel(Command.STDOUT, Level.FINE);
        startCmd.setLogLevel(Command.STDERR, Level.FINE);
        startCmd.setSynchronous(false); // to run in bg
        ch = new CommandHandle[myServers.length];
        for (int i = 0; i < myServers.length; i++) {
            String server = myServers[i];
            try {
                // Run the command in the foreground and wait for the start
                ch[i] = RunContext.exec(server, startCmd);

                if (checkServerStarted(server)) {
                    logger.fine("Completed lightttpd startup successfully on " + server);
                } else {
                    logger.severe("Failed to find " + pidFile + " on " + server);
                }

            } catch (Exception e) {
                logger.warning("Failed to start lighttpd server with " + e.toString());
                logger.log(Level.FINE, "Exception", e);
            }
        }
        logger.fine("Completed lighttpd server(s) startup");
    }

    /*
     * Check if lighttpd server started by looking for pidfile
	 * @param String hostName
	 * @return boolean
     */
    private boolean checkServerStarted(String hostName) throws Exception {
        boolean val = false;

        // Just to make sure we don't wait for ever.
        // We try to read the msg 120 times before we give up
        // Sleep 1 sec between each try. So wait for 1 min
        int attempts = 60;
        while (attempts > 0) {
            if (RunContext.isFile(hostName, pidFile)) {
               val = true;
               break;
            } else {
                // Sleep for some time
                 try {
                     Thread.sleep(1000);
                     attempts--;
                 } catch (Exception e) {
                     break;
                 }
            }
        }
        return (val);
    }

    /*
     * Return lighttpd pid
     * It reads the pid file from the remote server host and
     * returns the pid stored in it.
     * @param String hostName
     * @return int pid
     * @throws Exception
     */
    private static int getPid(String hostName) throws Exception {
        int pid;

        pid = RunContext.exec(hostName, new RemoteCallable<Integer>() {
            public Integer call() throws Exception {
                String pidval;

                FileInputStream is = new FileInputStream(pidFile);
                BufferedReader bufR =
                        new BufferedReader(new InputStreamReader(is));
                pidval = bufR.readLine();
                bufR.close();
                return (Integer.parseInt(pidval));
            }
         });
        return (pid);
    }

    /**
     * Shuts down the Lighttpd servers.
     */
    @Stop public void shutdown() {
        int pid = -1;
        for (String hostName : myServers) {
            if (RunContext.isFile(hostName, pidFile)) {
                // we retrieve the pid value
                try {
                    pid = getPid(hostName);
                    logger.fine("Found lighttpd pidvalue of " + pid +
                            " on host " + hostName);
                } catch (Exception ee) {
                    logger.log(Level.WARNING,
                            "Failed to read lighttpd pidfile on " +
                            hostName + " with " + ee);
                    logger.log(Level.FINE, "Exception", ee);
                }
                if (pid <= 0)
                    continue;
                // Now kill the server
                Command cmd = new Command("kill", String.valueOf(pid));
                try {
                    RunContext.exec(hostName, cmd);
                    // Check if the server truly stopped
                    int attempts = 60;
                    boolean b = false;
                    while (attempts > 0) {
                        if ( ! RunContext.isFile(hostName, pidFile)) {
                            b = true;
                            break;
                        } else {
                            // Sleep for some time
                            try {
                                Thread.sleep(2000);
                                attempts--;
                            } catch (Exception e) {
                                break;
                            }
                        }
                    }
                    if ( !b) {
                        logger.severe("Cannot kill lighttpd pid " + pid +
                                " on " + hostName);
                    }
                } catch (Exception e) {
                    logger.severe("kill " + pid + " failed on " + hostName);
                    logger.log(Level.FINE, "Exception", e);
                }
            }
        }
    }

    /**
	 * Clears access log, error log, pidfile and session files.
     * It assumes that session files are in /tmp/sess*.
     */
    @ClearLogs public void clearLogs() {
        for (int i = 0; i < myServers.length; i++) {
            if (RunContext.isFile(myServers[i], errlogFile)) {
                if (!RunContext.deleteFile(myServers[i], errlogFile)) {
                    logger.log(Level.WARNING, "Delete of " + errlogFile +
                            " failed on " + myServers[i]);
                }
            }
            if (RunContext.isFile(myServers[i], acclogFile)) {
                if (!RunContext.deleteFile(myServers[i], acclogFile)) {
                    logger.log(Level.WARNING, "Delete of " + acclogFile +
                            " failed on " + myServers[i]);
                }
            }
           if (RunContext.isFile(myServers[i], pidFile)) {
                if (!RunContext.deleteFile(myServers[i], pidFile)) {
                    logger.log(Level.WARNING, "Delete of " + pidFile +
                            " failed on " + myServers[i]);
                }
            }

            logger.fine("Logs cleared for " + myServers[i]);
             // Now delete the session files
            if(sessionDir != null && sessionDir.trim().length() > 0) {
                 if (RunContext.deleteFiles(myServers[i], "/tmp",
                            new WildcardFileFilter("sess*")))
                     logger.fine("Deleted session files for " + myServers[i]);
                 else
                     logger.warning("Error deleting session files for " +
                                myServers[i]);
            }
        }
    }

    /**
     * Transfer the log files.
	 * This method copies over the error log to the run output directory
	 * and keeps only the portion of the log relevant for this run.
     */
    @GetLogs public void xferLogs() {
        for (int i = 0; i < myServers.length; i++) {
            String outFile = RunContext.getOutDir() + "httpd_err.log." +
                             RunContext.getHostName(myServers[i]);

            // copy the error_log to the master
            if (!RunContext.getFile(myServers[i], errlogFile, outFile)) {
                logger.warning("Could not copy " + errlogFile + " to " +
                        outFile);
                return;
            }
            RunContext.truncateFile(myServers[i], errlogFile);
            logger.fine("XferLog Completed for " + myServers[i]);
        }

    }

}