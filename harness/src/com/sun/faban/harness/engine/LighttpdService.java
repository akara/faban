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
 * $Id: LighttpdService.java,v 1.3 2008/02/25 20:41:22 shanti_s Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.common.Command;

import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.RemoteCallable;
import com.sun.faban.harness.RunContext;
import java.io.BufferedReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.*;
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
 */
final public class LighttpdService implements WebServerService {

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
     * Start all apache servers on configured hosts
     * @return boolean true if start succeeded on all machines, else false
     */
    public boolean startServers() {
        Integer success = 0;
        String cmd = lightyCmd + "-f " + confFile;
        Command startCmd = new Command(cmd);
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
     */
    private static boolean checkServerStarted(String hostName) throws Exception {
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
        if (this.stopServers())
            this.clearLogs();

        // Now start the servers
        if (!startServers()) {
            // cleanup and return
            if (stopServers())
                clearLogs();
            return false;
        }
        return (true);
    }

    /**
     * stop Servers
     * @return true if stop succeeded on all machines, else false
     */
    public boolean stopServers() {
        boolean success = true;

        // First check if servers were started
        // If there weren't started, simply return
        if (ch == null || ch.length == 0) {
            return (success);
        }
        for (int i = 0; i < myServers.length; i++) {
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
        }         
        return (success);
    }

    /**
     * clear server logs and session files
     * It assumes that session files are in /tmp/sess*
     * @return true if operation succeeded, else fail
     */
    public boolean clearLogs() {
        Command cmd = new Command("rm -f /tmp/sess*");

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
           if (RunContext.isFile(myServers[i], pidFile)) {
                if (!RunContext.deleteFile(myServers[i], pidFile)) {
                    logger.log(Level.WARNING, "Delete of " + pidFile +
                            " failed on " + myServers[i]);
                    return (false);
                }
            }

            logger.fine("Logs cleared for " + myServers[i]);
            try {
                // Now delete the session files
                CommandHandle ch = RunContext.exec(myServers[i], cmd);
                logger.fine("Deleted session files for " + myServers[i]);
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
     * transfer log files
	 * This method copies over the error log to the run output directory
	 * and keeps only the portion of the log relevant for this run
	 * @param totalRunTime - the time in seconds for this run
     */
    public void xferLogs(int totalRunTime) {

        for (int i = 0; i < myServers.length; i++) {
            String outFile = RunContext.getOutDir() + "error_log." + myServers[i];

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
                CommandHandle ch = RunContext.exec(parseCommand);

            } catch (Exception e) {

                logger.log(Level.WARNING, "Failed to tranfer log of " +
                        myServers[i] + '.', e);
                logger.log(Level.FINE, "Exception", e);
            }

            logger.fine("XferLog Completed for " + myServers[i]);
        }

    }

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
     * Kill all servers
     * We simply stop them instead of doing a hard kill
     */
    public void kill() {
        stopServers();
        logger.info("Killed all lighttpd servers");
    }
}
