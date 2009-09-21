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

import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This class implements the Registry interface
 * The Registry is the single remote object that runs on the master
 * machine and with which all other instances of remote servers reregister.
 * A remote reference to any remote service is obtained by the GUI as
 * well as the Engine through the registry. There is only one remote
 * server object (the Registry) that is known by rmiregistry running
 * on the master machine. Once a reference to the Registry is obtained
 * by the client, it should use the 'getReference' method to obtain a
 * reference to any type of remote server.<p>
 *
 * Although the Registry implementation uses rmi underneath, the rmi registry
 * is fully encapsulated inside the Registry and RegistryLocator to avoid any
 * confusion in the agent programs and other programs accessing the registry.
 *
 * @author Shanti Subramanyam
 */
public class RegistryImpl extends UnicastRemoteObject implements Registry {

    // This field is a legal requirement and serves no other purpose.
    static final String COPYRIGHT =
            "Copyright \251 2006-2009 Sun Microsystems, Inc., 4150 Network " +
            "Circle, Santa Clara, California 95054, U.S.A. All rights " +
            "reserved.\nU.S. Government Rights - Commercial software. " +
            "Government users are subject to the Sun Microsystems, Inc. " +
            "standard license agreement and applicable provisions of the FAR " +
            "and its supplements.\n" +
            "Use is subject to license terms.\n" +
            "This distribution may include materials developed by third " +
            "parties.\n" +
            "Sun,  Sun Microsystems,  the Sun logo and  Java are trademarks " +
            "or registered trademarks of Sun Microsystems, Inc. in the U.S. " +
            "and other countries.\n" +
            "Apache is a trademark of The Apache Software Foundation, and is " +
            "used with permission.\n" +
            "This product is covered and controlled by U.S. Export Control " +
            "laws and may be subject to the export or import laws in other " +
            "countries.  Nuclear, missile, chemical biological weapons or " +
            "nuclear maritime end uses or end users, whether direct or " +
            "indirect, are strictly prohibited.  Export or reexport to " +
            "countries subject to U.S. embargo or to entities identified on " +
            "U.S. export exclusion lists, including, but not limited to, the " +
            "denied persons and specially designated nationals lists is " +
            "strictly prohibited.\n" +
            "\n" +
            "Copyright \251 2006-2009 Sun Microsystems, Inc., 4150 Network " +
            "Circle, Santa Clara, California 95054, Etats-Unis. Tous droits " +
            "r\351serv\351s.\n" +
            "L'utilisation est soumise aux termes de la Licence.\n" +
            "Cette distribution peut comprendre des composants " +
            "d\351velopp\351s par des tierces parties.\n" +
            "Sun,  Sun Microsystems,  le logo Sun et  Java sont des marques " +
            "de fabrique ou des marques d\351pos\351es de " +
            "Sun Microsystems, Inc. aux Etats-Unis et dans d'autres pays.\n" +
            "Apache est une marque d\264Apache Software Foundation, utilis\351e " +
            "avec leur permission.\n" +
            "Ce produit est soumis \340 la l\351gislation am\351ricaine " +
            "en mati\350re de contr\364le des exportations et peut \352tre " +
            "soumis \340 la r\350glementation en vigueur dans d'autres pays " +
            "dans le domaine des exportations et importations. Les " +
            "utilisations, ou utilisateurs finaux, pour des armes " +
            "nucl\351aires, des missiles, des armes biologiques et chimiques " +
            "ou du nucl\351aire maritime, directement ou indirectement, sont " +
            "strictement interdites. Les exportations ou r\351exportations " +
            "vers les pays sous embargo am\351ricain, ou vers des entit\351s " +
            "figurant sur les listes d'exclusion d'exportation " +
            "am\351ricaines, y compris, mais de mani\350re non exhaustive, " +
            "la liste de personnes qui font objet d'un ordre de ne pas " +
            "participer, d'une fa\347on directe ou indirecte, aux " +
            "exportations des produits ou des services qui sont r\351gis par " +
            "la l\351gislation am\351ricaine en mati\350re de contr\364le " +
            "des exportations et la liste de ressortissants sp\351cifiquement " +
            "d\351sign\351s, sont rigoureusement interdites.\n";

    private static final long serialVersionUID = 20070523L;
    
