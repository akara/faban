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
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.services;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.Utilities;
import com.sun.faban.harness.*;
import com.sun.faban.harness.services.ClearLogs;
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
    private static Logger logger = Logger.getLogger(ApacheHttpdService.class.getName());
    private String[] myServers;
    private static String apacheCmd,  errlogFile,  acclogFile;
    CommandHandle apacheHandles[];

    /**
     * Configures the service.
     */
    @Configure public void configure() {
        myServers = ctx.getUniqueHosts();
        apacheCmd = ctx.getProperty("cmdPath");
        if (!apacheCmd.endsWith(" "))
            apacheCmd = apacheCmd + " ";

        String logsDir = ctx.getProperty("logsDir");
        if (!logsDir.endsWith(File.separator))
            logsDir = logsDir + File.separator;

        errlogFile = logsDir + "error_log";
        acclogFile = logsDir + "access_log";
        logger.info("ApacheHttpdService setup complete.");
    }

    
    /**
     * Starts up the Apache web server.
     */
    @Start public void startup() {
        String cmd = apacheCmd + "start";
        logger.info("Starting Apache Service with command = "+ cmd);
        Command startCmd = new Command(cmd);
        startCmd.setSynchronous(false); // to run in bg
 
        for (int i = 0; i < myServers.length; i++) {
            String server = myServers[i];
            try {
                // Run the command in the foreground and wait for the start
                ctx.exec(server, startCmd);
                /*
                 * Read the log file to make sure the server has started.
                 * We do this by running the code block on the server via
                 * RemoteCallable
                 */
                if (checkServerStarted(server, ctx)) {
                    logger.fine("Completed apache httpd server(s) startup successfully on " + server);
                } else {
                    logger.severe("Failed to find start message in " + errlogFile +
                            " on " + server);
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to start apache server on " + server, e);
            }
        }
    }

    /*
	 * Check if apache server started by looking in the error_log
	 */
    private static boolean checkServerStarted(String hostName, ServiceContext ctx) throws Exception {
        Integer val = 0;
        final String err = errlogFile;

        val = ctx.exec(hostName, new RemoteCallable<Integer>() {

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
                    CommandHandle ch = ctx.exec(myServers[i], stopCmd);
                    // Check if the server was even running before stop was issued
                    // If not running, apachectl will print that on stdout
                    byte[] output = ch.fetchOutput(Command.STDOUT);

                    if (output != null)
                        if ((output.toString()).indexOf("not running") != -1) {
                           continue;
                        }

                    if (checkServerStopped(myServers[i], ctx)) {
                        logger.fine("Completed apache httpd server(s) shutdown successfully on " + myServers[i]);
                        continue;
                    } else {
                        logger.severe("Failed to find start message in " + errlogFile +
                                " on " + myServers[i]);
                    }

                } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to stop Apache httpd server" +
                                myServers[i] + " with " + e.toString(), e);
                }                
        }
    }

    /*
	 * Check if apache server stopped by scanning error_log
	 */
    private static boolean checkServerStopped(String hostName, ServiceContext ctx) throws Exception {
        Integer val = 0;
        final String err = errlogFile;
        val = ctx.exec(hostName, new RemoteCallable<Integer>() {

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
            try {
                // Now delete the session files
                if (RunContext.deleteFiles(myServers[i], "/tmp",
                        new WildcardFileFilter("sess*")))
                    logger.fine("Deleted session files for " + myServers[i]);
                else
                    logger.warning("Error deleting session files for " +
                            myServers[i]);

                if (RunContext.deleteFiles(myServers[i], "/tmp",
                        new WildcardFileFilter("php*")))
                    logger.fine("Deleted php temp files for " + myServers[i]);
                else
                    logger.warning("Error deleting php temp files for " +
                            myServers[i]);

            } catch (Exception e) {
                logger.log(Level.FINE, "Delete session files failed on " +
                        myServers[i] + ".", e);
                logger.log(Level.FINE, "Exception", e);
            }
        }
    }   
}