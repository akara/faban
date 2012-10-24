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

import com.sun.faban.common.*;
import com.sun.faban.harness.RemoteCallable;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.CmdMap;
import com.sun.faban.harness.util.Invoker;

import java.io.*;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.server.Unreferenced;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * CmdAgentImpl is the class that runs remote commands for the CmdService
 * This implementation provides a robust means of running remote
 * commands. All error messages from the remote commands are logged
 * to the error log, which should help in debugging.
 * The user is encouraged not to run huge shell scripts using this
 * interface as the debugging advantages will be lost. Rather, try and
 * break up the task to running Java/native apps as far as possible
 * and use shell scripts sparingly. If the shell scripts spit out
 * periodic status messages indicating the position in its execution
 * cycle, this will aid in debugging.
 * <ul>
 * <li> It implements the CmdAgent interface; see the
 *      CmdAgent.java file for its description.
 * <li> Application-defined exceptions.
 * </ul>
 *
 * @author Ramesh Ramachandran
 * @see com.sun.faban.harness.agent.CmdAgent
 * @see com.sun.faban.harness.engine.CmdService
 */
public class CmdAgentImpl extends UnicastRemoteObject
        implements CmdAgent, CommandChecker, Unreferenced {

    private static Logger logger =
            Logger.getLogger(CmdAgentImpl.class.getName());

    private final List<CommandHandle> handleList = Collections.synchronizedList(
                                                new ArrayList<CommandHandle>());

    private static HashMap<String, HashMap<String, List<String>>> servicesBinMap =
                           new HashMap<String, HashMap<String, List<String>>>();

    private static HashMap<String, List<String>> servicesClassPath =
                                            new HashMap<String, List<String>>();

    private Timer timer;

    String[] baseClassPath;
    String[] allClassPath; // All class paths including all services class paths
    String libPath;
    Map<String, List<String>> binMap;
    private ArrayList<String> javaCmd;

    static class CmdProcess {
        String ident;
        Process process;
        String logs;

        public CmdProcess() {
        }

        public CmdProcess(String ident, Process process, String logs) {
            this.ident = ident;
            this.process = process;
            this.logs = logs;
        }
    }


    // This class must be created only through the main method.
    CmdAgentImpl() throws RemoteException {
        super();
    }

    /**
     * Sets the benchmark name in the command map file.
     * @param benchName
     * @param libPath
     * @throws java.lang.Exception
     */
    void setBenchName(String benchName, List<String> libPath) throws Exception {
        baseClassPath = getBaseClassPath(benchName);
        this.libPath = getLibPath(benchName, libPath);
        binMap = CmdMap.getCmdMap(benchName);
        servicesBinMap = new HashMap<String, HashMap<String, List<String>>>();
        servicesClassPath = new HashMap<String, List<String>>();
    }

    // CmdAgent implementation

    /**
     * Return the hostname of this machine as known to this machine
     * itself. This method is included in order to solve a Naming problem
     * related to the names of the tpcw result files to be transferred to the
     * the master machine.
     * @return The hostname
     */
    public String getHostName() {
        return AgentBootstrap.host;
    }

    /**
     * Only Other Agents should access the command agent using this method.
     * @return this Command Agent
     */
    public static CmdAgentImpl getHandle() {
        return AgentBootstrap.cmd;
    }

    /**
     * Obtains the tmp directory of a remote host.
     *
     * @return The tmp directory.
     */
    public String getTmpDir() {
        return Config.TMP_DIR;
    }

    /**
     * Set the logging level of the specified logger.
     * @param name Name of the logger. If "" is passed the root logger level will be set.
     * @param level The Log level to set
     */
    public void setLogLevel(String name, Level level) {
        LogManager.getLogManager().getLogger(name).setLevel(level);

        //Update logging.properties file which is used by faban driver

    }

    /**
     * Updates the paths, usually in the local command agent.
     * @param pathList The list of paths to download
     */
    public void updatePaths(List<String> pathList) {
        ArrayList<String> allClassPathList = new ArrayList<String>();
        for (String path : pathList)  {
            try {
                if (servicesBinMap.get(path) == null) {
                    servicesBinMap.put(path, CmdMap.getServiceBinMap(path));
                }
            } catch (Exception ex) {
                logger.log(Level.INFO, ex.getMessage() , ex);
            }

            ArrayList<String> libList = new ArrayList<String>();
            getClassPath(Config.SERVICE_DIR + path + "/lib/", libList);
            if (libList.size() > 0) {
                if (servicesClassPath.get(path) == null) {
                    servicesClassPath.put(path, libList);
                    allClassPathList.addAll(libList);
                }
            }
        }
        for (String classPath : baseClassPath)
            allClassPathList.add(classPath);

        allClassPath = allClassPathList.toArray(
                new String[allClassPathList.size()]);
    }

    /**
     * Downloads the files used by services and tools to
     * the remote agent system.
     * @param pathList The list of service bundle paths
     */
    public void downloadServices(List<String> pathList) {
        for (String path : pathList)  {
            try {
                new Download().loadService(path, AgentBootstrap.downloadURL);
            } catch (Exception ex) {
                logger.log(Level.INFO, ex.getMessage() , ex);
            }
        }
        updatePaths(pathList);
    }

    /**
     * Similar to the which shell command, 'which' returns the actual path
     * to the given command. If it maps to a series of commands, they will
     * be returned as a single string separated by spaces. Note that 'which'
     * does not actually try to check the underlying system for commands
     * in the search path. It only checks the Faban infrastructure for
     * existence of such a command.
     * @param cmd The command to search for
     * @param svcPath The service path, if any
     * @return The actual command to execute, or null if not found.
     */
    public String which(String cmd, String svcPath) {
        Map<String, List<String>> extMap = null;
        extMap = servicesBinMap.get(svcPath);
        if (extMap != null && cmd.indexOf(File.separator) == -1) {
            List<String> realCmd = extMap.get(cmd);
            if (realCmd != null) {
                return Utilities.print(realCmd, " ");
            }
        }
        if (cmd.indexOf(File.separator) == -1) { // not an absolute path
            List<String> realCmd = binMap.get(cmd);
            if (realCmd != null) {
                return Utilities.print(realCmd, " ");
            }
        } else { // Check for pathext in case of absolute path...
            File f = new File(cmd);
            if (!f.exists()) {
                logger.finer(cmd + " does not exist as a file.");
                String[] exts = CmdMap.getPathExt();
                if (exts != null) {
                    for (String ext : exts) {
                        String cext = cmd + ext;
                        logger.finer("Trying " + cext);
                        f = new File(cext);
                        if (f.exists()) {
                            logger.finer("Found " + cext);
                            return cext;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Executes a command from the remote command agent.
     *
     * @param c The command to be executed
     * @param svcPath The service location, if any
     * @return A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle execute(Command c, String svcPath)
                throws IOException, InterruptedException {
        Map<String, List<String>> extMap = null;
        if (svcPath != null)
            extMap = servicesBinMap.get(svcPath);

        c.register(handleList);
        try {
            return c.execute(this, extMap);
        } catch (IOException ex) {
            logger.log(Level.WARNING, ex.getMessage(), ex);
            throw ex;
        } catch (InterruptedException ex) {
            logger.log(Level.WARNING, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Executes a java command from the remote command agent.
     * @param c The command containing the main class
     * @param svcPath
     * @return A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle java(Command c, String svcPath)
            throws IOException, InterruptedException {
        List<String> extClassPath = null;
        if (svcPath != null)
            extClassPath = servicesClassPath.get(svcPath);

        c.register(handleList);
        try {
            return c.executeJava(this, extClassPath);
        } catch (IOException ex) {
            logger.log(Level.WARNING, ex.getMessage(), ex);
            throw ex;
        } catch (InterruptedException ex) {
            logger.log(Level.WARNING, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Executes the RemoteCallable on the target instance.
     *
     * @param callable The callable to execute
     * @param contextLocation The context location of the invoker
     * @return The type specified at creation of the callable.
     * @throws Exception Any exception from the callable
     */
    public <V extends Serializable> V exec(RemoteCallable<V> callable,
                                           String contextLocation)
            throws Exception {
        Invoker.setContextLocation(contextLocation);
        try {
            return callable.call();
        } catch (Exception ex) {
            logger.log(Level.WARNING, ex.getMessage(), ex);
            throw ex;
        } finally {
            Invoker.setContextLocation(null);
        }
    }

    /**
     * Registers and starts agent.
     * @param agentClass The agent class
     * @param identifier The agent id
     * @return always returns true
     * @throws java.lang.Exception If an error occurs in registering the class
     */
    public boolean startAgent(Class agentClass, String identifier) throws Exception {
        try {
            Remote agent = (Remote)agentClass.newInstance();
            logger.fine("Agent class " + agent.getClass().getName() + " created");
            AgentBootstrap.registry.reregister(identifier, agent);
            logger.fine("Agent started and Registered as " + identifier);
        }catch(Exception e) {
            logger.log(Level.WARNING, "Failed to create " +
                    agentClass.getName(), e);
        }
        return true;
    }

    /**
     * Kill off all processes started.
     */
    public void kill() {
        // Now iterate the handle list and kill'em all.
        synchronized (handleList) {
            for (CommandHandle handle : handleList) {
                try {
                    handle.destroy();
                } catch (RemoteException e) {
                    logger.log(Level.SEVERE, "Caught RemoteException on " +
                            "local CommandHandle destroy. " +
                            "Please report bug.", e);
                }
            }

            for (int retries = 0; handleList.size() > 0 && retries < 20;
                 retries++){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.log(Level.WARNING, "Sleep Interrupted. Strange!", e);
                }

                // We need to use iterator instead of foreach loop as we need
                // to remove handles from the list while going through it.
                Iterator<CommandHandle> iter = handleList.iterator();
                while (iter.hasNext()) {
                    CommandHandle handle = iter.next();
                    boolean terminated = false;
                    try {
                        int exitValue = handle.exitValue();
                        logger.finer("Command exited with exit value " +
                                exitValue + '.');
                        terminated = true;
                    } catch (RemoteException e) {
                        logger.log(Level.SEVERE, "Caught RemoteException on " +
                                "local CommandHandle exitValue. " +
                                "Please report bug.", e);
                    } catch (IllegalThreadStateException e) {
                        logger.log(Level.FINER, "Registry did not terminate! ",
                                                                            e);
                    }

                    if (terminated)
                        iter.remove();
                    else
                        try { // kill again...
                            handle.destroy();
                        } catch (RemoteException e) {
                            logger.log(Level.SEVERE, "Caught RemoteException" +
                                    "on local CommandHandle destroy. " +
                                    "Please report bug.", e);
                        }
                }
            }
        }

        int leftover = handleList.size();
        if (leftover > 0) {
            StringBuilder msg = new StringBuilder();
            msg.append("Process termination/cleanup unsuccessful after ");
            msg.append("20 attempts. ");
            msg.append(leftover);
            msg.append(" processes remaining. ");
            msg.append("This may affect subsequent runs:");
            synchronized(handleList) {
                for (CommandHandle handle : handleList) {
                    msg.append("<br>\n");
                    try {
                        msg.append(handle.getCommandString());
                    } catch (RemoteException e) {
                        logger.log(Level.SEVERE, "Caught RemoteException on " +
                                "local CommandHandle.getCommandString(). " +
                                "Please report bug.", e);
                    }
                }
            }

            logger.warning(msg.toString());
            handleList.clear();
        }

        /* Exit application */
        try {
            AgentBootstrap.deregisterAgents();
        } catch (RemoteException re){
            logger.log(Level.WARNING, re.getMessage(), re);
        }

        logger.fine("Killing itself");

        // *** This is to gracefully return from this method.
        // *** The Agent will exit after 5 seconds
        // *** If the System.exit(0) is called in this method
        // *** the Service will get a RemoteException
        Thread exitThread = new Thread() {
            @Override
            public void run() {
                try {
                    Thread.sleep(5000);
                    AgentBootstrap.terminateAgents();
                } catch (Exception e) {}
            }
        };
        exitThread.start();
    }

    /**
     * When this instance is unreferenced the application must exit.
     *
     * @see         java.rmi.server.Unreferenced
     *
     */
    public void unreferenced()
    {
        kill();
    }

    /**
     * Obtains the registry.
     * @return Registry
     */
    public static Registry getRegistry() {
        return AgentBootstrap.registry;
    }

    /**
     * Obtains the hostname.
     * @return hostname
     */
    public static String getHost() {
        return AgentBootstrap.host;
    }

    /**
     * Obtains the master hostname.
     * @return master hostname
     */
    public static String getMaster() {
        return AgentBootstrap.master;
    }

    // Convert command and arguments to use OS-specific paths, if applicable.
    private static void convertCommand(List<String> cmd) {
        int size = cmd.size();
        for (int i = 0; i < size; i++) {
            String arg = cmd.get(i);
            String converted = Utilities.convertPath(arg);
            if (converted != arg) // Conversion is actually done.
                cmd.set(i, converted);
        }
    }

    /**
     * Checks and completes the command list, if possible.
     * @param cmd The command and arg list
     * @param extMap The external map, if any
     * @return The checked command
     */
    public List<String> checkCommand(List<String> cmd,
                                     Map<String, List<String>> extMap) {

        convertCommand(cmd);

        String bin = cmd.get(0);
        // Check for the external/service bin map first.
        if (extMap != null && bin.indexOf(File.separator) == -1) {
            List<String> mods = extMap.get(bin);
            if (mods != null) {
                CmdMap.replaceFirst(cmd, mods);
            }
        }
        if (bin.indexOf(File.separator) == -1) { // not an absolute path
            List<String> mods = binMap.get(bin);
            // If we find modified commands, replace the command with the mods.
            if (mods != null) {
                CmdMap.replaceFirst(cmd, mods);
            }
        } else { // Check for pathext in case of absolute path...
            File f = new File(bin);
            if (!f.exists()) {
                logger.finer(bin + " does not exist as a file.");
                String[] exts = CmdMap.getPathExt();
                if (exts != null) {
                    for (String ext : exts) {
                        String cext = bin + ext;
                        logger.finer("Trying " + cext);
                        f = new File(cext);
                        if (f.exists()) {
                            cmd.set(0, cext);
                            logger.finer("Found " + cext);
                            break;
                        }
                    }
                }
            }
        }

        return cmd;
    }

    /**
     * Checks and completes the java command, if possible.
     *
     * @param cmd The original command
     * @param extClassPath The extended classpath, if any
     * @return The completed java command
     */
    public List<String> checkJavaCommand(List<String> cmd, List<String> extClassPath) {

        convertCommand(cmd);

        if (javaCmd == null) { // Initialize javaCmd if needed.
            javaCmd = new ArrayList<String>();

            StringBuilder buf = new StringBuilder(AgentBootstrap.javaHome);
            buf.append(File.separator);
            buf.append("bin");
            buf.append(File.separator);
            buf.append("java");
            javaCmd.add(buf.toString());
            buf.setLength(0);

            javaCmd.addAll(AgentBootstrap.jvmOptions);

            javaCmd.add("-cp");

            boolean falseEnding = false;
            // Externally specified classpath takes precedence.
            for (String pathElement : AgentBootstrap.extClassPath) {
                buf.append(pathElement);
                buf.append(File.pathSeparator);
                falseEnding = true;
            }
            if (extClassPath != null) {
                for (String pathElement : extClassPath) {
                    buf.append(pathElement);
                    buf.append(File.pathSeparator);
                    falseEnding = true;
                }
            }
            for (String pathElement : baseClassPath) {
                buf.append(pathElement);
                buf.append(File.pathSeparator);
                falseEnding = true;
            }
            if (falseEnding)
                buf.setLength(buf.length() - File.pathSeparator.length());
            javaCmd.add(buf.toString());

            if (libPath != null)
                javaCmd.add(libPath);
        }

        ArrayList<String> tmp = new ArrayList<String>(cmd);
        cmd.clear();
        cmd.addAll(javaCmd);
        cmd.addAll(tmp);
        return cmd;
    }

    private static void getClassPath(String libDirPath,
                                     ArrayList<String> libList) {
        File libDir = new File(libDirPath);
        if (libDir.isDirectory()) {
            File[] libFiles = libDir.listFiles();
            for (int i = 0; i < libFiles.length; i++)
                if (libFiles[i].isFile())
                    libList.add(libFiles[i].getAbsolutePath());
        }
    }

    private static String[] getBaseClassPath(String benchName) {
        // The benchmark-specific libs take precedence, add first to list
        ArrayList<String> libList = new ArrayList<String>();
        getClassPath(Config.BENCHMARK_DIR + benchName + "/lib/", libList);
        getClassPath(Config.LIB_DIR, libList);
        String[] baseClassPath = new String[libList.size()];
        baseClassPath = libList.toArray(baseClassPath);
        return baseClassPath;
    }

    private static String getLibPath(String benchName, List<String> libPath) {
        File libDir = new File(Config.BENCHMARK_DIR + benchName + "/lib");
        File osLibDir = new File(libDir, Config.OS_DIR);
        File archLibDir = new File(osLibDir, Config.ARCH_DIR);
        if (containsNonJarFiles(archLibDir)) {
            libPath.add(archLibDir.getAbsolutePath());
        }
        if (containsNonJarFiles(osLibDir)) {
            libPath.add(osLibDir.getAbsolutePath());
        }
        if (containsNonJarFiles(libDir)) {
            libPath.add(libDir.getAbsolutePath());
        }

        String libPathString = null;
        if (libPath.size() > 0) {
            StringBuilder b = new StringBuilder();
            b.append("-Djava.library.path=");
            Iterator<String> iter = libPath.iterator();
            b.append(iter.next());
            while (iter.hasNext())
                b.append(File.pathSeparator).append(iter.next());
            libPathString = b.toString();
        }
        return libPathString;
    }

    private static boolean containsNonJarFiles(File dir) {
        if (!dir.isDirectory())
            return false;
        File[] files = dir.listFiles();
        for (File file : files) {
            if (file.isFile() && !file.getName().toLowerCase().endsWith(".jar"))
                return true;
        }
        return false;
    }

    /**
     * Sets the time on the agent host, in GMT. The time string
     * must be in the format MMddHHmmyyyy.ss according to Unix date specs
     * and must be in GMT time.
     *
     * @param gmtTimeString Time string in format
     * @throws IOException An I/O error occurred
     */
    public void setTime(String gmtTimeString) throws IOException {
        Command c = new Command("date", "-u", gmtTimeString);
        c.setLogLevel(Command.STDOUT, Level.FINER);
        c.setLogLevel(Command.STDERR, Level.WARNING);
        try {
            int exitValue = c.execute(this, null).exitValue();
            if (exitValue != 0)
                logger.log(Level.WARNING, "Error on \"" + c +
                        "\" command trying to set the date. Exit value: " +
                        exitValue);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error setting date.", e);
            throw e;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "Interrupted setting date.", e);
        }
    }

    /**
     * Gets the time on the agent host, in millis.
     *
     * @return The time on the remote system.
     */
    public long getTime() {
        return System.currentTimeMillis();
    }

    /**
     * Obtains the timer associated with this command agent.
     * @return the timer for this command agent
     */
    public synchronized Timer getTimer() {
        if (timer == null)
            timer = new Timer("CmdAgent Timer", true);
        return timer;
    }

    // The class which spawns a thread to read the stream of the process
    // and dumps it into the tmp file.
    class LogWriter extends Thread {
        BufferedReader in;
        PrintStream out;

        /**
         * Constructor.
         * Open files and start thread.
         *
         * @param is InputStream to read from
         * @param logfile String filename to log to
         * @throws IOException An I/O Error occurred
         */
        public LogWriter(InputStream is, String logfile) throws IOException {
            in = new BufferedReader(new InputStreamReader(is));
            out = new PrintStream(new FileOutputStream(logfile));
            this.start();
        }

        /**
         * Run, copying input stream's contents to output until no
         * more data in input file. Exit thread automatically.
         */
        @Override
        public void run() {
            try {
                String str = in.readLine();
                while (str != null) {
                    out.println(str);
                    str = in.readLine();
                }
            } catch (IOException ie) {
                return;
            }
            return;
        }
    }
}
