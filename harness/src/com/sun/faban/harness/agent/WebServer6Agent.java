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
 * $Id: WebServer6Agent.java,v 1.2 2006/06/29 19:38:40 akara Exp $
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
public interface WebServer6Agent extends Remote {
        
  /**
   * This method configures the instance.
   */
   public void setup(Run run, String serverHome, String[] instances) throws RemoteException, Exception;

    /**
     * This method set the configure parameters
     */
    public void setConfig(Properties params, String[] instances) throws RemoteException, IOException;
  
    /**
     * start the server.
     * boolean force if true the instances will be restarted even if there is 
     * no change in the config from the last start
     */
    public void start(boolean force, String[] instances) throws RemoteException, IOException;
  
    /**
     * stop Server.
     */
    public void stop(String[] instances) throws RemoteException, IOException;

    /**
     * transfer Server logs.
     */
    public void xferLogs(String[] instances) throws RemoteException;
    
    /**
     * clear log files.
     */
    public void clearLogs(String[] instances) throws RemoteException;
        
    /**
     * kill server agent started
     */        
    public void kill() throws RemoteException;
    
}