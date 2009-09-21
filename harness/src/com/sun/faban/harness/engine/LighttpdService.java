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
package com.sun.faban.harness.engine;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.RemoteCallable;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.WildcardFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
 * @author Shanti Subramanyam
 * @deprecated
 */
@Deprecated final public class LighttpdService implements WebServerService {

    private static LighttpdService service = null;
    private String[] myServers = new String[1];
    private Logger logger;
    private static String lightyCmd,  errlogFile,  acclogFile, confFile, pidFile;
    private CommandHandle[] ch = null;

    /**
     *
     * Private Constructor for a singleton object.
     *
     */
    private LighttpdService() {
        logger = Logger.getLogger(this.getClass().getName());
        logger.fine(this.getClass().getName() + " Created");
    }

    /**
     *
     * Get the reference to the singleton object.
     * Use this method to get access to the service.
	 * @return LighttpdService - service object handle
     */
    public static LighttpdService getHandle() {
        if (service == null) {
            service = new LighttpdService();
        }

        return service;
    }

    /**
     * The setup method is called to set up a benchmark run. 
     * It is assumed that all servers have the same installation directory
     *
     * @param serverMachines - array specifying the web server machines. 
     * @param binDir - lighttpd binary location
     * @param logsDir - lighttpd logs location
     * @param confDir - lighttpd conf file location
     * @param pidDir - lighttpd.pid file location
     */
    public void setup(String[] serverMachines, String binDir, String logsDir, 
            String confDir, String pidDir) {
        myServers = serverMachines;

        lightyCmd = binDir + File.separator + "lighttpd ";
        errlogFile = logsDir + File.separator + "error.log";
        acclogFile = logsDir + File.separator + "access.log";
        confFile = confDir + File.separator + "lighttpd.conf";
        pidFile = pidDir + File.separator + "lighttpd.pid";
        logger.info("LighttpdService setup complete.");

    }

    /**
     * Start all apache servers on configured hosts.
     * @return boolean true if start succeeded on all machines, else false
     */
    public boolean startServers() {

        Command startCmd = new Command(lightyCmd, "-f", confFile);
        startCmd.setSynchronous(false);
        startCmd.setLogLevel(Command.STDOUT, Level.FINE);
        startCmd.setLogLevel(Command.STDERR, Level.FINE);
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
                    return (false);
                }

            } catch (Exception e) {
                logger.warning("Failed to start lighttpd server with " + e.toString());
                logger.log(Level.FINE, "Exception", e);
                return (false);
            }
        }
        logger.info("Completed lighttpd server(s) startup");
        return (true);
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

    /**
     * Restart all servers. It first stops servers, clear logs
     * and then attempts to start them up again. If startup fails on
	 * any server, it will stop all servers and cleanup.
	 * @return true if all servers restarted successfully, otherwise false
     */
    public boolean restartServers() {

        logger.info("Restarting lighttpd server(s). Please wait ... ");
        // We first stop and clear the logs
        if (stopServers())
            clearLogs();

        // Now start the servers
        if (!startServers()) {
            // cleanup and return
            if (stopServers())
                clearLogs();
            return false;
        }
        return true;
    }

    /**
     * Stop Servers.
     * @return true if stop succeeded on all machines, else false
     */
    public boolean stopServers() {
        boolean success = true;
	int pid = -1;

        /******* Changing logic to check for pidfile and kill process
        // First check if servers were started
        // If there weren't started, simply return
        if (ch == null || ch.length == 0) {
            return (success);
        }
         */
        for (String hostName : myServers) {
            /***
            try {
                if (ch[i] != null) {
                    ch[i].destroy();
                }                
            } catch (RemoteException re) {
                logger.log(Level.WARNING, "Failed to stop lighttpd on " +
                        myServers[i] + " with " + re);
                logger.log(Level.FINE, "lighttpd stop Exception", re);                
                success = false;
            }
             */
            if (RunContext.isFile(hostName, pidFile)) {
                // we retrieve the pid value
                try {
                    pid = getPid(hostName);
                    logger.fine("Found lighttpd pidvalue of " + pid + " on host " + hostName);
                } catch (Exception ee) {
                    logger.log(Level.WARNING, "Failed to read lighttpd pidfile on " +
                            hostName + " with " + ee);
                    logger.log(Level.FINE, "Exception", ee);                
                    success = false;
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
                        success = false;
                        logger.severe("Cannot kill lighttpd pid " + pid + " on " + hostName);
                    }
                } catch (Exception e) {
                    success = false;
                    logger.severe("kill " + pid + " failed on " + hostName);
                    logger.log(Level.FINE, "Exception", e);
                }
            }
        }                
        return (success);
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
                BufferedReader bufR = new BufferedReader(new InputStreamReader(is));
                pidval = bufR.readLine();
                bufR.close();
                return (Integer.parseInt(pidval));
            }
         });
        return (pid);
    }

    /**
     * Clear server logs and session files.
	 * Clears access log, error log, pidfile and session files.
     * It assumes that session files are in /tmp/sess*.
     * @return true if operation succeeded, else fail
     */
    public boolean clearLogs() {

        boolean success = true;

        for (int i = 0; i < myServers.length; i++) {
            if (RunContext.isFile(myServers[i], errlogFile)) {
                if (!RunContext.deleteFile(myServers[i], errlogFile)) {
                    logger.log(Level.WARNING, "Delete of " + errlogFile +
                            " failed on " + myServers[i]);
                    success = false;
                }
            }
            if (RunContext.isFile(myServers[i], acclogFile)) {
                if (!RunContext.deleteFile(myServers[i], acclogFile)) {
                    logger.log(Level.WARNING, "Delete of " + acclogFile +
                            " failed on " + myServers[i]);
                    success = false;
                }
            }
           if (RunContext.isFile(myServers[i], pidFile)) {
                if (!RunContext.deleteFile(myServers[i], pidFile)) {
                    logger.log(Level.WARNING, "Delete of " + pidFile +
                            " failed on " + myServers[i]);
                    success = false;
                }
            }

            logger.fine("Logs cleared for " + myServers[i]);
             // Now delete the session files
             if (RunContext.deleteFiles(myServers[i], "/tmp",
                        new WildcardFileFilter("sess*")))
                 logger.fine("Deleted session files for " + myServers[i]);
             else
                 logger.warning("Error deleting session files for " +
                            myServers[i]);
        }
        return (success);
    }

    /**
     * Transfer log files to the master.
	 * This method copies over the error log to the run output directory
	 * and keeps only the portion of the log relevant for this run.
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
                SimpleDateFormat df = new SimpleDateFormat("MM,dd,HH:mm:ss");
                String endDate = df.format(calendar.getTime());

                calendar.add(Calendar.SECOND, (totalRunTime * (-1)));

                String beginDate = df.format(calendar.getTime());

                //parse the log file
				/*****
                Command parseCommand = new Command("apache_trunc_errorlog.sh \"" +
                        beginDate + "\"" + " \"" + endDate + "\" " +
                        outFile);
				****/
                Command parseCommand = new Command("lighttpd_trunc_errorlog.sh " +
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
        return RunContext.exec(hostName, new RemoteCallable<GregorianCalendar>() {

            public GregorianCalendar call() {
                return new GregorianCalendar();
            }
        });
    }

    /**
     *
     * Kill all servers.
     * We simply stop them instead of doing a hard kill.
     */
    public void kill() {
        stopServers();
        logger.info("Killed all lighttpd servers");
    }
}