    static int rmiPort = RegistryLocator.DEFAULT_PORT;
    private HashMap<String, Remote> servicesTable =
            new HashMap<String, Remote>();
    private HashMap<String, HashMap<String, Remote>> servicesTypeTable =
            new HashMap<String, HashMap<String, Remote>>();
//    private String className;
    private static Logger logger =
            Logger.getLogger(RegistryImpl.class.getName());

    private RegistryImpl() throws RemoteException {
        super();
    }

    /**
     * Registers service with Registry.
     * The service driverName is of the form <driverName>@<host>
     * For example, a CmdAgent will reregister itself as CmdAgent@<host>
     * so all CmdAgents on different machines can be uniquely
     * identified by driverName.
     *
     * @param name    public driverName of service
     * @param service Remote reference to service
     * @return true if registration succeeded, false if there is already
     *         an object registered by this name.
     */
    public synchronized boolean register(String name, Remote service) {

        logger.fine("Registry: Registering " + name +
                " on machine " + getCaller());
        if (servicesTable.get(name) == null) {
            servicesTable.put(name, service);
            return true;
        } else {
            logger.fine("Failed registering. Service " + name +
                        " already exists");
            return false;
        }
    }

    /**
     * Registers service with Registry.
     * The service driverName is of the form <driverName>@<host>
     * For example, a CmdAgent will reregister itself as CmdAgent@<host>
     * so all CmdAgents on different machines can be uniquely
     * identified by driverName.
     *
     * @param type    of service
     * @param name    of service
     * @param service Remote reference to service
     * @return true if registration succeeded, false if there is already
     *         an object registered by this name.
     */
    public synchronized boolean register(String type, String name,
                                         Remote service) {
        if (service == null)
            throw new NullPointerException("Type: " + type + ", Name: " +
                    name + "| Service reference is null");
        // First check if the type of service exists
        HashMap<String, Remote> h = servicesTypeTable.get(type);

        if (h == null) {
            h = new HashMap<String, Remote>();
            servicesTypeTable.put(type, h);
        } else if (h.get(name) != null) {
            logger.fine("Failed registering. Service " + name +
                        " already exists");
            return false;
        }
        logger.fine("Registry: Registering " + name +
                " on machine " + getCaller());
        h.put(name, service);
        return true;
    }


    /**
      * Re-registers service with Registry, replacing old entry if exists.
      * The service driverName is of the form &lt;driverName&gt;@&lt;host&gt;
      * For example, a CmdAgent will reregister itself as CmdAgent@&lt;host&gt;
      * so all CmdAgents on different machiens can be uniquely
      * identified by driverName.
      * @param name public driverName of service
      * @param service Remote reference to service
      */
    public synchronized void reregister(String name, Remote service)
    {
        logger.fine("Registry: Registering " + name +
                " on machine " + getCaller());
        servicesTable.put(name, service);
    }

    /**
      * Re-registers service with Registry, replacing old entry if exists.
      * The service driverName is of the form &lt;driverName&gt;@&lt;host&gt;
      * For example, a CmdAgent will reregister itself as CmdAgent@&lt;host&gt;
      * so all CmdAgents on different machiens can be uniquely
      * identified by driverName.
      * @param type of service
      * @param name of service
      * @param service Remote reference to service
      */
    public synchronized void reregister(String type, String name, Remote service) {

        if (service == null)
            throw new NullPointerException("Type: " + type + ", Name: " +
                    name + "| Service reference is null");
        // First check if the type of service exists
        HashMap<String, Remote> h = servicesTypeTable.get(type);

        if (h == null) {
            h = new HashMap<String, Remote>();
            servicesTypeTable.put(type, h);
        }

        logger.fine("Registry: Registering " + name +
                " on machine " + getCaller());
        h.put(name, service);
    }

    /**
      * unregister service from Registry
      * The registry removes this service from its list and clients
      * can no longer access it. This method is typically called when
      * the service exits.
      * @param name public driverName of service
      */
    public synchronized void unregister(String name) {
        servicesTable.remove(name);
    }

    /**
      * unregister service from Registry
      * The registry removes this service from its list and clients
      * can no longer access it. This method is typically called when
      * the service exits.
      * @param type of service
      * @param name public driverName of service
      */
    public synchronized void unregister(String type, String name) {
        // First check if the type of service exists
        HashMap<String, Remote> h = servicesTypeTable.get(type);

        if (h == null) {
            logger.warning("Registry.unregister : " +
                    "Cannot find Service type : " + type);
        }
        else {
            h.remove(name);
        }
    }


