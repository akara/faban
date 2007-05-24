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
 * $Id: Command.java,v 1.5 2007/05/24 01:06:40 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import java.io.*;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a command that can be sent to execute on any machine. It
 * allows setting the command behavior before executing the command.
 *
 * @author Akara Sucharitakul
 */
public class Command implements Serializable {

    private static final long serialVersionUID = 20070523L;

    /**
     * Stream identifier for standard output.
     */
    public static final int STDOUT = 0;

    /**
     * Stream identifier for standard error output.
     */
    public static final int STDERR = 1;


    public static final String[] STREAM_NAME = {"stdout", "stderr"};
    /**
     * Bulk logging of the stdout or stderr. All output is saved and logged
     * at once when the corresponding stream closes or reaches EOF.
     */
    public static final int BULK_LOG = 0;


    /**
     * Trickle logging logs the stream whenever something is read from
     * the corresponding stream. This causes multiple log messages to
     * be created and is useful for detecting the timing of the message
     * as well as correlating stdout and stderr.
     */
    public static final int TRICKLE_LOG = 1;

    /**
     * Captures the stream in a buffer for later retrieval. If the size of
     * the stream output is more than 8KB, the output will automatically
     * be written to a temporary file for later retrieval. Small output within
     * 8KB will not cause any file I/O.
     */
    public static final int CAPTURE = 2;

    String command;
    String[] env = null;
    String dir = null;
    Process process;
    boolean synchronous = true;
    boolean remote = false;
    boolean killed = false;
    Level[] level = { Level.INFO, Level.WARNING };
    int[] streamHandling = { BULK_LOG, BULK_LOG };
    String[] streamMatch = new String[2];
    InputStream[] stream = new InputStream[2];
    String[] outputFile = new String[2];
    boolean[] forceFile = new boolean[2];
    boolean daemon = false;
    byte[] input;
    String inputFile;

    /**
     * Constructs a command with default settings. Please use the accessor
     * methods to set non-default command behavior.
     * @param command
     */
    public Command(String command) {
        this.command = command;
    }

    /**
     * Called from the command agent allowing the agent to ensure the command
     * will work in the agent environment.
     * @param checker The command checker
     * @return A handle to the executing command
     * @throws IOException Error dealing with the stdin, stdout, or stderr
     * @throws InterruptedException The execute thread got interrupted.
     */
    public CommandHandle execute(CommandChecker checker)
            throws IOException, InterruptedException {
        command = checker.checkCommand(command);
        remote = true;
        return execute();
    }

   /**
    * Called from the command agent to execute java allowing the agent to
    * ensure the java command will work in the agent environment.
    * @param checker The command checker
    * @return A handle to the executing command
    * @throws IOException Error dealing with the stdin, stdout, or stderr
    * @throws InterruptedException The execute thread got interrupted.
    */
    public CommandHandle executeJava(CommandChecker checker)
            throws IOException, InterruptedException {
        command = checker.checkJavaCommand(command);
        remote = true;
        return execute();
    }

    /**
     * Executes the command locally on this system. Please use CmdAgent.execute
     * instead to execute this command in a remote location.
     * @throws IOException Error dealing with the stdin, stdout, or stderr
     * @throws InterruptedException The execute thread got interrupted
     */
    public CommandHandle execute() throws IOException, InterruptedException {
        CommandHandleImpl handle = null;
        try {
            handle = new CommandHandleImpl(this);
        } catch (RemoteException e) {
            Logger logger = Logger.getLogger(CommandHandleImpl.class.getName());
            logger.log(Level.SEVERE,
                    "Could not export remote command handle.", e);
            return null;
        }
        Logger logger = Logger.getLogger(this.getClass().getName());
        logger.fine("Executing " + command);
        process = Runtime.getRuntime().exec(command, env,
                this.dir == null ? null : new File(this.dir));
        stream[STDOUT] = process.getInputStream();
        stream[STDERR] = process.getErrorStream();
        String tmpName = System.getProperty("java.io.tmpdir") + "/cmd" +
                process.hashCode();
        if (outputFile[1] == null)
            outputFile[1] = tmpName;
        else
            forceFile[1] = true;

        if (outputFile[0] == null)
            outputFile[0] = tmpName + "-out";
        else
            forceFile[0] = true;

        handle.processLogs(this);

        if (inputFile != null && inputFile.length() > 0)
            input = CommandHandleImpl.readFile(inputFile);

        if (input != null && input.length > 0) {
            OutputStream stdin = process.getOutputStream();
            try {
                stdin.write(input);
                stdin.flush();
                stdin.close();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error writing stdin.", e);
            }
        }

        if (synchronous)
            handle.waitFor();
        else
            handle.waitMatch();
        return handle;
    }

    /**
     * Sets the environment under which this command should run.
     * @param env The shell environment relevant to this command
     */
    public void setEnvironment(String[] env) {
        this.env = env;
    }

