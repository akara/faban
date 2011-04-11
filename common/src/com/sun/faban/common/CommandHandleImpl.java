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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implementation of the remote command handle.
 *
 * @author Akara Sucharitakul
 */
public class CommandHandleImpl implements CommandHandle {

    static ThreadLocal<ReaderThread[]> streamReaders =
            new ThreadLocal<ReaderThread[]>();

    Command command;

    /**
     * Saves the ReaderThreads of this Command in case some of the
     * threads get abandoned.
     */
    ReaderThread[] readers = new ReaderThread[2];


    /**
     * Constructs the command handle implementation from a command.
     * @param command The command object
     * @throws RemoteException If the iml's stub is not available, run rmic
     */
    CommandHandleImpl(Command command) throws RemoteException {
        this.command = command;
        if (command.remote)
            UnicastRemoteObject.exportObject(this);
    }

    /**
     * Obtains the command string this command handle represents.
     *
     * @return The command string executed.
     */
    public String getCommandString() {
        return command.toString();
    }

    /**
     * Forcefully terminates the command.
     */
    public void destroy() {
        command.killed = true;
        command.process.destroy();
    }

    /**
     * Waits for the command to terminate.
     *
     * @throws InterruptedException The waiting thread got interrupted
     */
    public void waitFor() throws InterruptedException {
        // Wait for the process to terminate
        command.process.waitFor();
        if (!command.daemon)
            // For non-daemon, wait another max 10 secs to clear the streams
            synchronized (this) {
                for (int i = 0; i < readers.length; i++)
                    if (readers[i] != null)
                        readers[i].waitFor(10000);
            }
    }

    /**
     * Waits for the command to terminate, with a given timeout.
     * @param timeout The time out
     * @throws InterruptedException The waiting thread got interrupted.
     */
    public void waitFor(int timeout) throws InterruptedException {
        long t = System.currentTimeMillis();
        long dt = 0l;
        if (!command.daemon) {
            synchronized (this) {
                for (int i = 0; i < readers.length; i++) {
                    if (readers[i] != null) {
                        readers[i].waitFor((int) (timeout - dt));
                        dt = System.currentTimeMillis() - t;
                        if (timeout <= dt)
                            break;
                    }
                }
            }
        }
    }


    static byte[] readFile(String fileName) throws IOException {
        File f = new File(fileName);
        long size = f.length();
        if (size >= Integer.MAX_VALUE)
            throw new IOException("Cannot handle file size >= 2GB");

        int readSize = (int) size;
        FileInputStream in = new FileInputStream(f);
        byte[] content = new byte[readSize];
        int position = 0;
        while (position < readSize) {
            int c = in.read(content, position, readSize - position);
            if (c >= 0) {
                position += c;
            } else { // Prematurely ran into eof
                throw new IOException("Unexpected EOF reading file!");
            }
        }
        in.close();
        return content;
    }

    /**
     * Obtains the exit value of the command.
     *
     * @return The exit value of the command
     */
    public int exitValue() {
        return command.process.exitValue();
    }

    /**
     * Obtains the stdout or stderr of the command.
     * @param streamId Command.STDOUT or Command.STDERR
     * @return The output from stdout or stderr, or null if there is no output
     * @throws IOException There is an error getting the output
     */
    public byte[] fetchOutput(int streamId) throws IOException {
        if (command.streamHandling[streamId] == Command.TRICKLE_LOG)
            throw new IllegalStateException("Output not available if " +
                    "StreamHandling is TRICKLE_LOG");
        synchronized (this) {
            if (readers[streamId] == null)
                return readFile(command.outputFile[streamId]);
            else
                return readers[streamId].fetchOutput();
        }
    }

