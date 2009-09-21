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
package com.sun.faban.harness.util;

import com.sun.faban.harness.ConfigurationException;
import java.util.concurrent.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.*;
import java.io.IOException;

/**
 * The InterfaceProbe probes interfaces and finds the best interface to
 * communicate with an IP address or host name.
 */
public class InterfaceProbe {

    Logger logger = Logger.getLogger(this.getClass().getName());
    ExecutorService threadPool = null;
    static final int TIMEOUT = 10000;
    static final int LOCAL_TIMEOUT = 5000;

    // Parallel probing is still buggy, especially for ICMP echo (root ping)
    // mode. So we'll not use it just yet. This seems like a JDK bug.
    // Once we get it working in all instances, we'll change this.
    static final int PARALLEL_THRESHOLD = Integer.MAX_VALUE;

    ArrayList<NetworkInterface> ifList;
    List<IFAddressInfo> ifAInfoList;

    /**
     * Unit tests the interface probe.
     * @param args The commmand line arguments
     */
    public static void main(String[] args) {
        try {
            ArrayList<String> hosts = new ArrayList<String>();
            for (String arg : args) {
                hosts.add(arg);
            }
            InterfaceProbe ifs = new InterfaceProbe();
            List<Route> rtes = ifs.getRoutes(hosts);
            System.out.println("destination\tiface\tifAddress\tttl\treachable");
            for (Route rte : rtes) {
                System.out.print(rte.host + '\t');
                if (rte.nif != null)
                    System.out.print(rte.nif.getDisplayName() + '\t');
                else
                    System.out.print("-\t");
                if (rte.nifAddress != null)
                    System.out.print(rte.nifAddress);
                else
                    System.out.print("-\t");
                System.out.println("\t" + rte.ttl + '\t' + rte.reachable);
            }
            System.exit(0);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    static class Route {
        String host;
        InetAddress target;
        NetworkInterface nif;
        String nifAddress;
        short ttl = 0;
        boolean reachable = false;
    }

    static class IFAddressInfo {
        NetworkInterface nif;
        InterfaceAddress nifAddress;
        short prefixLength;
        byte[] netAddress;
    }

    /**
     * .
     * @throws java.net.SocketException
     */
    public InterfaceProbe() throws SocketException {
        Enumeration<NetworkInterface> interfaces =
                NetworkInterface.getNetworkInterfaces();
        ifList = new ArrayList<NetworkInterface>();
        addInterfaces(interfaces, ifList);
        ifAInfoList = listIFAddressInfo(ifList);
    }

    /**
     * .
     * @param executor
     * @throws java.net.SocketException
     */
    public InterfaceProbe(ExecutorService executor) throws SocketException {
        this();
        threadPool = executor;
    }

    /**
     * Sets the thread pool to be used for parallel probing.
     * @param executor The thread pool
     */
    public void setExecutorService(ExecutorService executor) {
        threadPool = executor;
    }

    private List<IFAddressInfo> listIFAddressInfo(
                                            List<NetworkInterface> ifList) {
        ArrayList<IFAddressInfo> ifAInfos = new ArrayList<IFAddressInfo>();
        ifSearchLoop:
        for (NetworkInterface nif : ifList) {
            List<InterfaceAddress> ifAddresses =
                    nif.getInterfaceAddresses();
            for (InterfaceAddress ifAddress : ifAddresses) {
                if (ifAddress == null)
                    continue;
                IFAddressInfo ifAInfo = new IFAddressInfo();
                ifAInfo.nif = nif;
                ifAInfo.nifAddress = ifAddress;
                InetAddress ia = ifAddress.getAddress();
                ifAInfo.prefixLength = ifAddress.getNetworkPrefixLength();
                try {
                    ifAInfo.netAddress =
                            getNetworkAddress(ia, ifAInfo.prefixLength);
                } catch (ConfigurationException e) {
                    StringBuilder buffer = new StringBuilder();
                    buffer.append(e.getMessage()).append('\n');
                    buffer = printIfInfo(ifAInfo, buffer);
                    logger.log(Level.WARNING, buffer.toString(), e);
                    continue;
                }
                ifAInfos.add(ifAInfo);
                printIfInfo(ifAInfo);
            }
        }
        return ifAInfos;
    }

    /**
     * Fills the interface maps identifying which interface is to be used
     * to communicate to a particular host.
     * @param hosts The host list
     * @param ifMap The host to interface map to fill
     * @return The host to interface map provided, filled in
     */
    public Map<String, String> getIfMap(Collection<String> hosts,
                                        Map<String, String> ifMap) {

        // Grab only the hosts without interfaces from ifMap.
        HashSet<String> hostsToProbe = new HashSet<String>();
        for (String host : hosts) {
            String iface = ifMap.get(host);
            if (iface == null || iface.length() == 0)
                hostsToProbe.add(host);
        }

        // Obtain the routes
        List<Route> routes = getRoutes(hostsToProbe);

        // Complete the ifMap
        for (Route route : routes)
            ifMap.put(route.host, route.nifAddress);
        return ifMap;
    }

    /**
     * Obtains the route list for a list of hosts.
     * @param hosts The list of hosts
     * @return the corresponding list of routes
     */
    List<Route> getRoutes(Collection<String> hosts) {
        ArrayList<Route> routes = new ArrayList<Route>();
        if (hosts.size() < PARALLEL_THRESHOLD) {
            for (String host : hosts) {
                Route rte = getOrFindRoute(host);
                routes.add(rte);
            }
        } else {
            if (threadPool == null)
                threadPool = Executors.newCachedThreadPool();
            ArrayList<Future<Route>> futures = new ArrayList<Future<Route>>();
            for (String target : hosts)
                futures.add(threadPool.submit(new GetRouteTask(target)));
            for (Future<Route> f : futures) {
                try {
                    routes.add(f.get());
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Route scan interrupted", e);
                } catch (ExecutionException e) {
                    Throwable e2 = e.getCause();
                    logger.log(Level.WARNING, e2.getMessage(), e2);
                }
            }
        }
        return routes;
    }

    private Route getOrFindRoute(String host) {
        Route rte = new Route();
        rte.host = host;
        try {
            rte.target = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING, "Host " + host + " unknown!", e);
        }
        try {
            if (!rte.target.equals(InetAddress.getLocalHost())) {
                getRoute(rte);
                if (rte.nifAddress == null)
                    findRoute(rte);
            } else {
                logger.log(Level.WARNING, "localhost ignored!");
            }
        } catch (UnknownHostException e) {
            logger.log(Level.SEVERE, "localhost unknown!", e);
        }
        return rte;
    }

    class GetRouteTask implements Callable<Route> {

        String target;

        GetRouteTask(String target) {
            this.target = target;
        }

        public Route call() {
            return getOrFindRoute(target);
        }
    }


    private void findRoute(Route rte) {
        // First see whether we can even get to this system
        try {
            if (!rte.target.isReachable(TIMEOUT)) {
                return;
            }
            ArrayList<Route> routeList = new ArrayList<Route>();
            for (NetworkInterface nif : ifList) {

                // Try each interface...
                if (!rte.target.isReachable(nif, 0, TIMEOUT))
                    continue;

                Route rte2 = new Route();
                rte2.host = rte.host;
                rte2.target = rte.target;
                rte2.nif = nif;
                rte2.ttl = Short.MAX_VALUE;
                rte2.reachable = true;
                routeList.add(rte2);
            }

            if (routeList.size() == 0) {
                logger.log(Level.SEVERE, "Target is reachable but not " +
                       "reachable through any listed route. Seems like a bug!");
                return;
            }


            // If we can reach with multiple interfaces, find the best one
            selectLoop:
            while (routeList.size() > 1) {
                int listSize = routeList.size();
                for (int rteIdx = 0; rteIdx < listSize; rteIdx++) {
                    Route rte2 = routeList.get(rteIdx);
                    if (rte2.ttl == Integer.MAX_VALUE)
                        rte2.ttl = 63;
                    else if (rte2.ttl == 0)
                        break selectLoop;
                    if (!rte2.target.isReachable(rte.nif, rte.ttl, TIMEOUT)) {
                        routeList.remove(rteIdx);
                        continue selectLoop;
                    }
                    --rte2.ttl; // Keep decreasing...
                }
            }

            Route rte2 = routeList.get(0); // Even if two of them make it to 0,
            // just choose the first.

            // We populate the value, but do not change the rte reference.
            rte.nif = rte2.nif;
            rte.ttl = rte2.ttl;
            rte.reachable = rte2.reachable;

            // We assume an interface has only one ip address per type.
            List<InterfaceAddress> ifAddresses =
                                            rte.nif.getInterfaceAddresses();
            for (InterfaceAddress ifAddress : ifAddresses)
                if (ifAddress.getAddress().getAddress().length ==
                        rte.target.getAddress().length) {
                    rte.nifAddress = ifAddress.getAddress().getHostAddress();
                    break;
                }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Network error trying to reach " +
                        rte.host + '!', e);
        }
    }