    /**
     * Obtains the current environment this command is set to run. May
     * return null if the default process' environment is used.
     * @return The current set environment, or null if not set
     */
    public String[] getEnvironment() {
        return env;
    }

    /**
     * Sets the working directory to run this command.
     * @param dir The command's working directory.
     */
    public void setWorkingDirectory(String dir) {
        this.dir = dir;
    }

    /**
     * Obtains the current working directory this command is set to run.
     * May return null if the parent process' working directory is used.
     * @return The set working directory, or null if not set
     */
    public String getWorkingDirectory() {
       return dir;
    }

    /**
     * Sets this command to be synchronous. The execute() call will only return
     * after the command has terminated. By default, a command is synchronous.
     * @param synchronous True if the command shall be synchronous
     */
    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    /**
     * Checks whether the command is synchronous, i.e. the execute() call will
     * only return after the command has terminated. By default, a command is
     * synchronous.
     * @return True if the command shall be synchronous, false otherwise
     */
    public boolean isSynchronous() {
        return synchronous;
    }

    /**
     * Waits for the match string to match all or part of the corresponding
     * stream before execute() returns. If waitMatch is set, the command is
     * implicitly set to asynchronous as we are only interested in the match.
     * @param streamId The stream identifier, STDOUT or STDERR
     * @param match The match string, null if execute shall not wait
     */
    public void waitMatch(int streamId, String match) {
        synchronous = false;
        streamMatch[streamId] = match;
    }

    /**
     * Sets the log level to log a certain stream. Note that the default
     * log level is INFO for the STDOUT stream and WARNING for the STDERR
     * stream.
     * @param streamId The stream identifier, STDOUT or STDERR
     * @param level The log level
     */
    public void setLogLevel(int streamId, Level level) {
        this.level[streamId] = level;
    }

    /**
     * Obtains the current log level for a certain stream.
     * @param streamId The stream identifier, STDOUT or STDERR
     * @return The current log level
     */
    public Level getLogLevel(int streamId) {
        return this.level[streamId];
    }

    /**
     * Sets the stream handling mode for a certain stream associated with
     * this command.
     * @param streamId The stream identifier, STDOUT or STDERR
     * @param mode The stream handling mode, BULK_LOG, TRICKLE_LOG, or CAPTURE
     */
    public void setStreamHandling(int streamId, int mode) {
        streamHandling[streamId] = mode;
    }

    /**
     * Obtains the stream handling mode for a certain stream associated
     * with this command.
     * @param streamId The stream identifier, STDOUT or STDERR
     * @return The stream handling mode, BULK_LOG, TRICKLE_LOG, or CAPTURE
     */
    public int getStreamHandling(int streamId) {
        return streamHandling[streamId];
    }

    /**
     * Directs the command to save the output from the stream to an output file.
     * Note that this option will implicitly set the stream handling mode to
     * CAPTURE.
     * @param streamId The stream identifier, STDOUT or STDERR
     * @param fileName The target file name on the target machine
     */
    public void setOutputFile(int streamId, String fileName) {
        outputFile[streamId] = fileName;
        streamHandling[streamId] = CAPTURE;
    }

    /**
     * Obtains the output file name, if set. Otherwise returns null.
     * @param streamId The stream identifier, STDOUT or STDERR
     * @return A file name, if set, or null
     */
    public String getOutputFile(int streamId) {
        return outputFile[streamId];
    }

    /**
     * Sets the data to be sent to stdin of the command.
     * @param input The data buffer
     */
    public void setInput(byte[] input) {
        this.input = input;
        this.inputFile = null;
    }

    /**
     * Obtains the data for the stdin of the command, if set.
     * @return The data buffer
     */
    public byte[] getInput() {
        return input;
    }

    /**
     * Sets the input file for the command. The content of the
     * file is sent to stdin of the process. Note that the file path
     * pertains to the path of the file on the system the command is
     * executed.
     * @param filePath The full path of the input file.
     */
    public void setInputFile(String filePath) {
        inputFile = filePath;
        input = null;
    }

    /**
     * Obtains the file name to be used as input file to the command.
     * @return The file name, if set.
     */
    public String getInputFile() {
        return inputFile;
    }

    /**
     * The daemon property determines whether the process spawned from this
     * command will again spawn children that keep outputting to stdout or
     * stderr. Such spawning behavior is common for daemons. However,
     * daemons will unlikely keep writing to stdout or stderr. They usually
     * manage their logging. This property is false by default and shall be
     * falls for 99.9% of the cases. Use extreme care when changing.
     *
     * @param daemon True for spawning daemons that write to stdout/stderr
     */
    public void setDaemon(boolean daemon) {
        this.daemon = daemon;
    }

    /**
     * Checks whether this command is set to be a daemon or not.
     * @return True if this is set to be a daemon, false otherwise
     */
    public boolean isDaemon() {
        return daemon;
    }

    /* Main for testing the command facility */
    public static void main(String[] args) {
        try {
            Command cmd = new Command("cat /etc/system");
            cmd.setSynchronous(true);
            cmd.execute();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
}
