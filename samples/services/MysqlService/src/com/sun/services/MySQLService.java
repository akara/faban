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
import com.sun.faban.harness.ConfigurationException;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.services.ServiceContext;
import com.sun.faban.harness.Context;

import com.sun.faban.harness.Configure;
import com.sun.faban.harness.services.GetLogs;
import com.sun.faban.harness.Start;
import com.sun.faban.harness.Stop;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class implements the service to start/stop the MySQL server.
 * It also provides functionality to transfer the portion of the mysql
 * errlog (host.err) for a run to the run output directory.
 * It can be used by any MySQL benchmark to manage a mysql server and
 * perform these operations remotely using this Service.
 * NOTE: It is assumed that the pid and err file are in $DBHOME/data.
 * 
 * @author Shanti Subramanyam modified by Sheetal Patil
 */
public class MySQLService {

    /** Injected service context. */
    @Context public ServiceContext ctx;
    Logger logger = Logger.getLogger(MySQLService.class.getName());
    String dbHome,  myServers[];
    String mysqlCmd,  dataDir, confFile;

    /**
     * Configures the MySQL service.
     */
    @Configure public void configure() throws ConfigurationException {
        logger.fine("Configuring mysql Started");
        myServers = ctx.getUniqueHosts();
        if(myServers == null){
            throw new ConfigurationException("MySQL hostname is not provided");
        }
        dbHome = ctx.getProperty("serverHome");
        if(dbHome != null && dbHome.trim().length() > 0) {
            if (!dbHome.endsWith(File.separator))
                dbHome = dbHome + File.separator;
        }else{
            throw new ConfigurationException("MySQL serverHome is not provided");
        }
        confFile = ctx.getProperty("confPath");
        if(confFile == null || confFile.trim().length() <= 0){
            logger.warning("confPath is not provided");
        }
        dataDir = dbHome + "data" + File.separator;
        mysqlCmd = dbHome + "bin" + File.separator + "mysqld_safe ";
        logger.fine("MysqlService Configure complete.");
    }

    /**
     * Starts up the MySQL instances.
     */
    @Start public void startup() {
        for (int i = 0; i < myServers.length; i++) {
            String pidFile = dataDir + myServers[i] + ".pid";
            logger.fine("Starting mysql on " + myServers[i]);
            Command startCmd = new Command(mysqlCmd + "--user=mysql " +
                "--datadir=" + dataDir + " --pid-file=" + pidFile);
            logger.fine("Starting mysql with: " + mysqlCmd);
            startCmd.setWorkingDirectory(dbHome);
            startCmd.setSynchronous(false); // to run in bg
            try {
                // Run the command in the background
                RunContext.exec(myServers[i], startCmd);
                /*
                 * Make sure the server has started.
                 */
                if ( !checkServerStarted(myServers[i])) {
                    logger.severe("Failed to find MySQL pidfile " + pidFile +
                            " on " + myServers[i]);
                }
                logger.fine("Completed MySQL server startup successfully on" + myServers[i]);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to start MySQL server.", e);
            }

        }
    }

    private boolean checkServerStarted(String hostName) throws Exception {
        boolean val = false;
        String pidFile = dataDir + hostName + ".pid";
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
     * Shuts down the MySQL instances.
     * @throws IOException Error executing the shutdown
     * @throws InterruptedException Interrupted waiting for the shutdown
     */
    @Stop public void shutdown() throws IOException, InterruptedException {
        for (int i = 0; i < myServers.length; i++) {
            String pidFile = dataDir + myServers[i] + ".pid";
            String myServer = myServers[i];
            logger.fine("Stopping MySQL server on" + myServer);
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
                    logger.fine("MySQL server stopped successfully on" + myServer);
                } catch (Exception ie) {
                    logger.warning("Kill mysqld failed with " + ie.toString());
                    logger.log(Level.FINE, "kill mysqld Exception", ie);
                    success = false;
                }
            }
        }
    }

    /**
     * Transfer log files.
     * This method copies over the error_log to the run output directory
     * and keeps only the portion of the log relevant for this run.
     * TODO: Modify code for mysql date/time format
     */
    @GetLogs public void getLogs() {
        for (int i = 0; i < myServers.length; i++) {
            String myServer = myServers[i];
            String outFile = RunContext.getOutDir() + "mysql_err.log." + RunContext.getHostName(myServer);
            String errFile = dataDir + myServers[i] + ".err";
            // copy the error_log to the master
            if (!RunContext.getFile(myServer, errFile, outFile)) {
                logger.warning("Could not copy " + errFile + " to " + outFile);
                return;
            }           
            RunContext.truncateFile(myServer, errFile);

            if(confFile != null && confFile.trim().length() > 0) {
                String outConfFile = RunContext.getOutDir() + "mysql_conf.log." + RunContext.getHostName(myServer);
                // copy the conf file to the master
                if (!RunContext.getFile(myServer, confFile, outConfFile)) {
                    logger.warning("Could not copy " + confFile + " to " + outConfFile);
                    return;
                }
            }
            logger.fine("XferLog Completed for " + myServer);
        }

    }   
}