    private void getRoute(Route route) {

        // Scan for a matching interface
        for (IFAddressInfo ifAInfo : ifAInfoList) {
            if (ifAInfo.netAddress.length != route.target.getAddress().length)
                continue;
            byte[] targetNet;
            try {
                targetNet = getNetworkAddress(route.target,
                                              ifAInfo.prefixLength);
            } catch (ConfigurationException e) {
                StringBuilder buffer = new StringBuilder();
                buffer.append(e.getMessage()).append('\n');
                buffer = printIfInfo(ifAInfo, buffer);
                logger.log(Level.WARNING, buffer.toString(), e);
                continue;
            }
            if (Arrays.equals(ifAInfo.netAddress, targetNet)) {
                route.nif = ifAInfo.nif;
                route.nifAddress = ifAInfo.nifAddress.getAddress().
                                                            getHostAddress();
                break;
            }
        }

        // Now try to ping it through the interface.
        try {
            route.reachable =
                    route.target.isReachable(route.nif, 1, LOCAL_TIMEOUT);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Network error trying to reach " +
                        route.host + '!', e);
        }
        return;
    }

    void printIfInfo(IFAddressInfo ifAInfo) {
        if (logger.isLoggable(Level.FINE)) {
            logger.fine(printIfInfo(ifAInfo, new StringBuilder()).toString());
        }
    }

    StringBuilder printIfInfo(IFAddressInfo ifAInfo, StringBuilder buffer) {
        InetAddress ia = ifAInfo.nifAddress.getAddress();
        String ifName = ifAInfo.nif.getDisplayName();
        Formatter fmt = new Formatter(buffer);
        buffer.append(ifName);
        if (ia instanceof Inet4Address) {
            buffer.append("\tv4: " + ia.getHostAddress());
            buffer.append("\n\t\tNetwork " + ifAInfo.prefixLength + " bits\n");
            if (ifAInfo.netAddress != null) {
                int[] netAddressI = new int[ifAInfo.netAddress.length];
                for (int i = 0; i < netAddressI.length; i++) {
                    if (ifAInfo.netAddress[i] < 0) {
                        netAddressI[i] = 256 + ifAInfo.netAddress[i];
                    } else {
                        netAddressI[i] = ifAInfo.netAddress[i];
                    }
                }
                fmt.format("\t\tNet address: %d", netAddressI[0]);
                for (int i = 1; i < ifAInfo.netAddress.length; i++) {
                    fmt.format(".%d", netAddressI[i]);
                }
                buffer.append('\n');
            } else {
                buffer.append("\t\tNet address: null\n");
            }
        } else if (ia instanceof Inet6Address) {
            buffer.append("\tv6: " + ia.getHostAddress());
            buffer.append("\n\t\tNetwork " + ifAInfo.prefixLength + " bits\n");
            if (ifAInfo.netAddress != null) {
            int[] netAddressI = new int[ifAInfo.netAddress.length / 2];
            for (int i = 0; i < netAddressI.length; i++) {
                int au = ifAInfo.netAddress[2 * i];
                if (au < 0)
                    au += 256;
                au <<= 8;
                int au1 = ifAInfo.netAddress[2 * i + 1];
                if (au1 < 0)
                    au1 += 256;
                au |= au1;
                netAddressI[i] = au;
            }
            fmt.format("\t\tNet address: %x", netAddressI[0]);
            for (int i = 1; i < netAddressI.length; i++)
                fmt.format(":%x", netAddressI[i]);
            buffer.append('\n');
            } else {
                buffer.append("\t\tNet address: null\n");
            }
        }
        return buffer;
    }

    private byte[] getNetworkAddress(InetAddress ia, short prefixLen)
            throws ConfigurationException {
        byte[] byteAddress = ia.getAddress();
        short networkBytes = (short) (prefixLen / 8);
        if (networkBytes > byteAddress.length) {
            throw new ConfigurationException(
            "Netmask too long. Network address " + ia + " has " +
                    prefixLen + "bit netmask while having " +
                    byteAddress.length + "bytes in the address. " +
                    "The address is a " + ia.getClass().getName());
        }
        byte[] netAddress = new byte[byteAddress.length];
        int bytePos = 0;
        for (; bytePos < networkBytes; bytePos++)
            netAddress[bytePos] = byteAddress[bytePos];
        short networkBits = (short) (prefixLen % 8);
        if (networkBits > 0) {
            byte mask = Byte.MIN_VALUE;
            for (int bitPos = 1; bitPos < networkBits; bitPos++)
                mask |= mask >>> 1;
            netAddress[bytePos] = (byte) (byteAddress[bytePos] & mask);
        }
        return netAddress;
    }

    private void addInterfaces(Enumeration<NetworkInterface> interfaces,
                                     ArrayList<NetworkInterface> ifList)
            throws SocketException {
        while (interfaces.hasMoreElements()) {
            NetworkInterface nif = interfaces.nextElement();
            if (nif.isUp() && !nif.isLoopback()) {
                ifList.add(nif);
                addInterfaces(nif.getSubInterfaces(), ifList);
            }
        }
    }
}
