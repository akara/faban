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
 * $Id: CommandHandleImpl.java,v 1.1 2006/06/29 18:51:31 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.common;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
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

    static ThreadLocal streamReaders = new ThreadLocal() {

        protected Object initialValue() {
            ReaderThread[] readers = new ReaderThread[2];
            for (int i = 0; i < readers.length; i++) {
                readers[i] = new ReaderThread(i);
            }
            return readers;
        }
    };

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
     * Forfully terminates the command.
     */
    public void destroy() {
        command.process.destroy();
    }

    /**
     * Waits for the command to terminate.
     *
     * @throws InterruptedException The waiting thread got interrupted
     */
    public void waitFor() throws InterruptedException {
        if (!command.daemon)
            for (int i = 0; i < readers.length; i++)
                readers[i].waitFor();

        command.process.waitFor();
    }

    static byte[] readFile(String fileName) throws IOException {
        FileChannel channel = (new FileInputStream(fileName)).getChannel();
        long channelSize = channel.size();
        if (channelSize >= Integer.MAX_VALUE)
            throw new IOException("Cannot handle file size >= 2GB");
        ByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY,
                0, channelSize);
        byte[] content = new byte[(int) channelSize];
        buffer.get(content);
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
     * @throws IOException There is an error getting the output
     */
    public byte[] fetchOutput(int streamId) throws IOException {
        if (command.streamHandling[streamId] == Command.TRICKLE_LOG)
            throw new IllegalStateException("Output not available if " +
                    "StreamHandling is TRICKLE_LOG");
        return readers[streamId].fetchOutput();
    }

    public void waitMatch() throws InterruptedException {
        for (int i = 0; i < readers.length; i++)
            readers[i].waitMatch();
    }

    void processLogs(Command command) {
        ReaderThread[] readers = (ReaderThread[]) streamReaders.get();
        for (int i = 0; i < readers.length; i++) {
            while (!readers[i].read(command))
                // Thread is abandoned if read returns false. Create new.
                readers[i] = new ReaderThread(i);
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
        Command command = null;
        String cmdString;
        boolean matched = true;
        boolean abandoned = false;
        Logger logger;

        ReaderThread(int streamId) {
            logger = Logger.getLogger(getClass().getName());
            this.streamId = streamId;
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
                        cmdString = command.command;
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
                        logger.log(Level.WARNING,
                                "There is an error reading the log stream.", e);
                    } finally {
                        synchronized(this) {
                            command = null;
                            notify();
                        }                        
                    }
            }
        }

        synchronized boolean read(Command command) {
            // If we start many things asynchronously, we may need to keep
            // many threads. Threads that are not reusable are called
            // abandoned threads.
            if (this.command != null) {
                abandoned = true;
                logger.fine("Abandoning ReaderThread for " +
                        Command.STREAM_NAME[streamId]);
                return false;
            }

            this.command = command;
            notify();
            return true;
        }

        synchronized void waitFor() throws InterruptedException {
            while (command != null) {
                if (command.daemon)
                    return;
                wait();
            }
        }

        private void capture() throws IOException {
            logger.finest("Capturing " + Command.STREAM_NAME[streamId] +
                          " of " + cmdString);
            // Re-initialize buffer.
            offset = 0;

            // Save the outputFile name for after command no longer exists.
            outputFile = command.outputFile[streamId];
            outStream = null;
            int length = command.stream[streamId].
                    read(buffer, offset, buffer.length);

            while (length != -1) {
                match(buffer, offset, length);
                offset += length;
                // buffer full, flush it
                if (offset == buffer.length) {
                    // Open file if not yet opened.
                    if (outStream == null) {
                        logger.finest("Writing " + Command.STREAM_NAME[streamId]
                                + " to " + outputFile);
                        outStream = new FileOutputStream(outputFile);
                    }
                    outStream.write(buffer);
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
                outStream = new FileOutputStream(outputFile);
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
                wait();
        }
    }
}
