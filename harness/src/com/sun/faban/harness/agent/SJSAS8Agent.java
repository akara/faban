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
 * $Id: SJSAS8Agent.java,v 1.1 2006/06/29 18:51:41 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.io.*;
import java.util.*;

import com.sun.faban.harness.common.Run;

/**
 *
 *
 * @author Ramesh Ramachandran
 */
public interface SJSAS8Agent extends Remote {
        
  /**
   * This method configures the S1ASAgent instance.
   */
   public void setup(Run run, String serverHome, String[] instanceHomes, String[] instanceLogs)
               throws RemoteException, Exception;

    /**
     * This method set the configure parameters of the S1AS instance.
     * @param instanceHomes - instance names, if its null config is set for
     * all instances in the server.
     */
    public void setConfig(String[] instanceHomes, Properties wlsParams) throws RemoteException, IOException;
  
    /**
     * start a S1AS instance. If its null start 
     * all instances in the server.
     * boolean force if true the instances will be restarted even if there is 
     * no change in the config from the last start
     */
    public void start(String[] instanceHomes, boolean force) throws RemoteException, IOException;
  
    /**
     * stop Server. If its null stop all instances in the server.
     */
    public void stop(String[] instanceHomes) throws RemoteException, IOException;

    /**
     * transfer Server logs. If instanceHomes is  null transfer logs of  all instances.
     */
    public void xferLogs(String[] instanceHomes) throws RemoteException;
    
    /**
     * clear log files. If instanceHomes is null clear logs of all instances in the server.
     */
    public void clearLogs(String[] instanceHomes) throws RemoteException, Exception;
        
    /**
     * kill all server agents started 
     */        
    public void kill() throws RemoteException, Exception;
    
}