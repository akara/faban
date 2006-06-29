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
 * $Id: Registry.java,v 1.1 2006/06/29 18:51:31 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import java.rmi.RemoteException;
import java.rmi.Remote;


/**
 * The methods in this interface are the public face of Registry
 * The Registry is the single remote object that runs on the master
 * machine and with which all other instances of remote servers register.
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
     * register service with Registry
     * The service driverName is of the form <driverName>@<host>
     * For example, a CmdAgent will register itself as CmdAgent@<host>
     * so all CmdAgents on different machiens can be uniquely
     * identified by driverName.
     * @param name public driverName of service
     * @param service Remote reference to service
     */
    public void register(String name, Remote service) throws RemoteException;

    /**
           * register service with Registry
           * The service driverName is of the form <driverName>@<host>
           * For example, a CmdAgent will register itself as CmdAgent@<host>
           * so all CmdAgents on different machiens can be uniquely
           * identified by driverName.
           * @param type of service
           * @param name of service
           * @param service Remote reference to service
           */
    public void register(String type, String name, Remote service) throws RemoteException;

    /**
     * unregister service from Registry
     * The registry removes this service from its list and clients
     * can no longer access it. This method is typically called when
     * the service exits.
     * @param name public driverName of service
     */
    public void unregister(String name) throws RemoteException;

    /**
           * unregister service from Registry
           * The registry removes this service from its list and clients
           * can no longer access it. This method is typically called when
           * the service exits.
           * @param type of service
           * @param name public driverName of service
           */
    public void unregister(String type, String name) throws RemoteException;
    
    /**
     * get reference to service from Registry
     * The registry searches in its list of registered services
     * and returns a remote reference to the requested one.
     * The service driverName is of the form <driverName>@<host>
     * @param name public driverName of service
     * @return remote reference
     */
    public Remote getService(String name) throws RemoteException;

    /**
           * get reference to service from Registry
           * The registry searches in its list of registered services
           * and returns a remote reference to the requested one.
           * The service driverName is of the form <driverName>@<host>
           * @param type of service
           * @param name public driverName of service
           * @return remote reference
           */
    public Remote getService(String type, String name) throws RemoteException;
    
    /**
           * get all references to a type of services from Registry
           * The registry searches in its list of registered services
           * and returns all  remote references to the requested type.
           * The service driverName is of the form <driverName>@<host>
           * @param type of service
           * @return remote references
           */
    public Remote[] getServices(String type) throws RemoteException;
    
    /**
        * Get the number of registered Services of a type
        * @param type of service
        * @return int number of registered services
        */
    public int getNumServices(String type) throws RemoteException;

    /**
     * Kill is called to exit the RMI registry and Registry
     */
    public void kill() throws RemoteException;
}