    /**
     * Obtains the stdout or stderr of the command and put it into file.
     *
     * @param streamId Command.STDOUT or Command.STDERR
     * @param destFile The destination file on the calling system
     * @return The FileTransfer, if called from remote system, the file is saved
     * @throws java.io.IOException      There is an error getting the output
     * @throws IllegalStateException    The command is not yet terminated or
     *                                  does not record output
     */
    public FileTransfer fetchOutput(int streamId, String destFile)
            throws IOException, IllegalStateException {
        if (command.streamHandling[streamId] == Command.TRICKLE_LOG)
            throw new IllegalStateException("Output not available if " +
                    "StreamHandling is TRICKLE_LOG");
        synchronized (this) {
            if (readers[streamId] == null)
                return new FileTransfer(command.outputFile[streamId], destFile);

            else
                return readers[streamId].fetchOutput(destFile);
        }
    }

    /**
     * Waits for the command until it matches a certain string it its
     * output streams.
     * @throws InterruptedException The wait was interrupted
     */
    public synchronized void waitMatch() throws InterruptedException {
        for (int i = 0; i < readers.length; i++)
            if (readers[i] != null)
                readers[i].waitMatch();
    }

    void processLogs() {
        ReaderThread[] readers = streamReaders.get();
        if (readers == null) {
            readers = new ReaderThread[2];
            streamReaders.set(readers);
        }
        for (int i = 0; i < readers.length; i++) {
            if (readers[i] == null)
                readers[i] = new ReaderThread(i, this);
            while (!readers[i].read(this))
                // Thread is abandoned if read returns false. Create new.
                readers[i] = new ReaderThread(i, this);
            // Save the references to prevent loss due to abandonment.
            this.readers[i] = readers[i];
        }
    }

    static class ReaderThread extends Thread {

        int streamId;
        byte[] buffer = new byte[8192];
        byte[] matchSequence;
        int matchSeqOffset = 0;
        int offset = 0;
        String outputFile = null;
        FileOutputStream outStream = null;
        Command command = null;  // command is set to null if it is done.
        // handle is reset once the reader is reassigned to a new command.
        CommandHandleImpl handle;
        String cmdString;
        boolean bufferOutput = Boolean.parseBoolean(
                            System.getProperty("faban.command.buffer", "true"));
        boolean matched = true;
        boolean abandoned = false;
        static Logger logger = Logger.getLogger(ReaderThread.class.getName());

        ReaderThread(int streamId, CommandHandleImpl handle) {
            this.streamId = streamId;
            this.handle = handle;
            setDaemon(true);
            start();
        }

        public void run() {
            logger.finest(Command.STREAM_NAME[streamId] +
                    " ReaderThread started.");

            while (!abandoned) {
                boolean go = false;
                synchronized (this) {
                    if (command != null)
                        go = true;
                    else
                        try {
                            wait();
                        } catch(InterruptedException e) {
                        }
                }
                if (go)
                    try {
                        cmdString = command.toString();
                        logger.finest("Starting reading " + Command.
                                STREAM_NAME[streamId] + " of " + cmdString);

                        if (command.streamMatch[streamId] != null) {
                            matchSequence = command.streamMatch[streamId].getBytes();
                            matched = false;
                        } else {
                            matchSequence = null;
                            matched = true;
                        }
                        matchSeqOffset = 0;

                        if (command.streamHandling[streamId] ==
                                Command.TRICKLE_LOG) {
                            trickleLog();
                        } else {
                            capture();
                            if (command.streamHandling[streamId] ==
                                Command.BULK_LOG) {
                                byte[] b = fetchOutput();
                                if (b != null)
                                    logger.log(command.level[streamId],
                                            cmdString + '\n' +
                                            Command.STREAM_NAME[streamId] +
                                            ":\n" + new String(b));
                            }
                        }
                    } catch (IOException e) {
                        Level level;
                        if (command.killed)
                            level = Level.FINER;
                        else
                            level = Level.WARNING;

                        logger.log(level, "Error reading from log stream " +
                                "from command " + command.command + '.', e);
                    } catch (Exception e) {
                        logger.log(Level.WARNING,
                                "There is an error reading the log stream " +
                                "from command " + command.command + '.', e);
                    } finally {
                        synchronized(this) {
                            logger.fine(this + ": " + command + " terminated.");
                            command = null;
                            notify();
                        }                        
                    }
            }
        }

