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
 * Copyright 2008-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.services;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.Utilities;
import com.sun.faban.harness.*;
import com.sun.faban.harness.services.ClearLogs;
import com.sun.faban.harness.services.GetLogs;
import com.sun.faban.harness.services.ServiceContext;
import com.sun.faban.harness.util.FileHelper;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the service to start/stop ApacheHttpd instances.
 * It also provides functionality to transfer the portion of the apache
 * error_log for a run to the run output directory.
 * It can be used by any Apache benchmark to manage apache servers and
 * perform these operations remotely using this Service.
 *
 * @author Sheetal Patil based on work done by Shanti Subramanyam
 * 
 */
public class ApacheHttpdService {

    /** Injected service context. */
    @Context public ServiceContext ctx;
    private static Logger logger =
            Logger.getLogger(ApacheHttpdService.class.getName());
    private String[] myServers;
    private static String apacheCmd,  errlogFile,  acclogFile, sessionDir,
            iniFile, confFile;
    CommandHandle apacheHandles[];

    /**
     * Configures the service.
     */
    @Configure public void configure() throws ConfigurationException {
        myServers = ctx.getUniqueHosts();
        if(myServers == null){
            throw new ConfigurationException("Apache hostname is not provided");
        }
        apacheCmd = ctx.getProperty("cmdPath");
        if(apacheCmd != null && apacheCmd.trim().length() > 0) {
            apacheCmd = apacheCmd + " ";
        }else{
            throw new ConfigurationException("Apache cmdPath is not provided");
        }
        
        String logsDir = ctx.getProperty("logsDir");
        if(logsDir != null && logsDir.trim().length() > 0) {
            if (!logsDir.endsWith(File.separator))
                logsDir = logsDir + File.separator;
        }else{
            throw new ConfigurationException("Apache logsDir is not provided");
        }

        sessionDir = ctx.getProperty("sessionDir");
        if(sessionDir != null && sessionDir.trim().length() > 0) {
            if (sessionDir.endsWith(File.separator)) {
                sessionDir = sessionDir.substring(0,
                        sessionDir.length() - File.separator.length());
            }
        }else{
            logger.warning("Apache sessionDir is not provided");
        }

        iniFile = ctx.getProperty("phpIniPath");
        if(iniFile == null || iniFile.trim().length() <= 0){
            logger.warning("iniPath is not provided");
        }

        confFile = ctx.getProperty("confPath");
        if(confFile == null || confFile.trim().length() <= 0){
            logger.warning("confPath is not provided");
        }

        errlogFile = logsDir + "error_log";
        acclogFile = logsDir + "access_log";
        logger.fine("ApacheHttpdService setup complete.");
    }

    
    /**
     * Starts up the Apache web server.
     */
    @Start public void startup() {
        String cmd = apacheCmd + "start";
        logger.fine("Starting Apache Service with command = "+ cmd);
        Command startCmd = new Command(cmd);
        startCmd.setSynchronous(false); // to run in bg
 
        for (int i = 0; i < myServers.length; i++) {
            String server = myServers[i];
            try {
                // Run the command in the foreground and wait for the start
                RunContext.exec(server, startCmd);
                /*
                 * Read the log file to make sure the server has started.
                 * We do this by running the code block on the server via
                 * RemoteCallable
                 */
                if (checkServerStarted(server)) {
                    logger.fine("Completed apache httpd server(s) startup " +
                            "successfully on " + server);
                } else {
                    logger.severe("Failed to find start message in " +
                            errlogFile + " on " + server);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to start apache server on " +
                        server, e);
            }
        }
    }

