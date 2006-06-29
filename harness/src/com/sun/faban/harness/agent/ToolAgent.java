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
 * $Id: ToolAgent.java,v 1.1 2006/06/29 18:51:41 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The methods in this interface are the public face of 
 * all ToolAgents configured on the various machines.
 * The ToolService starts up all the ToolAgents and gets a
 * handle to this interface. All Tool reequests from the
 * benchmark should be routed through the ToolService.
 *
 * @see com.sun.faban.harness.engine.ToolService
 * @author Ramesh Ramachandran
 */
public interface ToolAgent extends Remote {

    /**
     * This method configures the tools that must be run on
     * this machine
     * @param toolslist - each element in the array is the
     * name of a tool and optional arguments, e.g "sar -u -c"
     * @param outDir output directory of the run
     */
    void configure(String toolslist[], String outDir) throws RemoteException;

    void kill() throws RemoteException;

    /**
     * This method is responsible for starting all tools
     * @param 	delay - time to delay before starting
     * @return 	true if tool started successfully
     */
    boolean start(int delay) throws RemoteException;
	
    /**
     * This method is responsible for starting all tools
     * @return 	true if tool started successfully
     * @param 	delay - time to delay before starting
     * @param duration after which tools must be stopped
     */
    boolean start(int delay, int duration) throws RemoteException;

    /**
     * Start only specified tools 
     */
    boolean start(int delay, String[] tools)
	throws RemoteException;

    /**
     * Start only specified tools
     * @param 	delay - time to delay before starting
     * @param duration after which tools must be stopped
     */
    boolean start(int delay, String[] tools, int duration)
	throws RemoteException;

    /**
     * This method is responsible for stopping the tools
     */
    public void stop () throws RemoteException;
    public void stop (String tools[]) throws RemoteException;

}
