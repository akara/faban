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
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Run;

import java.util.List;
import java.util.Properties;

/**
 *
 * This abstract class is extended by Application Server Services.
 *
 * @author Ramesh Ramachandran
 * @deprecated
 */
@Deprecated public interface J2eeService {
    
    /**
     * Setup Service and agents.
     * The instanceArray vector should contain  array of instance dirs and 
     * logArray should contain array of log files for the corresponging instances.
     * @param r The run
     * @param serverMachines The server machines to start the service
     * @param serverHomes The server homes to start the service
     * @param instanceArray Array of instance directories
     * @param logsArray The array of log files
     */
    public void setup(Run r, String[] serverMachines,
                String[] serverHomes, List instanceArray, List logsArray);
    
    /**
     * Set server configuration parameters.
     * If serverMachine is null then config will be set for all instances in all servers.
     * If instanceHomes is null then config will be set for all instances in the serverMachine.
     * @param serverMachine The target system
     * @param instanceHomes The instance home directories
     * @param params The server configuration parameters
     */
    public void setConfig(String serverMachine, String[] instanceHomes, Properties params);
    
    /**
     * Start/restart server.
     * If serverMachine is null then restart all instances in all servers.
     * If instanceHomes is null then restart all instances in the serverMachine
     * boolean force if true the instances will be restarted even if there is 
     * no change in the config from the last start.
     * @param serverMachine The target system
     * @param instanceHomes The instance home directories
     * @param force Whether to force the restart or not
     * @return If the restart succeeded
     */
    public boolean restartServer(String serverMachine, String[] instanceHomes, boolean force);
    
    /**
     * Stop Server.
     * If serverMachine is null then stop all instances in all servers.
     * If instanceHomes is null then stop all instances in the serverMachine.
     * @param serverMachine The target system
     * @param instanceHomes The instance home directories
     * @return If the stop succeeded
     */
    public boolean stopServer(String serverMachine, String[] instanceHomes);
    
    /**
     * Transfer log files.
     * If serverMachine is null then transfer logs of all instances in all servers.
     * If instanceHomes is null then transfer logs of all instances in the serverMachine.
     * @param serverMachine The target system
     * @param instanceHomes The instance home directories
     */
    public void xferLogs(String serverMachine, String[] instanceHomes);
    
    /**
     * Clear log files.
     * If serverMachine is null then clear all instances in all servers.
     * If instanceHomes is null then clear all instances in the serverMachine.
     * @param serverMachine The target system
     * @param instanceHomes The instance home directories
     */
    public void clearLogs(String serverMachine, String[] instanceHomes);
    
    /**
     * Kill the all agents associated with this service.
     * The App server agents will stop the app server instances.
     */
    public void kill ();
     
}