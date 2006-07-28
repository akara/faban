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
 * $Id: RunContext.java,v 1.2 2006/07/28 07:33:46 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.harness.engine.CmdService;
import com.sun.faban.harness.engine.RunFacade;

import java.io.IOException;

/**
 * The RunContext provides callbacks into the harness and the run environment.
 * All methods are static and it is suitable for static imports making calls
 * much shorter and easier.
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
     * Obtains the output directory for this run.
     * @return The run output directory
     */
    public static String getOutDir() {
        return RunFacade.getInstance().getOutDir();
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
     * Executes a command on the master.
     * @param c The command to be executed
     * @return  A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public static CommandHandle exec(Command c)
            throws IOException, InterruptedException {
        return CmdService.getHandle().execute(c);
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
        return CmdService.getHandle().execute(host, c);
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
        return CmdService.getHandle().execute(hosts, c);
    }

    /**
     * Executes a java command on the master.
     *
     * @param java The command to be executed
     * @return A handle to the command
     * @throws java.io.IOException  Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public static CommandHandle java(Command java) throws IOException, InterruptedException {
        return CmdService.getHandle().java(java);
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
    public static CommandHandle java(String host, Command java) throws IOException, InterruptedException {
        return CmdService.getHandle().execute(host, java);
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
    public static CommandHandle[] java(String[] hosts, Command java) throws IOException, InterruptedException {
      return CmdService.getHandle().execute(hosts, java);
    }

    /**
     * Obtains the name of the master machine.
     *
     * @return The name of the master machine
     */
    public static String getMaster() {
        return CmdService.getHandle().getMaster();
    }
}