        boolean read(CommandHandleImpl handle) {
            // Lock old outer object first before locking this.
            // Same sequence avoids deadlocks.
            synchronized (this.handle) {
                synchronized (this) {
                    // If we start many things asynchronously, we may need to
                    // keep many threads. Threads that are not reusable are
                    // called abandoned threads.
                    if (command != null) {
                        abandoned = true;
                        logger.fine("Abandoning ReaderThread for " +
                                Command.STREAM_NAME[streamId]);
                        return false;
                    }
                    // Reassigning an old thread to a new handle
                    // Set the old handle's reader to null to avoid confusion.
                    this.handle.readers[streamId] = null;
                    this.handle = handle;
                    command = handle.command;
                    notify();
                    return true;
                }
            }
        }

        synchronized void waitFor() throws InterruptedException {
            while (command != null) {
                if (command.daemon)
                    return;
                wait(10000);
            }
        }

        /**
         * Waits for the reader to timeout.
         * @param timeOut The given timeout
         * @throws InterruptedException
         */
        synchronized void waitFor(int timeOut) throws InterruptedException {
            long dt = 0l;
            long t = System.currentTimeMillis();
            while (command != null && timeOut > dt) {
                if (command.daemon)
                    return ;
                wait(timeOut - dt);
                dt = System.currentTimeMillis() - t;
            }
            if (command != null)
                logger.warning("Timed out waiting for command " + command.command);
        }

        private void capture() throws IOException {
            logger.fine(this + ": Capturing " + Command.STREAM_NAME[streamId] +
                          " of " + command);
            // Re-initialize buffer.
            offset = 0;

            // Save the outputFile name for after command no longer exists.
            outputFile = command.outputFile[streamId];
            logger.fine(this + ": Setting outputFile to " + outputFile);
            outStream = null;
            int length = command.stream[streamId].
                    read(buffer, offset, buffer.length);

            while (length != -1) {
                match(buffer, offset, length);
                offset += length;
                // buffer full or not buffering, flush it
                if (!bufferOutput || offset == buffer.length) {
                    // Open file if not yet opened.
                    if (outStream == null) {
                        logger.finest("Writing " + Command.STREAM_NAME[streamId]
                                + " to " + outputFile);
                        outStream = new FileOutputStream(outputFile,
                                        command.outputFileAppend[streamId]);
                    }
                    outStream.write(buffer, 0, offset);
                    offset = 0;
                }
                length = command.stream[streamId]
                        .read(buffer, offset, buffer.length - offset);
            }
            command.stream[streamId].close();

            logger.finest(Command.STREAM_NAME[streamId] + " outputFile: " +
                    outputFile + " outStream: " + outStream + " forceFile: " +
                    command.forceFile[streamId] + " buffer size: " + offset);
            if (outStream == null && command.forceFile[streamId] &&
                    offset > 0) {
                logger.finest("Writing " + Command.STREAM_NAME[streamId] +
                        " to " + outputFile);
                outStream = new FileOutputStream(outputFile,
                                command.outputFileAppend[streamId]);
            }

            if (outStream != null) {
                // Flush the rest first.
                outStream.write(buffer, 0, offset);
                outStream.close();
            }
        }

        private void trickleLog() throws IOException {
            logger.finest("Trickeling log for " +
                    Command.STREAM_NAME[streamId]);
            int length = command.stream[streamId].
                    read(buffer, 0, buffer.length);

            while (length != -1) {
                logger.log(command.level[streamId], cmdString + '\n' +
                        Command.STREAM_NAME[streamId] + ":\n" +
                        new String(buffer, 0, length));

                match(buffer, 0, length);

                length = command.stream[streamId]
                        .read(buffer, 0, buffer.length);
            }
            command.stream[streamId].close();
        }

