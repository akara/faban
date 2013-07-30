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

import com.sun.faban.common.Registry;
import com.sun.faban.common.RegistryLocator;
import com.sun.faban.common.Utilities;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.CmdMap;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.RMISecurityManager;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Bootstrap class for the CmdAgent and FileAgent.
 */
public class AgentBootstrap {

    private static int daemonPort = 9981;

    private static Logger logger =
                            Logger.getLogger(AgentBootstrap.class.getName());
    static AgentSocketFactory socketFactory;
    static String progName;
    static boolean daemon = false;
    static String host;
    static String ident;
    static String master;
    static Registry registry;
    static String javaHome;
    static String downloadURL;
    // Initialize it to make sure it doesn't end up a 'null'
    static ArrayList<String> jvmOptions = new ArrayList<String>();
    static ArrayList<String> extClassPath = new ArrayList<String>();
    static CmdAgentImpl cmd;
    static FileAgentImpl file;
    static final Set<String> registeredNames =
                    Collections.synchronizedSet(new HashSet<String>());

    /**
     * Starts the agent bootstrap.
     * @param args The command line arguments
     */
    public static void main(String[] args) {
        System.setSecurityManager (new RMISecurityManager());

        progName = System.getProperty("faban.cli.command");
        String usage = "Usage: " + progName + " [port]";

        if (args.length < 2) {
            if (args.length == 1) {
                if ("-h".equals(args[0]) || "--help".equals(args[0]) ||
                                            "-?".equals(args[0])) {
                    System.err.println(usage);
                    System.exit(0);
                } else {
                    daemonPort = Integer.parseInt(args[0]);
                }
            }
            startDaemon();
        } else if (args.length > 3) {
            try {
                startAgents(args);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            // We do not expose the start params for agent mode as that
            // is not supposed to be called by the user. The daemon mode
            // has only one optional param - port.
            System.err.println(usage);
            System.exit(-1);
        }
    }

    private static void startDaemon() {
        daemon = true;
        /* Note that the daemon is not designed to accept any concurrency at
         * all and hence the accept/dispatch is not threaded. This is not a
         * bug. It should only receive one and only one connection request per
         * run. Requests to start an agent while one is running will return
         * with an error. We don't care if a concurrent request has to wait.
         * Simplicity is the goal here.
         */
        try {
            ServerSocket serverSocket = new ServerSocket(daemonPort);
            for (;;) {
                Socket socket = serverSocket.accept();
                ObjectInputStream in =
                        new ObjectInputStream(socket.getInputStream());
                OutputStream out = socket.getOutputStream();
                ArrayList<String> argList = null;
                try {
                    argList = (ArrayList<String>) in.readObject();
                } catch (ClassNotFoundException e) {
                    System.err.println("WARNING: Object class not found.");
                    e.printStackTrace();
                    continue;
                }
                int length = argList.size();
                if (length > 0) {
                    System.out.println("Agent(Daemon) starting agent with " +
                            "options: " + argList);
                    Utilities.masterPathSeparator = argList.remove(--length);
                    Utilities.masterFileSeparator = argList.remove(--length);

                    if (length < 4) {
                       out.write("400 ERROR: Inadequate params.".getBytes());
                       continue;
                    }

                    String[] args = new String[length];
                    args = argList.toArray(args);

                    try {
                        startAgents(args);
                        out.write("200 OK".getBytes());
                    } catch (Exception e) {
                        e.printStackTrace();
                        out.write(("500 ERROR: " + e.getMessage()).getBytes());
                    }
                }
                try {
                    in.close();
                } catch (IOException e) {
                }
                try {
                    out.close();
                } catch (IOException e) {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();  // We don't use logger here 'cause we don't
            // know the harness at this time.
            // The logger may not be configured properly.
            System.exit(1);
        }

    }

    private static synchronized void startAgents(String[] args)
            throws Exception {

        String hostname = args[0];
        master = args[1];
        String masterLocal = args[2];
        javaHome = args[3];

        String benchName = null;

        // Setup the basic jvmOptions for this environment which may not
        // be the same as passed down from the master.
        // We need to be careful to escape properties having '\\' on win32
        String escapedHome = Config.FABAN_HOME.replace("\\", "\\\\");
        String fs = File.separatorChar == '\\' ? "\\\\" : File.separator;
        jvmOptions.add("-Dfaban.home=" + escapedHome);
        jvmOptions.add("-Djava.security.policy=" + escapedHome + "config" +
                                                        fs + "faban.policy");
        host = InetAddress.getLocalHost().getHostName();
        jvmOptions.add("-Djava.util.logging.config.file=" + escapedHome +
                                        "config" + fs + "logging." + host +
                                        ".properties");

        ArrayList<String> libPath = new ArrayList<String>();
        String libPrefix = "-Djava.library.path=";

        // There may be optional JVM args
        boolean isClassPath = false;
        if(args.length > 4) {
            for(int i = 4; i < args.length; i++)
                if(args[i].startsWith("faban.download")) {
                    downloadURL = args[i].substring(
                            args[i].indexOf('=') + 1);
                }else if (args[i].startsWith("faban.benchmarkName")) {
                    benchName = args[i].substring(args[i].indexOf('=') + 1);
                } else if (args[i].indexOf("faban.logging.port") != -1) {
                    jvmOptions.add(args[i]);
                    Config.LOGGING_PORT = Integer.parseInt(
                            args[i].substring(args[i].indexOf("=") + 1));
                } else if (args[i].indexOf("faban.registry.port") != -1) {
                    jvmOptions.add(args[i]);
                    Config.RMI_PORT = Integer.parseInt(
                            args[i].substring(args[i].indexOf("=") + 1));
                } else if (args[i].startsWith("-Dfaban.command.buffer=")) {
                    String[] prop = args[i].substring(2).split("=");
                    System.setProperty(prop[0], prop[1]);
                    // Pass it along, too.
                    jvmOptions.add(args[i]);
                } else if ("-server".equals(args[i]) ||
                        "-client".equals(args[i])) { // prepend these options
                    jvmOptions.add(0, args[i]);
                } else if (args[i].startsWith("-Dfaban.home=") ||
                        args[i].startsWith("-Djava.security.policy=") ||
                        args[i].startsWith("-Djava.util.logging.config.file=")){
                    // These are sometimes passed down from the master.
                    // Ignore these. Use our local settings instead.
                    // NOOP
                } else if ("-cp".equals(args[i])) {
                    isClassPath = true;
                } else if ("-classpath".equals(args[i])) {
                    isClassPath = true;
                } else if (isClassPath) {
                    String[] cp = pathSplit(args[i]);
                    for (String cpElement : cp)
                        extClassPath.add(Utilities.convertPath(cpElement));
                    isClassPath = false;
                } else if (args[i].startsWith(libPrefix)) {
                    String[] lp = pathSplit(
                            args[i].substring(libPrefix.length()));
                    for (String lpElement : lp)
                        libPath.add(Utilities.convertPath(lpElement));
                } else {
                    jvmOptions.add(args[i]);
                }
        }

        setLogger();

        // Ensure proper JAVA_HOME by searching for the java executable.
        File java = null;
        File javaBin = new File(javaHome, "bin");
        if (javaBin.isDirectory()) {
            String[] pathExts = CmdMap.getPathExt();
            if (pathExts != null) {
                for (String ext : pathExts) {
                    ext = ext.trim();
                    if (ext == null || ext.length() == 0)
                        continue;
                    File javaPath = new File(javaBin, "java" + ext);
                    if (javaPath.exists()) {
                        java = javaPath;
                        break;
                    }
                }
            }
            if (java == null) {
                java = new File(javaBin, "java");
                if (!java.exists()) {
                    java = null;
                }
            }
        }

        // If not found, just use the process' JAVA_HOME instead.
        if (java == null) {
            String newJavaHome = Utilities.getJavaHome();
            if (!newJavaHome.equals(javaHome)) {
                logger.warning("JAVA_HOME " + javaHome +
                        " does not exist. Using " + newJavaHome + " instead.");
                javaHome = newJavaHome;
            }
        }

        logger.finer("JVM options for child processes:" + jvmOptions);

        // We cannot set the socket factory twice. So we need to reconfigure it.
        if (socketFactory == null) {
            socketFactory = new AgentSocketFactory(master, masterLocal);
            RMISocketFactory.setSocketFactory(socketFactory);
        } else {
            socketFactory.setMaster(master, masterLocal);
        }

        // Get hold of the registry
        registry = RegistryLocator.getRegistry(master, Config.RMI_PORT);
        logger.fine("Succeeded obtaining registry.");

        // Sometimes we get the host name with the whole domain baggage.
        // The host name is widely used in result files, tools, etc. We
        // do not want that baggage. So we make sure to crop it off.
        // i.e. brazilian.sfbay.Sun.COM should just show as brazilian.

        // Keep just the one dot after the host. In example above, brazilian.sfbay
        logger.finer("Original host is " + host);
        int dotIdx = host.indexOf(".");
        int nextDotIdx = host.substring(dotIdx+1).indexOf('.');
        if (nextDotIdx > 0)
            host = host.substring(0, dotIdx + nextDotIdx + 1);
        logger.finer("dotIdx is " + dotIdx + ", nextDotIdx is " + nextDotIdx +
                ", Modified Host is " + host);

        String shortHost = host;
        if (dotIdx > 0) {
          shortHost = host.substring(0, dotIdx);   // eliminate all dots
        }

        // Check which host is the valid name
        host = checkHost(host, shortHost);
        logger.finer("checkHost returned " + host);
        //ident will be unique
        ident = Config.CMD_AGENT + "@" + host;

        // Make sure there is only one agent running in a machine
        CmdAgent agent = (CmdAgent) registry.getService(ident);

        if (agent == null) { // If not found, reregister new agent.
            boolean agentCreated = false;
            if (cmd == null) {
                cmd = new CmdAgentImpl();
                agentCreated = true;
                logger.fine(hostname + "(Realname: " + host +
                                                ") created CmdAgentImpl");
            }

            if (register(ident, cmd)) { // Double check for race condition
                agent = cmd;

                // setBenchName scans all resources.
                // Benchmark needs to be loaded first.
                new Download().loadBenchmark(benchName, downloadURL);
                cmd.setBenchName(benchName, libPath);

                if(host.equals(master)) {
                    ident = Config.CMD_AGENT;
                    reregister(ident, cmd);
                } else if (sameHost(host, master)) {
                    ident = Config.CMD_AGENT;
                    reregister(ident, cmd);
                }

                // Create and reregister FileAgent
                if (file == null)
                    file = new FileAgentImpl();
                reregister(Config.FILE_AGENT + "@" + host, file);

                // Register a blank Config.FILE_AGENT for the master's
                // file agent.
                if (sameHost(host, master))
                    reregister(Config.FILE_AGENT, file);
            } else { // If we run into that, we just grab the agent again.
                if (agentCreated) {
                    UnicastRemoteObject.unexportObject(cmd, true);
                    agentCreated = false;
                    logger.fine(hostname + "(Realname: " + host +
                                                ") unexported CmdAgentImpl");
                }
                agent = (CmdAgent) registry.getService(ident);
            }
        }

        // Only if the 'hostname' is an interface name and not equal
        // the actual host name, we re-reregister the agents with the 'hostname'
        if (!host.equals(hostname)) {
            reregister(Config.CMD_AGENT + "@" + hostname, agent);

            // The FileAgent registration may have a significant lag time
            // from the CmdAgent registration due to downloads, etc.
            // We just need to wait.
            FileAgent f = (FileAgent) registry.getService(
                                            Config.FILE_AGENT + "@" + host);
            int retry = 0;
            while (f == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                f = (FileAgent) registry.getService(
                        Config.FILE_AGENT + "@" + host);
                if (++retry > 100)
                    break;
            }
            if (f != null)
                reregister(Config.FILE_AGENT + "@" + hostname, f);
            else
                logger.severe("Giving up re-registering file agent at " + host +
                        " as " + hostname +" after " + retry + " retries.");
        }
    }

    private static boolean register(String name, Remote service)
            throws RemoteException {
        boolean success = false;
        if (registeredNames.add(name)) {
            success = registry.register(name, service);
            if (success) {
                logger.fine("Succeeded registering " + name);
            } else {
                logger.fine("Failed registering " + name +
                                        ". Entry already exists.");
            }
        }
        return success;
    }

    private static void reregister(String name, Remote service)
            throws RemoteException {
        if (registeredNames.add(name)) {
            registry.reregister(name, service);
            logger.fine("Succeeded re-registering " + name);
        }
    }

    /**
     * Unregisters all the registered services.
     * @throws RemoteException A network error occurred
     */
    static void deregisterAgents() throws RemoteException {
        synchronized(registeredNames) {
            for (String name : registeredNames)
                registry.unregister(name);

            registeredNames.clear();
        }
    }

    /**
     * Terminates the agents.
     */
    static void terminateAgents() {
        if (!daemon) {
            System.exit(0);
        }
    }

    /**
     * This method is for splitting both Unix and Windows paths into their
     * pathElements. It detects the path separator whether it is Unix or
     * Windows style and takes care of the separators accordingly.
     * @param path The path to split
     * @return The splitted path
     */
    private static String[] pathSplit(String path) {
        char pathSeparator = ':';  // Unix style by default.

        // Check for '\' used in Windows paths.
        if (path.indexOf('\\') >= 0) {
            pathSeparator=';';
        }

        // Check for "c:/foo/bar" sometimes used in Windows paths
        if (pathSeparator == ':' ) {
            Pattern p = Pattern.compile("\\A[a-zA-Z]:/");
            Matcher m = p.matcher(path);
            if (m.find())
                pathSeparator = ';';
        }

        // Check for ...;c:/foo/bar at any place in the path
        if (pathSeparator == ':' ) {
            Pattern p = Pattern.compile(";[a-zA-Z]:/");
            Matcher m = p.matcher(path);
            if (m.find())
                pathSeparator = ';';
        }

        String delimiter = "" + pathSeparator;

        return path.split(delimiter);
    }

    private static String checkHost(String longHost, String shortHost) {
        InetAddress[] host1Ip = new InetAddress[0];
        String h;
        try {
            host1Ip = InetAddress.getAllByName(shortHost);
            h = shortHost;
        } catch (UnknownHostException e) {
            logger.finer("Host " + shortHost + " not found. Trying " + longHost);
            try {
                host1Ip = InetAddress.getAllByName(longHost);
                h = longHost;
            } catch (UnknownHostException un) {
                logger.warning("Host " + longHost + " not found.");
                return null;
            }
        }
        return(h);
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

    private static void setLogger() {
        try {
            // Update the logging.properties file in config dir
            Properties log = new Properties();
            FileInputStream in = new FileInputStream(Config.CONFIG_DIR +
                                                    "logging.properties");
            log.load(in);
            in.close();

            logger.fine("Updating " + Config.CONFIG_DIR + "logging." +
                    host + ".properties");
            log.setProperty("java.util.logging.SocketHandler.host", master);
            log.setProperty("java.util.logging.SocketHandler.port",
                    String.valueOf(Config.LOGGING_PORT));
            FileOutputStream out = new FileOutputStream(
                    new File(Config.CONFIG_DIR + "logging." + host +
                    ".properties"));
            log.store(out, "Faban logging properties");
            out.close();

            LogManager.getLogManager().readConfiguration(new FileInputStream(
                    Config.CONFIG_DIR + "logging." + host + ".properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
