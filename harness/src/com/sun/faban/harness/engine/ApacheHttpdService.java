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

import com.sun.faban.common.Command;

import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.Utilities;
import com.sun.faban.harness.RemoteCallable;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.WildcardFileFilter;

import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class implements the service to start/stop ApacheHttpd instances.
 * It also provides functionality to transfer the portion of the apache
 * error_log for a run to the run output directory.
 * It can be used by any Apache benchmark to manage apache servers and
 * perform these operations remotely using this Service.
 *
 * @author Shanti Subramanyam
 * @deprecated
 */
@Deprecated final public class ApacheHttpdService implements WebServerService {

    private static ApacheHttpdService service = null;
    private String[] myServers = new String[1];
    private Logger logger;
    private static String apachectlCmd,  errlogFile,  acclogFile;

    /**
     *
     * Private Constructor for a singleton object.
     *
     */
    private ApacheHttpdService() {
        logger = Logger.getLogger(this.getClass().getName());
        logger.fine(this.getClass().getName() + " Created");
    }

    /**
     *
     * Get the reference to the singleton object.
     * Use this method to get access to the service.
	 * @return ApacheHttpdService - service object handle
     */
    public static ApacheHttpdService getHandle() {
        if (service == null) {
            service = new ApacheHttpdService();
        }

        return service;
    }

    /**
     * The setup method is called to set up a benchmark run. 
     * It is assumed that all servers have the same installation directory
     *
     * @param serverMachines - array specifying the apache server machines. 
     * @param binDir - Apache binaries location
     * @param logsDir - Apache logs location
     * @param confDir - Apache httpd.conf file location
     * @param pidDir - Apache httpd.pid file location
     */
    public void setup(String[] serverMachines, String binDir, String logsDir, 
            String confDir, String pidDir) {
        myServers = serverMachines;

        apachectlCmd = binDir + File.separator + "apachectl ";
        errlogFile = logsDir + File.separator + "error_log";
        acclogFile = logsDir + File.separator + "access_log";
        logger.info("ApacheHttpdService setup complete.");
    }

