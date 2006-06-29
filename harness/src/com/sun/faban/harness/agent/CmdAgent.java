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
 * $Id: CmdAgent.java,v 1.1 2006/06/29 18:51:41 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.Command;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
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
     * itself. This method is included in order to solve a Naming problem 
     * related to the names of the tpcw result files to be transferred to the
     * the master machine.
     *
     */
    public String getHostName() throws RemoteException;

     /**
      * Set the logging level of the Agents
      */
    public void setLogLevel(String name, Level level) throws RemoteException;

    /**
     * Executes a command from the remote command agent.
     * @param c The command to be executed
     * @return  A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     * @throws RemoteException If there is a communication error to the
     *                         remote agent
     */
    public CommandHandle execute(Command c)
            throws IOException, InterruptedException, RemoteException;
    /**
	 * This method is responsible for starting the command in foreground
	 * The caller waits for the command to complete
	 * @param command to start
	 * @param priority in which to run command
	 * @return 	true if command started successfully
	 */
    public boolean start(String command, int priority)
	throws RemoteException, Exception;

    /**
     * This method is responsible for starting the script in foreground
     * The caller waits for the command to complete
     * @param command to start
     * @param priority in which to run command
     * @return 	true if command started successfully
     */
    public boolean runScript(String command, int priority)
	throws RemoteException, Exception;

    /**
     * This method is responsible for starting the command in background
     * @param command to start
     * @param ident to associate with this command
     * @param priority in which to run command
     * @return 	true if command started successfully
     */
    public boolean start(String command, String ident, int priority)
	throws RemoteException, Exception;

    /**
     * Start command in background and wait for the 
     * specified message
     * @param cmd command to be started
     * @param ident to identify this command later null if you don't want to do wait 
     *              or kill the process when the cmdAgent exits.
     * @param msg message message to which wait for
     * @param priority (default or higher priority) for command
     */
    public boolean start(String cmd, String ident, String msg, int priority)
    throws Exception;

    /**
     * This method is responsible for starting a java cmd in background
     * @param cmd JVM args and class to start
     * @param ident identifier to associate with this command
     * @param env in which to run command
     * @return 	true if command started successfully
     */
	public boolean startJavaCmd(String cmd, String ident, String env[])
    throws RemoteException, Exception;

    /**
     * This method is responsible for starting a java cmd in background
     * with some additional classpaths.
     * @param cmd args and class to start the JVM
     * @param identifier to associate with this command
     * @param env in which to run command
     * @param classPath the class path to prepend to the base class path
     * @return 	true if command started successfully
     */
    public boolean startJavaCmd(String cmd, String identifier, String[] env,
                                String[] classPath)
            throws RemoteException, Exception;
    /**
     * This method creates the Agent class and registers it with
     * the registry using ident@<host> name
     * @param agentClass Impl class of the Agent to be started
     * @param ident Identifier to be used
     * @return true if the initialization was successful
     */
    public boolean startAgent(Class agentClass, String ident)
    throws RemoteException, Exception;

    /**
     * This method is responsible for starting the command in background
     * and returning the first line of output.
     * @param cmd command to start
     * @param identifier to associate with this command
     * @param priority in which to run command
     * @return String the first line of output from the command
     */  
    public String startAndGetOneOutputLine(String cmd, String identifier, int priority)
	throws Exception, RemoteException;

    /**
     * This method starts a command in foreground
     * The stdout from command is captured and returned.
     * @param cmd : command to be started
     * @param priority - class in which cmd should be run
     * @return StringBuffer
     */
     public String startAndGetStdOut (String cmd, int priority)
     throws Exception, RemoteException;


    /**
     * This method is responsible for aborting a command
     * @param ident identifier associated with command in 'start' call
     */
    public void kill (String ident) throws RemoteException;
    
    /**
     * This method is responsible for aborting a running command
     */
    public void kill () throws RemoteException;

    /**
     * This method is responsible for aborting a command using the killem 
     * script.
     * @param identifier associated with command in 'start' call.
     * @param processString search string to grep the process while killing 
     *                      (same as in killem)
     * @param sigNum the signal number to be used to kill.
     *
     */
    public void killem (String identifier, String processString, int sigNum)
    throws RemoteException, IOException;
    /**
     * This method waits for the command started in BG to complete.
     * @param ident identifier associated with command in 'start' call
     */
    public boolean wait (String ident) throws RemoteException, Exception;
}