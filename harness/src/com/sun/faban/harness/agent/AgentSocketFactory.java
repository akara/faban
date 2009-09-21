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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMISocketFactory;

/**
 * This class implements the SocketFactory used by all machines in the
 * SUT to communicate with the master-machine. All machines should use
 * this SocketFactory instead of the RMI default one to take care of
 * RMI problems related to multi-homed hosts.
 *
 * @author Ramesh Ramachandran
 */
public class AgentSocketFactory extends RMISocketFactory implements RMIClientSocketFactory {

    String masterMachine = null;
    String masterLocal = null;
    RMISocketFactory rsf = RMISocketFactory.getDefaultSocketFactory();

    /**
     * Constructor.
     *
     */
    AgentSocketFactory() {
        super();
    }

    /**
     * Constructor with two arguments which are used in making a socket
     * connection to the master-machine.
     *
     * @param masterMachine - the interface name/IP address
     *        of the master-machine to be used to create the socket.
     * @param masterLocal - this is the name of the master machine
     *        by which it knows itself.
     */
    AgentSocketFactory(String masterMachine, String masterLocal) {
        super();
        this.masterMachine = masterMachine;
        this.masterLocal = masterLocal;
    }

    /**
     * Reconfigures the factory for a different Faban master.
     * @param machine The new master machine name or ip address
     * @param localName The master host name i.e. from uname -n
     */
    void setMaster(String machine, String localName) {
        masterMachine = machine;
        masterLocal = localName;
    }

    /**
     * Overrides the createSocket method of the RMISocketFactory 
     * superclass. It uses the superclass SocketFactory methods to 
     * create a socket to the appropriate master-machine interface.
     *
     * @param host - the destination host.
     * @param port - the port number to connect to.
     * @return Socket - returns a client socket representing this 
     *         connection.
     * @throws IOException Error creating the socket
     * @see java.rmi.server.RMISocketFactory
     *
     */
    public Socket createSocket(String host, int port) throws IOException {
        if (host.equals(masterLocal)) {
            return rsf.createSocket(masterMachine, port);
        }
        else {
            return rsf.createSocket(host, port);
        }
    }

    /**
     * This method just calls the superClass createServerSocket method
     * to create a ServerSocket.
     *
     * @param port The port to bind
     * @return The resulting server socket
     * @throws IOException Error creating the socket
     * @see java.rmi.server.RMISocketFactory
     */
    public ServerSocket createServerSocket(int port) throws IOException {
        return rsf.createServerSocket(port);
    }

}
