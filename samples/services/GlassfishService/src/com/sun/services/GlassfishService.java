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
 * Copyright 2007-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.services;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.*;
import com.sun.faban.harness.services.ClearLogs;
import com.sun.faban.harness.services.GetLogs;
import com.sun.faban.harness.services.ServiceContext;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.faban.harness.RunContext.*;

/**
 *
 * This class implements the service to start/stop GlassFish instances.
 * It also provides functionality to transfer the portion of the glassfish
 * error_log for a run to the run output directory.
 * It can be used by any benchmark to GlassFish servers and
 * perform these operations remotely using this Service.
 *
 * @author Akara Sucharitakul modified by Sheetal Patil
 */
public class GlassfishService {

    /** The injected context. */
    @Context public ServiceContext ctx;
    Logger logger = Logger.getLogger(GlassfishService.class.getName());
    private String[] myServers;
    private static String asadminCmd,  errlogFile,  acclogFile, confFile;


    /**
     * The setup method is called to set up a benchmark run.
     * It is assumed that all servers have the same installation directory
     *
     */
     @Configure public void configure() throws ConfigurationException {
        myServers = ctx.getUniqueHosts();
        if (myServers == null) {
            throw new ConfigurationException("Glassfish hostname is not provided");
        }
        confFile = ctx.getProperty("confPath");
        if (confFile == null || confFile.trim().length() == 0)
            logger.warning("Glassfish confPath is not set.");
        
        String logsDir = ctx.getProperty("logsDir");
        if (logsDir == null || logsDir.trim().length() == 0)
            logger.warning("Glassfish logsDir not set.");
        if (!logsDir.endsWith(File.separator))
            logsDir = logsDir + File.separator;

        asadminCmd = ctx.getProperty("cmdPath");
        if(asadminCmd != null && asadminCmd.trim().length() > 0) {
            asadminCmd = asadminCmd.trim();
        } else
            throw new ConfigurationException("Glassfish cmdPath is not set.");

        errlogFile = logsDir + "server.log";
        acclogFile = logsDir + "access";
        logger.fine("GlassfishService Configure completed.");
    }

    /**
     * Start all glassfish servers on configured hosts.
     */
    @Start public void startup() {
        Command startCmd = new Command(asadminCmd, "start-domain");       

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
                    logger.fine("Completed GlassFish startup successfully on " +
                            server);
                } else {
                    logger.severe("Failed to start GlassFish on " + server);
                }

            } catch (Exception e) {
                logger.warning("Failed to start GlassFish server with " +
                                                                e.toString());
                logger.log(Level.FINE, "Exception", e);
            }
        }
        logger.fine("Completed GlassFish server(s) startup");
    }

    /*
	 * Check if Glassfish server is started.
	 */
    private static boolean checkServerStarted(String hostName)
            throws Exception {
        Command checkCmd = new Command(asadminCmd, "list-domains");     
        CommandHandle handle = RunContext.exec(hostName, checkCmd);
        byte[] output = handle.fetchOutput(Command.STDOUT);
        if (output != null) {
            String outStr = new String(output);
        if (outStr.indexOf("domain1 running") != -1)
            return true;
        else
            return false;
        }
        return false;
    }

    /**
     * Shuts down the Glassfish servers.
     */
    @Stop public void shutdown() {
        for (int i = 0; i < myServers.length; i++) {
            Integer retVal = 0;
            try {
                Command stopCmd = new Command(asadminCmd, "stop-domain");
               
                // Run the command in the foreground
                CommandHandle ch = RunContext.exec(myServers[i], stopCmd);

                // Check if the server was even running before stop was issued
                // If not running, asadmin will print that on stdout
                byte[] output = ch.fetchOutput(Command.STDOUT);

                if (output != null) {
                    String outStr = new String(output);
                    if (outStr.indexOf("stopped.") != -1 ||
                            outStr.indexOf("isn't running.") != -1) {
                       continue;
                    }
                }
                retVal = checkServerStopped(myServers[i]);
                if (retVal == 0) {
                    logger.warning("GlassFish on " + myServers[i] +
                                        " is apparently still runnning");
                    continue;
                }
            } catch (Exception ie) {
                logger.log(Level.WARNING, "Failed to stop GlassFish on " +
                        myServers[i] + "with " + ie, ie);
            }
        }
    }

    /*
	 * Check if glassfish server is stopped.
	 */
    private static Integer checkServerStopped(String hostName)
            throws Exception {
        Command checkCmd = new Command(asadminCmd, "list-domains");
        CommandHandle handle = RunContext.exec(hostName, checkCmd);
        byte[] output = handle.fetchOutput(Command.STDOUT);
        if (output != null) {
            String outStr = new String(output);
            if (outStr.indexOf("domain1 not running") != -1)
                return 1;
            else
                return 0;
        }
        return 0;
    }

    /**
     * Clear glassfish logs and session files.
	 * It assumes that session files are in /tmp/sess*
     */
    @ClearLogs public void clearLogs() {

        for (int i = 0; i < myServers.length; i++) {
            boolean err = false;
            if (isFile(myServers[i], errlogFile)) {
                if (!deleteFile(myServers[i], errlogFile)) {
                    logger.log(Level.WARNING, "Delete of " + errlogFile +
                            " failed on " + myServers[i]);
                    err = true;
                }
            }
            if (isFile(myServers[i], acclogFile)) {
                if (!deleteFile(myServers[i], acclogFile)) {
                    logger.log(Level.WARNING, "Delete of " + acclogFile +
                            " failed on " + myServers[i]);
                    err = true;
                }
            }
            if (!err)
                logger.fine("Logs cleared for " + myServers[i]);
        }
    }


    /**
     * Transfer log files.
     */
    @GetLogs public void xferLogs() {
        for (int i = 0; i < myServers.length; i++) {
            String outFile = getOutDir() + "glassfish_err.log." +
                    getHostName(myServers[i]);

            // copy the error_log to the master
            if (!getFile(myServers[i], errlogFile, outFile)) {
                logger.warning("Could not copy " + errlogFile + " to " +
                        outFile);
                return;
            }
            RunContext.truncateFile(myServers[i], errlogFile);

			// Copy the config file to master
			outFile = getOutDir() + "domain.xml.log." + 
                    getHostName(myServers[i]);
            if (!getFile(myServers[i], confFile, outFile)) {
                logger.warning("Could not copy " + confFile + " to " +
                        outFile);
                return;
            }
            logger.fine("XferLog Completed for " + myServers[i]);
        }
    }

}
