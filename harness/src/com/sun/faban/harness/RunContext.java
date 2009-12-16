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
package com.sun.faban.harness;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.engine.CmdService;
import com.sun.faban.harness.engine.RunFacade;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.Invoker;
import com.sun.faban.harness.agent.CmdAgentImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.File;
import java.util.List;

/**
 * The RunContext provides callbacks into the harness and the run environment.
 * All methods are static and it is suitable for static imports making calls
 * much shorter and easier. RunContext calls are available during a run on the
 * master. With exception to two methods, calling RunContext methods from tools
 * or inside RemoteCallable.call() will result in a NullPointerException.
 * The two methods callable from the agent are the local versions of
 * RunContext.exec() and RunContext.java(). These are the signatures without
 * a host name.
 *
 * @author Akara Sucharitakul
 */
public class RunContext {

    /**
     * Obtains the benchmark deployment directory.
     * @return The benchmark deployment directory
     */
    public static String getBenchmarkDir() {
        return RunFacade.getInstance().getBenchmarkDir();
    }

    /**
     * Obtains the sequence part of the run id.
     * @return The run sequence
     */
    public static String getRunSeq() {
        return RunFacade.getInstance().getRunSeq();
    }

    /**
     * Obtains the full name of the run in the form benchmark.id.
     * @return The run name
     */
    public static String getRunId() {
        return RunFacade.getInstance().getRunId();
    }

    /**
     * Obtains the output directory for this run.
     * @return The run output directory
     */
    public static String getOutDir() {
        return RunFacade.getInstance().getOutDir();
    }

    /**
     * Obtains the tmp directory used by Faban on the target host.
     * This may not be the same as the local one. Each OS is different
     * and the tmpdir for the web container is also different.
     * @param host The host to obtain the tmp dir, or null for local JVM
     * @return The tmp directory
     */
    public static String getTmpDir(String host) {
        if (host == null)
            return Config.TMP_DIR;
        else
            return CmdService.getHandle().getTmpDir(host);
    }

    /**
     * Obtains the param repository for this run.
     * @return The param repository
     */
    public static ParamRepository getParamRepository() {
        return RunFacade.getInstance().getParamRepository();
    }

    /**
     * Obtains the parameter/config file path for this run.
     * @return The parameter file path
     */
    public static String getParamFile() {
        return RunFacade.getInstance().getParamFile();
    }

    /**
     * Returns the location of this command on the local system. This is
     * one of the few methods callable from tools and RemoteCallable.call().
     * Similar to the which shell command, 'which' returns the actual path
     * to the given command. If it maps to a series of commands, they will
     * be returned as a single string separated by spaces. Note that 'which'
     * does not actually try to check the underlying system for commands
     * in the search path. It only checks the Faban infrastructure for
     * existence of such a command.
     * @param c The command to search for
     * @return The actual command to execute, or null if not found.
     * @throws IOException If there is a communication error to the
     *                         remote agent
     */
    public static String which(String c) throws IOException {
        CmdAgentImpl agent = CmdAgentImpl.getHandle();
        if (agent != null) // Running on agent
            return agent.which(c, Invoker.getContextLocation());
        else // Running on master
            return CmdService.getHandle().which(c,
                    Invoker.getContextLocation());
    }

    /**
     * Returns the location of this command on the target system.
     * Similar to the which shell command, 'which' returns the actual path
     * to the given command. If it maps to a series of commands, they will
     * be returned as a single string separated by spaces. Note that 'which'
     * does not actually try to check the underlying system for commands
     * in the search path. It only checks the Faban infrastructure for
     * existence of such a command.
     * @param host The host to search
     * @param c The command to search for
     * @return The actual command to execute, or null if not found.
     * @throws IOException If there is a communication error to the
     *                         remote agent
     */
    public static String which(String host, String c) throws IOException {
        return CmdService.getHandle().which(host, c,
                Invoker.getContextLocation());
    }

    /**
     * Returns the location of this command on the target systems.
     * Similar to the which shell command, 'which' returns the actual path
     * to the given command. If it maps to a series of commands, they will
     * be returned as a single string separated by spaces. Note that 'which'
     * does not actually try to check the underlying system for commands
     * in the search path. It only checks the Faban infrastructure for
     * existence of such a command.
     * @param hosts The hosts to search
     * @param c The command to search for
     * @return The actual command paths to execute, or null elements if not found.
     */
    public static String[] which(String[] hosts, String c) {
        return CmdService.getHandle().which(hosts, c,
                Invoker.getContextLocation());
    }

