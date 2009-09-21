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
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Startup class for LogServer.
 *
 * @author Akara Sucharitakul
 */
public class LogServer extends Thread {

    /** The log configuration. */
    public LogConfig config;

    private Logger logger;
    private Selector selector;
    private Listener[] listeners;
    private Acceptor acceptor;

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

        ConcurrentLinkedQueue<SocketChannel> acceptQueue =
                new ConcurrentLinkedQueue<SocketChannel>();

        selector = Selector.open();
        logger.finer("Selector created");

        if (numListeners > 0) {
            listeners = new Listener[numListeners];

            for (int i = 0; i < listeners.length; i++) {
                listeners[i] = new Listener(selector, config, acceptQueue);
                Thread t = new Thread(listeners[i]);
                listeners[i].listenerThread = t;
                t.setName("Listener-" + i);
                t.setDaemon(true);
                t.start();
            }
        }

        config.primaryListener = new PrimaryListener(selector, config,
                                                     acceptQueue);
        logger.finer("Listeners created.");

        if (config.threadPool == null) {
            config.threadPool = new ThreadPoolExecutor(
                    config.coreServiceThreads, config.maxServiceThreads,
                    config.serviceThreadTimeout, TimeUnit.SECONDS,
                    new LinkedBlockingQueue());
        logger.finer("Service thread pool created.");
        }

        acceptor = new Acceptor(conf, acceptQueue, selector);
        Thread t = new Thread(acceptor);
        t.setName("Acceptor");
        t.setDaemon(true);
        t.start();
        logger.finer("Acceptor created on port " + config.port + ".");

        logger.info("Log Server Started Successfully");
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

    /**
     * This method stops the server.
     */
    public void shutdown() {
        isShutdown = true;
        try {
            acceptor.shutdown();
            config.primaryListener.shutdown();
            logger.fine("Primary Listener Shutdown.");
            if (listeners != null) {
                for (int i = 0; i < listeners.length; i++)
                    listeners[i].shutdown();
                logger.fine("Listener Threads Shutdown.");
            }            
            selector.close();
            logger.fine("Socket Shutdown.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Exception shutting down socket.", e);
        }

        config.threadPool.shutdown();

        logger.info("LogServer Shutdown Complete");
    }

}
