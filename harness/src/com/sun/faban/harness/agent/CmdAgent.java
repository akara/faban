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

import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.Command;
import com.sun.faban.harness.RemoteCallable;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.logging.Level;

/**
 * The methods in this interface are the public face of CmdAgent
 * They are used by the CmdService class in the Engine. There should
 * be no requirement for a benchmark to access the CmdAgent object
 * directly and thus this interface is not public.
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.engine.CmdService
 * @see com.sun.faban.harness.agent.CmdAgentImpl
 * @see com.sun.faban.harness.engine.GenericBenchmark
 */
public interface CmdAgent extends Remote {

    /**
     * Return the hostname of this machine as known to this machine
     * itself. This method is included in order to solve a naming conflict
     * related to the names of the result files to be transferred to the
     * the master machine.
     * @return The host name on this agent
     * @throws RemoteException A communications error occurred
     */
    public String getHostName() throws RemoteException;

    /**
     * Obtains the tmp directory of a remote host.
     * @return The tmp directory.
     * @throws RemoteException A communications error occurred
     */
    public String getTmpDir() throws RemoteException;

     /**
      * Set the logging level of the Agents.
      * @param name The name of the logger
      * @param level The log level
      * @throws RemoteException A communications error occurred
      */
    public void setLogLevel(String name, Level level) throws RemoteException;

    /**
     * Updates the paths, usually in the local command agent.
     * @param pathList The list of paths to download
     * @throws RemoteException If there is an error updating the paths
     */    
    public void updatePaths(List<String> pathList) throws RemoteException;

    /**
     * Downloads the files used by services and tools to 
     * the remote agent system.
     * @param pathList The list of service bundle paths
     * @throws RemoteException If there is an error downloading
     */
    public void downloadServices(List<String> pathList) throws RemoteException;


    /**
     * Similar to the which shell command, 'which' returns the actual path
     * to the given command. If it maps to a series of commands, they will
     * be returned as a single string separated by spaces. Note that 'which'
     * does not actually try to check the underlying system for commands
     * in the search path. It only checks the Faban infrastructure for
     * existence of such a command.
     * @param cmd The command to search for
     * @param svcPath The service path, if any
     * @return The actual command to execute, or null if not found.
     * @throws RemoteException If there is a communication error to the
     *                         remote agent
     */
    String which(String cmd, String svcPath) throws RemoteException;

    /**
     * Executes a command from the remote command agent.
     * @param c The command to be executed
     * @param svcPath
     * @return  A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     * @throws RemoteException If there is a communication error to the
     *                         remote agent
     */
    public CommandHandle execute(Command c, String svcPath)
            throws IOException, InterruptedException, RemoteException;
    /**
     * Executes a java command from the remote command agent.
     * @param c The command containing the main class
     * @param svcPath
     * @return A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle java(Command c, String svcPath)
            throws IOException, InterruptedException, RemoteException;

    /**
     * Executes the RemoteCallable on the target instance.
     * @param callable The callable to execute
     * @param contextLocation The context location of the invoker
     * @return The type specified at creation of the callable.
     * @throws Exception Any exception from the callable
     */
    public <V extends Serializable> V exec(RemoteCallable<V> callable,
                                           String contextLocation)
            throws Exception;

    /**
     * This method creates the Agent class and registers it with
     * the registry using ident@<host> name.
     * @param agentClass Impl class of the Agent to be started
     * @param ident Identifier to be used
     * @return true if the initialization was successful
     * @throws Exception An error occurred
     */
    public boolean startAgent(Class agentClass, String ident)
    throws Exception;

    /**
     * This method is responsible for aborting a running command.
     * @throws RemoteException A communications error occurred
     */
    public void kill() throws RemoteException;

    /**
     * Sets the time on the agent host, in GMT.
     * @param gmtTimeString
     * @throws IOException A I/O error occurred
     */
    public void setTime(String gmtTimeString) throws IOException;

    /**
     * Gets the time on the agent host, in millis.
     * @return The time on the remote system.
     * @throws RemoteException A communications error occurred
     */
    long getTime() throws RemoteException;
}