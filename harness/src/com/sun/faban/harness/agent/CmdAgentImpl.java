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
 * $Id: CmdAgentImpl.java,v 1.10 2007/10/12 01:32:10 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.agent;

import com.sun.faban.common.Command;
import com.sun.faban.common.CommandChecker;
import com.sun.faban.common.CommandHandle;
import com.sun.faban.common.Registry;
import com.sun.faban.harness.RemoteCallable;
import com.sun.faban.harness.common.Config;
import com.sun.faban.harness.util.CmdMap;

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

    // This is actually deprecated. The field is responsible for old style
    // ident based executions.
    private Map processMap = Collections.synchronizedMap(new HashMap());

    private List handleList = Collections.synchronizedList(new ArrayList());

    private String[] baseClassPath;
    Map<String, String> binMap;

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
    CmdAgentImpl(String benchName) throws RemoteException {
        super();

        try {
            // Update the logging.properties file in config dir
            Properties log = new Properties();
            FileInputStream in = new FileInputStream(Config.CONFIG_DIR + "logging.properties");
            log.load(in);
            in.close();

            // Update if it has changed.
            if(!(log.getProperty("java.util.logging.SocketHandler.host").equals(AgentBootstrap.master) &&
                 log.getProperty("java.util.logging.SocketHandler.port").equals(String.valueOf(Config.LOGGING_PORT)))){
                logger.fine("Updating " + Config.CONFIG_DIR + "logging.properties");
                log.setProperty("java.util.logging.SocketHandler.host", AgentBootstrap.master);
                log.setProperty("java.util.logging.SocketHandler.port", String.valueOf(Config.LOGGING_PORT));
                FileOutputStream out = new FileOutputStream(new File(Config.CONFIG_DIR + "logging.properties"));
                log.store(out, "Faban logging properties");
                out.close();
            }
            LogManager.getLogManager().readConfiguration(new FileInputStream(
                    Config.CONFIG_DIR + "logging.properties"));
            setBenchName(benchName);
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Failed to initialize CmdAgent.", e);
        }
    }

    void setBenchName(String benchName) throws Exception {
        baseClassPath = getBaseClassPath(benchName);
        binMap = CmdMap.getCmdMap(benchName);
    }

    // CmdAgent implementation

    /**
     * Return the hostname of this machine as known to this machine
     * itself. This method is included in order to solve a Naming problem
     * related to the names of the tpcw result files to be transferred to the
     * the master machine.
     *
     */
    public String getHostName() {
        return AgentBootstrap.host;
    }

    /**
     * Only Other Agents should access the command agent using this method.
     * So the access is limited to package level
     * @return this Command Agent
     */
    static CmdAgent getHandle() {
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
    public void setLogLevel(String name, Level level) throws RemoteException {
        LogManager.getLogManager().getLogger(name).setLevel(level);

        //Update logging.properties file which is used by faban driver

    }

    /**
     * Executes a command from the remote command agent.
     *
     * @param c The command to be executed
     * @return A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle execute(Command c)
            throws IOException, InterruptedException {
        CommandHandle handle = c.execute(this);
        if (!c.isSynchronous())
            handleList.add(handle);
        return handle;
    }

    /**
     * Executes a java command from the remote command agent.
     * @param c The command containing the main class
     * @return A handle to the command
     * @throws IOException Error communicating with resulting process
     * @throws InterruptedException Thread got interrupted waiting
     */
    public CommandHandle java(Command c)
            throws IOException, InterruptedException {
        CommandHandle handle = c.executeJava(this);
        if (!c.isSynchronous())
            handleList.add(handle);
        return handle;
    }

    /**
     * Executes the RemoteCallable on the target instance.
     *
     * @param callable The callable to execute
     * @return The type specified at creation of the callable.
     * @throws Exception Any exception from the callable
     */
    public <V extends Serializable> V exec(RemoteCallable<V> callable)
            throws Exception {

        return callable.call();
    }

    /**
     * This method is responsible for starting a java cmd in background
     * @param cmd args and class to start the JVM
     * @param identifier to associate with this command
     * @param env in which to run command
     * @return 	true if command started successfully
     */
    public boolean startJavaCmd(String cmd, String identifier, String[] env)
            throws Exception {
        return startJavaCmd(cmd, identifier, env, null);
    }

    /**
     * This method is responsible for starting a java cmd in background
     * @param cmd args and class to start the JVM
     * @param identifier to associate with this command
     * @param env in which to run command
     * @param classPath the class path to prepend to the base class path
     * @return 	true if command started successfully
     */
    public boolean startJavaCmd(String cmd, String identifier, String[] env,
                                String[] classPath) throws Exception {

        Process p;

        StringBuffer buf = new StringBuffer(" -cp ");
        boolean falseEnding = false;
        if (classPath != null)
            for (int i = 0; i < classPath.length; i++) {
                buf.append(classPath[i]);
                buf.append(File.pathSeparator);
                falseEnding = true;
            }
        for (int i = 0; i < baseClassPath.length; i++) {
            buf.append(baseClassPath[i]);
            buf.append(File.pathSeparator);
            falseEnding = true;
        }
        if (falseEnding)
            buf.setLength(buf.length() - File.pathSeparator.length());

        buf.append(' ');
        String classpath = buf.toString();

        cmd = AgentBootstrap.javaHome + File.separator + "bin" + File.separator + "java " +
                AgentBootstrap.jvmOptions + classpath + cmd;
        try {
            logger.fine("Starting Java " + cmd);
            p = Runtime.getRuntime().exec(cmd, env);
        }
        catch (IOException e) {
            p = null;
            logger.log(Level.WARNING, "Command " + cmd + " failed.", e);
            throw e;
        }
        processLogs(p);
        if (p != null) {
            processMap.put(identifier,
                    new CmdProcess(identifier, p, processLogs(p)));

            return true;
        }
        else
            return false;
    }

    public boolean startAgent(Class agentClass, String identifier) throws Exception {
        try {
            Remote agent = (Remote)agentClass.newInstance();
            logger.info("Agent class " + agent.getClass().getName() + " created");
            AgentBootstrap.registry.register(identifier, agent);
            logger.fine("Agent started and Registered as " + identifier);
        }catch(Exception e) {
            logger.log(Level.WARNING, "Failed to create " +
                    agentClass.getName(), e);
        }
        return true;
    }

    /**
     * This method is responsible for starting up the specified command
     * in background
     * The stderr from command is captured and logged to the errorlog.
     * @param cmd - actual command to execute
     * @param identifier	- String to identify this command later null if you don't want to do wait
     *              or kill the process when the cmdAgent exits.
     * @param priority to run command in
     */
    public boolean start (String cmd, String identifier, int priority)
            throws Exception {

        Process p = createProcess(cmd, priority);
        if (p != null) {
            if(identifier != null)
                processMap.put(identifier,
                        new CmdProcess(identifier, p, processLogs(p)));
            return(true);
        }
        else
            return false;
    }

    /**
     * Start command in background and wait for the specified message
     * @param cmd to be started
     * @param ident to identify this command later null if you don't want to do wait
     *              or kill the process when the cmdAgent exits.
     * @param msg message to which wait for
     * @param priority (default or higher priority) for command
     */
    public boolean start(String cmd, String ident, String msg, int priority)
            throws Exception {

        boolean ret = false;

        Process p = createProcess(cmd, priority);
        if (p != null) {
            try {
                InputStream is = p.getInputStream();
                BufferedReader bufR = new BufferedReader(new InputStreamReader(is));

                // Just to make sure we don't wait for ever.
                // We try for 1000 times to read before we give up
                int attempts = 1000;
                while(attempts-- > 0) {
                    // make sure we don't block
                    if(is.available() > 0) {
                        String s = bufR.readLine();
                        if((s !=  null) && (s.indexOf(msg) != -1)) {
                            ret = true;
                            break;
                        }
                    }
                    else {
                        try {
                            Thread.sleep(100);
                        } catch(Exception e){
                            break;
                        }
                    }
                }
                bufR.close();
            }
            catch (Exception e){}

            if(ident != null) {
                processMap.put(ident,
                        new CmdProcess(ident, p, processLogs(p)));
            } // else we don't want to wait or kill this process later.
        }
        return(ret);
    }
    /**
     * This method is responsible for starting the command in background
     * and returning the first line of output.
     * @param cmd command to start
     * @param identifier to associate with this command, null if you don't want to do wait
     *              or kill the process when the cmdAgent exits.
     * @param priority in which to run command
     * @return String the first line of output from the command
     */
    public String startAndGetOneOutputLine(String cmd, String identifier, int priority)
            throws Exception {

        String retVal = null;

        Process p = createProcess(cmd, priority);
        if (p != null) {
            try {
                BufferedReader bufR = new BufferedReader(
                        new InputStreamReader(p.getInputStream()));
                retVal = bufR.readLine();
            }
            catch (Exception e){}

            if(identifier != null) {
                processMap.put(identifier,
                        new CmdProcess(identifier, p, processLogs(p)));
            }
        }
        return(retVal);
    }

    /**
     * This method starts a command in foreground
     * The stderr from command is captured and logged to the errorlog.
     * @param cmd : command to be started
     * @param priority - class in which cmd should be run
     * @return boolean true if command completed successfully
     */
    public boolean start (String cmd, int priority)
            throws Exception {
        boolean status = false;
        Process p = createProcess(cmd, priority);

        String errfile = processLogs(p);

        /* Since this is in foreground, wait for it to complete */
        try {
            p.waitFor();
        }
        catch (InterruptedException ie) {
            /* If we are interrupted, we were probably sent the kill signal */
            p.destroy();
        }

        /* Now xfer logs (if any) */
        xferLogs(errfile, cmd);

        // Look at the exit value
        if(p.exitValue() == 0)
            status = true;

        return(status);
    }

    /**
         * This method starts a command in foreground
         * The stdout from command is captured and returned.
         * @param cmd : command to be started
         * @param priority - class in which cmd should be run
         * @return StringBuffer
         */
        public String startAndGetStdOut (String cmd, int priority)
                throws Exception {
            int readSize = 0;
            int errReadSize = 0;
            InputStream in,err;
            byte[] buffer = new byte[8096];
            StringBuffer std_out = new StringBuffer();
            StringBuffer std_err = new StringBuffer();
            Process p = createProcess(cmd, priority);

            in = p.getInputStream();
            err = p.getErrorStream();

            // std_err.append(cmd);
            // std_err.append("\nstderr:\n");

            boolean outClosed = false;
            boolean errClosed = false;
            for (;;) {
                if (!outClosed && (readSize = in.read(buffer)) > 0)
                    std_out.append(new String(buffer, 0, readSize));
                if (!outClosed && readSize < 0)
                    outClosed = true;
                if (!errClosed && (errReadSize = err.read(buffer)) > 0)
                    std_err.append(new String(buffer, 0, errReadSize));
                if (!errClosed && errReadSize < 0)
                    errClosed = true;
                if (outClosed && errClosed)
                    break;
            }
            logger.info(cmd + "\nstdout:\n" + std_out + "\nstderr:\n" + std_err);
            /* Since this is in foreground, wait for it to complete */
            try {
                p.waitFor();
                int exitValue = p.exitValue();
                if (exitValue != 0) {
                    logger.info("Warning: " + "Command exited with exit value - " + exitValue );
                }
            } catch (InterruptedException e) {
                /* If we are interrupted, we were probably sent the kill signal */
                p.destroy();
            }
            in.close();
            err.close();
            return(std_out.toString());
        }


    /**
     * This method runs a script in foreground
     * The stderr from command is captured and logged to the errorlog.
     * @param cmd to be started
     * @param priority - class in which cmd should be run
     * @return boolean true if command completed successfully
     */
    public boolean runScript (String cmd, int priority) throws Exception {
        return start(cmd, priority);
    }

    /**
     * This method is responsible for waiting for a command started
     * earlier in background
     * @param identifier with which this cmd was started
     * @return true if command completed successfully
     */
    public boolean wait(String identifier) throws Exception {
        boolean status;
        CmdProcess cproc = (CmdProcess) processMap.get(identifier);
        if (cproc == null) {
            Exception e = new Exception(AgentBootstrap.ident + " wait " + identifier + " : No such identifier");
            logger.throwing(AgentBootstrap.ident, "wait", e);
            throw e;
        }
        logger.fine("Waiting for Command Identifier " + identifier);

        // Make sure nobody else is waiting for it.
        synchronized (cproc) {
            try {
                cproc.process.waitFor();
            }
            catch (InterruptedException ie) {
                cproc.process.destroy();
            }
            /* Now xfer logs (if any)*/
            xferLogs(cproc.logs, cproc.ident);

            if(cproc.process.exitValue() == 0)
                status = true;
            else
                status = false;
        }

        /* Remove this command from our cache */
        processMap.remove(cproc.ident);

        return(status);
    }

    /**
     * This method kills off the process specified
     *
     */
    public void kill(String identifier) {
        CmdProcess cproc = (CmdProcess) processMap.remove(identifier);
        if (cproc == null)
            // Such process no longer exists.
            return;

        cproc.process.destroy();

        /* Now xfer logs (if any) to status and error logs*/
        xferLogs(cproc.logs, cproc.ident);
    }

    /**
     * This method is responsible for aborting a command using the killem
     * script
     * @param  identifier for the process. null if not started through
     *               command service.
     * @param processString search string to grep the process while killing
     *                      (same as in killem)
     * @param sigNum the signal number to be used to kill.
     *
     */
    public void killem (String identifier,
                                     String processString, int sigNum)
            throws RemoteException, IOException {

        CmdProcess cproc = (CmdProcess) processMap.remove(identifier);
        Object sync = cproc;
        if (cproc == null)
            sync = this;

        // use exec to avoid creating another process
        String s = "exec " + Config.BIN_DIR + "perfkillem " +
                processString + " -y -" + sigNum;

        synchronized (sync) {
            logger.warning("Killing process with command " + s);

            try {
                Process p = createProcess(s, Config.DEFAULT_PRIORITY);
                p.waitFor();
            }
            catch (InterruptedException ie){
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Killem failed.", e);
            }
        }
    }

    /**
     * Kill off all processes started
     */
    public void kill() {
        // Use an array of Idents as the vector gets manipulated by the kill(ident) call
        String[] keys = new String[processMap.size()];
        keys = (String[]) processMap.keySet().toArray(keys);
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] != null)
                kill(keys[i]);
        }

        // Now iterate the handle list and kill'em all.
        for (Iterator iter = handleList.iterator(); iter.hasNext();) {
            CommandHandle handle = (CommandHandle) iter.next();
            try {
                handle.destroy();
            } catch (RemoteException e) {
                // These handles are local. RemoteException should not occur.
            }
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

    public static Registry getRegistry() {
        return AgentBootstrap.registry;
    }

    public static String getHost() {
        return AgentBootstrap.host;
    }

    public static String getMaster() {
        return AgentBootstrap.master;
    }

    private Process createProcess(String cmd, int priority) throws Exception {
        Process p;
        cmd = checkCommand(cmd);
        String args[] = new String[7];

        args[0] = "priocntl";
        args[1] = "-e";
        args[2] = "-c";
        args[3] = (priority == Config.DEFAULT_PRIORITY) ? "TS" : "RT";
        args[4] = "sh";
        args[5] = "-c";
        args[6] = cmd;

        try {
            logger.fine("Starting Command : " + cmd );
            p = Runtime.getRuntime().exec(args);
        }
        catch (IOException ie) {
            p = null;
            logger.log(Level.WARNING, "Command " + cmd + " failed.", ie);
            throw ie;
        }
        return p;
    }

    /**
     * Method that handles errors from commands
     * This method captures any messages on stderr and
     * stdout of the given Process and logs them.
     * @param proc Process whose stderr needs to be logged
     * @return filename in which errors are logged
     */
    private String processLogs(Process proc)
    {
        InputStream err = proc.getErrorStream();
        InputStream log = proc.getInputStream();
        // Create a unique temporary log file in tmp dir
        String errFile =
                Config.TMP_DIR + "cmd" + proc.hashCode();
        String logFile = errFile + "-out";

        // Create Log writer for errors
        try {
            new LogWriter(err, errFile);
        }
        catch (IOException ie) {
            logger.warning("Could not write to " + errFile);
            return(null);
        }

        // Create Log writer for stdout
        try {
            new LogWriter(log, logFile);
        }
        catch (IOException ie) {
            try {
                logger.warning("Could not write to " + logFile);
            }
            catch (Exception e){}
        }

        logger.fine("Created Error File " + errFile);
        logger.fine("Created Log File " + logFile);
        return errFile;
    }

    /**
     * This method saves messages on stderr of the given
     * Process and logs them to the errorlog.
     * @param errFile Filename of the stderr log
     * @param cmd command which generated errors
     */
    private boolean xferLogs(String errFile, String cmd) {
        boolean status = true;
        // First check if file has any  error messages
        File f = new File(errFile);
        if (f.exists() && f.canRead()) {
            try {
                StringBuffer buf = new StringBuffer();
                BufferedReader in = new BufferedReader(new FileReader(f));
                String line = in.readLine();
                if (line != null) {
                    // Create entry in log identifying source of errors
                    buf.append(cmd);
                    buf.append("\nstderr:\n");

                    // Loop, logging messages
                    while (line != null) {
                        buf.append("\n          " + line);
                        line = in.readLine();
                    }
                    logger.warning(buf.toString());
                    status = false;
                }
                in.close();
                f.delete();
            }
            // We don't bother with exceptions as none of these should occur
            catch (SecurityException se){}
            catch (FileNotFoundException fe){}
            catch (IOException ie){}
        }

        // Copy std out
        // First check if file has any message
        f = new File(errFile + "-out");
        if (f.exists() && f.canRead()) {
            try {
                StringBuffer buf = new StringBuffer();
                BufferedReader in = new BufferedReader(new FileReader(f));
                String line = in.readLine();
                if (line != null) {
                    // Create entry in ststus log identifying source of messages
                    buf.append(cmd);
                    buf.append("\nstdout:\n");

                    // Loop, logging messages
                    while (line != null) {
                        buf.append("\n          " + line);
                        line = in.readLine();
                    }
                    logger.info(buf.toString());
                }
                in.close();
                f.delete();
            }
                    // We don't bother with exceptions as none of these should occur
            catch (SecurityException se){}
            catch (FileNotFoundException fe){}
            catch (IOException ie){}
        }
        return status;
    }

    /**
     * Checks and completes the command, if possible.
     * @param cmd The original command
     * @return The completed command
     */
    public String checkCommand(String cmd) {
        String bin;
        int idx = cmd.indexOf(' ');
        if (idx == -1)
            bin = cmd;
        else
            bin = cmd.substring(0, idx);
        if (bin.indexOf(File.separator) != -1)
            // The path is part of the command, use it as is
            return cmd;
        String path = (String) binMap.get(bin);
        if (path == null) // Don't find it, just try as is
            return cmd;
        if (idx == -1)
            return path;
        return path + cmd.substring(idx);
    }

    /**
     * Checks and completes the java command, if possible.
     *
     * @param cmd The original command
     * @return The completed java command
     */
    public String checkJavaCommand(String cmd) {

        StringBuilder buf = new StringBuilder(AgentBootstrap.javaHome);
        buf.append(File.separator);
        buf.append("bin");
        buf.append(File.separator);
        buf.append("java ");
        buf.append(AgentBootstrap.jvmOptions);
        buf.append(" -cp ");

        boolean falseEnding = false;
        for (int i = 0; i < baseClassPath.length; i++) {
            buf.append(baseClassPath[i]);
            buf.append(File.pathSeparator);
            falseEnding = true;
        }
        if (falseEnding)
            buf.setLength(buf.length() - File.pathSeparator.length());

        buf.append(' ');
        buf.append(cmd);
        return buf.toString();
    }

    private static String[] getBaseClassPath(String benchName) {
        // The benchmark-specific libs take precedence, add first to list
        ArrayList libList = new ArrayList();
        File libDir = new File(Config.BENCHMARK_DIR + benchName + "/lib/");
        if (libDir.exists() && libDir.isDirectory()) {
            File[] libFiles = libDir.listFiles();
            for (int i = 0; i < libFiles.length; i++)
                if (libFiles[i].isFile())
                    libList.add(libFiles[i].getAbsolutePath());
        }
        libDir = new File(Config.LIB_DIR);
        if (libDir.exists() && libDir.isDirectory()) {
            File[] libFiles = libDir.listFiles();
            for (int i = 0; i < libFiles.length; i++)
                if (libFiles[i].isFile())
                    libList.add(libFiles[i].getAbsolutePath());
        }
        String[] baseClassPath = new String[libList.size()];
        baseClassPath = (String[]) libList.toArray(baseClassPath);
        return baseClassPath;
    }

    // The class which spawns a thread to read the stream of the process
    // and dumps it into the tmp file.
    class LogWriter extends Thread {
        BufferedReader in;
        PrintStream out;

        /**
         * Constructor
         * Open files and start thread
         *
         * @param is InputStream to read from
         * @param logfile String filename to log to
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
