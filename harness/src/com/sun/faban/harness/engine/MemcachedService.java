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
import com.sun.faban.harness.RunContext;

import java.io.File;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * This class implements the service to start/stop Memcached instances.
 * It can be used by any benchmark to manage memcached servers and
 * perform these operations remotely using this Service.
 *
 * @author Shanti Subramanyam
 * @deprecated
 */
@Deprecated final public class MemcachedService {

    private static final int DEFAULT_PORT = 11211;  // default port
    private static MemcachedService service;
    private String[] myServers = new String[1];
    private int[] myPorts;
    private Logger logger;
    private static String memcachedctlCmd;
    private CommandHandle memcacheHandles[] = null;

    /**
     *
     * Private Constructor for a singleton object.
     *
     */
    private MemcachedService() {
        logger = Logger.getLogger(this.getClass().getName());
        logger.fine(this.getClass().getName() + " Created");
    }

    /**
     *
     * Get the reference to the singleton object.
     * Use this method to get access to the service.
     * @return MemcachedService - service object handle
     */
    public static MemcachedService getHandle() {
        if (service == null) {
            service = new MemcachedService();
        }

        return service;
    }

    /**
     * The setup method is called to set up a benchmark run. 
     * It is assumed that all servers have the same installation directory
     * The servers will be started on the default port (11211) 
     * with a memory size of 256MB. 
     * @param serverMachines - array specifying the memcached server machines. 
     * @param binDir - Memcached binary location
     */
    public void setup(String[] serverMachines, String binDir) {
        myServers = serverMachines;
        for (int i = 0; i < serverMachines.length; i++) {
            myPorts[i] = DEFAULT_PORT;
        }

        memcachedctlCmd = binDir + File.separator + "memcached -m 256";
        logger.info("MemcachedService setup complete.");

    }

    /**
     * The setup method is called to set up a benchmark run. 
     * It is assumed that all servers have the same installation directory
     * This version of the method accepts more parameters for more control.
     * @param serverMachines - array specifying the memcached server machines. 
     * @param ports - array specifying the ports for memcached - one per machine.
     * @param parameters - any other memcached arguments (e.g. "-u mysql")
     * @param binDir - Memcached binary location
     */
    public void setup(String[] serverMachines, int[] ports, String parameters, String binDir) {
        myServers = serverMachines;
        myPorts = ports;
        memcachedctlCmd = binDir + File.separator + "memcached " + parameters;
        logger.info("MemcachedService setup complete.");
    }

    /**
     * Start all memcached servers on configured hosts.
     * @return boolean true if start succeeded on all machines, else false
     */
    public boolean startServers() {
        Command startCmd;

        memcacheHandles = new CommandHandle[myServers.length];
        for (int i = 0; i < myServers.length; i++) {
            String server = myServers[i];
            startCmd = new Command(memcachedctlCmd + " -p " + myPorts[i]);
            startCmd.setSynchronous(false);
            startCmd.setLogLevel(Command.STDOUT, Level.INFO);
            startCmd.setLogLevel(Command.STDERR, Level.INFO);
            try {
                // Run the command in the background
                logger.fine("Starting memcached on " + server + " with: " + startCmd);
                memcacheHandles[i] = RunContext.exec(server, startCmd);
            } catch (Exception e) {
                logger.severe("Failed to start memcached server on " +
                        myServers[i] + " with " + e.toString());
                logger.log(Level.FINE, "Exception", e);
                return (false);
            }
        }
        logger.info("Completed memcached server(s) startup");
        return (true);
    }

    /**
     * Restart all servers. It first stops the servers, 
     * and then attempts to start them up again. If startup fails on
     * any server, it will stop all servers and cleanup.
     * @return true if all servers restarted successfully, otherwise false
     */
    public boolean restartServers() {

        logger.info("Restarting Memcached server(s). Please wait ... ");
        // We first stop servers
        this.stopServers();

        // Now start the memcached servers
        if (!startServers()) {
            // cleanup and return
            stopServers();
            return false;
        }
        return (true);
    }

    /**
     * stop Servers.
     * @return true if stop succeeded on all machines, else false
     */
    public boolean stopServers() {
        boolean success = true;
        // First check if servers were started
        // If there weren't started, simply return
        if (memcacheHandles == null || memcacheHandles.length == 0) {
            return (success);
        }

        for (int i = 0; i < myServers.length; i++) {
			if (memcacheHandles[i] != null) {
            try {
                int exit = memcacheHandles[i].exitValue();
                logger.warning("Memcached on " + myServers[i] +
                        " has exited unexpectedly during run with exit value of " + exit);
            } catch (IllegalThreadStateException ie) {
                // The server has not yet exited. Kill it
                try {
                    memcacheHandles[i].destroy();
                } catch (RemoteException re) {
                    logger.warning("Failed to stop memcached on " + myServers[i] + " with " + re.toString());
                    logger.log(Level.FINE, "Exception", re);
                    success = false;
                }
            } catch (RemoteException re) {
                logger.warning("exception while trying to get exitValue on " + myServers[i] + " - " + re.toString());
                logger.log(Level.FINE, "Exception", re);
                success = false;
            }
			}
        }
        memcacheHandles = null;
        return (success);
    }
}
