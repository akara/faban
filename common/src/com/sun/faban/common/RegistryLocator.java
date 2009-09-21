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
package com.sun.faban.common;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The registry locator is the single access point for clients to locate
 * and access the Faban registry either on the local or remote system.<p>
 *
 * Although the Registry implementation uses rmi underneath, the rmi registry
 * is fully encapsulated inside the Registry and RegistryLocator to avoid any
 * confusion in the agent programs and other programs accessing the registry.
 *
 * @author Akara Sucharitakul
 */
public class RegistryLocator {

    static Logger logger = Logger.getLogger(RegistryLocator.class.getName());

    /**
     * The default registry port for Faban - 9998.
     */
    public static final int DEFAULT_PORT = 9998;

    /**
     * The rmi registry bind name used to find the registry - FabanRegistry.
     */
    static final String BIND_NAME = "FabanRegistry";

    private static int getPort() {
        int port = DEFAULT_PORT;
        String portString = System.getProperty("faban.registry.port");
        if (portString != null)
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                logger.log(Level.SEVERE, "Property faban.registry.port " +
                        e.getMessage(), e);
                throw e;
            }
        return port;
    }

    /**
     * Obtains a reference to the registry on the local system using the
     * system property faban.registry.port as the port.
     * @return A remote reference to the registry
     * @throws RemoteException Cannot access the registry.
     * @throws NotBoundException The registry itself is not bound.
     */
    public static Registry getRegistry()
            throws RemoteException, NotBoundException {

        int port = getPort();
        logger.fine("Obtaining registry at localhost:" + port);
        java.rmi.registry.Registry rmiRegistry =
                                LocateRegistry.getRegistry(port);
        return (Registry) rmiRegistry.lookup(BIND_NAME);
    }

    /**
     * Obtains a reference to the registry on the remote master system using the
     * system property faban.registry.port as the port.
     * @param master The master host name
     * @return A remote reference to the registry
     * @throws RemoteException Cannot access the registry
     * @throws NotBoundException The registry itself is not bound
     */
    public static Registry getRegistry(String master)
            throws RemoteException, NotBoundException {

        int port = getPort();
        logger.fine("Obtaining registry at " + master + ':' + port);
        java.rmi.registry.Registry rmiRegistry =
                                LocateRegistry.getRegistry(master, port);
        return (Registry) rmiRegistry.lookup(BIND_NAME);
    }

    /**
     * Obtains a reference to the registry on the local system using the
     * port specified.
     * @param port The port to connect to the registry
     * @return A remote reference to the registry
     * @throws RemoteException Cannot access the registry.
     * @throws NotBoundException The registry itself is not bound.
     */
    public static Registry getRegistry(int port)
            throws RemoteException, NotBoundException {

        logger.fine("Obtaining registry at localhost:" + port);
        java.rmi.registry.Registry rmiRegistry =
                                LocateRegistry.getRegistry(port);
        return (Registry) rmiRegistry.lookup(BIND_NAME);
    }

    /**
     * Obtains a reference to the registry on the remote master system using the
     * port specified.
     * @param master The master host name
     * @param port The port to connect to the registry
     * @return A remote reference to the registry
     * @throws RemoteException Cannot access the registry
     * @throws NotBoundException The registry itself is not bound
     */
    public static Registry getRegistry(String master, int port)
            throws RemoteException, NotBoundException {

        logger.fine("Obtaining registry at " + master + ':' + port);
        java.rmi.registry.Registry rmiRegistry =
                                LocateRegistry.getRegistry(master, port);
        return (Registry) rmiRegistry.lookup(BIND_NAME);
    }

}
