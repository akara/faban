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
package com.sun.faban.harness.logging;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The request proxy represents the request for both the selector end
 * and the service thread end. The selector will set the proxy to
 * ready state and if the request is not already processed by a service
 * thread, it will also schedule the request to be processed by the thread
 * pool. Otherwise it will use the request proxy to notify the processing
 * thread that the channel is ready to perform I/O.<p>
 *
 * The service thread side will process the request and wait for notification
 * if more I/O is needed and the channel is not ready.
 *
 * @author Akara Sucharitakul
 */
public class RequestProxy implements Runnable {

    /**
     * The timeout factor should be set to 1 for production. For
     * testing purposes it can be set very high so there is no
     * timeout during manual tests.
     */
    private static final int TIMEOUT_FACTOR = 1;

    private LogConfig config;
    private SelectionKey key;
    private ProtocolHandlerFactory handlerFactory;
    private ByteBuffer buffer;
    private Logger logger;
    private String className;

    /**
     * We use boolean arrays for flags and preallocate 8 booleans as they
     * consume 1 byte just like one boolean would.
     */
    private boolean[] flags = new boolean[2];

    /**
     * The READY flag marks that this request proxy is servicing a request.
     */
    private static final int READY = 0;

    /**
     * The PENDING_NOTIFICATION flag marks whether a channelReady has been
     * sent from the listener. If it has been sent and the worker thread
     * was not waiting, the notify will get lost. So this flag allows the
     * worker thread to determine whether there's IO waiting.
     */
    private static final int PENDING_NOTIFY = 1;

    /**
     * Constructs a request proxy.
     * @param config The log configuration
     * @param key The nio selection key identifying the socket channel
     */
    public RequestProxy(LogConfig config, SelectionKey key) {
        this.config = config;
        this.key = key;
        className = getClass().getName();
        logger = Logger.getLogger(className);
        handlerFactory = new ProtocolHandlerFactory(config);
        buffer = ByteBuffer.allocateDirect(config.readBufferSize);
    }

    /**
     * Tells the proxy that the channel is ready for IO.
     * @return true if read was already ready, false otherwise
     */
    public boolean channelReady() {
        boolean wasReady = flags[READY];
        if (wasReady) {
            synchronized(this) {
                flags[PENDING_NOTIFY] = true;
                notify();
            }
        } else {
            flags[READY] = true;
        }
        return wasReady;
    }

    /**
     * Waits for a channel ready notification for read.
     * @param maxWait The maximum wait time, 0 for indefinite.
     */
    private synchronized void waitForReadChannel(int maxWait) {
        // Reads are likely to have pending notifies
        // 'cause we spend time reading and processing
        // before we come back.
        if (!flags[PENDING_NOTIFY])
            try {
                wait(maxWait);
            } catch (InterruptedException e) {
                // Interrupting just causes a retry read.
            }
        flags[PENDING_NOTIFY] = false;
    }

    /**
     * Waits for channel ready notification for write.
     */
    private synchronized void waitForWriteChannel() {
        // We get false notifications on write and
        // this is unlikely a write ready signal. So we ignore it.
        try {
            wait(1500); // And therefore we wait with a timeout instead.
        } catch (InterruptedException e) {
            // Interrupting just causes retry write.
        }
        flags[PENDING_NOTIFY] = false;
    }

    /**
     * Reads data from the channel and waits till timeout.
     * @param channel The channel to read from
     * @param timeout The timeout in milliseconds
     * @return The number of bytes read
     * @throws IOException If there is an I/O error
     */
    private int read(SocketChannel channel, int timeout)
            throws IOException {
        int count = -1;
        long currentTime = System.currentTimeMillis();
        long endWaitTime = currentTime + timeout;
        for (;;) {
            count = channel.read(buffer);
            if (logger.isLoggable(Level.FINEST))
                logger.finest("Read " + count + " bytes from channel " +
                        channel + " !");

            if (count != 0)
                break;
            currentTime = System.currentTimeMillis();
            if (currentTime >= endWaitTime)
                break;
            waitForReadChannel((int) (endWaitTime - currentTime));
        }
        return count;
    }

    /**
     * Run is called from the thread pool to process the request.
     * With the non-blocking I/O, we might not have all the data when
     * run gets called. Subsequent reads/writes after the initial one
     * will not go to the thread pool but will notify a waiting
     * run method directly.
     */
    public void run() {

        logger.finest("Start processing request!");

        // The flags[READY] is set on the first call
        // to channelReady() in the request
        ProtocolHandler handler = null;
        SocketChannel channel = (SocketChannel) key.channel();

        try {
            buffer.clear();

            // Reading step 1. Just read some data
            // If we can still not read anything within 100ms from the
            // select/poll, we return the thread to the pool.
            int count = read(channel, 100 * TIMEOUT_FACTOR);
            if (count == 0)
                throw new TimedOutException(
                        "Service thread read 0 bytes, timed out!");
            else if (count < 0)
                throw new EOFException(
                        "Encountering EOF after reading 0 bytes");

            // Reading step 2. Identifying protocol header
            // handler initialization loop, reads until handler identified.
            while ((handler = handlerFactory.getHandler(buffer, count))
                    == null) {
                // A little more time for the header
                int readCount = read(channel, 1000 * TIMEOUT_FACTOR);
                logger.finest("Trying to determine protocol\n Immediate read" +
                        " count: " + readCount + ", total: " + count);
                if (readCount == 0)
                    throw new TimedOutException(
                            "Service thread timed out reading header!");
                else if (readCount < 0)
                    throw new EOFException("Encountering EOF reading header!");
                count += readCount;
            }

            handler.setKey(key);

            do { // read-write loop

                // Reading step 3. Read request
                // read loop
                while (handler.doProcessRequest(buffer, count)) {
                    buffer.clear();
                     // Wait up to 10 seconds in production
                    count = read(channel, 10000 * TIMEOUT_FACTOR);
                    logger.finest("Reading request, read count: " + count);
                    if (count == 0)
                        throw new TimedOutException(
                                "Service thread timed out reading request!");
                    else if (count < 0)
                        throw new EOFException(
                                "Encountering EOF reading request");
                }
                count = -1; // reset count after done reading

                // start processing, writing response
                if (handler.doProcessResponse()) {

                    // If we're here, the channel cannot take the whole
                    // write we wanted. So we just register this channel
                    // for notification once it is ready for writes.
                    config.primaryListener.register(channel,
                            SelectionKey.OP_WRITE, this);

                    do { // write continuation loop
                        waitForWriteChannel();
                    } while (handler.doContinueResponse());

                    // We're done writing, so we change the registration
                    // back to read.
                    config.primaryListener.register(channel,
                            SelectionKey.OP_READ,  this);
                }
            } while (handler.requestPending());

        } catch (TimedOutException e) {
            logger.log(Level.FINER, e.getMessage(), e);
        } catch (EOFException e) {
            logger.log(Level.FINEST, e.getMessage(), e);
            cancelKey();
        } catch (UnsupportedProtocolException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            cancelKey();
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage(), e);
            cancelKey();
        }
        flags[READY] = false;
        logger.finest("End processing request");
    }

    private void cancelKey() {
        try {
            key.channel().close();
        } catch (IOException e) {
            logger.log(Level.FINE, "Error closing socket channel", e);
        } finally {
            key.cancel();
        }
    }
}
