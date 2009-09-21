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
 * Copyright 2007-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import static com.sun.faban.harness.RunContext.*;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.RemoteCallable;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.GregorianCalendar;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/**
 *
 * This class implements the service to start/stop GlassFish instances.
 * It also provides functionality to transfer the portion of the glassfish
 * error_log for a run to the run output directory.
 * It can be used by any benchmark to GlassFish servers and
 * perform these operations remotely using this Service.
 *
 * @author Akara Sucharitakul
 * @deprecated
 */
@Deprecated final public class GlassfishService implements WebServerService {

    private static GlassfishService service = null;
    private String[] myServers = new String[1];
    private Logger logger;
    private static String asadminCmd,  errlogFile,  acclogFile;

    /**
     *
     * Private Constructor for a singleton object.
     *
     */
    private GlassfishService() {
        logger = Logger.getLogger(this.getClass().getName());
        logger.fine(this.getClass().getName() + " Created");
    }

    /**
     *
     * Get the reference to the singleton object.
     * Use this method to get access to the service.
	 * @return GlassfishService - service object handle
     */
    public static GlassfishService getHandle() {
        if (service == null) {
            service = new GlassfishService();
        }

        return service;
    }

    /**
     * The setup method is called to set up a benchmark run. 
     * It is assumed that all servers have the same installation directory
     *
     * @param serverMachines - array specifying the glassfish server machines.
     * @param binDir - glassfish binaries location
     * @param logsDir - glassfish logs location
     * @param confDir - glassfish httpd.conf file location
     * @param pidDir - glassfish httpd.pid file location
     */
    public void setup(String[] serverMachines, String binDir, String logsDir, 
            String confDir, String pidDir) {
        myServers = serverMachines;

        asadminCmd = binDir + File.separator + "asadmin";
        errlogFile = logsDir + File.separator + "server.log";
        acclogFile = logsDir + File.separator + "access";
        logger.info("GlassfishService setup complete.");
    }

    /**
     * Start all glassfish servers on configured hosts.
     * @return boolean true if start succeeded on all machines, else false
     */
    public boolean startServers() {

        Command startCmd = new Command(asadminCmd, "start-domain");
        // startCmd.setLogLevel(Command.STDOUT, Level.FINE);
        // startCmd.setLogLevel(Command.STDERR, Level.FINE);

        for (int i = 0; i < myServers.length; i++) {
            String server = myServers[i];
            try {
                // Run the command in the foreground and wait for the start
                exec(server, startCmd);
                /*
                 * Read the log file to make sure the server has started.
                 * We do this by running the code block on the server via
                 * RemoteCallable
                 */
                if (checkServerStarted(server)) {
                    logger.fine("Completed GlassFish startup successfully on " + server);
                } else {
                    logger.severe("Failed to start GlassFish on " + server);
                    return (false);
                }

            } catch (Exception e) {
                logger.warning("Failed to start GlassFish server with " +
                                                                e.toString());
                logger.log(Level.FINE, "Exception", e);
                return false;
            }
        }
        logger.info("Completed GlassFish server(s) startup");
        return true;
    }

    /*
	 * Check if Glassfish server is started.
	 */
    private static boolean checkServerStarted(String hostName) throws Exception {
        Command checkCmd = new Command(asadminCmd, "list-domains");
        // checkCmd.setLogLevel(Command.STDOUT, Level.FINE);
        // checkCmd.setLogLevel(Command.STDERR, Level.FINE);
        CommandHandle handle = exec(hostName, checkCmd);
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
     * Restart all servers. It first stops servers, clear logs
     * and then attempts to start them up again. If startup fails on
	 * any server, it will stop all servers and cleanup.
	 * @return true if all servers restarted successfully, otherwise false
     */
    public boolean restartServers() {

        logger.info("Restarting GlassFish server(s). Please wait ... ");
        // We first stop and clear the logs
        this.stopServers();
        this.clearLogs();

        // Now start the glassfish servers
        if (!startServers()) {
            // cleanup and return
            stopServers();
            clearLogs();
            return false;
        }
        return true;
    }

    /**
     * Stop servers.
     * @return true if stop succeeded on all machines, else false
     */
    public boolean stopServers() {
        boolean success = true;
        for (int i = 0; i < myServers.length; i++) {
            Integer retVal = 0;
            try {
                Command stopCmd = new Command(asadminCmd, "stop-domain");
                // stopCmd.setLogLevel(Command.STDOUT, Level.FINE);
                // stopCmd.setLogLevel(Command.STDERR, Level.FINE);

                // Run the command in the foreground
                CommandHandle ch = exec(myServers[i], stopCmd);
                
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
                    success = false;
                    continue;
                }
            } catch (Exception ie) {
                logger.log(Level.WARNING, "Failed to stop GlassFish on " +
                        myServers[i] + "with " + ie, ie);
                success = false;
            }
        }         
        return success;
    }