    /**
     * Executes a command on the local system. This is one of the few
     * methods callable from tools and RemoteCallable.call().
     * @param c The command to be executed
     * @return  A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public static CommandHandle exec(Command c)
            throws IOException, InterruptedException {
        CmdAgentImpl agent = CmdAgentImpl.getHandle();
        if (agent != null) // Running on agent
            return agent.execute(c, Invoker.getContextLocation());
        else // Running on master
            return CmdService.getHandle().execute(c,
                    Invoker.getContextLocation());
    }

    /**
     * Executes a command on a remote host.
     * @param host The target machine to execute the command
     * @param c The command to be executed
     * @return A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public static CommandHandle exec(String host, Command c)
            throws IOException, InterruptedException {
        return CmdService.getHandle().execute(host, c,
                Invoker.getContextLocation());
    }

    /**
     * Executes a command on a set of remote hosts.
     * @param hosts The target machines to execute the command
     * @param c The command to be executed
     * @return Handles to the command on each of the target machines
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public static CommandHandle[] exec(String[] hosts, Command c)
            throws IOException, InterruptedException {
        return CmdService.getHandle().execute(hosts, c,
                Invoker.getContextLocation());
    }

    /**
     * Executes a java command on the local system.  This is one of the two
     * methods callable from tools and RemoteCallable.call(). 
     * @param java The command to be executed
     * @return A handle to the command
     * @throws java.io.IOException  Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public static CommandHandle java(Command java)
            throws IOException, InterruptedException {
        CmdAgentImpl agent = CmdAgentImpl.getHandle();
        if (agent != null) // Running on agent
            return agent.java(java, Invoker.getContextLocation());
        else // Running on master
            return CmdService.getHandle().java(java,
                    Invoker.getContextLocation());
    }

    /**
     * Executes a java command on a remote host.
     *
     * @param host The target machine to execute the command
     * @param java The command to be executed
     * @return A handle to the command
     * @throws java.io.IOException  Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public static CommandHandle java(String host, Command java)
            throws IOException, InterruptedException {
        return CmdService.getHandle().java(host, java,
                Invoker.getContextLocation());
    }

    /**
     * Executes a java command on a set of remote hosts.
     *
     * @param hosts The target machines to execute the command
     * @param java  The command to be executed
     * @return Handles to the command on each of the target machines
     * @throws java.io.IOException  Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public static CommandHandle[] java(String[] hosts, Command java)
            throws IOException, InterruptedException {
      return CmdService.getHandle().java(hosts, java,
              Invoker.getContextLocation());
    }

    /**
     * Execute a code block defined as a RemoteCallable on a remote host.
     * The only RunContext methods available to the code block are the local
     * exec and java calls. All other calls to RunContext will result in a
     * NullPointerException.
     * @param host The remote host
     * @param callable The callable defining the code block
     * @return The result of the callable
     * @throws Exception An error occurred making the call
     */
    public static <V extends Serializable> V
                        exec(String host, RemoteCallable<V> callable)
            throws Exception {
        return CmdService.getHandle().execute(host, callable,
                Invoker.getContextLocation());
    }

    /**
     * Execute a code block defined as a RemoteCallable on a set of
     * remote hosts. The only RunContext methods available to the code block
     * are the local exec and java calls. All other calls to RunContext will
     * result in a NullPointerException.
     * @param hosts The remote hosts
     * @param callable The callable defining the code block
     * @return The result of the callable
     * @throws Exception An error occurred making the call
     */
    public static <V extends Serializable> List<V>
                        exec(String[] hosts, RemoteCallable<V> callable)
            throws Exception {
        return CmdService.getHandle().execute(hosts, callable,
                Invoker.getContextLocation());
    }

    /**
     * Obtains the name of the master machine.
     *
     * @return The name of the master machine
     */
    public static String getMaster() {
        return CmdService.getHandle().getMaster();
    }

    /**
     * Returns the ip address of the master.
     * @return The ip address of the master
     */
    public static String getMasterIP() {
        return CmdService.getHandle().getMasterIP();
    }

