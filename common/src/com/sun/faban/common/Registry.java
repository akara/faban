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

import java.rmi.RemoteException;
import java.rmi.Remote;


/**
 * The methods in this interface are the public face of Registry
 * The Registry is the single remote object that runs on the master
 * machine and with which all other instances of remote servers reregister.
 * A remote reference to any remote service is obtained by the GUI as 
 * well as the Engine through the registry. There is only one remote server
 * object (the Registry) that is on the master machine. Once a reference to
 * the Registry is obtained by the client, it should use the
 * 'getReference' method to obtain a reference to any type of
 * remote server.<p>
 *
 * Although the Registry implementation uses rmi underneath, the rmi registry
 * is fully encapsulated inside the Registry and RegistryLocator to avoid any
 * confusion in the agent programs and other programs accessing the registry.
 *
 * @author Shanti Subrmanyam
 */
public interface Registry extends Remote {

    /**
     * Registers service with Registry.
     * The service driverName is of the form <driverName>@<host>
     * For example, a CmdAgent will reregister itself as CmdAgent@<host>
     * so all CmdAgents on different machines can be uniquely
     * identified by driverName.
     * @param name public driverName of service
     * @param service Remote reference to service
     * @return true if registration succeeded, false if there is already
     *         an object registered by this name.
     * @throws RemoteException A network error occurred
     */
    public boolean register(String name, Remote service) throws RemoteException;


    /**
     * Registers service with Registry.
     * The service driverName is of the form <driverName>@<host>
     * For example, a CmdAgent will reregister itself as CmdAgent@<host>
     * so all CmdAgents on different machines can be uniquely
     * identified by driverName.
     * @param type of service
     * @param name of service
     * @param service Remote reference to service
     * @return true if registration succeeded, false if there is already
     *         an object registered by this name.
     * @throws RemoteException A network error occurred
     */
    public boolean register(String type, String name, Remote service)
            throws RemoteException;

    /**
     * Re-registers service with Registry, replacing old entry if exists.
     * The service driverName is of the form <driverName>@<host>
     * For example, a CmdAgent will reregister itself as CmdAgent@<host>
     * so all CmdAgents on different machines can be uniquely
     * identified by driverName.
     * @param name public driverName of service
     * @param service Remote reference to service
     * @throws RemoteException A network error occurred
     */
    public void reregister(String name, Remote service) throws RemoteException;

    /**
     * Re-registers service with Registry, replacing old entry if exists.
     * The service driverName is of the form <driverName>@<host>
     * For example, a CmdAgent will reregister itself as CmdAgent@<host>
     * so all CmdAgents on different machines can be uniquely
     * identified by driverName.
     * @param type of service
     * @param name of service
     * @param service Remote reference to service
     * @throws RemoteException A network error occurred
     */
    public void reregister(String type, String name, Remote service)
            throws RemoteException;

    /**
     * Unregisters service from Registry.
     * The registry removes this service from its list and clients
     * can no longer access it. This method is typically called when
     * the service exits.
     * @param name public driverName of service
     * @throws RemoteException A network error occurred
     */
    public void unregister(String name) throws RemoteException;

    /**
     * Unregisters service from Registry.
     * The registry removes this service from its list and clients
     * can no longer access it. This method is typically called when
     * the service exits.
     * @param type of service
     * @param name public driverName of service
     * @throws RemoteException A network error occurred
     */
    public void unregister(String type, String name) throws RemoteException;
    
    /**
     * Get reference to the service from the registry.
     * The registry searches in its list of registered services
     * and returns a remote reference to the requested one.
     * The service driverName is of the form <driverName>@<host>
     * @param name public driverName of service
     * @return remote reference
     * @throws RemoteException A network error occurred
     */
    public Remote getService(String name) throws RemoteException;

    /**
     * Get reference to service from registry.
     * The registry searches in its list of registered services
     * and returns a remote reference to the requested one.
     * The service driverName is of the form <driverName>@<host>
     * @param type of service
     * @param name public driverName of service
     * @return remote reference
     * @throws RemoteException A network error occurred
     */
    public Remote getService(String type, String name) throws RemoteException;
    
    /**
     * Get all references to a type of services from registry.
     * The registry searches in its list of registered services
     * and returns all  remote references to the requested type.
     * The service driverName is of the form <driverName>@<host>
     * @param type of service
     * @return remote references
     * @throws RemoteException A network error occurred
     */
    public Remote[] getServices(String type) throws RemoteException;
    
    /**
     * Get the number of registered services of a given type.
     * @param type The type of service
     * @return The number of registered services
     * @throws RemoteException A network error occurred
     */
    public int getNumServices(String type) throws RemoteException;

    /**
     * Kill is called to exit the RMI registry and Registry.
     * @throws RemoteException A network error occurred
     */
    public void kill() throws RemoteException;
}