    /**
      * get reference to service from Registry
      * The registry searches in its list of registered services
      * and returns a remote reference to the requested one.
      * The service driverName is of the form <driverName>@<host>
      * @param name public driverName of service
      * @return remote reference
      */
    public synchronized Remote getService(String name) {
        return servicesTable.get(name);
    }

    /**
      * get reference to service from Registry
      * The registry searches in its list of registered services
      * and returns a remote reference to the requested one.
      * The service driverName is of the form <driverName>@<host>
      * @param type of service
      * @param name public driverName of service
      * @return remote reference
      */
    public synchronized Remote getService(String type, String name) {
        Remote r = null;
        // First check if the type of service exists
        HashMap<String, Remote> h = servicesTypeTable.get(type);

        if (h == null) {
            logger.warning("Registry.getService : " +
                    "Cannot find Service type : " + type);
        }
        else {
            r = h.get(name);
        }
        return r;
    }

    /**
      * get all references to a type of services from Registry
      * The registry searches in its list of registered services
      * and returns all  remote references to the requested type.
      * The service driverName is of the form <driverName>@<host>
      * @param type of service
      * @return remote references
      */
    public synchronized Remote[] getServices(String type) {
        Remote[] r = null;
        // First check if the type of service exists
        HashMap<String, Remote> h = servicesTypeTable.get(type);

        if (h == null) {
            logger.warning("Registry.getServices : " +
                    "Cannot find Service type : " + type);
        }
        else {
            r = new Remote[h.size()];
            r = h.values().toArray(r);
        }
        return r;
    }

    /**
      * Get the number of registered Services of a type.
      * @param type of service
      * @return int number of registered services
      */
    public synchronized int getNumServices(String type) {
        // First check if the type of service exists
        HashMap<String, Remote> h = servicesTypeTable.get(type);
        int i = 0;
        if (h == null) {
            logger.warning("Registry.getNumServices : " +
                    "Cannot find Service type : " + type);
        }
        else {
            i = h.size();
        }
        return i;
    }

    // Get the caller
    private String getCaller() {
        String s = null;

        try {
            s = getClientHost();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return s;
    }

    /**
     * Kill is called to exit the RMI registry and Registry.
     */
    public void kill() {
        logger.info("Unregistering Services");

        for (Iterator<String> iter = servicesTable.keySet().iterator();
             iter.hasNext();) {
            logger.fine("Unregistering " + iter.next());
            iter.remove();
        }

        for (Iterator<String> iter = servicesTypeTable.keySet().iterator();
             iter.hasNext();) {
            logger.fine("Unregistering " + iter.next());
            iter.remove();
        }

        // *** This is to gracefully return from this method.
        // *** The Agent will exit after 5 seconds
        // *** If the System.exit(0) is called in this method
        // *** the Service will get a RemoteException
        Thread exitThread = new Thread() {
            public void run() {
                try {
                    Thread.sleep(5000);
                    System.exit(0);
                }
                catch(Exception e) {}
            }
        };
        exitThread.start();
        logger.info("Registry will exit in 5 secs");
    }

    /**
     * Registration for RMI serving.
     *
     * @param args Command line arguments, not used
     */
    public static void main(String[] args) {

        String portString = System.getProperty("faban.registry.port");
        if (portString != null)
            try {
                rmiPort = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                System.err.println("Property faban.registry.port " +
                        e.getMessage());
                e.printStackTrace();
                System.exit(-1);
            }

        java.rmi.registry.Registry rmiRegistry = null;
        try {
            rmiRegistry = LocateRegistry.createRegistry(rmiPort);
            logger.fine("Registry listening on port " +
                    rmiPort + ".");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception starting registry.", e);
            System.exit(-1);
        }

        System.setSecurityManager (new RMISecurityManager());
        try {
            Registry registry = new RegistryImpl();
            rmiRegistry.bind(RegistryLocator.BIND_NAME, registry);
//            debug.println(3, "Binding registry to " + s);
            // If the debug level is set to too high and if this is not
            // printed driver will hang !!!!
            logger.info("Registry started.");
        }
        catch (Exception e) {
            e.printStackTrace();
            try {
                rmiRegistry.unbind(RegistryLocator.BIND_NAME);
            }
            catch (Exception ei) { }
            System.exit(-1);
        }
    }
}

