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
 * $Id: CommandHandle.java,v 1.2 2006/06/29 19:38:35 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.io.IOException;

/**
 * A handle interface for a command executed on a local or remote system.
 *
 * @author Akara Sucharitakul
 */
public interface CommandHandle extends Remote {


    /**
     * Forfully terminates the command.
     */
    public void destroy() throws RemoteException;

    /**
     * Waits for the command to terminate.
     * @throws InterruptedException The waiting thread got interrupted
     * @throws RemoteException A network error occurred
     */
    public void waitFor() throws InterruptedException, RemoteException;

    /**
     * Obtains the exit value of the command.
     * @return The exit value of the command
     * @throws IllegalStateException If the command has not yet terminated
     * @throws RemoteException A network error occurred
     */
    public int exitValue() throws IllegalStateException, RemoteException;

    /**
     * Obtains the stdout or stderr of the command.
     * @param streamId Command.STDOUT or Command.STDERR
     * @throws java.io.IOException There is an error getting the output
     * @throws IllegalStateException The command is not yet terminated or
     *                               does not record output
     * @throws RemoteException A network error occurred
     */
    byte[] fetchOutput(int streamId) throws IOException, IllegalStateException,
            RemoteException;

}