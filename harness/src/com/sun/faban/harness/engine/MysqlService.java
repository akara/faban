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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class implements the service to start/stop the Mysql server.
 * It also provides functionality to transfer the portion of the mysql
 * errlog (host.err) for a run to the run output directory.
 * It can be used by any MySQL benchmark to manage a mysql server and
 * perform these operations remotely using this Service.
 * NOTE: It is assumed that the pid and err file are in $DBHOME/data.
 * 
 * @author Shanti Subramanyam
 * @deprecated
 */
@Deprecated final public class MysqlService {

    private static MysqlService service;
    private String dbHome,  myServer;
    private Logger logger;
    private static String mysqlCmd,  errFile,  pidFile;

    /**
     *
     * Private Constructor for a singleton object.
     *
     */
    private MysqlService() {
        logger = Logger.getLogger(this.getClass().getName());
        logger.fine(this.getClass().getName() + " Created");
    }

    /**
     *
     * Get the reference to the singleton object.
     * Use this method to get access to the service.
     * @return MysqlService - service object handle
     */
    public static MysqlService getHandle() {
        if (service == null) {
            service = new MysqlService();
        }

        return service;
    }

    /**
     * The setup method is called to set up a benchmark run. 
     *
     * @param serverMachine - the name of the mysql server machine. 
     * @param dbDir - MySQL installation directory
     */
    public void setup(String serverMachine, String dbDir) {
        myServer = serverMachine;
        dbHome = dbDir;
        String dataDir = dbHome + File.separator + "data";

        errFile = dataDir + File.separator + myServer + ".err";
        pidFile = dataDir + File.separator + myServer + ".pid";
        mysqlCmd = dbHome + File.separator + "bin" +
                File.separator + "mysqld_safe --user=mysql " +
                "--datadir=" + dataDir + " --pid-file=" + pidFile;
        logger.info("MysqlService setup complete.");

    }

    /**
     * Start the mysql server on the configured host.
     * @return boolean true if start succeeded, else false
     */
    public boolean startServer() {
        Integer success = 0;

        Command startCmd = new Command(mysqlCmd);
        logger.fine("Starting mysql with: " + mysqlCmd);
        startCmd.setSynchronous(false); // to run in bg
        try {
            // Run the command in the background
            CommandHandle ch = RunContext.exec(myServer, startCmd);
            /*
             * Make sure the server has started.
             */
            if ( !checkServerStarted(myServer)) {
                logger.severe("Failed to find MySQL pidfile " + pidFile +
                        " on " + myServer);
                return (false);
            }

        } catch (Exception e) {
            logger.warning("Failed to start MySQL server with " + e.toString());
            logger.log(Level.FINE, "Exception", e);
            return (false);
        }

        logger.info("Completed MySQL server startup successfully");
        return (true);
    }

    /*
     * Check if apache server started by looking in the error_log
     */
    private boolean checkServerStarted(String hostName) throws Exception {
        boolean val = false;

        /*
         * Just to make sure we don't wait for ever.
         * We try to check 60 times for the existence of the
         * before we give up
         * Sleep 1 sec between each try. So wait for 1 min
         */
        int attempts = 60;
        while (attempts > 0) {

            val = RunContext.isFile(hostName, pidFile);
            if (val) {
                return val;

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
     * Restart the MySQL server. It first stops the server
     * and then attempts to start it up again. 
     * @return true if server restarted successfully, otherwise false
     */
    public boolean restartServer() {

        logger.info("Restarting MySQL server. Please wait ... ");
        // We first stop and clear the logs
        this.stopServer();

        // Now start the server
        return startServer();

    }

    /**
     * Stop server.
     * @return true if stop succeeded, else false
     */
    public boolean stopServer() {
        boolean success;
        String pidString;
        
        // First check if server is up
        success = RunContext.isFile(myServer, pidFile);
        if (success) {
            try {
                // First kill mysqld_safe
                Command stopCmd = new Command("pkill mysqld_safe");
                CommandHandle ch = RunContext.exec(myServer, stopCmd);

                // Get the pid from the pidFile
                ByteArrayOutputStream bs = new ByteArrayOutputStream(10);
                RunContext.writeFileToStream(myServer, pidFile, bs);
                pidString = bs.toString();

                stopCmd = new Command("kill " + pidString);
                logger.fine("Attempting to kill mysqld pid " + pidString);
                ch = RunContext.exec(myServer, stopCmd);

            } catch (Exception ie) {
                logger.warning("Kill mysqld failed with " + ie.toString());
                logger.log(Level.FINE, "kill mysqld Exception", ie);
                success = false;
            }

        }
        return (success);
    }

    /**
     * Transfer log files to the master.
     * This method copies over the error_log to the run output directory
     * and keeps only the portion of the log relevant for this run.
     * @param totalRunTime - the time in seconds for this run
     * 
     * TODO: Modify code for mysql date/time format
     */
    public void xferLogs(int totalRunTime) {
        String outFile = RunContext.getOutDir() + "mysql_err." + myServer;

        // copy the error_log to the master
        if (!RunContext.getFile(myServer, errFile, outFile)) {
            logger.warning("Could not copy " + errFile + " to " + outFile);
            return;
        }

        try {
            // Now get the start and end times of the run
            GregorianCalendar calendar = getGregorianCalendar(myServer);

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
            Command parseCommand = new Command("mysql_trunc_err.sh " +
                    beginDate + " " + endDate + " " + outFile);
            CommandHandle ch = RunContext.exec(parseCommand);

        } catch (Exception e) {

            logger.log(Level.WARNING, "Failed to tranfer log of " +
                    myServer + '.', e);
            logger.log(Level.FINE, "Exception", e);
        }

        logger.fine("XferLog Completed for " + myServer);


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
     * Kill the MySQL server.
     * Same as stopServer.
     */
    public void kill() {
        stopServer();

    }
}