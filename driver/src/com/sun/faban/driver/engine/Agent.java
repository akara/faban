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
 * $Id: Agent.java,v 1.2 2009/03/27 16:27:53 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.driver.engine;

import com.sun.faban.driver.util.Timer;

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * The methods in this interface are the public face of all benchmark
 * agents. The agents reregister with the Registry on startup.
 * The Master gets a reference to all the agents from the Registry
 * and can then communicate with them using this (the Agent) interface.
 * @author Shanti Subrmanyam
 * @see com.sun.faban.driver.engine.MasterImpl
 */
public interface Agent extends Remote {

	/**
	 * initialize remote Agents
	 * @param master 
	 * @param runInfo run properties
	 * @param driverType 
     * @param timer the timer
	 * @throws RemoteException 
	 */
	public void configure(Master master, RunInfo runInfo, int driverType,
                          Timer timer) throws RemoteException;

    /**
     * Obtains the id of this agent.
     * @return The id of this agent.
     */
    public int getId() throws RemoteException;
    
    /**
     * Wait until all threads are started.
     * @throws RemoteException 
     */
    public void waitForThreadStart() throws RemoteException;

    /**
     * Sets the actual run start time.
     * @param msTime The relative millisec time of the benchmark start.
     * @throws RemoteException
     */
    public void setStartTime(int msTime) throws RemoteException;

	/**
	 * Report stats from a run, aggregating across all threads of
	 * the Agent.
	 * The stats object is actually different for each Agent.
	 * @return The stats of the run.
	 * @throws RemoteException 
	 * @see com.sun.faban.driver.engine.Metrics
	 */
	public Serializable getResults() throws RemoteException;

    /**
     * Waits for all the agentImpl's threads to terminate.
     * @throws java.rmi.RemoteException
     */
    public void join() throws RemoteException;

    /**
     * This method is responsible for aborting a run
     * @throws RemoteException 
     */
    public void kill() throws RemoteException;

    /**
     * Terminates all leftover threads remaining at the end of the run.
     * Logs the stack trace for all these threads but does not actually
     * wait for the threads to terminate (join). Terminate is called
     * while join is hanging on some thread that refuses to terminate.
     * @throws RemoteException 
     */
    public void terminate() throws RemoteException;


}
