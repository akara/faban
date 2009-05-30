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
 * $Id: J2eeService.java,v 1.3 2009/05/30 04:48:49 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.engine;

import com.sun.faban.harness.common.Run;

import java.util.List;
import java.util.Properties;

/**
 *
 * This abstract class is extended by Application Server Services
 *
 * @author Ramesh Ramachandran
 * @deprecated
 */
@Deprecated public interface J2eeService {
    
    /**
     * setup Service and agents
     * The instanceArray vector should contain  array of instance dirs and 
     * logArray should contain array of log files for the corresponging 
     * instances in the serverMachines.
     */
    
    public void setup(Run r, String[] serverMachines,
                String[] serverHomes, List instanceArray, List logsArray);
    
    /**
     * set server configuration parameters  
     * if serverMachine is null then config will be set for all instances in all servers
     * if instanceHomes is null then config will be set for all instances in the serverMachine
     */
    public void setConfig(String serverMachine, String[] instanceHomes, Properties params);
    
    /**
     * start/restart Server 
     * if serverMachine is null then restart all instances in all servers
     * if instanceHomes is null then restart all instances in the serverMachine
     * boolean force if true the instances will be restarted even if there is 
     * no change in the config from the last start
     */
    public boolean restartServer(String serverMachine, String[] instanceHomes, boolean force);
    
    /**
     * stop Server
     * if serverMachine is null then stop all instances in all servers
     * if instanceHomes is null then stop all instances in the serverMachine
     */
    public boolean stopServer(String serverMachine, String[] instanceHomes);
    
    /**
     * transfer log files 
     * if serverMachine is null then transfer logs of all instances in all servers
     * if instanceHomes is null then transfer logs of all instances in the serverMachine
     */
    public void xferLogs(String serverMachine, String[] instanceHomes);
    
    /**
     * clear log files 
     * if serverMachine is null then clear all instances in all servers
     * if instanceHomes is null then clear all instances in the serverMachine
     */     
    public void clearLogs(String serverMachine, String[] instanceHomes);
    
    /**
     * kill the all agents associated with this service.
     * The App server agents will stop the app server instances
     */
    public void kill ();
     
}