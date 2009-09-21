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

import java.util.*;

/**
 *
 * This is the interface implemented by all webserver services.
 *
 * @author Shanti Subramanyam
 * @deprecated
 */
@Deprecated public interface WebServerService {

    /**
     * The setup method is called to set up a benchmark run. 
     * It is assumed that all servers have the same installation directory
     *
     * @param serverMachines - array specifying the web server machines. 
     * @param binDir - webserver binary location
     * @param logsDir - webserver logs location
     * @param confDir - webserver configuration file location
     * @param pidDir - webserver pidfile location
     */
    public void setup(String[] serverMachines, String binDir, String logsDir, 
            String confDir, String pidDir);
    
    /**
     * Start all web servers on configured hosts.
     * @return boolean true if start succeeded on all machines, else false
     */
    public boolean startServers();

    /**
     * Restart all servers. It first stops servers, clear logs
     * and then attempts to start them up again. If startup fails on
     * any server, it will stop all servers and cleanup.
     * @return boolean true if all servers restarted successfully, otherwise false
     */
    public boolean restartServers();
    
    /**
     * Stop servers.
     * @return boolean true if stop succeeded on all machines, else false
     */
    public boolean stopServers();
    
    /**
     * Clear webserver logs.
     * @return boolean true if operation succeeded, else fail
     */
    public boolean clearLogs();
    
    /**
     * Transfer log files.
     * This method copies over the error_log to the run output directory
     * and keeps only the portion of the log relevant for this run.
     * @param totalRunTime - the time in seconds for this run
     */
    public void xferLogs(int totalRunTime);

    /**
     *
     * Kill all web servers.
     */
    public void kill();
}