    /**
     * Returns the ip address of the master's interface best used for
     * communicating with the target host.
     * @param agentHost The target host
     * @return The ip address of the master
     */
    public static String getMasterIP(String agentHost) {
        return CmdService.getHandle().getMasterIP(agentHost);
    }

    /**
     * Obtains the actual host name of a host. In most cases, the configuration
     * refers to a host with the interface name. If the host has multiple
     * interfaces, the interface name is not the same as the host name. This
     * method tries to detect the true hostname given a host or interface name.
     * @param host The known host name
     * @return The true host name
     */
    public static String getHostName(String host) {
        return CmdService.getHandle().getHostRoles().getHostByAlias(host);
    }

    /**
     * Pushes a local file on the Faban master to the remote host. If a
     * relative path is given for the filename (local file), it is looked
     * up in the run directory.
     * @param fileName The source file name
     * @param destHost The destination machine
     * @param destFile The destination file name
     * @return true if successful, false otherwise
     */
    public static boolean pushFile(String fileName, String destHost,
                                   String destFile) {
        File src = new File(fileName);
        if (!src.isAbsolute())
            fileName = getOutDir() + fileName;
        return CmdService.getHandle().push(fileName, destHost, destFile);
    }

    /**
     * Deletes a file on a remote host.
     * @param hostName The remote host name
     * @param fileName The file name
     * @return true if successful, false otherwise
     */
    public static boolean deleteFile(String hostName, String fileName) {
        return CmdService.getHandle().delete(hostName, fileName);
    }

    /**
     * Deletes files on a target remote host.
     * @param hostName The remote host name
     * @param dir The file name
     * @param filter The file name filter
     * @return true if successful, false otherwise
     */
    public static boolean deleteFiles(String hostName, String dir,
                                      FileFilter filter) {
        return CmdService.getHandle().delete(hostName, dir, filter);
    }

    /**
     * Truncates a file on a remote host.
     * @param hostName The remote host name
     * @param fileName The file name
     * @return true if successful, false otherwise
     */
    public static boolean truncateFile(String hostName, String fileName) {
        return CmdService.getHandle().truncate(hostName, fileName);
    }

    /**
     * Gets/copies a file from a remote host. If a relative path is given for
     * localFileName, the file will be placed in the current run output
     * directory.
     * @param hostName The remote host name
     * @param fileName The file name on the remote host
     * @param localFileName The target file name on the local host
     * @return true if successful, false otherwise
     */
    public static boolean getFile(String hostName, String fileName,
                                  String localFileName) {
        File dest = new File(localFileName);
        if (!dest.isAbsolute())
            localFileName = getOutDir() + localFileName;        
        return CmdService.getHandle().get(hostName, fileName, localFileName);
    }

    /**
     * Checks whether the given remote file exists.
     * @param hostName The host name to check.
     * @param fileName The file name to test.
     * @return true if exists, false otherwise.
     */
    public static boolean exists(String hostName, String fileName) {
        CmdService cs = CmdService.getHandle();
        return cs.doesFileExist(hostName, fileName);
    }

    /**
     * Checks whether the given remote file exists and is a normal file.
     * @param hostName The host name to check.
     * @param fileName The file name to test.
     * @return true if file is a normal file, false otherwise.
     */
    public static boolean isFile(String hostName, String fileName) {
        CmdService cs = CmdService.getHandle();
        return cs.isFile(hostName, fileName);
    }

    /**
     * Checks whether the given remote file exists and is a directory.
     * @param hostName The host name to check.
     * @param fileName The file name to test.
     * @return true if file is a directory, false otherwise.
     */
    public static boolean isDirectory(String hostName, String fileName) {
        CmdService cs = CmdService.getHandle();
        return cs.isDirectory(hostName, fileName);        
    }

    /**
     * Reads a file from a remote host and writes the contents to a
     * given output stream. This method is useful for filtering the content
     * of a remote file through a filtering stream.
     * @param hostName The remote host name
     * @param fileName The file name on the remote host
     * @param stream The stream to output the data to
     * @return true if successful, false otherwise
     */
    public static boolean writeFileToStream(String hostName, String fileName,
                                            OutputStream stream) {
        return CmdService.getHandle().copyToStream(hostName, fileName, stream);
    }
}
