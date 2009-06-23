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
 * $Id: ApacheHttpdService.java,v 1.1 2009/06/23 19:02:02 sheetalpatil Exp $
 *
 * Copyright 2008 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.services;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.RunContext;
import com.sun.faban.harness.services.ServiceContext;
import com.sun.faban.harness.Context;

import com.sun.faban.harness.WildcardFileFilter;
import com.sun.faban.harness.services.ClearLogs;
import com.sun.faban.harness.services.Configure;
import com.sun.faban.harness.services.Startup;
import com.sun.faban.harness.services.Shutdown;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
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
 * @author Sheetal Patil based on work done by Shanti Subramanyam
 * 
 */

public class ApacheHttpdService {

    @Context public ServiceContext ctx;    
    private Logger logger = Logger.getLogger(ApacheHttpdService.class.getName());
    private String[] myServers = new String[1];
    private static String apacheCmd,  errlogFile,  acclogFile;
    CommandHandle apacheHandles[];

    @Configure public void configure() {
        myServers = ctx.getHosts();
        apacheCmd = ctx.getProperty("cmdPath");
        if (!apacheCmd.endsWith(" "))
            apacheCmd = apacheCmd + " ";

        String logsDir = ctx.getProperty("logsDir");
        if (!logsDir.endsWith(File.separator))
            logsDir = logsDir + File.separator;

        errlogFile = logsDir + "error_log";
        acclogFile = logsDir + "access_log";
        logger.info("ApacheHttpdService setup complete.");
        apacheHandles = new CommandHandle[myServers.length];
    }

    
    @Startup public void startup() {
        String cmd = apacheCmd + "start";
        logger.info("Starting Apache Service with command = "+ cmd);
        Command startCmd = new Command(cmd);
        startCmd.setSynchronous(false); // to run in bg
 
        for (int i = 0; i < myServers.length; i++) {
            String server = myServers[i];
            try {
                // Run the command in the foreground and wait for the start
                apacheHandles[i] = RunContext.exec(server, startCmd);
                logger.info("Completed apache httpd server(s) startup successfully on "
                        + server);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Failed to start apache server.", e);
            }
        }
    }
  
    @Shutdown public void shutdown() throws IOException, InterruptedException {
        for (int i = 0; i < myServers.length; i++) {
            if (apacheHandles[i] != null) {
                //Try to Stop it.
                try {
                        String cmd = apacheCmd + "stop";
                        Command stopCmd = new Command(cmd);
                        stopCmd.setLogLevel(Command.STDOUT, Level.FINE);
                        stopCmd.setLogLevel(Command.STDERR, Level.FINE);

                        // Run the command in the foreground
                        RunContext.exec(myServers[i], stopCmd);
                        apacheHandles[i].destroy();
                } catch (RemoteException re) {
                        logger.log(Level.WARNING, "Failed to stop Apache httpd server" +
                                myServers[i] + " with " + re.toString(), re);
                }                
			}
        }
        apacheHandles = null;
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