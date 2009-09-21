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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.AsynchronousCloseException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Queue;

/**
 * The acceptor thread does nothing but accepts incoming network connections.
 */
public class Acceptor implements Runnable {

    Logger logger = Logger.getLogger(this.getClass().getName());
    ServerSocketChannel acceptChannel;
    Queue<SocketChannel> acceptQueue;
    Selector selector;
    boolean isShutdown = false;


    Acceptor(LogConfig config, Queue<SocketChannel> acceptQueue,
             Selector selector)
            throws IOException {

        this.acceptQueue = acceptQueue;
        this.selector = selector;
        acceptChannel = ServerSocketChannel.open();
        ServerSocket serverSocket = acceptChannel.socket();
        if (config.listenQSize == -1)
            serverSocket.bind(new InetSocketAddress(config.port));
        else
            serverSocket.bind(new InetSocketAddress(config.port),
                    config.listenQSize);

    }

    /**
     * Runs the accept loop.
     */
    public void run() {
        while (!isShutdown) {
            try {
                SocketChannel channel = acceptChannel.accept();
                acceptQueue.add(channel);
                selector.wakeup();
            } catch (AsynchronousCloseException e) {
                // This happens if another thread calls shutdown.
                // Do nothing - the accept loop will terminate.
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error accepting connections!", e);
            }
        }
    }

    void shutdown() throws IOException {
        isShutdown = true;
        acceptChannel.close();
    }
}