    /*
	 * Check if apache server started by looking in the error_log
	 */
    private static boolean checkServerStarted(String hostName)
            throws Exception {
        Integer val = 0;
        final String err = errlogFile;

        val = RunContext.exec(hostName, new RemoteCallable<Integer>() {

            static final int RETRIES = 30;

            public Integer call() throws Exception {
                Integer retVal = 0;
                String msg = "resuming normal operations";

                // Ensure filenames are not impacted by path differences.
                File errFile = new File(Utilities.convertPath(err));
                for (int retry = 0;retry < RETRIES; retry++) {
                    if (errFile.exists()) {
                        if (FileHelper.hasString(errFile, msg)) {
                            retVal = 1;
                            break;
                        } 
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                return retVal;
            }
        });
        if (val == 1) {
            return (true);
        } else {
            return (false);
        }
    }

    /**
     * Shuts down the Apache web server.
     * @throws IOException Error executing the shutdown
     * @throws InterruptedException Interrupted waiting for the shutdown
     */
    @Stop public void shutdown() throws IOException, InterruptedException {
        for (int i = 0; i < myServers.length; i++) {
                //Try to Stop it.
                try {
                    String cmd = apacheCmd + "stop";
                    Command stopCmd = new Command(cmd);
                    stopCmd.setLogLevel(Command.STDOUT, Level.FINE);
                    stopCmd.setLogLevel(Command.STDERR, Level.FINE);

                    // Run the command in the foreground
                    CommandHandle ch = RunContext.exec(myServers[i], stopCmd);
                    // Check if the server was running before stop was issued
                    // If not running, apachectl will print that on stdout
                    byte[] output = ch.fetchOutput(Command.STDOUT);

                    if (output != null)
                        if ((output.toString()).indexOf("not running") != -1) {
                           continue;
                        }

                    if (checkServerStopped(myServers[i])) {
                        logger.fine("Completed apache httpd server(s) " +
                                "shutdown successfully on " + myServers[i]);
                        continue;
                    } 

                } catch (Exception e) {
                        logger.log(Level.WARNING,
                                "Failed to stop Apache httpd server" +
                                myServers[i] + " with " + e.toString(), e);
                }                
        }
    }

    /*
	 * Check if apache server stopped by scanning error_log
	 */
    private static boolean checkServerStopped(String hostName)
            throws Exception {
        Integer val = 0;
        final String err = errlogFile;
        val = RunContext.exec(hostName, new RemoteCallable<Integer>() {

            static final int RETRIES = 30;

            public Integer call() throws Exception {
                Integer retVal = 0;
                // Read the log file to make sure the server has shutdown
                String msg = "shutting down";

                // Ensure filenames are not impacted by path differences.
                File errFile = new File(Utilities.convertPath(err));
                for (int retry = 0;retry < RETRIES; retry++) {
                    if (errFile.exists()) {
                        if (FileHelper.hasString(errFile, msg)) {
                            retVal = 1;
                            break;
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                    }
                }
                return retVal;
            }
        });
        if (val == 1) {
            return (true);
        } else {
            return (false);
        }
    }

    /**
     * Clears the Apache web server logs.
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

            logger.fine("Logs cleared for " + myServers[i]);
            if(sessionDir != null && sessionDir.trim().length() > 0) {
                try {
                    // Now delete the session files
                    if (RunContext.deleteFiles(myServers[i], sessionDir,
                            new WildcardFileFilter("sess*")))
                        logger.fine("Deleted session files for " + myServers[i]);
                    else
                        logger.warning("Error deleting session files for " +
                                myServers[i]);

                } catch (Exception e) {
                    logger.log(Level.FINE, "Delete session files failed on " +
                            myServers[i] + ".", e);
                    logger.log(Level.FINE, "Exception", e);
                }
            }
        }
    }

    /**
     * Transfer log files.
     * This method copies over the error_log to the run output directory
     * and keeps only the portion of the log relevant for this run.
     */
    @GetLogs public void getLogs() {
        String outDir = RunContext.getOutDir();
        for (int i = 0; i < myServers.length; i++) {
            String outFile = outDir + "httpd_err.log." +
                             RunContext.getHostName(myServers[i]);

            // copy the error_log to the master
            if (!RunContext.getFile(myServers[i], errlogFile, outFile)) {
                logger.warning("Could not copy " + errlogFile + " to " +
                        outFile);
                return;
            }
            // copy the php.ini file if it has been specified
            if(iniFile != null && iniFile.trim().length() > 0) {
                String outIniFile = outDir + "php_ini.log." +
                                 RunContext.getHostName(myServers[i]);
                if (!RunContext.getFile(myServers[i], iniFile, outIniFile)) {
                    logger.warning("Could not copy " + iniFile + " to " + outIniFile);
                }
            }
            // copy the httpd.conf file if it has been specified
            if(confFile != null && confFile.trim().length() > 0) {
                outFile = outDir + "httpd_conf.log." +
                                 RunContext.getHostName(myServers[i]);
                if (!RunContext.getFile(myServers[i], confFile, outFile)) {
                    logger.warning("Could not copy " + confFile + " to " + outFile);
                }
            }
            RunContext.truncateFile(myServers[i], errlogFile);
            logger.fine("XferLog Completed for " + myServers[i]);
        }
    }
}