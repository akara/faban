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
 * $Id: ApacheHttpd.java,v 1.1 2009/06/01 17:02:47 sheetalpatil Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.services;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.services.ServiceContext;
import com.sun.faban.harness.Context;

import com.sun.faban.harness.RemoteCallable;
import com.sun.faban.harness.WildcardFileFilter;
import com.sun.faban.harness.services.ClearLogs;
import com.sun.faban.harness.services.Configure;
import com.sun.faban.harness.services.GetLogs;
import com.sun.faban.harness.services.Startup;
import com.sun.faban.harness.services.Shutdown;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ApacheHttpd {

    @Context public ServiceContext ctx;    
    private Logger logger = Logger.getLogger(ApacheHttpd.class.getName());
    private String[] myServers = new String[1];
    private static String apachectlCmd,  errlogFile,  acclogFile;
    
    @Configure public void configure() {
        myServers = ctx.getHosts();
        apachectlCmd = ctx.getProperty("binPath") + File.separator + "apachectl ";
        errlogFile = ctx.getProperty("logPath") + File.separator + "error_log";
        acclogFile = ctx.getProperty("logPath") + File.separator + "access_log";
        logger.info("ApacheHttpdService setup complete.");
    }

    
    @Startup public void startup() {
        String cmd = apachectlCmd + "start";
        logger.info("Starting command ="+cmd);
        Command startCmd = new Command(cmd);
        startCmd.setLogLevel(Command.STDOUT, Level.FINE);
        startCmd.setLogLevel(Command.STDERR, Level.FINE);
        startCmd.setSynchronous(false); // to run in bg
 
        for (int i = 0; i < myServers.length; i++) {
            String server = myServers[i];
            try {
                // Run the command in the foreground and wait for the start
                CommandHandle ch = RunContext.exec(server, startCmd);
                /*
                 * Read the log file to make sure the server has started.
                 * We do this by running the code block on the server via
                 * RemoteCallable
                 */
                if (checkServerStarted()) {
                    logger.fine("Completed apache httpd startup successfully on " + server);
                } else {
                    logger.severe("Failed to find start message in " + errlogFile +
                            " on " + server);
                }

            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to start apache server.", e);
            }
        }
        logger.info("Completed apache httpd server(s) startup");
    }

    private boolean checkServerStarted() throws Exception {
        Integer val = 0;
        final String err = errlogFile;
        String msg = "resuming normal operations";
        FileInputStream is = new FileInputStream(err);
        BufferedReader bufR = new BufferedReader(new InputStreamReader(is));

        // Just to make sure we don't wait for ever.
        // We try to read the msg 120 times before we give up
        // Sleep 1 sec between each try. So wait for 1 min
        int attempts = 60;
        while (attempts > 0) {
            // make sure we don't block
            if (bufR.ready()) {
                String s = bufR.readLine();
                if ((s != null) && (s.indexOf(msg) != -1)) {
                    val = 1;
                    break;
                }
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
        bufR.close();
            
        if (val == 1) {
            return (true);
        } else {
            return (false);
        }
    }

   
    @Shutdown public void shutdown() throws IOException, InterruptedException {
        boolean success = true;
        for (int i = 0; i < myServers.length; i++) {
            Integer retVal = 0;
            try {
                String cmd = apachectlCmd + "stop";
                Command stopCmd = new Command(cmd);
                stopCmd.setLogLevel(Command.STDOUT, Level.FINE);
                stopCmd.setLogLevel(Command.STDERR, Level.FINE);

                // Run the command in the foreground
                CommandHandle ch = RunContext.exec(myServers[i], stopCmd);

                // Check if the server was even running before stop was issued
                // If not running, apachectl will print that on stdout
                byte[] output = ch.fetchOutput(Command.STDOUT);

                if (output != null)
                    if ((output.toString()).indexOf("not running") != -1) {
                       continue;
                    }
                retVal = checkServerStopped(myServers[i]);
                if (retVal == 0) {
                    logger.warning("Could not find expected message  'shutting down' in " +
                            errlogFile + " on " + myServers[i]);
                    success = false;
                    continue;
                }
            } catch (Exception ie) {
                logger.log(Level.WARNING, "Failed to stop ApacheHttpd on " +
                        myServers[i] + "with " + ie);
                logger.log(Level.FINE, "apachectl stop Exception", ie);
                success = false;
            }
        }
    }

    private static Integer checkServerStopped(String hostName) throws Exception {
        Integer val = 0;
        final String err = errlogFile;
        val = RunContext.exec(hostName, new RemoteCallable<Integer>() {

            public Integer call() throws Exception {
                Integer retVal = 0;
                // Read the log file to make sure the server has shutdown
                String msg = "shutting down";
                FileInputStream is = new FileInputStream(err);
                BufferedReader bufR = new BufferedReader(new InputStreamReader(is));

                // Just to make sure we don't wait for ever.
                // We try to read the msg 60 times before we give up
                // Sleep 1 sec between each try. So wait for about 1 min
                int attempts = 60;
                while (attempts > 0) {
                    // make sure we don't block
                    if (bufR.ready()) {
                        String s = bufR.readLine();
                        if ((s != null) && (s.indexOf(msg) != -1)) {
                            retVal = 1;
                            break;
                        }

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
                bufR.close();
                return (retVal);
            }
        });
        return (val);
    }

    @GetLogs public void getLogs() {
        logger.info("GetLogs Completed");
    }

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