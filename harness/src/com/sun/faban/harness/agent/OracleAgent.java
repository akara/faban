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
package com.sun.faban.harness.agent;

import com.sun.faban.harness.common.Run;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.io.*;
import java.util.*;

/**
 * An agent for controlling the Oracle database.
 * @author Ramesh Ramachandran
 * @deprecated Replaced by the services/tools infrastructure
 */
@Deprecated public interface OracleAgent extends Remote {

    /**
     * This method get the configure parameters of the oracle instance.
     * @param serverID Instance name
     * @return The list of configuration parameters
     * @throws IOException An I/O error occurred
     */
    public List getConfig(String serverID) throws IOException;
    
    /**
     * This method set the configure parameters of the oracle instance.
     * @param serverID Instance name
     * @param oracleParams The parameter list
     * @throws IOException An I/O error occurred
     */
    public void setConfig(String serverID, List oracleParams) throws IOException;
  
    /**
     * Start an Oracle instance.
     * @param serverID Instance name
     * @return Whether the server started successfully
     * @throws Exception An error occurred in the process
     */
    public boolean start(String serverID) throws Exception;
    
    /**
     * Stop an Oracle instance.
     * @param serverID Instance name
     * @return Whether the server stopped successfully
     * @throws Exception An error occurred in the process
     */
    public boolean stop(String serverID) throws Exception;
    
    /**
     * Start the Oracle listener.
     * @return Whether the listener started successfully
     * @throws Exception An error occurred in the process
     */
    public boolean startListener() throws Exception;

    /**
     * Stop an Oracle listener.
     * @return Whether the listener stopped successfully
     * @throws Exception An error occurred in the process
     */
    public boolean stopListener() throws Exception;

    /**
     * Checks the listener status.
     * @return Whether the listener is running
     * @throws Exception An error occurred in the process
     */
    public boolean checkListenerStatus() throws Exception;
    
    /**
     * Start gathering statistics for an instance.
     * @throws Exception An error occurred in the process
     */
    public void startStats() throws Exception;

    /**
     * Stop gathering statistics fpr an instance.
     * @throws Exception An error occurred in the process
     */
    public void stopStats() throws Exception;

    /**
     * Clear log files.
     * @throws Exception An error occurred in the process
     */
    public void clearLogs() throws Exception;

    /**
     * Configures the agent.
     * @param run The benchmark run
     * @param oracleHome ORACLE_HOME
     * @param oracleSid ORACLE_SID
     * @param allConfigs Pathnames to all config files
     * @throws Exception An error occurred in the process
     */
    public void configure(Run run, String oracleHome, String oracleSid,
                          String[] allConfigs) throws Exception;
    
    /**
     * Kills the Oracle instance.
     * @throws Exception An error occurred in the process
     */
    public void kill() throws Exception;

    /**
     * Executes an SQL statement.
     * @param sql The statement
     * @return Whether the SQL statement executed successfully
     * @throws Exception An error occurred in the process
     */
    public boolean execSQL(String sql) throws Exception;
}
