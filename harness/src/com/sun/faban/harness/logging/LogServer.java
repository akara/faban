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
 * $Id: LogServer.java,v 1.3 2006/10/08 08:36:56 akara Exp $
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */
package com.sun.faban.harness.logging;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Startup class for LogServer.
 *
 * @author Akara Sucharitakul
 */
public class LogServer extends Thread {

    public LogConfig config;

    private Logger logger;
    private ServerSocket serverSocket;
    private Selector selector;
    private Listener[] listeners;

    static boolean isShutdown = false;
    /**
     * Constructs the server. This will setup all server facilities. If only
     * one listener thread is configured, the main thread will act as the
     * listener thread. Otherwise additional listener threads are started.
     * @param conf The singleton config object
     * @exception IOException The log server cannot bind socket
     */
    public LogServer(LogConfig conf) throws IOException {
        this.config = conf;
        this.logger = Logger.getLogger(this.getClass().getName());
        int numListeners = config.listenerThreads - 1;

        selector = getSelector(config.port);
        logger.finer("Selector created");

        if (numListeners > 0) {
            listeners = new Listener[numListeners];

            for (int i = 0; i < listeners.length; i++) {
                listeners[i] = new Listener(selector, config);
                Thread t = new Thread(listeners[i]);
                listeners[i].listenerThread = t;
                t.setName("Listener-" + i);
                t.setDaemon(true);
            }
        }

        config.primaryListener = new PrimaryListener(selector, config);
        logger.finer("Listeners created.");

        config.threadPool = new ThreadPoolExecutor(config.coreServiceThreads,
                config.maxServiceThreads, config.serviceThreadTimeout,
                TimeUnit.SECONDS, new LinkedBlockingQueue());
        logger.finer("Service thread pool created.");
        logger.info("Log Server Started Successfully");
    }

    /**
     * Configures a selector to listen to the configured port.
     * @param port The port to listen to
     * @return The selector
     * @exception IOException The selector cannot bind the socket
     */
    private Selector getSelector(int port) throws IOException {
        logger.info("Opening port " + port + " for logger");

        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        Selector selector = Selector.open();
        serverSocket = serverChannel.socket();
        if (config.listenQSize == -1)
            serverSocket.bind(new InetSocketAddress(port));
        else
            serverSocket.bind(new InetSocketAddress(port),
                    config.listenQSize);
        serverChannel.configureBlocking(false);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        return selector;
    }

    /**
     * Causes the server to listen to incoming requests.
     */
    public void listen() {
        // 1. Start the extra listener threads, if any.
        if (listeners != null) {
            for (int i = 0; i < listeners.length; i++)
                listeners[i].listenerThread.start();
            logger.finer("Listener Threads Started.");
        }

        // 2. Run the primary listener in this thread.
        logger.finer("Primary listener (main thread) starting...");
        config.primaryListener.run();
    }

    /**
     * The run method starts the server.
     */
    public void run() {
            while(!isShutdown)
                listen();
    }

    public void shutdown() {
        isShutdown = true;

        config.primaryListener.shutdown();
        logger.fine("Primary Listener Shutdown.");
        if (listeners != null) {
            for (int i = 0; i < listeners.length; i++)
                listeners[i].shutdown();
            logger.fine("Listener Threads Shutdown.");
        }
        try {
            selector.close();
            serverSocket.close();
            logger.fine("Socket Shutdown.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception shutting down socket.", e);
        }

        config.threadPool.shutdown();

        logger.info("LogServer Shutdown Complete");
    }

}
