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
 * $Id: AgentBootstrap.java,v 1.1 2007/05/24 01:04:36 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import com.sun.faban.common.Registry;
import com.sun.faban.common.RegistryLocator;
import com.sun.faban.harness.common.Config;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.RMISecurityManager;
import java.rmi.server.RMISocketFactory;
import java.util.logging.Logger;

/**
 * Bootstrap class for the CmdAgent and FileAgent
 */
public class AgentBootstrap {

    private static Logger logger =
                            Logger.getLogger(AgentBootstrap.class.getName());
    static String host;
    static String ident;
    static String master;
    static Registry registry;
    static String javaHome;
    // Initialize it to make sure it doesn't end up a 'null'
    static String jvmOptions = " ";
    static CmdAgent cmd;

    public static void main(String[] args) {
        System.setSecurityManager (new RMISecurityManager());

        if (args.length < 4) {
            String usage = "Usage: AgentBootstrap <cmdagent_machine_name> " +
                    "<master_host_interface_name> <master_local_hostname> " +
                    "<java_home> <optional_jvm_arguments>";
            logger.severe(usage);
            System.err.println(usage);
            System.exit(-1);
        }

        try {
            String hostname = args[0];
            master = args[1];
            String masterLocal = args[2];
            javaHome = args[3];

            String downloadURL = null;
            String benchName = null;
            // There may be optional JVM args
            if(args.length > 4) {
                for(int i = 4; i < args.length; i++)
                    if(args[i].startsWith("faban.download")) {
                        downloadURL = args[i].substring(
                                args[i].indexOf('=') + 1);
                    }else if (args[i].startsWith("faban.benchmarkName")) {
                        benchName = args[i].substring(args[i].indexOf('=') + 1);
                    } else if (args[i].indexOf("faban.logging.port") != -1) {
                        jvmOptions = jvmOptions + ' ' + args[i];
                        Config.LOGGING_PORT = Integer.parseInt(
                                args[i].substring(args[i].indexOf("=") + 1));
                    } else if(args[i].indexOf("faban.registry.port") != -1) {
                        jvmOptions = jvmOptions + " " + args[i];
                        Config.RMI_PORT = Integer.parseInt(
                                args[i].substring(args[i].indexOf("=") + 1));
                    } else {
                        jvmOptions = jvmOptions + ' ' + args[i];
                    }
            }
            logger.finer("JVM options for child processes:" + jvmOptions);

            RMISocketFactory.setSocketFactory(
                                new AgentSocketFactory(master, masterLocal));
            // Get hold of the registry
            registry = RegistryLocator.getRegistry(master, Config.RMI_PORT);
            logger.fine("Succeeded obtaining registry.");

            // host and ident will be unique
            host = InetAddress.getLocalHost().getHostName();

            // Sometimes we get the host name with the whole domain baggage.
            // The host name is widely used in result files, tools, etc. We
            // do not want that baggage. So we make sure to crop it off.
            // i.e. brazilian.sfbay.Sun.COM should just show as brazilian.
            int dotIdx = host.indexOf('.');
            if (dotIdx > 0)
                host = host.substring(0, dotIdx);

            ident = Config.CMD_AGENT + "@" + host;

            // Make sure there is only one agent running in a machine
            CmdAgent agent = (CmdAgent)registry.getService(ident);

            if((agent != null) && (!host.equals(hostname))){
                // re-register the agents with the 'hostname'
                registry.register(Config.CMD_AGENT + "@" + hostname, agent);
                logger.fine("Succeeded re-registering " + Config.CMD_AGENT +
                                                            "@" + hostname);
                FileAgent f = (FileAgent)registry.getService(Config.FILE_AGENT +
                                                            "@" + host);
                registry.register(Config.FILE_AGENT + "@" + hostname, f);
                logger.fine("Succeeded re-registering " + Config.FILE_AGENT +
                                                            "@" + hostname);
            }
            else {
                new BenchmarkLoader().loadBenchmark(benchName, downloadURL);
                cmd = new CmdAgentImpl(benchName);

                registry.register(ident, cmd);

                logger.fine("Succeeded registering " + ident);

                // Register it with the 'hostname' also if host != hostname
                if(!host.equals(hostname))
                    registry.register(Config.CMD_AGENT + "@" + hostname, cmd);

                if(host.equals(master)) {
                    ident = Config.CMD_AGENT;
                    registry.register(ident, cmd);
                } else if (sameHost(host, master)) {
                    ident = Config.CMD_AGENT;
                    registry.register(ident, cmd);
                }

                // Create and register FileAgent
                FileAgent f = new FileAgentImpl();
                registry.register(Config.FILE_AGENT + "@" + host, f);
                logger.fine("Succeeded registering " +
                        Config.FILE_AGENT + "@" + host);

                // Register it with the 'hostname' also if host != hostname
                if(!host.equals(hostname))
                    registry.register(Config.FILE_AGENT + "@" + hostname, f);

                // Register a blank Config.FILE_AGENT for the master's
                // file agent.
                if (sameHost(host, master))
                    registry.register(Config.FILE_AGENT, f);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private static boolean sameHost(String host1, String host2) {
        InetAddress[] host1Ip = new InetAddress[0];
        try {
            host1Ip = InetAddress.getAllByName(host1);
        } catch (UnknownHostException e) {
            logger.severe("Host " + host1 + " not found.");
            return false;
        }
        InetAddress[] host2Ip = new InetAddress[0];
        try {
            host2Ip = InetAddress.getAllByName(host2);
        } catch (UnknownHostException e) {
            logger.severe("Host " + host2 + " not found.");
            return false;
        }
        for (int i = 0; i < host1Ip.length; i++) {
            for (int j = 0; j < host2Ip.length; j++) {
                if (host1Ip[i].equals(host2Ip[j]))
                    return true;
            }
        }
        return false;
    }
}