        byte[] fetchOutput() throws IOException {
            logger.finest("Fetching output for " +
                    Command.STREAM_NAME[streamId]);

            byte[] retBuffer = null;
            if (outStream == null) { // If everything is still in memory
                if (offset == 0) // Nothing read
                    return null;
                retBuffer = new byte[offset];
                System.arraycopy(buffer, 0, retBuffer, 0, offset);
            } else {
                retBuffer = readFile(outputFile);
            }
            return retBuffer;
        }

        FileTransfer fetchOutput(String destFile) throws IOException {
            logger.finest("Fetching output for " +
                    Command.STREAM_NAME[streamId] + " to " + destFile);

            FileTransfer transfer;
            if (outStream == null) { // If everything is still in memory
                if (offset == 0) // Nothing read
                    return null;
                transfer = new FileTransfer(buffer, 0, offset, destFile);
                logger.fine(this + ": Constructing transfer from buffer to " + destFile);
            } else {
                transfer = new FileTransfer(outputFile, destFile);
                logger.fine(this + ": Constructing transfer " + outputFile + " -> " + destFile);
            }
            return transfer;
        }

        /**
         * Matches the match string against the match buffer. Controls
         * partial matches and calls the matchOld method to deal with the old
         * buffer after missing the partial match. Calls matchNew to match
         * the current buffer.
         * @param b The buffer
         * @param offset The offset into the buffer
         * @param length The length of the part to match
         */
        private void match(byte[] b, int offset, int length) {
            if (matched)
                return;
            if (matchSeqOffset > 0) { // A partial match
                if (matchNew(b, offset, length)) // Do the partial match
                    return;
                while (matchOld())  // Scan whether there's another partial
                    if (matchNew(b, offset, length)) // Do the other partial
                        return;
            }
            matchNew(b, offset, length); // Do the full match
        }

        /**
         * Matches the rest of the previous buffer. This method only gets
         * called if there was a partial match. In that case, the interesting
         * part of the previous buffer also is the match string all the way to
         * but not including the offset. So we do not need to keep a copy of
         * the previous buffer but just need to do a match of the match string
         * against itself starting at offset 1 - offset 0 was already checked
         * at previous buffer scan.
         * @return True if there is a partial match, false otherwise
         */
        private boolean matchOld() {
            mainLoop:
            for (int i = 1; i < matchSeqOffset; i++) {
                for (int j = 0; j < matchSequence.length; j++) {
                    if (i + j >= matchSeqOffset) {
                        matchSeqOffset = j;
                        return true;
                    }
                    if (matchSequence[j] != matchSequence[i + j])
                        continue mainLoop;
                }
            }
            matchSeqOffset = 0;
            return false;
        }

        /**
         * Matches the rest of the sequence to the buffer. Note, returning
         * true does not mean we found the match. It can also mean that we
         * found a partial match ending the buffer.
         * @param b The buffer
         * @param offset Offset into the buffer
         * @param length Length of the buffer
         * @return Whether or not to stop matching this buffer
         */
        private boolean matchNew(byte[] b, int offset, int length) {
            int endOffset = offset + length;
            mainLoop:
            for (int i = offset; i < endOffset; i++) {
                for (int j = matchSeqOffset; j < matchSequence.length; j++) {
                    if (i + j >= endOffset) {
                        matchSeqOffset = j; // If we come to an end on this
                        return true; // buffer, we need to stop matching it.
                    }
                    if (matchSequence[j] != b[i + j])
                        continue mainLoop;
                }
                synchronized (this) {
                    matched = true;
                    notify();
                    return true; // Stop matching if we found it.
                }
            }
            return false; // Otherwise, continue checking matchOld
        }

        synchronized void waitMatch() throws InterruptedException {
            while (!matched)
                wait(1000);
        }
    }
}
