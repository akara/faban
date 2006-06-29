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
 * at faban/src/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: OracleAgent.java,v 1.1 2006/06/29 18:51:41 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import com.sun.faban.harness.common.Run;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.io.*;
import java.util.*;

/**
 *
 *
 * @author Ramesh Ramachandran
 */
public interface OracleAgent extends Remote {

  /**
   * This method get the configure parameters of the oracle instance.
   * @param serverID - instance name
   */
    public List getConfig(String serverID) throws RemoteException, IOException;
    
    /**
     * This method set the configure parameters of the oracle instance.
     * @param serverID - instance name
     */
    public void setConfig(String serverID, List oracleParams) throws RemoteException, IOException;
  
    /**
     * start an oracle instance.
     */
    public boolean start(String serverID) throws RemoteException, Exception;
    
    /**
     * stop Server
     */
    public boolean stop(String serverID) throws RemoteException, Exception;
    
    
    public boolean startListener() throws RemoteException, Exception;

    public boolean stopListener() throws RemoteException, Exception;

    public boolean checkListenerStatus() throws RemoteException, Exception;
    
    /**
     * get statistics of a web server instance
     */
    public void startStats() throws RemoteException, Exception;
    
    public void stopStats() throws RemoteException, Exception;

    /**
     * clear log files 
     */
    public void clearLogs() throws RemoteException, Exception;
    
    public void configure(Run run, String oracleHome, String oracleSid, String[] allConfigs) throws RemoteException, Exception;
    
    public void kill() throws RemoteException, Exception;

    public boolean execSQL(String sql) throws RemoteException, Exception;

}