    /*
	 * Check if glassfish server is stopped.
	 */
    private static Integer checkServerStopped(String hostName) throws Exception {
        Command checkCmd = new Command(asadminCmd, "list-domains");
        // checkCmd.setLogLevel(Command.STDOUT, Level.FINE);
        // checkCmd.setLogLevel(Command.STDERR, Level.FINE);
        CommandHandle handle = exec(hostName, checkCmd);
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
     * Clears glassfish logs and session files.
	 * It assumes that session files are in /tmp/sess*.
     * @return true if operation succeeded, else fail
     */
    public boolean clearLogs() {

        for (int i = 0; i < myServers.length; i++) {
            if (isFile(myServers[i], errlogFile)) {
                if (!deleteFile(myServers[i], errlogFile)) {
                    logger.log(Level.WARNING, "Delete of " + errlogFile +
                            " failed on " + myServers[i]);
                    return (false);
                }
            }
            if (isFile(myServers[i], acclogFile)) {
                if (!deleteFile(myServers[i], acclogFile)) {
                    logger.log(Level.WARNING, "Delete of " + acclogFile +
                            " failed on " + myServers[i]);
                    return (false);
                }
            }

            logger.fine("Logs cleared for " + myServers[i]);
        }
        return (true);
    }

    /**
     * Transfer log files.
	 * This method copies over the error_log to the run output directory
	 * and keeps only the portion of the log relevant for this run.
	 * @param totalRunTime - the time in seconds for this run
     */
    public void xferLogs(int totalRunTime) {

        for (int i = 0; i < myServers.length; i++) {
            String outFile = getOutDir() + "server_log." +
                    getHostName(myServers[i]);

            // copy the error_log to the master
            if (!getFile(myServers[i], errlogFile, outFile)) {
                logger.warning("Could not copy " + errlogFile + " to " + outFile);
                return;
            }


            try {
                // Now get the start and end times of the run
                GregorianCalendar calendar = getGregorianCalendar(myServers[i]);

                //format the end date
                SimpleDateFormat df = new SimpleDateFormat("MMM,dd,HH:mm:ss");
                String endDate = df.format(calendar.getTime());

                calendar.add(Calendar.SECOND, (totalRunTime * -1));

                String beginDate = df.format(calendar.getTime());

                Command parseCommand = new Command("truncate_errorlog.sh",
                        beginDate, endDate, outFile);
                exec(parseCommand);

            } catch (Exception e) {

                logger.log(Level.WARNING, "Failed to tranfer log of " +
                        myServers[i] + '.', e);
            }

            logger.fine("XferLog Completed for " + myServers[i]);

            logger.fine("XferLog Completed for " + myServers[i]);
        }
    }


    /**
     * Kill all glassfish servers.
     * We simply stop them instead of doing a hard kill.
     */
    public void kill() {
        stopServers();
        logger.info("Killed all GlassFish servers");
    }

    /**
     * Obtains the gregorian calendar representing the current time.
     * @param hostName The host name to get the calendar from
     * @return The calendar
     * @throws Exception Error obtaining calendar
     */
    public static GregorianCalendar getGregorianCalendar(
            String hostName)
            throws Exception {
        return exec(hostName, new RemoteCallable<GregorianCalendar>() {

            public GregorianCalendar call() {
                return new GregorianCalendar();
            }
        });
    }


}