    /**
     * Start all apache servers on configured hosts.
     * @return boolean true if start succeeded on all machines, else false
     */
    public boolean startServers() {

        String cmd = apachectlCmd + "start";
        Command startCmd = new Command(cmd);
        startCmd.setLogLevel(Command.STDOUT, Level.FINE);
        startCmd.setLogLevel(Command.STDERR, Level.FINE);

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
                    logger.fine("Completed apache httpd startup successfully on " + server);
                } else {
                    logger.severe("Failed to find start message in " + errlogFile +
                            " on " + server);
                    return (false);
                }

            } catch (Exception e) {
                logger.warning("Failed to start apache server with " + e.toString());
                logger.log(Level.FINE, "Exception", e);
                return (false);
            }
        }
        logger.info("Completed apache httpd server(s) startup");
        return (true);
    }

    /*
	 * Check if apache server started by looking in the error_log
	 */
    private static boolean checkServerStarted(String hostName) throws Exception {
        Integer val = 0;
        final String err = errlogFile;
        val = RunContext.exec(hostName, new RemoteCallable<Integer>() {

            public Integer call() throws Exception {
                Integer retVal = 0;
                String msg = "resuming normal operations";

                // Ensure filenames are not impacted by path differences.
                FileInputStream is = new FileInputStream(
                        Utilities.convertPath(err));
                BufferedReader bufR = new BufferedReader(
                        new InputStreamReader(is));

                // Just to make sure we don't wait for ever.
                // We try to read the msg 120 times before we give up
                // Sleep 1 sec between each try. So wait for 1 min
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
        if (val == 1) {
            return (true);
        } else {
            return (false);
        }
    }

    /**
     * Restart all servers. It first stops servers, clear logs
     * and then attempts to start them up again. If startup fails on
	 * any server, it will stop all servers and cleanup.
	 * @return true if all servers restarted successfully, otherwise false
     */
    public boolean restartServers() {

        logger.info("Restarting Apache server(s). Please wait ... ");
        // We first stop and clear the logs
        this.stopServers();
        this.clearLogs();

        // Now start the apache servers
        if (!startServers()) {
            // cleanup and return
            stopServers();
            clearLogs();
            return false;
        }
        return (true);
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
        return (success);
    }

    /*
	 * Check if apache server stopped by scanning error_log
	 */
    private static Integer checkServerStopped(String hostName) throws Exception {
        Integer val = 0;
        final String err = errlogFile;
        val = RunContext.exec(hostName, new RemoteCallable<Integer>() {

            public Integer call() throws Exception {
                Integer retVal = 0;
                // Read the log file to make sure the server has shutdown
                String msg = "shutting down";

                // Ensure filenames are not impacted by path differences.
                FileInputStream is = new FileInputStream(
                        Utilities.convertPath(err));
                BufferedReader bufR = new BufferedReader(
                        new InputStreamReader(is));

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

    /**
     * Clear apache logs and session files.
	 * It assumes that session files are in /tmp/sess*.
     * @return true if operation succeeded, else fail
     */
    public boolean clearLogs() {

        for (int i = 0; i < myServers.length; i++) {
            if (RunContext.isFile(myServers[i], errlogFile)) {
                if (!RunContext.deleteFile(myServers[i], errlogFile)) {
                    logger.log(Level.WARNING, "Delete of " + errlogFile +
                            " failed on " + myServers[i]);
                    return (false);
                }
            }
            if (RunContext.isFile(myServers[i], acclogFile)) {
                if (!RunContext.deleteFile(myServers[i], acclogFile)) {
                    logger.log(Level.WARNING, "Delete of " + acclogFile +
                            " failed on " + myServers[i]);
                    return (false);
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
                return (false);
            }
        }
        return (true);
    }

    /**
     * Transfer the log files.
	 * This method copies over the error_log to the run output directory
	 * and keeps only the portion of the log relevant for this run
	 * @param totalRunTime - the time in seconds for this run
     */
    public void xferLogs(int totalRunTime) {

        for (int i = 0; i < myServers.length; i++) {
            String outFile = RunContext.getOutDir() + "httpd_err.log." +
                             RunContext.getHostName(myServers[i]);

            // copy the error_log to the master
            if (!RunContext.getFile(myServers[i], errlogFile, outFile)) {
                logger.warning("Could not copy " + errlogFile + " to " + outFile);
                return;
            }

            try {
                // Now get the start and end times of the run
                GregorianCalendar calendar = getGregorianCalendar(myServers[i]);

                //format the end date
                SimpleDateFormat df = new SimpleDateFormat("MMM,dd,HH:mm:ss");
                String endDate = df.format(calendar.getTime());

                calendar.add(Calendar.SECOND, (totalRunTime * (-1)));

                String beginDate = df.format(calendar.getTime());

                //parse the log file
				/*****
                Command parseCommand = new Command("apache_trunc_errorlog.sh \"" +
                        beginDate + "\"" + " \"" + endDate + "\" " +
                        outFile);
				****/
                Command parseCommand = new Command("apache_trunc_errorlog.sh " +
                        beginDate + " " + endDate + " " + outFile);
                RunContext.exec(parseCommand);

            } catch (Exception e) {

                logger.log(Level.WARNING, "Failed to tranfer log of " +
                        myServers[i] + '.', e);
                logger.log(Level.FINE, "Exception", e);
            }

            logger.fine("XferLog Completed for " + myServers[i]);
        }

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
        return RunContext.exec(hostName,
                new RemoteCallable<GregorianCalendar>() {

                    public GregorianCalendar call() {
                        return new GregorianCalendar();
                    }
                });
    }

    /**
     *
     * Kill all ApacheHttpd servers.
     * We simply stop them instead of doing a hard kill.
     */
    public void kill() {
        stopServers();
        logger.info("Killed all ApacheHttpd servers");
    }
}
